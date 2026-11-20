package com.zodiary.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets

/** Reads the shared daily batch from Supabase's public, read-only Data API. */
class SupabaseDailyContentSource(
    projectUrl: String,
    private val publishableKey: String,
) : DailyContentSource {
    private val baseUrl = projectUrl.trim().trimEnd('/').also { value ->
        val uri = runCatching { URI(value) }.getOrNull()
        require(uri?.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null) {
            "Supabase URL must be a valid HTTPS project URL"
        }
    }

    override fun fetchDailyAll(): String {
        if (publishableKey.isBlank()) throw IOException("Supabase publishable key is not configured")

        val url = URL(
            "$baseUrl/rest/v1/daily_readings" +
                "?select=sign_slug,sign_name,provider_date,weekday,generated_at,content,closing,moon" +
                "&order=sign_slug.asc",
        )
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("apikey", publishableKey)

        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw HttpStatusFailure(status)
            }
            val rows = JSONArray(readBoundedBody(connection))
            if (rows.length() == 0) throw IOException("Shared daily readings have not been imported yet")
            return adaptRows(rows)
        } catch (failure: org.json.JSONException) {
            throw IOException("Supabase returned an invalid daily readings response", failure)
        } finally {
            connection.disconnect()
        }
    }

    private fun adaptRows(rows: JSONArray): String {
        val horoscopes = JSONObject()
        var batchDate: String? = null
        var batchGeneratedAt: String? = null
        val seen = mutableSetOf<String>()

        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index)
                ?: throw IOException("Supabase returned an invalid daily reading row")
            val slug = row.optionalString("sign_slug")?.lowercase()
            if (slug == null || slug !in CosmyDayResponseAdapter.supportedSigns) {
                throw IOException("Supabase returned an unsupported zodiac sign")
            }
            if (!seen.add(slug)) throw IOException("Supabase returned a duplicate daily reading")

            val providerDate = row.optionalString("provider_date")
                ?: throw IOException("Supabase daily reading did not include a provider date")
            if (batchDate == null) batchDate = providerDate
            else if (batchDate != providerDate) throw IOException("Supabase daily readings contain mixed dates")

            val signName = row.optionalString("sign_name") ?: slug.replaceFirstChar(Char::uppercase)
            val content = row.optionalString("content")
                ?: throw IOException("Supabase daily reading did not include content")
            val reading = JSONObject()
                .put("sign", signName)
                .put("date", providerDate)
                .put("content", content)

            row.optionalString("weekday")?.let { reading.put("weekday", it) }
            row.optionalString("generated_at")?.let { value ->
                reading.put("generated_at", value)
                if (batchGeneratedAt == null) batchGeneratedAt = value
            }
            row.optionalString("closing")?.let { reading.put("closing", it) }
            row.optJSONObject("moon")?.let { reading.put("moon", it) }
            horoscopes.put(slug, reading)
        }

        val missing = CosmyDayResponseAdapter.supportedSigns.toSet() - seen
        if (missing.isNotEmpty()) throw IOException("Supabase daily readings are incomplete")

        val response = JSONObject()
            .put("date", batchDate)
            .put("horoscopes", horoscopes)
        batchGeneratedAt?.let { response.put("generated_at", it) }
        return response.toString()
    }

    private fun readBoundedBody(connection: HttpURLConnection): String =
        connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
            val result = StringBuilder()
            val buffer = CharArray(8_192)
            var total = 0
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_RESPONSE_CHARS) throw IOException("Supabase response exceeded the supported size")
                result.append(buffer, 0, read)
            }
            result.toString()
        }

    private fun JSONObject.optionalString(key: String): String? {
        val value = opt(key) ?: return null
        if (value === JSONObject.NULL || value !is String) return null
        return value.trim().takeIf(String::isNotEmpty)
    }

    private class HttpStatusFailure(val statusCode: Int) :
        IOException("Supabase daily readings request failed (HTTP $statusCode)")

    companion object {
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 12_000
        private const val MAX_RESPONSE_CHARS = 1_000_000
    }
}
