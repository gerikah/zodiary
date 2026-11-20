package com.zodiary.app

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zodiary.app.data.AccountSyncResult
import com.zodiary.app.data.CosmyDayResponseAdapter
import com.zodiary.app.data.CosmyDayApiService
import com.zodiary.app.data.DailyReading
import com.zodiary.app.data.DailyContentSource
import com.zodiary.app.data.DailyReadingRepository
import com.zodiary.app.data.SharedPreferencesDailyReadingCache
import com.zodiary.app.data.SupabaseAccountService
import com.zodiary.app.data.SupabaseDailyContentSource
import com.zodiary.app.data.ZodiaryProfile
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.time.LocalDate
import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.Executors

enum class Destination { Welcome, Birthday, SignReveal, Preferences, Trust, Home, Journal, Calendar, Profile, Reading }

data class ZodiacSign(val name: String, val symbol: String, val startMonth: Int, val startDay: Int, val accent: Long)

data class JournalPrompt(val topic: String?, val text: String) {
    fun sourceLabel(): String = topic?.let { "A prompt on $it" } ?: "A thought to begin with"
}

data class JournalEntry(
    val date: LocalDate,
    val mood: String,
    val text: String,
    val prompt: String,
    val updatedAt: Instant = Instant.EPOCH,
)

class ZodiaryViewModel(application: Application) : AndroidViewModel(application), ZodiaryScreenModel {
    private val storage = application.getSharedPreferences("zodiary.local", Application.MODE_PRIVATE)
    private val configuredSupabaseUrl = BuildConfig.SUPABASE_URL.trim()
    private val configuredSupabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
    private val hasSupabaseSettings = configuredSupabaseUrl.isNotBlank() || configuredSupabaseKey.isNotBlank()
    private val supabaseAccountService = if (configuredSupabaseUrl.isNotBlank() && configuredSupabaseKey.isNotBlank()) {
        runCatching { SupabaseAccountService(configuredSupabaseUrl, configuredSupabaseKey) }.getOrNull()
    } else null
    private val dailyContentSource: DailyContentSource = when {
        !hasSupabaseSettings -> CosmyDayApiService()
        else -> runCatching { SupabaseDailyContentSource(configuredSupabaseUrl, configuredSupabaseKey) }
            .getOrElse { ConfiguredDailyContentFailure() }
    }
    private val dailyReadingRepository = DailyReadingRepository(
        SharedPreferencesDailyReadingCache(application),
        dailyContentSource,
    )
    private val dailyReadingExecutor = Executors.newSingleThreadExecutor()
    private val accountExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var profileUpdatedAt = storage.getString(KEY_PROFILE_UPDATED_AT, null).toInstantOrNull() ?: Instant.EPOCH
    private var accountSyncPending = false

    override val isSupabaseConfigured: Boolean get() = supabaseAccountService != null
    override val supabaseConfigurationError: String?
        get() = if (hasSupabaseSettings && !isSupabaseConfigured) "Supabase settings could not be initialized." else null

    override var isAccountInitializing by mutableStateOf(isSupabaseConfigured)
        private set
    override var isAccountBusy by mutableStateOf(false)
        private set
    override var accountEmail by mutableStateOf<String?>(null)
        private set
    override var accountMessage by mutableStateOf(when {
        !hasSupabaseSettings -> "Your journal is saved on this device. Connect an account whenever you're ready to sync it."
        supabaseConfigurationError != null -> supabaseConfigurationError.orEmpty()
        else -> ""
    })
        private set

    override var destination by mutableStateOf(
        if (storage.getBoolean(KEY_ONBOARDED, false)) Destination.Home else Destination.Welcome,
    )
        private set

    override var sign by mutableStateOf(findSign(storage.getString(KEY_SIGN, "Libra") ?: "Libra"))
        private set
    override var birthDate by mutableStateOf<LocalDate?>(null)
        private set
    override var topics by mutableStateOf(storage.getStringSet(KEY_TOPICS, setOf("Personal growth"))?.toSet() ?: setOf("Personal growth"))
        private set
    override var focus by mutableStateOf(storage.getString(KEY_FOCUS, "Reflect") ?: "Reflect")
        private set
    override var reminder by mutableStateOf(storage.getString(KEY_REMINDER, "Not now") ?: "Not now")
        private set
    var promptIndex by mutableStateOf(0)
        private set
    override var journalDate by mutableStateOf(LocalDate.now())
        private set
    override var journalText by mutableStateOf(loadDraft(LocalDate.now()))
        private set
    override var mood by mutableStateOf(loadMood(LocalDate.now()))
        private set
    override var saveMessage by mutableStateOf("")
        private set
    override var preferencesReturnTo by mutableStateOf(Destination.Trust)
        private set
    override var selectedDate by mutableStateOf(LocalDate.now())
        private set
    override val entries = mutableStateMapOf<LocalDate, JournalEntry>().apply { putAll(loadEntries()) }

    override val topicOptions = listOf("Love", "Career", "Well-being", "Personal growth")
    override val focusOptions = listOf("Reflect", "Track moods", "Keep memories", "A bit of everything")
    override val reminderOptions = listOf("Morning", "Evening", "Not now")
    override val moodOptions = listOf("Calm", "Hopeful", "Tender", "Restless", "Heavy")

    var dailyReadings by mutableStateOf<Map<String, DailyReading>>(emptyMap())
        private set
    override var dailyFetchedAt by mutableStateOf<Instant?>(null)
        private set
    override var isDailyLoading by mutableStateOf(true)
        private set
    override var isDailyRefreshing by mutableStateOf(false)
        private set
    override var dailyRefreshError by mutableStateOf<String?>(null)
        private set
    private var lastManualRefreshAt = 0L

    override val currentDailyReading: DailyReading?
        get() = CosmyDayResponseAdapter.normalizeSign(sign.name)?.let(dailyReadings::get)

    override val currentPrompt: JournalPrompt
        get() {
            val chosenTopics = topicOptions.filter { it in topics }
            val pool = if (chosenTopics.isEmpty()) {
                prompts.filter { it.topic == null }
            } else {
                prompts.filter { it.topic in chosenTopics }
            }
            return pool[promptIndex.mod(pool.size)]
        }

    init {
        dailyReadingRepository.readCached()?.let(::applyDailySnapshot)
        isDailyLoading = dailyReadings.isEmpty()
        refreshDaily()
        initializeAccount()
    }

    override fun refreshDaily(force: Boolean) {
        if (isDailyRefreshing) return
        if (force) {
            val now = System.currentTimeMillis()
            if (lastManualRefreshAt != 0L && now - lastManualRefreshAt < MANUAL_RETRY_COOLDOWN_MS) return
            lastManualRefreshAt = now
        }

        isDailyRefreshing = true
        if (dailyReadings.isEmpty()) isDailyLoading = true
        dailyReadingExecutor.execute {
            val result = dailyReadingRepository.refreshIfNeeded(force)
            mainHandler.post {
                result.snapshot?.let(::applyDailySnapshot)
                dailyRefreshError = result.error
                isDailyLoading = dailyReadings.isEmpty()
                isDailyRefreshing = false
            }
        }
    }

    private fun applyDailySnapshot(snapshot: com.zodiary.app.data.CachedDailyBatch) {
        dailyReadings = snapshot.batch.readings
        dailyFetchedAt = snapshot.fetchedAt
    }

    override fun onCleared() {
        dailyReadingExecutor.shutdownNow()
        accountExecutor.shutdownNow()
        super.onCleared()
    }

    override fun navigate(to: Destination) {
        destination = to
        if (to == Destination.Journal) openJournal(LocalDate.now())
    }

    override fun discoverSignFromBirthday(date: LocalDate) {
        birthDate = date
        val monthDay = date.monthValue * 100 + date.dayOfMonth
        val name = when {
            monthDay >= 1222 || monthDay <= 119 -> "Capricorn"
            monthDay <= 218 -> "Aquarius"
            monthDay <= 320 -> "Pisces"
            monthDay <= 419 -> "Aries"
            monthDay <= 520 -> "Taurus"
            monthDay <= 620 -> "Gemini"
            monthDay <= 722 -> "Cancer"
            monthDay <= 822 -> "Leo"
            monthDay <= 922 -> "Virgo"
            monthDay <= 1022 -> "Libra"
            monthDay <= 1121 -> "Scorpio"
            else -> "Sagittarius"
        }
        sign = findSign(name)
    }

    override fun selectSign(value: ZodiacSign) {
        sign = value
        touchProfile()
        storage.edit().putString(KEY_SIGN, value.name).apply()
        requestAccountSyncAfterLocalChange()
    }

    override fun changeTopic(value: String) {
        topics = if (value in topics) topics - value else topics + value
        promptIndex = 0
    }

    override fun changeFocus(value: String) { focus = value }
    override fun changeReminder(value: String) { reminder = value }

    override fun savePreferences() {
        touchProfile()
        storage.edit()
            .putStringSet(KEY_TOPICS, topics.toSet())
            .putString(KEY_FOCUS, focus)
            .putString(KEY_REMINDER, reminder)
            .apply()
        if (preferencesReturnTo == Destination.Trust) destination = Destination.Trust
        else destination = preferencesReturnTo
        requestAccountSyncAfterLocalChange()
    }

    override fun editPreferences() {
        preferencesReturnTo = Destination.Profile
        destination = Destination.Preferences
    }

    override fun finishOnboarding() {
        touchProfile()
        storage.edit().putBoolean(KEY_ONBOARDED, true).putString(KEY_SIGN, sign.name).apply()
        destination = Destination.Home
        requestAccountSyncAfterLocalChange()
    }

    override fun nextPrompt() { promptIndex += 1 }

    override fun openJournal(date: LocalDate) {
        journalDate = date
        val saved = entries[date]
        journalText = saved?.text ?: loadDraft(date)
        mood = saved?.mood ?: loadMood(date)
        saveMessage = if (saved != null) "Saved on this device" else ""
        destination = Destination.Journal
    }

    override fun changeMood(value: String) {
        mood = value
        storage.edit().putString("draft.mood.${journalDate}", value).apply()
    }

    override fun editJournalText(value: String) {
        journalText = value
        saveMessage = if (entries.containsKey(journalDate)) "Unsaved changes" else "Draft saved on this device"
        storage.edit().putString("draft.text.${journalDate}", value).apply()
    }

    override fun saveEntry() {
        if (journalText.isBlank()) return
        val saved = JournalEntry(journalDate, mood, journalText.trim(), currentPrompt.text, Instant.now())
        entries[journalDate] = saved
        storage.edit()
            .putString(entryKey(journalDate, "text"), saved.text)
            .putString(entryKey(journalDate, "mood"), saved.mood)
            .putString(entryKey(journalDate, "prompt"), saved.prompt)
            .putString(entryKey(journalDate, "updated_at"), saved.updatedAt.toString())
            .remove("draft.text.${journalDate}")
            .apply()
        journalText = saved.text
        requestAccountSyncAfterLocalChange()
        saveMessage = "A moment, kept · saved on this device"
    }

    override fun selectCalendarDate(date: LocalDate) { selectedDate = date }

    private fun loadDraft(date: LocalDate): String = storage.getString("draft.text.$date", "") ?: ""
    private fun loadMood(date: LocalDate): String = storage.getString("draft.mood.$date", "") ?: ""

    private fun loadEntries(): Map<LocalDate, JournalEntry> {
        val dates = storage.all.keys
            .filter { it.startsWith("entry.") && it.endsWith(".text") }
            .mapNotNull { key -> runCatching { LocalDate.parse(key.removePrefix("entry.").removeSuffix(".text")) }.getOrNull() }
        return dates.associateWith { date ->
            JournalEntry(
                date = date,
                mood = storage.getString(entryKey(date, "mood"), "") ?: "",
                text = storage.getString(entryKey(date, "text"), "") ?: "",
                prompt = storage.getString(entryKey(date, "prompt"), "") ?: "",
                updatedAt = storage.getString(entryKey(date, "updated_at"), null).toInstantOrNull() ?: Instant.EPOCH,
            )
        }
    }

    override fun createAccount(email: String, password: String) = authenticate(email, password, createAccount = true)

    override fun signIn(email: String, password: String) = authenticate(email, password, createAccount = false)

    override fun syncAccount() {
        val service = supabaseAccountService ?: return
        if (accountEmail == null) return
        if (isAccountBusy || isAccountInitializing) {
            accountSyncPending = true
            return
        }
        isAccountBusy = true
        accountMessage = "Syncing your journal…"
        val localEntries = entries.values.toList()
        val localProfile = currentProfile()
        accountExecutor.execute {
            val result = runCatching {
                runBlocking {
                    service.restoreSession()
                    service.sync(localEntries, localProfile)
                }
            }
            mainHandler.post {
                isAccountBusy = false
                result.onSuccess { sync ->
                    applySyncResult(sync)
                    accountMessage = syncMessage(sync)
                }.onFailure { failure ->
                    accountMessage = failure.message ?: "Sync didn't finish. Your entries are still saved on this device."
                }
                drainAccountSyncRequest()
            }
        }
    }

    override fun signOutAccount() {
        val service = supabaseAccountService ?: return
        if (isAccountBusy || isAccountInitializing) return
        isAccountBusy = true
        accountExecutor.execute {
            val result = runCatching { runBlocking { service.signOut() } }
            mainHandler.post {
                isAccountBusy = false
                result.onSuccess {
                    accountEmail = null
                    accountMessage = "Signed out. Your journal remains saved on this device."
                }.onFailure { failure ->
                    accountMessage = failure.message ?: "Couldn't sign out right now."
                }
                drainAccountSyncRequest()
            }
        }
    }

    fun handleAuthDeepLink(intent: android.content.Intent?) {
        val service = supabaseAccountService ?: return
        if (intent == null || intent.data?.scheme != SupabaseAccountService.DEEP_LINK_SCHEME) return
        val localEntries = entries.values.toList()
        val localProfile = currentProfile()
        accountExecutor.execute {
            val userResult = runCatching {
                runBlocking {
                    service.handleDeepLink(intent)
                    service.currentUser()
                }
            }
            val user = userResult.getOrNull()
            val syncResult = user?.let { runCatching { runBlocking { service.sync(localEntries, localProfile) } } }
            mainHandler.post {
                isAccountInitializing = false
                if (userResult.isFailure) {
                    accountMessage = userResult.exceptionOrNull()?.message ?: "Couldn't finish account confirmation."
                } else if (user == null) {
                    accountMessage = "The link was received. Sign in to sync your journal."
                } else {
                    accountEmail = user.email
                    syncResult?.onSuccess { sync ->
                        applySyncResult(sync)
                        accountMessage = syncMessage(sync)
                    }?.onFailure { failure ->
                        accountMessage = "Signed in, but sync didn't finish. ${failure.message ?: "Your local journal is safe."}"
                    }
                }
                drainAccountSyncRequest()
            }
        }
    }

    private fun initializeAccount() {
        val service = supabaseAccountService ?: return
        val localEntries = entries.values.toList()
        val localProfile = currentProfile()
        accountExecutor.execute {
            val userResult = runCatching {
                runBlocking {
                    service.restoreSession()
                    service.currentUser()
                }
            }
            val user = userResult.getOrNull()
            val syncResult = user?.let { runCatching { runBlocking { service.sync(localEntries, localProfile) } } }
            mainHandler.post {
                isAccountInitializing = false
                if (userResult.isFailure) {
                    accountMessage = userResult.exceptionOrNull()?.message ?: "Your account couldn't be reached. Local entries are safe."
                    drainAccountSyncRequest()
                    return@post
                }
                accountEmail = user?.email
                syncResult?.onSuccess { sync ->
                    applySyncResult(sync)
                    accountMessage = syncMessage(sync)
                }?.onFailure { failure ->
                    accountMessage = "Signed in, but sync didn't finish. ${failure.message ?: "Your local journal is safe."}"
                }
                drainAccountSyncRequest()
            }
        }
    }

    private fun authenticate(email: String, password: String, createAccount: Boolean) {
        val service = supabaseAccountService ?: return
        if (isAccountBusy || isAccountInitializing) return
        isAccountBusy = true
        accountMessage = if (createAccount) "Creating your account…" else "Signing in…"
        val localEntries = entries.values.toList()
        val localProfile = currentProfile()
        accountExecutor.execute {
            val userResult = runCatching {
                runBlocking {
                    service.restoreSession()
                    if (createAccount) service.createAccount(email.trim(), password)
                    else service.signIn(email.trim(), password)
                    service.currentUser()
                }
            }
            val user = userResult.getOrNull()
            val syncResult = user?.let { runCatching { runBlocking { service.sync(localEntries, localProfile) } } }
            mainHandler.post {
                isAccountBusy = false
                if (userResult.isFailure) {
                    accountMessage = userResult.exceptionOrNull()?.message
                        ?: "Account request failed. Your journal is still saved on this device."
                    drainAccountSyncRequest()
                    return@post
                }
                accountEmail = user?.email
                if (user == null) {
                    accountMessage = if (createAccount) {
                        "Check your email to confirm your account. Your journal stays on this device until you sign in."
                    } else {
                        "Sign in completed, but the account session wasn't available. Try again."
                    }
                } else {
                    syncResult?.onSuccess { sync ->
                        applySyncResult(sync)
                        accountMessage = syncMessage(sync)
                    }?.onFailure { failure ->
                        accountMessage = "Signed in, but sync didn't finish. ${failure.message ?: "Your local journal is safe."}"
                    }
                }
                drainAccountSyncRequest()
            }
        }
    }

    private fun currentProfile(): ZodiaryProfile = ZodiaryProfile(
        sign = sign.name.lowercase(),
        topics = topics.toSet(),
        focus = focus,
        reminder = reminder,
        updatedAt = profileUpdatedAt,
    )

    private fun applySyncResult(result: AccountSyncResult) {
        entries.clear()
        entries.putAll(result.entries)
        result.entries.values.forEach { entry ->
            storage.edit()
                .putString(entryKey(entry.date, "text"), entry.text)
                .putString(entryKey(entry.date, "mood"), entry.mood)
                .putString(entryKey(entry.date, "prompt"), entry.prompt)
                .putString(entryKey(entry.date, "updated_at"), entry.updatedAt.toString())
                .apply()
        }
        sign = findSign(result.profile.sign)
        topics = result.profile.topics
        focus = result.profile.focus
        reminder = result.profile.reminder
        profileUpdatedAt = result.profile.updatedAt
        storage.edit()
            .putString(KEY_SIGN, sign.name)
            .putStringSet(KEY_TOPICS, topics.toSet())
            .putString(KEY_FOCUS, focus)
            .putString(KEY_REMINDER, reminder)
            .putString(KEY_PROFILE_UPDATED_AT, profileUpdatedAt.toString())
            .apply()
    }

    private fun syncMessage(result: AccountSyncResult): String = when {
        result.sentToCloud > 0 && result.addedFromCloud > 0 ->
            "Synced ${result.sentToCloud} local and ${result.addedFromCloud} account entries."
        result.sentToCloud > 0 -> "Synced ${result.sentToCloud} entries from this device."
        result.addedFromCloud > 0 -> "Added ${result.addedFromCloud} entries from your account."
        else -> "Your journal and preferences are up to date."
    }

    private fun touchProfile() {
        profileUpdatedAt = Instant.now()
        storage.edit().putString(KEY_PROFILE_UPDATED_AT, profileUpdatedAt.toString()).apply()
    }

    private fun requestAccountSyncAfterLocalChange() {
        if (!isSupabaseConfigured) return
        if (isAccountInitializing || isAccountBusy) {
            accountSyncPending = true
            return
        }
        if (accountEmail != null) syncAccount()
    }

    private fun drainAccountSyncRequest() {
        if (!accountSyncPending) return
        accountSyncPending = false
        if (accountEmail != null && !isAccountBusy && !isAccountInitializing) syncAccount()
    }

    private fun entryKey(date: LocalDate, field: String) = "entry.$date.$field"

    companion object {
        const val KEY_ONBOARDED = "onboarded"
        private const val MANUAL_RETRY_COOLDOWN_MS = 30_000L
        private const val KEY_SIGN = "sign"
        private const val KEY_TOPICS = "topics"
        private const val KEY_FOCUS = "focus"
        private const val KEY_REMINDER = "reminder"
        private const val KEY_PROFILE_UPDATED_AT = "profile.updated_at"

        val zodiacSigns = listOf(
            ZodiacSign("Aries", "♈", 3, 21, 0xFFE89A73),
            ZodiacSign("Taurus", "♉", 4, 20, 0xFF79A583),
            ZodiacSign("Gemini", "♊", 5, 21, 0xFFE3B866),
            ZodiacSign("Cancer", "♋", 6, 21, 0xFF8A9CC8),
            ZodiacSign("Leo", "♌", 7, 23, 0xFFE29B58),
            ZodiacSign("Virgo", "♍", 8, 23, 0xFF83A68D),
            ZodiacSign("Libra", "♎", 9, 23, 0xFFB78DB6),
            ZodiacSign("Scorpio", "♏", 10, 23, 0xFF9C657A),
            ZodiacSign("Sagittarius", "♐", 11, 22, 0xFF7F86B8),
            ZodiacSign("Capricorn", "♑", 12, 22, 0xFF7C877B),
            ZodiacSign("Aquarius", "♒", 1, 20, 0xFF71A6A2),
            ZodiacSign("Pisces", "♓", 2, 19, 0xFF8A9AC6),
        )

        private val prompts = listOf(
            JournalPrompt("Love", "What kind of care felt meaningful to you today?"),
            JournalPrompt("Love", "Where could you offer yourself the patience you give others?"),
            JournalPrompt("Career", "What part of your work felt most like you today?"),
            JournalPrompt("Career", "What is one small thing you learned about how you work best?"),
            JournalPrompt("Well-being", "What helped your body or mind feel a little more at ease?"),
            JournalPrompt("Well-being", "What would a gentler pace look like for the rest of today?"),
            JournalPrompt("Personal growth", "What are you learning to give yourself permission to feel?"),
            JournalPrompt("Personal growth", "Which moment today would you like to remember, and why?"),
            JournalPrompt(null, "What is taking up space in your thoughts right now?"),
            JournalPrompt(null, "What would you like to leave on this page before you move on?"),
        )

        fun findSign(name: String): ZodiacSign = zodiacSigns.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) } ?: zodiacSigns[6]

    }
}

private class ConfiguredDailyContentFailure : DailyContentSource {
    override fun fetchDailyAll(): String = throw IOException("Supabase is configured but its connection settings are invalid")
}

private fun String?.toInstantOrNull(): Instant? = this?.let { value ->
    runCatching { Instant.parse(value) }.recoverCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
}
