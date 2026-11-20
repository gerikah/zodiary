package com.zodiary.app.data

import android.content.Intent
import com.zodiary.app.JournalEntry
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

data class ZodiaryProfile(
    val sign: String,
    val topics: Set<String>,
    val focus: String,
    val reminder: String,
    val updatedAt: Instant,
)

data class AccountSyncResult(
    val entries: Map<LocalDate, JournalEntry>,
    val profile: ZodiaryProfile,
    val addedFromCloud: Int,
    val sentToCloud: Int,
)

data class AccountUser(val id: String, val email: String?)

/** Supabase Auth plus owner-scoped REST calls; local storage remains the guest/offline source of truth. */
class SupabaseAccountService(
    projectUrl: String,
    private val publishableKey: String,
) {
    private val projectUrl = validateProjectUrl(projectUrl)
    private val client = createSupabaseClient(this.projectUrl, publishableKey) {
        install(Auth) {
            scheme = DEEP_LINK_SCHEME
            host = DEEP_LINK_HOST
            flowType = FlowType.PKCE
            alwaysAutoRefresh = true
            autoLoadFromStorage = true
            autoSaveToStorage = true
        }
    }

    suspend fun restoreSession() {
        client.auth.awaitInitialization()
    }

    fun currentUser(): AccountUser? = client.auth.currentUserOrNull()?.let {
        AccountUser(id = it.id, email = it.email)
    }

    suspend fun createAccount(email: String, password: String) {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    suspend fun handleDeepLink(intent: Intent) {
        client.handleDeeplinks(intent)
        client.auth.awaitInitialization()
    }

    suspend fun sync(
        localEntries: List<JournalEntry>,
        localProfile: ZodiaryProfile,
    ): AccountSyncResult = withContext(Dispatchers.IO) {
        val user = currentUser() ?: throw IOException("Sign in to sync your journal")
        val accessToken = client.auth.currentSessionOrNull()?.accessToken
            ?: throw IOException("Your session expired. Sign in again to sync")

        val remoteEntries = loadEntries(user.id, accessToken)
        val remoteByDate = remoteEntries.associateBy(JournalEntry::date)
        val mergedEntries = remoteByDate.toMutableMap()
        var sentToCloud = 0
        var addedFromCloud = remoteByDate.keys.count { remoteDate -> localEntries.none { it.date == remoteDate } }

        localEntries.forEach { local ->
            val remote = remoteByDate[local.date]
            if (remote == null) {
                mergedEntries[local.date] = local
                sentToCloud += 1
            } else if (isNewer(local, remote)) {
                mergedEntries[local.date] = local
                sentToCloud += 1
            } else if (isNewer(remote, local)) {
                mergedEntries[local.date] = remote
                addedFromCloud += 1
            } else {
                // Matching timestamps prefer the account copy to make repeat syncs stable.
                mergedEntries[local.date] = remote
            }
        }

        val uploads = mergedEntries.values.filter { chosen ->
            val remote = remoteByDate[chosen.date]
            remote == null || chosen.updatedAt != remote.updatedAt || !sameEntryContent(chosen, remote)
        }
        uploads.chunked(ENTRY_UPLOAD_BATCH_SIZE).forEach { batch ->
            upsertEntries(user.id, accessToken, batch)
        }

        val profile = syncProfile(user.id, accessToken, localProfile)
        AccountSyncResult(
            entries = mergedEntries.toSortedMap(),
            profile = profile,
            addedFromCloud = addedFromCloud,
            sentToCloud = sentToCloud,
        )
    }

    private fun loadEntries(userId: String, accessToken: String): List<JournalEntry> {
        val output = mutableListOf<JournalEntry>()
        var offset = 0
        do {
            val response = request(
                method = "GET",
                path = "/rest/v1/journal_entries" +
                    "?select=diary_date,mood,entry_text,prompt,updated_at" +
                    "&user_id=eq.${encode(userId)}&order=diary_date.asc&limit=$ENTRY_PAGE_SIZE&offset=$offset",
                accessToken = accessToken,
            )
            val page = JSONArray(response)
            for (index in 0 until page.length()) {
                val row = page.optJSONObject(index) ?: throw IOException("Cloud journal contained an invalid entry")
                val date = row.readString("diary_date")?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
                    ?: throw IOException("Cloud journal contained an invalid date")
                output += JournalEntry(
                    date = date,
                    mood = row.readString("mood").orEmpty(),
                    text = row.readString("entry_text")
                        ?: throw IOException("Cloud journal contained an empty entry"),
                    prompt = row.readString("prompt").orEmpty(),
                    updatedAt = row.readString("updated_at")?.toInstantOrNull() ?: Instant.EPOCH,
                )
            }
            offset += page.length()
            if (output.size > MAX_SYNC_ENTRIES) throw IOException("Journal is too large to sync in one pass")
        } while (page.length() == ENTRY_PAGE_SIZE)
        return output
    }

    private fun upsertEntries(userId: String, accessToken: String, entries: List<JournalEntry>) {
        val payload = JSONArray()
        entries.forEach { entry ->
            payload.put(
                JSONObject()
                    .put("user_id", userId)
                    .put("diary_date", entry.date.toString())
                    .put("mood", entry.mood)
                    .put("entry_text", entry.text)
                    .put("prompt", entry.prompt)
                    .put("updated_at", entry.updatedAt.takeIf { it != Instant.EPOCH }?.toString() ?: Instant.now().toString()),
            )
        }
        request(
            method = "POST",
            path = "/rest/v1/journal_entries?on_conflict=user_id%2Cdiary_date",
            accessToken = accessToken,
            payload = payload.toString(),
        )
    }

    private fun syncProfile(userId: String, accessToken: String, local: ZodiaryProfile): ZodiaryProfile {
        val response = request(
            method = "GET",
            path = "/rest/v1/user_preferences" +
                "?select=sign_slug,topics,journal_focus,reminder,updated_at" +
                "&user_id=eq.${encode(userId)}&limit=1",
            accessToken = accessToken,
        )
        val rows = JSONArray(response)
        val remote = rows.optJSONObject(0)?.let { row ->
            val sign = row.readString("sign_slug") ?: return@let null
            val topics = row.optJSONArray("topics")?.let { values ->
                buildSet {
                    for (index in 0 until values.length()) {
                        values.optString(index).takeIf(String::isNotBlank)?.let(::add)
                    }
                }
            }.orEmpty()
            ZodiaryProfile(
                sign = sign,
                topics = topics,
                focus = row.readString("journal_focus") ?: DEFAULT_FOCUS,
                reminder = row.readString("reminder") ?: DEFAULT_REMINDER,
                updatedAt = row.readString("updated_at")?.toInstantOrNull() ?: Instant.EPOCH,
            )
        }

        val chosen = if (remote == null || local.updatedAt.isAfter(remote.updatedAt)) {
            val localToSave = local.copy(updatedAt = local.updatedAt.takeIf { it != Instant.EPOCH } ?: Instant.now())
            upsertProfile(userId, accessToken, localToSave)
            localToSave
        } else {
            remote
        }
        return chosen
    }

    private fun upsertProfile(userId: String, accessToken: String, profile: ZodiaryProfile) {
        val payload = JSONObject()
            .put("user_id", userId)
            .put("sign_slug", profile.sign.lowercase())
            .put("topics", JSONArray(profile.topics.sorted()))
            .put("journal_focus", profile.focus)
            .put("reminder", profile.reminder)
            .put("updated_at", profile.updatedAt.toString())
        request(
            method = "POST",
            path = "/rest/v1/user_preferences?on_conflict=user_id",
            accessToken = accessToken,
            payload = payload.toString(),
        )
    }

    private fun request(method: String, path: String, accessToken: String, payload: String? = null): String {
        val connection = (URL(projectUrl.trimEnd('/') + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("apikey", publishableKey)
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            if (payload != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
                connection.outputStream.use { it.write(payload.toByteArray(StandardCharsets.UTF_8)) }
            }

            val status = connection.responseCode
            if (status !in 200..299) {
                throw HttpStatusFailure(status)
            }
            if (method == "POST" && (status == HttpURLConnection.HTTP_NO_CONTENT || status == HttpURLConnection.HTTP_CREATED)) {
                return ""
            }
            return connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                val response = StringBuilder()
                val buffer = CharArray(8_192)
                var total = 0
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RESPONSE_CHARS) throw IOException("Supabase account response is too large")
                    response.append(buffer, 0, count)
                }
                response.toString()
            }
        } catch (failure: org.json.JSONException) {
            throw IOException("Supabase returned an invalid account response", failure)
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private class HttpStatusFailure(statusCode: Int) : IOException("Supabase account request failed (HTTP $statusCode)")

    companion object {
        const val DEEP_LINK_SCHEME = "zodiary"
        const val DEEP_LINK_HOST = "auth"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 20_000
        private const val ENTRY_PAGE_SIZE = 50
        private const val ENTRY_UPLOAD_BATCH_SIZE = 25
        private const val MAX_SYNC_ENTRIES = 10_000
        private const val MAX_RESPONSE_CHARS = 3_000_000
        private const val DEFAULT_FOCUS = "Reflect"
        private const val DEFAULT_REMINDER = "Not now"
    }
}

private fun JSONObject.readString(key: String): String? {
    val value = opt(key) ?: return null
    if (value === JSONObject.NULL || value !is String) return null
    return value.takeIf(String::isNotBlank)
}

private fun String.toInstantOrNull(): Instant? = runCatching { Instant.parse(this) }
    .recoverCatching { OffsetDateTime.parse(this).toInstant() }
    .getOrNull()

private fun isNewer(first: JournalEntry, second: JournalEntry): Boolean = first.updatedAt.isAfter(second.updatedAt)

private fun sameEntryContent(first: JournalEntry, second: JournalEntry): Boolean =
    first.mood == second.mood && first.text == second.text && first.prompt == second.prompt

private fun validateProjectUrl(value: String): String {
    val normalized = value.trim().trimEnd('/')
    val uri = runCatching { URI(normalized) }.getOrNull()
    require(uri?.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null) {
        "Supabase URL must be a valid HTTPS project URL"
    }
    return normalized
}
