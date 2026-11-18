package com.zodiary.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class DailyReadingRepositoryTest {
    @Test
    fun refreshFailureKeepsTheLastSuccessfulCachedBatch() {
        val previous = storedBatch("Previously fetched content")
        val cache = MemoryCache(previous)
        val result = DailyReadingRepository(cache, FakeSource { throw IOException("network unavailable") })
            .refreshIfNeeded(force = true)

        assertNotNull(result.snapshot)
        assertEquals("Previously fetched content · Libra", result.snapshot?.batch?.readings?.get("libra")?.content)
        assertNotNull(result.error)
        assertEquals(previous, cache.value)
    }

    @Test
    fun refreshFailureWithoutCacheReturnsAnUnavailableState() {
        val cache = MemoryCache(null)
        val result = DailyReadingRepository(cache, FakeSource { throw IOException("network unavailable") })
            .refreshIfNeeded(force = true)

        assertNull(result.snapshot)
        assertNotNull(result.error)
        assertNull(cache.value)
    }

    @Test
    fun invalidNewBatchDoesNotReplaceLastGoodCache() {
        val previous = storedBatch("Keep this reading")
        val cache = MemoryCache(previous)
        val incomplete = JSONObject()
            .put("date", "2026-11-19")
            .put("horoscopes", JSONObject().put("Libra", JSONObject().put("content", "Partial batch")))
        val result = DailyReadingRepository(cache, FakeSource { incomplete.toString() })
            .refreshIfNeeded(force = true)

        assertNotNull(result.error)
        assertEquals("Keep this reading · Libra", result.snapshot?.batch?.readings?.get("libra")?.content)
        assertEquals(previous, cache.value)
    }

    @Test
    fun olderProviderEditionDoesNotReplaceNewerCachedContent() {
        val previous = storedBatch("Keep the newer reading")
        val cache = MemoryCache(previous)
        val olderJson = allSignsJson("Yesterday's reading", date = "2026-11-17")
        val result = DailyReadingRepository(cache, FakeSource { olderJson })
            .refreshIfNeeded(force = true)

        assertEquals("Keep the newer reading · Libra", result.snapshot?.batch?.readings?.get("libra")?.content)
        assertNotNull(result.error)
        assertEquals(
            "Keep the newer reading · Libra",
            cache.value?.let { CosmyDayResponseAdapter.parseAllSigns(it.rawJson) }?.readings?.get("libra")?.content,
        )
    }

    @Test
    fun simultaneousRefreshesShareOneUpstreamRequest() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val calls = AtomicInteger()
        val source = FakeSource {
            calls.incrementAndGet()
            started.countDown()
            assertTrue(release.await(3, TimeUnit.SECONDS))
            allSignsJson("One daily response")
        }
        val repository = DailyReadingRepository(MemoryCache(null), source)
        val firstResult = AtomicReference<DailyRefreshResult?>()
        val first = Thread { firstResult.set(repository.refreshIfNeeded(force = true)) }
        first.start()

        assertTrue(started.await(3, TimeUnit.SECONDS))
        val secondResult = repository.refreshIfNeeded(force = true)
        release.countDown()
        first.join(3_000)

        assertEquals(1, calls.get())
        assertNull(secondResult.error)
        assertNotNull(firstResult.get()?.snapshot)
    }

    private fun storedBatch(content: String): StoredDailyBatch = StoredDailyBatch(
        rawJson = allSignsJson(content),
        fetchedAt = Instant.parse("2026-11-18T03:00:00Z"),
        nextRefreshAt = Instant.parse("2026-11-18T03:01:00Z"),
        sameEditionRecheckUsed = false,
    )

    private fun allSignsJson(content: String, date: String = "2026-11-18"): String {
        val names = listOf(
            "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
            "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces",
        )
        val horoscopes = JSONObject()
        names.forEach { name -> horoscopes.put(name, JSONObject().put("sign", name).put("content", "$content · $name")) }
        return JSONObject()
            .put("date", date)
            .put("generated_at", "2026-11-18T11:03:29+08:00")
            .put("horoscopes", horoscopes)
            .toString()
    }

    private class MemoryCache(var value: StoredDailyBatch?) : DailyReadingCache {
        override fun read(): StoredDailyBatch? = value
        override fun write(value: StoredDailyBatch) { this.value = value }
    }

    private class FakeSource(private val response: () -> String) : DailyContentSource {
        override fun fetchDailyAll(): String = response()
    }
}
