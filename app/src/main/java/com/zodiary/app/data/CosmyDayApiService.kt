package com.zodiary.app.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class CosmyDayApiService : DailyContentSource {
    override fun fetchDailyAll(): String {
        var lastFailure: IOException? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                return requestDailyAll()
            } catch (failure: IOException) {
                lastFailure = failure
                val retryable = failure !is HttpStatusFailure || failure.statusCode in 500..599
                if (!retryable || attempt == MAX_ATTEMPTS - 1) throw failure
                pauseBeforeRetry()
            }
        }
        throw lastFailure ?: IOException("CosmyDay request failed")
    }

    private fun requestDailyAll(): String {
        val connection = URL("$BASE_URL/content/daily").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", USER_AGENT)

        try {
            val status = connection.responseCode
            if (status !in 200..299) throw HttpStatusFailure(status)

            return connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                val result = StringBuilder()
                val buffer = CharArray(8_192)
                var total = 0
                while (true) {
                    val read = reader.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_RESPONSE_CHARS) throw IOException("CosmyDay response exceeded the supported size")
                    result.append(buffer, 0, read)
                }
                result.toString()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun pauseBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("CosmyDay retry was interrupted", interrupted)
        }
    }

    private class HttpStatusFailure(val statusCode: Int) : IOException("CosmyDay returned HTTP $statusCode")

    companion object {
        private const val BASE_URL = "https://api.cosmyday.com"
        private const val USER_AGENT = "Zodiary-Android/0.1.0"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 12_000
        private const val RETRY_DELAY_MS = 500L
        private const val MAX_ATTEMPTS = 2
        private const val MAX_RESPONSE_CHARS = 1_000_000
    }
}
