package com.zodiary.app

import com.zodiary.app.data.DailyReading
import java.time.Instant
import java.time.LocalDate

/** UI-facing state/actions implemented by the real ViewModel and the debug preview fixture. */
interface ZodiaryScreenModel {
    val destination: Destination
    val sign: ZodiacSign
    val birthDate: LocalDate?
    val topics: Set<String>
    val focus: String
    val reminder: String
    val preferencesReturnTo: Destination

    val topicOptions: List<String>
    val focusOptions: List<String>
    val reminderOptions: List<String>
    val moodOptions: List<String>

    val journalDate: LocalDate
    val journalText: String
    val mood: String
    val saveMessage: String
    val selectedDate: LocalDate
    val entries: Map<LocalDate, JournalEntry>
    val currentPrompt: JournalPrompt

    val currentDailyReading: DailyReading?
    val dailyFetchedAt: Instant?
    val isDailyLoading: Boolean
    val isDailyRefreshing: Boolean
    val dailyRefreshError: String?

    val isSupabaseConfigured: Boolean
    val supabaseConfigurationError: String?
    val isAccountInitializing: Boolean
    val isAccountBusy: Boolean
    val accountEmail: String?
    val accountMessage: String

    fun navigate(to: Destination)
    fun discoverSignFromBirthday(date: LocalDate)
    fun selectSign(value: ZodiacSign)
    fun changeTopic(value: String)
    fun changeFocus(value: String)
    fun changeReminder(value: String)
    fun savePreferences()
    fun editPreferences()
    fun finishOnboarding()
    fun nextPrompt()
    fun openJournal(date: LocalDate)
    fun changeMood(value: String)
    fun editJournalText(value: String)
    fun saveEntry()
    fun selectCalendarDate(date: LocalDate)
    fun createAccount(email: String, password: String)
    fun signIn(email: String, password: String)
    fun syncAccount()
    fun signOutAccount()
    fun refreshDaily(force: Boolean = false)
}
