package com.zodiary.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zodiary.app.data.DailyReading
import com.zodiary.app.data.MoonSnapshot
import com.zodiary.app.data.ReadingSection
import com.zodiary.app.ui.theme.ZodiaryColors
import com.zodiary.app.ui.theme.ZodiaryTheme
import java.time.Instant
import java.time.LocalDate

private val previewModel = SampleZodiaryScreenModel()

@Preview(name = "Splash", group = "Onboarding", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun SplashScreenPreview() = PreviewFrame { ZodiaryReferenceScreens.Splash(onComplete = {}) }

@Preview(name = "Welcome", group = "Onboarding", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun WelcomeScreenPreview() = PreviewFrame { ZodiaryReferenceScreens.Welcome(previewModel) }

@Preview(name = "Birthday", group = "Onboarding", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun BirthdayScreenPreview() = PreviewFrame { ZodiaryReferenceScreens.Birthday(previewModel) }

@Preview(name = "Sign reveal", group = "Onboarding", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun SignRevealScreenPreview() = PreviewFrame { ZodiaryReferenceScreens.SignReveal(previewModel) }

@Preview(name = "Preferences", group = "Onboarding", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun PreferencesScreenPreview() = PreviewFrame { ZodiaryReferenceScreens.Preferences(previewModel) }

@Preview(name = "Trust note", group = "Onboarding", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun TrustScreenPreview() = PreviewFrame { ZodiaryReferenceScreens.Trust(onContinue = {}) }

@Preview(name = "Home", group = "App", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun HomeScreenPreview() = PreviewFrame(Destination.Home) { ZodiaryReferenceScreens.Home(previewModel) }

@Preview(name = "Journal", group = "App", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun JournalScreenPreview() = PreviewFrame(Destination.Journal) { ZodiaryReferenceScreens.Journal(previewModel) }

@Preview(name = "Calendar", group = "App", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun CalendarScreenPreview() = PreviewFrame(Destination.Calendar) { ZodiaryReferenceScreens.Calendar(previewModel) }

@Preview(name = "Profile", group = "App", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun ProfileScreenPreview() = PreviewFrame(Destination.Profile) { ZodiaryReferenceScreens.Profile(previewModel) }

@Preview(name = "Daily reading", group = "App", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun ReadingScreenPreview() = PreviewFrame(Destination.Reading) {
    ZodiaryReferenceScreens.Reading(previewModel, onBack = {})
}

@Preview(name = "Login", group = "Account", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun LoginScreenPreview() = PreviewFrame { ZodiaryReferenceScreens.Login(previewModel) }

@Preview(name = "Register", group = "Account", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun RegisterScreenPreview() = PreviewFrame { ZodiaryReferenceScreens.Register(previewModel) }

@Composable
private fun PreviewFrame(destination: Destination? = null, content: @Composable () -> Unit) {
    ZodiaryTheme {
        Scaffold(
            containerColor = if (destination in setOf(Destination.Home, Destination.Journal, Destination.Calendar, Destination.Profile, Destination.Reading)) ZodiaryColors.Purple else ZodiaryColors.Canvas,
            bottomBar = {
                if (destination in setOf(Destination.Home, Destination.Journal, Destination.Calendar, Destination.Profile, Destination.Reading)) {
                    ZodiaryReferenceScreens.Navigation(destination ?: Destination.Home, onNavigate = {})
                }
            },
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding).padding(top = 8.dp)) {
                content()
            }
        }
    }
}

private class SampleZodiaryScreenModel : ZodiaryScreenModel {
    private val today = LocalDate.now()
    private val previousDay = today.minusDays(1)

    override val destination = Destination.Home
    override val sign = ZodiaryViewModel.findSign("Libra")
    override val birthDate: LocalDate? = null
    override val topics = setOf("Personal growth", "Well-being")
    override val focus = "Reflect"
    override val reminder = "Evening"
    override val preferencesReturnTo = Destination.Trust

    override val topicOptions = listOf("Love", "Career", "Well-being", "Personal growth")
    override val focusOptions = listOf("Reflect", "Track moods", "Keep memories", "A bit of everything")
    override val reminderOptions = listOf("Morning", "Evening", "Not now")
    override val moodOptions = listOf("Calm", "Hopeful", "Tender", "Restless", "Heavy")

    override val journalDate = today
    override val journalText = "Today felt quieter than I expected, and I made room to notice it."
    override val mood = "Hopeful"
    override val saveMessage = "Draft saved on this device."
    override val selectedDate = previousDay
    override val entries = mapOf(
        previousDay to JournalEntry(
            date = previousDay,
            mood = "Calm",
            text = "I took a slower walk and let the day settle before making a plan.",
            prompt = "What helped you feel at ease?",
            updatedAt = Instant.now().minusSeconds(86_400),
        ),
    )
    override val currentPrompt = JournalPrompt(
        topic = "Personal growth",
        text = "What are you learning to give yourself permission to feel?",
    )

    override val currentDailyReading = DailyReading(
        sign = sign.name,
        signSlug = sign.name.lowercase(),
        providerDate = today,
        weekday = today.dayOfWeek.name.lowercase().replaceFirstChar(Char::uppercase),
        generatedAt = Instant.now().minusSeconds(600),
        content = "A thoughtful conversation may help you see a familiar choice in a new light.",
        closing = "Let curiosity make a little room for change.",
        moon = MoonSnapshot(sign = "Gemini", phase = "waxing_crescent", phaseName = "Waxing Crescent", illumination = 34.0),
        sections = listOf(
            ReadingSection("Overview", "A thoughtful conversation may help you see a familiar choice in a new light."),
            ReadingSection("Reflection", "Notice which ideas leave you feeling more grounded and open."),
        ),
    )
    override val dailyFetchedAt = Instant.now().minusSeconds(120)
    override val isDailyLoading = false
    override val isDailyRefreshing = false
    override val dailyRefreshError: String? = null

    override val isSupabaseConfigured = false
    override val supabaseConfigurationError: String? = null
    override val isAccountInitializing = false
    override val isAccountBusy = false
    override val accountEmail: String? = null
    override val accountMessage = "Your journal is saved on this device. Connect an account whenever you're ready to sync it."

    override fun navigate(to: Destination) = Unit
    override fun discoverSignFromBirthday(date: LocalDate) = Unit
    override fun selectSign(value: ZodiacSign) = Unit
    override fun changeTopic(value: String) = Unit
    override fun changeFocus(value: String) = Unit
    override fun changeReminder(value: String) = Unit
    override fun savePreferences() = Unit
    override fun editPreferences() = Unit
    override fun finishOnboarding() = Unit
    override fun nextPrompt() = Unit
    override fun openJournal(date: LocalDate) = Unit
    override fun changeMood(value: String) = Unit
    override fun editJournalText(value: String) = Unit
    override fun saveEntry() = Unit
    override fun selectCalendarDate(date: LocalDate) = Unit
    override fun createAccount(email: String, password: String) = Unit
    override fun signIn(email: String, password: String) = Unit
    override fun syncAccount() = Unit
    override fun signOutAccount() = Unit
    override fun refreshDaily(force: Boolean) = Unit
}
