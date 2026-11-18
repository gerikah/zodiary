package com.zodiary.app.data

import android.content.Context
import android.content.SharedPreferences
import java.time.Duration
import java.time.Instant

data class CachedDailyBatch(
    val batch: DailyReadingBatch,
    val fetchedAt: Instant,
    val nextRefreshAt: Instant,
    val sameEditionRecheckUsed: Boolean,
)

data class DailyRefreshResult(
    val snapshot: CachedDailyBatch?,
    val error: String?,
    val refreshed: Boolean,
)

data class CacheSchedule(val nextRefreshAt: Instant, val sameEditionRecheckUsed: Boolean)

data class StoredDailyBatch(
    val rawJson: String,
    val fetchedAt: Instant,
    val nextRefreshAt: Instant,
    val sameEditionRecheckUsed: Boolean,
)

interface DailyContentSource {
    fun fetchDailyAll(): String
}

interface DailyReadingCache {
    fun read(): StoredDailyBatch?
    fun write(value: StoredDailyBatch)
}

object DailyCachePolicy {
    private val regularInterval = Duration.ofHours(24)
    private val sameEditionRecheckInterval = Duration.ofHours(6)

    fun isRefreshDue(snapshot: CachedDailyBatch?, now: Instant): Boolean =
        snapshot == null || !now.isBefore(snapshot.nextRefreshAt)

    fun isSameEdition(old: DailyReadingBatch, current: DailyReadingBatch): Boolean {
        if (old.providerDate != current.providerDate || old.generatedAt != current.generatedAt) return false
        return old.readings.keys == current.readings.keys && old.readings.all { (slug, reading) ->
            val candidate = current.readings[slug]
            candidate != null && candidate.content == reading.content && candidate.closing == reading.closing
        }
    }

    fun isOlderEdition(old: DailyReadingBatch, current: DailyReadingBatch): Boolean = when {
        current.providerDate.isBefore(old.providerDate) -> true
        current.providerDate.isAfter(old.providerDate) -> false
        old.generatedAt != null && current.generatedAt != null -> current.generatedAt.isBefore(old.generatedAt)
        else -> false
    }

    fun nextSchedule(
        old: CachedDailyBatch?,
        sameEdition: Boolean,
        now: Instant,
    ): CacheSchedule = when {
        old == null || !sameEdition -> CacheSchedule(now.plus(regularInterval), sameEditionRecheckUsed = false)
        !old.sameEditionRecheckUsed -> CacheSchedule(now.plus(sameEditionRecheckInterval), sameEditionRecheckUsed = true)
        else -> CacheSchedule(now.plus(regularInterval), sameEditionRecheckUsed = true)
    }
}

/** One all-sign response is persisted locally and reused across sign changes and app launches. */
class SharedPreferencesDailyReadingCache(context: Context) : DailyReadingCache {
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(): StoredDailyBatch? {
        val rawJson = preferences.getString(KEY_RAW_JSON, null) ?: return null
        val fetchedAt = preferences.getLong(KEY_FETCHED_AT, 0L).takeIf { it > 0L }?.let(Instant::ofEpochMilli) ?: return null
        val nextRefreshAt = preferences.getLong(KEY_NEXT_REFRESH_AT, 0L)
            .takeIf { it > 0L }
            ?.let(Instant::ofEpochMilli)
            ?: fetchedAt.plus(Duration.ofHours(24))
        return StoredDailyBatch(
            rawJson = rawJson,
            fetchedAt = fetchedAt,
            nextRefreshAt = nextRefreshAt,
            sameEditionRecheckUsed = preferences.getBoolean(KEY_RECHECK_USED, false),
        )
    }

    override fun write(value: StoredDailyBatch) {
        preferences.edit()
            .putString(KEY_RAW_JSON, value.rawJson)
            .putLong(KEY_FETCHED_AT, value.fetchedAt.toEpochMilli())
            .putLong(KEY_NEXT_REFRESH_AT, value.nextRefreshAt.toEpochMilli())
            .putBoolean(KEY_RECHECK_USED, value.sameEditionRecheckUsed)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "zodiary.local"
        private const val KEY_RAW_JSON = "cosmyday.daily.raw"
        private const val KEY_FETCHED_AT = "cosmyday.daily.fetched_at"
        private const val KEY_NEXT_REFRESH_AT = "cosmyday.daily.next_refresh_at"
        private const val KEY_RECHECK_USED = "cosmyday.daily.same_edition_recheck_used"
    }
}

class DailyReadingRepository(
    private val cache: DailyReadingCache,
    private val api: DailyContentSource = CosmyDayApiService(),
) {
    private val refreshLock = Any()

    @Volatile private var refreshInFlight = false

    fun readCached(): CachedDailyBatch? {
        val stored = cache.read() ?: return null
        return runCatching {
            val batch = CosmyDayResponseAdapter.parseAllSigns(stored.rawJson)
            CachedDailyBatch(
                batch = batch,
                fetchedAt = stored.fetchedAt,
                nextRefreshAt = stored.nextRefreshAt,
                sameEditionRecheckUsed = stored.sameEditionRecheckUsed,
            )
        }.getOrNull()
    }

    fun refreshIfNeeded(force: Boolean = false, now: Instant = Instant.now()): DailyRefreshResult {
        val storedBeforeRefresh = cache.read()
        val existing = storedBeforeRefresh?.let { stored ->
            runCatching { CosmyDayResponseAdapter.parseAllSigns(stored.rawJson) }
                .getOrNull()
                ?.let { batch -> CachedDailyBatch(batch, stored.fetchedAt, stored.nextRefreshAt, stored.sameEditionRecheckUsed) }
        }
        if (!force && !DailyCachePolicy.isRefreshDue(existing, now)) {
            return DailyRefreshResult(existing, error = null, refreshed = false)
        }

        synchronized(refreshLock) {
            if (refreshInFlight) return DailyRefreshResult(readCached(), error = null, refreshed = false)
            refreshInFlight = true
        }

        return try {
            val json = api.fetchDailyAll()
            val nextBatch = CosmyDayResponseAdapter.parseAllSigns(json)
            val fetchedAt = Instant.now()
            if (existing != null && DailyCachePolicy.isOlderEdition(existing.batch, nextBatch)) {
                val schedule = DailyCachePolicy.nextSchedule(existing, sameEdition = true, now = fetchedAt)
                cache.write(storedBeforeRefresh.copy(
                    nextRefreshAt = schedule.nextRefreshAt,
                    sameEditionRecheckUsed = schedule.sameEditionRecheckUsed,
                ))
                return DailyRefreshResult(
                    snapshot = existing.copy(
                        nextRefreshAt = schedule.nextRefreshAt,
                        sameEditionRecheckUsed = schedule.sameEditionRecheckUsed,
                    ),
                    error = "CosmyDay has not published a newer edition yet",
                    refreshed = true,
                )
            }
            val sameEdition = existing != null && DailyCachePolicy.isSameEdition(existing.batch, nextBatch)
            val schedule = DailyCachePolicy.nextSchedule(existing, sameEdition, fetchedAt)
            cache.write(StoredDailyBatch(json, fetchedAt, schedule.nextRefreshAt, schedule.sameEditionRecheckUsed))
            DailyRefreshResult(
                snapshot = CachedDailyBatch(nextBatch, fetchedAt, schedule.nextRefreshAt, schedule.sameEditionRecheckUsed),
                error = null,
                refreshed = true,
            )
        } catch (failure: Exception) {
            DailyRefreshResult(
                snapshot = existing,
                error = failure.message ?: "CosmyDay could not be reached",
                refreshed = true,
            )
        } finally {
            synchronized(refreshLock) { refreshInFlight = false }
        }
    }
}
