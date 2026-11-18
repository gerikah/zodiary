package com.zodiary.app

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zodiary.app.data.CosmyDayResponseAdapter
import com.zodiary.app.data.DailyReading
import com.zodiary.app.data.DailyReadingRepository
import com.zodiary.app.data.SharedPreferencesDailyReadingCache
import java.time.LocalDate
import java.time.Instant
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
)

class ZodiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = application.getSharedPreferences("zodiary.local", Application.MODE_PRIVATE)
    private val dailyReadingRepository = DailyReadingRepository(SharedPreferencesDailyReadingCache(application))
    private val dailyReadingExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    var destination by mutableStateOf(
        if (storage.getBoolean(KEY_ONBOARDED, false)) Destination.Home else Destination.Welcome,
    )
        private set

    var sign by mutableStateOf(findSign(storage.getString(KEY_SIGN, "Libra") ?: "Libra"))
        private set
    var birthDate by mutableStateOf<LocalDate?>(null)
        private set
    var topics by mutableStateOf(storage.getStringSet(KEY_TOPICS, setOf("Personal growth"))?.toSet() ?: setOf("Personal growth"))
        private set
    var focus by mutableStateOf(storage.getString(KEY_FOCUS, "Reflect") ?: "Reflect")
        private set
    var reminder by mutableStateOf(storage.getString(KEY_REMINDER, "Not now") ?: "Not now")
        private set
    var promptIndex by mutableStateOf(0)
        private set
    var journalDate by mutableStateOf(LocalDate.now())
        private set
    var journalText by mutableStateOf(loadDraft(LocalDate.now()))
        private set
    var mood by mutableStateOf(loadMood(LocalDate.now()))
        private set
    var saveMessage by mutableStateOf("")
        private set
    var preferencesReturnTo by mutableStateOf(Destination.Trust)
        private set
    var selectedDate by mutableStateOf(LocalDate.now())
        private set
    val entries = mutableStateMapOf<LocalDate, JournalEntry>().apply { putAll(loadEntries()) }

    val topicOptions = listOf("Love", "Career", "Well-being", "Personal growth")
    val focusOptions = listOf("Reflect", "Track moods", "Keep memories", "A bit of everything")
    val reminderOptions = listOf("Morning", "Evening", "Not now")
    val moodOptions = listOf("Calm", "Hopeful", "Tender", "Restless", "Heavy")

    var dailyReadings by mutableStateOf<Map<String, DailyReading>>(emptyMap())
        private set
    var dailyFetchedAt by mutableStateOf<Instant?>(null)
        private set
    var isDailyLoading by mutableStateOf(true)
        private set
    var isDailyRefreshing by mutableStateOf(false)
        private set
    var dailyRefreshError by mutableStateOf<String?>(null)
        private set
    private var lastManualRefreshAt = 0L

    val currentDailyReading: DailyReading?
        get() = CosmyDayResponseAdapter.normalizeSign(sign.name)?.let(dailyReadings::get)

    val currentPrompt: JournalPrompt
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
    }

    fun refreshDaily(force: Boolean = false) {
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
        super.onCleared()
    }

    fun navigate(to: Destination) {
        destination = to
        if (to == Destination.Journal) openJournal(LocalDate.now())
    }

    fun discoverSignFromBirthday(date: LocalDate) {
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

    fun selectSign(value: ZodiacSign) {
        sign = value
        storage.edit().putString(KEY_SIGN, value.name).apply()
    }

    fun changeTopic(value: String) {
        topics = if (value in topics) topics - value else topics + value
        promptIndex = 0
    }

    fun changeFocus(value: String) { focus = value }
    fun changeReminder(value: String) { reminder = value }

    fun savePreferences() {
        storage.edit()
            .putStringSet(KEY_TOPICS, topics.toSet())
            .putString(KEY_FOCUS, focus)
            .putString(KEY_REMINDER, reminder)
            .apply()
        if (preferencesReturnTo == Destination.Trust) destination = Destination.Trust
        else destination = preferencesReturnTo
    }

    fun editPreferences() {
        preferencesReturnTo = Destination.Profile
        destination = Destination.Preferences
    }

    fun finishOnboarding() {
        storage.edit().putBoolean(KEY_ONBOARDED, true).putString(KEY_SIGN, sign.name).apply()
        destination = Destination.Home
    }

    fun nextPrompt() { promptIndex += 1 }

    fun openJournal(date: LocalDate) {
        journalDate = date
        val saved = entries[date]
        journalText = saved?.text ?: loadDraft(date)
        mood = saved?.mood ?: loadMood(date)
        saveMessage = if (saved != null) "Saved on this device" else ""
        destination = Destination.Journal
    }

    fun changeMood(value: String) {
        mood = value
        storage.edit().putString("draft.mood.${journalDate}", value).apply()
    }

    fun editJournalText(value: String) {
        journalText = value
        saveMessage = if (entries.containsKey(journalDate)) "Unsaved changes" else "Draft saved on this device"
        storage.edit().putString("draft.text.${journalDate}", value).apply()
    }

    fun saveEntry() {
        if (journalText.isBlank()) return
        val saved = JournalEntry(journalDate, mood, journalText.trim(), currentPrompt.text)
        entries[journalDate] = saved
        storage.edit()
            .putString(entryKey(journalDate, "text"), saved.text)
            .putString(entryKey(journalDate, "mood"), saved.mood)
            .putString(entryKey(journalDate, "prompt"), saved.prompt)
            .remove("draft.text.${journalDate}")
            .apply()
        journalText = saved.text
        saveMessage = "A moment, kept · saved on this device"
    }

    fun selectCalendarDate(date: LocalDate) { selectedDate = date }

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
            )
        }
    }

    private fun entryKey(date: LocalDate, field: String) = "entry.$date.$field"

    companion object {
        const val KEY_ONBOARDED = "onboarded"
        private const val MANUAL_RETRY_COOLDOWN_MS = 30_000L
        private const val KEY_SIGN = "sign"
        private const val KEY_TOPICS = "topics"
        private const val KEY_FOCUS = "focus"
        private const val KEY_REMINDER = "reminder"

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

        fun findSign(name: String): ZodiacSign = zodiacSigns.firstOrNull { it.name == name } ?: zodiacSigns[6]

    }
}
