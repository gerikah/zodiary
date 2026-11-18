package com.zodiary.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DailyCachePolicyTest {
    @Test
    fun contentIsReusedUntilItsScheduledRefreshTime() {
        val now = Instant.parse("2026-11-18T04:00:00Z")
        val cached = cached(nextRefreshAt = now.plusSeconds(86_400))

        assertFalse(DailyCachePolicy.isRefreshDue(cached, now))
        assertTrue(DailyCachePolicy.isRefreshDue(cached, cached.nextRefreshAt))
    }

    @Test
    fun unchangedEditionGetsOneSixHourRecheckThenReturnsToDailyCadence() {
        val now = Instant.parse("2026-11-18T04:00:00Z")
        val first = DailyCachePolicy.nextSchedule(cached(), sameEdition = true, now)
        val afterRecheck = DailyCachePolicy.nextSchedule(
            cached(sameEditionRecheckUsed = first.sameEditionRecheckUsed),
            sameEdition = true,
            now = first.nextRefreshAt,
        )

        assertEquals(now.plusSeconds(21_600), first.nextRefreshAt)
        assertTrue(first.sameEditionRecheckUsed)
        assertEquals(first.nextRefreshAt.plusSeconds(86_400), afterRecheck.nextRefreshAt)
        assertTrue(afterRecheck.sameEditionRecheckUsed)
    }

    @Test
    fun newEditionRestartsTheDailyCadenceAndAllowsAnotherBoundedRecheck() {
        val now = Instant.parse("2026-11-18T04:00:00Z")
        val next = DailyCachePolicy.nextSchedule(cached(sameEditionRecheckUsed = true), sameEdition = false, now)

        assertEquals(now.plusSeconds(86_400), next.nextRefreshAt)
        assertFalse(next.sameEditionRecheckUsed)
    }

    private fun cached(
        nextRefreshAt: Instant = Instant.parse("2026-11-19T04:00:00Z"),
        sameEditionRecheckUsed: Boolean = false,
    ) = CachedDailyBatch(
        batch = DailyReadingBatch(LocalDate.parse("2026-11-18"), null, emptyMap()),
        fetchedAt = Instant.parse("2026-11-18T04:00:00Z"),
        nextRefreshAt = nextRefreshAt,
        sameEditionRecheckUsed = sameEditionRecheckUsed,
    )
}
