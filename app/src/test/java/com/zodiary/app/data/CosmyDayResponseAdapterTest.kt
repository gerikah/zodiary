package com.zodiary.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CosmyDayResponseAdapterTest {
    @Test
    fun allSignsResponseNormalizesTitleCaseKeysAndUsesSharedMoon() {
        val moon = JSONObject()
            .put("sign", "Pisces")
            .put("phase", "first_quarter")
            .put("phase_name", "First Quarter")
            .put("illumination", 59.9)
        val horoscopes = JSONObject()
        signNames.forEach { name ->
            horoscopes.put(name, JSONObject().put("sign", name).put("content", "Overview: A note for $name."))
        }
        val response = JSONObject()
            .put("date", "2026-11-18")
            .put("weekday", "Wednesday")
            .put("generated_at", "2026-11-18T11:03:29.718489+08:00")
            .put("sky", JSONObject().put("moon", moon))
            .put("horoscopes", horoscopes)

        val batch = CosmyDayResponseAdapter.parseAllSigns(response.toString())
        val libra = batch.readingFor("LIBRA")

        assertEquals(12, batch.readings.size)
        assertEquals(LocalDate.parse("2026-11-18"), batch.providerDate)
        assertEquals("Libra", libra?.sign)
        assertEquals("Overview: A note for Libra.", libra?.content)
        assertEquals("First Quarter", libra?.moon?.phaseName)
        assertEquals(59.9, libra?.moon?.illumination ?: 0.0, 0.001)
        assertEquals(Instant.parse("2026-11-18T03:03:29.718489Z"), batch.generatedAt)
        assertEquals("Scorpio", batch.readingFor("SCORPIO")?.sign)
    }

    @Test
    fun individualResponseReadsTopLevelMoonAndKeepsClosing() {
        val response = JSONObject()
            .put("date", "2026-11-18")
            .put("weekday", "Wednesday")
            .put("sign", "libra")
            .put("content", "Overview: Full original text.")
            .put("closing", "A closing reflection.")
            .put("moon", JSONObject().put("sign", "Pisces").put("phase_name", "First Quarter"))

        val reading = CosmyDayResponseAdapter.parseSingleSign(response.toString())

        assertEquals("libra", reading.signSlug)
        assertEquals("A closing reflection.", reading.closing)
        assertEquals("Pisces", reading.moon?.sign)
    }

    @Test
    fun incompleteBatchIsRejectedInsteadOfReturningPartialContent() {
        val response = JSONObject()
            .put("date", "2026-11-18")
            .put("horoscopes", JSONObject().put("Libra", JSONObject().put("content", "Overview: Present.")))

        val failure = runCatching { CosmyDayResponseAdapter.parseAllSigns(response.toString()) }.exceptionOrNull()

        assertNotNull(failure)
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure?.message.orEmpty().contains("incomplete"))
    }

    @Test
    fun unknownSignCannotSelectAReading() {
        assertNull(CosmyDayResponseAdapter.normalizeSign("Ophiuchus"))
        assertNull(CosmyDayResponseAdapter.normalizeSign(null))
    }

    @Test
    fun olderProviderDateIsDatedInsteadOfBeingCalledTodaysReading() {
        val reading = CosmyDayResponseAdapter.parseSingleSign(
            JSONObject().put("date", "2026-11-17").put("sign", "Libra").put("content", "Full reading").toString(),
        )

        assertEquals("Reading for Libra · Nov 17", DailyReadingLabels.title(reading, LocalDate.parse("2026-11-18")))
        assertEquals("Today's reading for Libra", DailyReadingLabels.title(reading, LocalDate.parse("2026-11-17")))
        assertNull(reading.moon)
    }

    private companion object {
        val signNames = listOf(
            "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
            "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces",
        )
    }
}
