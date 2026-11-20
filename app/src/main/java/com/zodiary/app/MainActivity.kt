package com.zodiary.app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.zodiary.app.ui.theme.ZodiaryColors
import com.zodiary.app.ui.theme.DmSansFontFamily
import com.zodiary.app.ui.theme.FrauncesFontFamily
import com.zodiary.app.ui.theme.ZodiaryTheme
import com.zodiary.app.data.DailyReading
import com.zodiary.app.data.DailyReadingLabels
import com.zodiary.app.data.MoonSnapshot
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val zodiaryViewModel: ZodiaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        zodiaryViewModel.handleAuthDeepLink(intent)

        setContent {
            ZodiaryTheme {
                ZodiaryApp(zodiaryViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        zodiaryViewModel.handleAuthDeepLink(intent)
    }
}

@Composable
private fun ZodiaryApp(model: ZodiaryViewModel) {
    val showNavigation = model.destination in setOf(
        Destination.Home,
        Destination.Journal,
        Destination.Calendar,
        Destination.Profile,
        Destination.Reading,
    )

    androidx.compose.material3.Scaffold(
        containerColor = ZodiaryColors.Canvas,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (showNavigation) AppNavigation(model.destination, model::navigate)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (model.destination) {
                Destination.Welcome -> WelcomeScreen(onBegin = { model.navigate(Destination.Birthday) })
                Destination.Birthday -> BirthdayScreen(model)
                Destination.SignReveal -> SignRevealScreen(model)
                Destination.Preferences -> PreferencesScreen(model)
                Destination.Trust -> TrustScreen(onContinue = model::finishOnboarding)
                Destination.Home -> HomeScreen(model)
                Destination.Journal -> JournalScreen(model)
                Destination.Calendar -> CalendarScreen(model)
                Destination.Profile -> ProfileScreen(model)
                Destination.Reading -> ReadingScreen(model, onBack = { model.navigate(Destination.Home) })
            }
        }
    }
}

@Composable
internal fun AppNavigation(destination: Destination, onNavigate: (Destination) -> Unit) {
    val selected = when (destination) {
        Destination.Journal -> Destination.Journal
        Destination.Calendar -> Destination.Calendar
        Destination.Profile -> Destination.Profile
        else -> Destination.Home
    }
    NavigationBar(
        containerColor = ZodiaryColors.White,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets.navigationBars,
    ) {
        NavigationBarItem(
            selected = selected == Destination.Home,
            onClick = { onNavigate(Destination.Home) },
            icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
            label = { Text("Home") },
        )
        NavigationBarItem(
            selected = selected == Destination.Journal,
            onClick = { onNavigate(Destination.Journal) },
            icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
            label = { Text("Journal") },
        )
        NavigationBarItem(
            selected = selected == Destination.Calendar,
            onClick = { onNavigate(Destination.Calendar) },
            icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            label = { Text("Calendar") },
        )
        NavigationBarItem(
            selected = selected == Destination.Profile,
            onClick = { onNavigate(Destination.Profile) },
            icon = { Icon(Icons.Outlined.PersonOutline, contentDescription = null) },
            label = { Text("Profile") },
        )
    }
}

@Composable
internal fun WelcomeScreen(onBegin: () -> Unit) {
    OnboardingColumn {
        BrandWordmark()
        Spacer(Modifier.height(36.dp))
        Text(
            text = "The universe is\nlistening. What's\nyour story?",
            style = displayStyle(38.sp),
            color = ZodiaryColors.Purple,
            lineHeight = 41.sp,
        )
        Spacer(Modifier.height(15.dp))
        Text("The stars inspire. You write.", style = bodyStyle(18.sp), color = ZodiaryColors.Ink)
        Spacer(Modifier.height(36.dp))
        WelcomeArtwork()
        Spacer(Modifier.height(28.dp))
        PrimaryAction("Begin your story", onBegin)
    }
}

@Composable
internal fun BirthdayScreen(model: ZodiaryScreenModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    OnboardingColumn {
        BackRow(onBack = { model.navigate(Destination.Welcome) })
        Spacer(Modifier.height(34.dp))
        Text("Let's find\nyour sign.", style = displayStyle(38.sp), color = ZodiaryColors.Purple, lineHeight = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text("Your birthday is where your story begins.", style = bodyStyle(17.sp), color = ZodiaryColors.Muted)
        Spacer(Modifier.height(42.dp))
        Text("When is your birthday?", style = bodyStyle(16.sp, FontWeight.SemiBold), color = ZodiaryColors.Ink)
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .clickable {
                    val start = model.birthDate ?: LocalDate.of(2000, 1, 1)
                    DatePickerDialog(
                        context,
                        { _, year, month, day -> model.discoverSignFromBirthday(LocalDate.of(year, month + 1, day)) },
                        start.year,
                        start.monthValue - 1,
                        start.dayOfMonth,
                    ).show()
                },
            shape = RoundedCornerShape(20.dp),
            color = ZodiaryColors.FormLavender,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    model.birthDate?.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)) ?: "Choose a date",
                    modifier = Modifier.weight(1f),
                    style = bodyStyle(17.sp),
                    color = if (model.birthDate == null) ZodiaryColors.Muted else ZodiaryColors.Ink,
                )
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choose birthday", tint = ZodiaryColors.Purple)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("We'll use this to find your zodiac sign. Your full birthday isn't saved.", style = bodyStyle(14.sp), color = ZodiaryColors.Muted)
        Spacer(Modifier.height(40.dp))
        Text("✦   ✧   ✦", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = ZodiaryColors.Gold, fontSize = 23.sp)
        Spacer(Modifier.height(28.dp))
        PrimaryAction("Discover my sign", onClick = { model.navigate(Destination.SignReveal) }, enabled = model.birthDate != null)
    }
}

@Composable
internal fun SignRevealScreen(model: ZodiaryScreenModel) {
    OnboardingColumn {
        BackRow(onBack = { model.navigate(Destination.Birthday) })
        Spacer(Modifier.height(24.dp))
        Text("A new page,\nin your colors.", style = displayStyle(35.sp), color = ZodiaryColors.Purple, lineHeight = 38.sp)
        Spacer(Modifier.height(25.dp))
        SignMedallion(model.sign, Modifier.align(Alignment.CenterHorizontally).size(218.dp))
        Spacer(Modifier.height(24.dp))
        Text("You're a ${model.sign.name}!", modifier = Modifier.fillMaxWidth(), style = displayStyle(31.sp), color = ZodiaryColors.DeepPurple, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Your Zodiary just took on your colors.", modifier = Modifier.fillMaxWidth(), style = bodyStyle(16.sp), color = ZodiaryColors.Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp))
        PrimaryAction("Explore my sign", onClick = { model.navigate(Destination.Preferences) })
        TextButton(onClick = { model.navigate(Destination.Birthday) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Not quite right? Change my sign", color = ZodiaryColors.Purple)
        }
    }
}

@Composable
internal fun PreferencesScreen(model: ZodiaryScreenModel) {
    OnboardingColumn {
        BackRow(onBack = {
            model.navigate(if (model.preferencesReturnTo == Destination.Profile) Destination.Profile else Destination.SignReveal)
        })
        Spacer(Modifier.height(29.dp))
        Text("Make it personal.", style = displayStyle(34.sp), color = ZodiaryColors.Purple)
        Spacer(Modifier.height(9.dp))
        Text("Choose what matters most to you.", style = bodyStyle(16.sp), color = ZodiaryColors.Muted)
        Spacer(Modifier.height(25.dp))
        ChoiceSection("What would you like to explore?", model.topicOptions, model.topics, model::changeTopic)
        Spacer(Modifier.height(19.dp))
        ChoiceSection("What brings you here?", model.focusOptions, setOf(model.focus), model::changeFocus)
        Spacer(Modifier.height(19.dp))
        ChoiceSection("Want a daily reminder?", model.reminderOptions, setOf(model.reminder), model::changeReminder)
        Spacer(Modifier.height(28.dp))
        PrimaryAction(if (model.preferencesReturnTo == Destination.Profile) "Save preferences" else "Save my preferences", model::savePreferences)
    }
}

@Composable
internal fun TrustScreen(onContinue: () -> Unit) {
    OnboardingColumn {
        BrandWordmark()
        Spacer(Modifier.height(48.dp))
        Text("A note before\nwe begin.", style = displayStyle(37.sp), color = ZodiaryColors.Purple, lineHeight = 40.sp)
        Spacer(Modifier.height(24.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = ZodiaryColors.Lavender.copy(alpha = 0.58f)) {
            Column(Modifier.padding(23.dp)) {
                Text("The stars offer a perspective; you decide what resonates.", style = displayStyle(24.sp), color = ZodiaryColors.DeepPurple, lineHeight = 30.sp)
                Spacer(Modifier.height(16.dp))
                Text("Readings are for reflection, not prediction or professional advice.", style = bodyStyle(15.sp), color = ZodiaryColors.Ink)
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("Your journal is ready for its first page.", style = bodyStyle(17.sp), color = ZodiaryColors.Muted)
        Spacer(Modifier.height(34.dp))
        PrimaryAction("Open my journal", onContinue)
    }
}

@Composable
internal fun HomeScreen(model: ZodiaryScreenModel) {
    val greeting = when (java.time.LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    AppScrollColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandWordmark(modifier = Modifier.weight(1f))
            Surface(shape = CircleShape, color = ZodiaryColors.FormLavender) {
                TextButton(onClick = { model.navigate(Destination.Profile) }) {
                    Text("${model.sign.symbol}  ${model.sign.name}", color = ZodiaryColors.DeepPurple, style = bodyStyle(14.sp, FontWeight.SemiBold))
                }
            }
        }
        Spacer(Modifier.height(26.dp))
        Text("$greeting, ${model.sign.name}", style = bodyStyle(16.sp, FontWeight.Medium), color = ZodiaryColors.Muted)
        Spacer(Modifier.height(7.dp))
        Text("Your page is\nwaiting.", style = displayStyle(36.sp), color = ZodiaryColors.Purple, lineHeight = 39.sp)
        Spacer(Modifier.height(18.dp))
        PrimaryAction("Write today's page", onClick = { model.openJournal(LocalDate.now()) })
        Spacer(Modifier.height(24.dp))
        val reading = model.currentDailyReading
        DailyReadingCard(
            reading = reading,
            loading = model.isDailyLoading,
            refreshing = model.isDailyRefreshing,
            refreshError = model.dailyRefreshError,
            fetchedAt = model.dailyFetchedAt,
            onOpen = { model.navigate(Destination.Reading) },
            onRetry = { model.refreshDaily(force = true) },
        )
        if (reading?.moon != null) {
            Spacer(Modifier.height(14.dp))
            MoonCard(date = reading.providerDate, moon = reading.moon)
        }
        Spacer(Modifier.height(16.dp))
        PromptCard(model = model)
        Spacer(Modifier.height(16.dp))
        AffirmationCard()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DailyReadingCard(
    reading: DailyReading?,
    loading: Boolean,
    refreshing: Boolean,
    refreshError: String?,
    fetchedAt: Instant?,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(25.dp), color = ZodiaryColors.DeepPurple, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(21.dp)) {
            val title = reading?.let(::readingTitle) ?: "Today's reading"
            Text(title, style = bodyStyle(15.sp, FontWeight.SemiBold), color = ZodiaryColors.Gold)
            Spacer(Modifier.height(11.dp))
            if (reading == null) {
                Text(
                    if (loading) "Finding your daily reading…" else "Your journal is still here.",
                    style = displayStyle(24.sp), color = ZodiaryColors.White, lineHeight = 28.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (loading) "This may take a moment." else "We couldn't load the daily reading. Try again when you're ready.",
                    style = bodyStyle(15.sp), color = ZodiaryColors.White.copy(alpha = 0.9f),
                )
                if (!loading) {
                    TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                        Text("Try again  ↻", color = ZodiaryColors.Lavender)
                    }
                }
            } else {
                Text("A general reading for ${reading.sign}.", style = bodyStyle(14.sp), color = ZodiaryColors.Lavender)
                Spacer(Modifier.height(8.dp))
                val overview = reading.sections.firstOrNull { it.title == "Overview" }?.body ?: reading.content
                Text(overview, style = bodyStyle(15.sp), color = ZodiaryColors.White.copy(alpha = 0.95f), maxLines = 4, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(9.dp))
                Text("Content date · ${formatProviderDate(reading.providerDate)}", style = bodyStyle(13.sp), color = ZodiaryColors.Lavender)
                if (refreshError != null) {
                    Text("Update unavailable · showing the saved reading", style = bodyStyle(13.sp), color = ZodiaryColors.Gold)
                } else if (refreshing) {
                    Text("Checking for a newer edition…", style = bodyStyle(13.sp), color = ZodiaryColors.Lavender)
                }
                fetchedAt?.let {
                    Text("Fetched · ${formatInstant(it)}", style = bodyStyle(12.sp), color = ZodiaryColors.Lavender.copy(alpha = 0.9f))
                }
                CosmyDayAttribution(onDark = true)
                TextButton(onClick = onOpen, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                    Text("Read the full reading  →", color = ZodiaryColors.Lavender, style = bodyStyle(15.sp, FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun MoonCard(date: LocalDate, moon: MoonSnapshot) {
    Surface(shape = RoundedCornerShape(23.dp), color = ZodiaryColors.FormLavender, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(19.dp)) {
            Text("Moon snapshot · ${formatProviderDate(date)}", style = bodyStyle(13.sp, FontWeight.SemiBold), color = ZodiaryColors.Purple)
            Spacer(Modifier.height(7.dp))
            moon.sign?.let { Text("Moon in $it", style = displayStyle(22.sp), color = ZodiaryColors.DeepPurple) }
            val phase = moon.phaseName ?: moon.phase?.replace('_', ' ')?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            phase?.let { Text(it, style = bodyStyle(15.sp), color = ZodiaryColors.Ink) }
            moon.illumination?.let {
                Text(String.format(Locale.getDefault(), "%.1f%% illuminated", it), style = bodyStyle(14.sp), color = ZodiaryColors.Muted)
            }
            CosmyDayAttribution(onDark = false)
        }
    }
}

@Composable
private fun CosmyDayAttribution(onDark: Boolean) {
    val uriHandler = LocalUriHandler.current
    TextButton(
        onClick = { runCatching { uriHandler.openUri("https://cosmyday.com") } },
        modifier = Modifier.heightIn(min = 44.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (onDark) ZodiaryColors.Lavender else ZodiaryColors.Purple,
        ),
    ) {
        Text("Astrology data by CosmyDay", style = bodyStyle(13.sp, FontWeight.SemiBold))
    }
}

@Composable
private fun PromptCard(model: ZodiaryScreenModel) {
    Surface(shape = RoundedCornerShape(24.dp), color = ZodiaryColors.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("A moment to notice", style = bodyStyle(14.sp, FontWeight.SemiBold), color = ZodiaryColors.Purple)
            Spacer(Modifier.height(10.dp))
            Text(model.currentPrompt.text, style = displayStyle(22.sp), color = ZodiaryColors.Ink, lineHeight = 28.sp)
            Spacer(Modifier.height(12.dp))
            Text(model.currentPrompt.sourceLabel(), style = bodyStyle(13.sp), color = ZodiaryColors.Muted)
            TextButton(onClick = model::nextPrompt, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                Text("Try another prompt  ↻", color = ZodiaryColors.Purple)
            }
        }
    }
}

@Composable
private fun AffirmationCard() {
    Surface(shape = RoundedCornerShape(24.dp), color = ZodiaryColors.Lavender, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(21.dp)) {
            Text("Words to carry today", style = bodyStyle(14.sp, FontWeight.SemiBold), color = ZodiaryColors.DeepPurple)
            Spacer(Modifier.height(11.dp))
            Text("I can meet this day one honest moment at a time.", style = displayStyle(22.sp), color = ZodiaryColors.DeepPurple, lineHeight = 28.sp)
        }
    }
}

@Composable
internal fun JournalScreen(model: ZodiaryScreenModel) {
    AppScrollColumn(modifier = Modifier.imePadding()) {
        Text("Today's page", style = displayStyle(34.sp), color = ZodiaryColors.Purple)
        Spacer(Modifier.height(6.dp))
        Text(model.journalDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)), style = bodyStyle(15.sp), color = ZodiaryColors.Muted)
        Spacer(Modifier.height(22.dp))
        Text("How's your inner universe?", style = bodyStyle(18.sp, FontWeight.SemiBold), color = ZodiaryColors.Ink)
        Spacer(Modifier.height(5.dp))
        Text("No right or wrong feelings. Just yours.", style = bodyStyle(14.sp), color = ZodiaryColors.Muted)
        Spacer(Modifier.height(13.dp))
        ChoiceGrid(model.moodOptions, setOf(model.mood), model::changeMood, columns = 3)
        Spacer(Modifier.height(23.dp))
        Surface(shape = RoundedCornerShape(22.dp), color = ZodiaryColors.FormLavender, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(model.currentPrompt.sourceLabel(), style = bodyStyle(13.sp, FontWeight.SemiBold), color = ZodiaryColors.Purple)
                Spacer(Modifier.height(8.dp))
                Text(model.currentPrompt.text, style = bodyStyle(16.sp), color = ZodiaryColors.Ink)
                TextButton(onClick = model::nextPrompt, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                    Text("Try another prompt", color = ZodiaryColors.Purple)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = model.journalText,
            onValueChange = model::editJournalText,
            modifier = Modifier.fillMaxWidth().heightIn(min = 190.dp),
            placeholder = { Text("Dear universe, today I…", color = ZodiaryColors.Muted) },
            shape = RoundedCornerShape(22.dp),
            minLines = 6,
            textStyle = bodyStyle(16.sp).copy(color = ZodiaryColors.Ink, lineHeight = 24.sp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ZodiaryColors.White,
                unfocusedContainerColor = ZodiaryColors.White,
                focusedBorderColor = ZodiaryColors.Purple,
                unfocusedBorderColor = ZodiaryColors.Border,
            ),
        )
        Spacer(Modifier.height(9.dp))
        Text(
            model.saveMessage.ifBlank { "Your writing is saved as a draft on this device." },
            style = bodyStyle(13.sp),
            color = if (model.saveMessage == "Unsaved changes") ZodiaryColors.DeepPurple else ZodiaryColors.Muted,
        )
        Spacer(Modifier.height(16.dp))
        PrimaryAction("Save entry", model::saveEntry, enabled = model.journalText.isNotBlank())
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
internal fun CalendarScreen(model: ZodiaryScreenModel) {
    var month by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    val first = month.withDayOfMonth(1)
    val offset = first.dayOfWeek.value % 7
    val cells = (0 until 42).map { first.plusDays((it - offset).toLong()) }
    AppScrollColumn {
        Text("Your story,\nover time.", style = displayStyle(34.sp), color = ZodiaryColors.Purple, lineHeight = 38.sp)
        Spacer(Modifier.height(8.dp))
        Text("A look back at the days you made space to write.", style = bodyStyle(15.sp), color = ZodiaryColors.Muted)
        Spacer(Modifier.height(22.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = ZodiaryColors.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)), modifier = Modifier.weight(1f), style = bodyStyle(19.sp, FontWeight.SemiBold), color = ZodiaryColors.Ink)
                    IconButton(onClick = { month = month.minusMonths(1) }) {
                        Text("‹", color = ZodiaryColors.Purple, fontSize = 28.sp, modifier = Modifier.semantics { contentDescription = "Previous month" })
                    }
                    IconButton(onClick = { if (month.isBefore(LocalDate.now().withDayOfMonth(1))) month = month.plusMonths(1) }) {
                        Text("›", color = ZodiaryColors.Purple, fontSize = 28.sp, modifier = Modifier.semantics { contentDescription = "Next month" })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                        Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = bodyStyle(12.sp, FontWeight.SemiBold), color = ZodiaryColors.Muted)
                    }
                }
                Spacer(Modifier.height(7.dp))
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            val hasEntry = model.entries.containsKey(date)
                            val isSelected = date == model.selectedDate
                            val isToday = date == LocalDate.now()
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(43.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) ZodiaryColors.Purple else Color.Transparent)
                                    .clickable(enabled = !date.isAfter(LocalDate.now())) { model.selectCalendarDate(date) }
                                    .semantics { contentDescription = buildString { append(date.format(DateTimeFormatter.ofPattern("MMMM d"))); if (hasEntry) append(", journal entry") } },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    color = when {
                                        isSelected -> ZodiaryColors.White
                                        date.month != month.month -> ZodiaryColors.Muted.copy(alpha = 0.45f)
                                        else -> ZodiaryColors.Ink
                                    },
                                    style = bodyStyle(14.sp, if (isToday || hasEntry) FontWeight.SemiBold else FontWeight.Normal),
                                )
                                if (hasEntry) Box(Modifier.padding(top = 1.dp).size(4.dp).clip(CircleShape).background(if (isSelected) ZodiaryColors.Gold else ZodiaryColors.Purple))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(11.dp))
        Text("Marked days contain a saved reflection.", style = bodyStyle(13.sp), color = ZodiaryColors.Muted)
        Spacer(Modifier.height(18.dp))
        val entry = model.entries[model.selectedDate]
        Surface(shape = RoundedCornerShape(23.dp), color = ZodiaryColors.FormLavender, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(19.dp)) {
                Text(model.selectedDate.format(DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH)), style = bodyStyle(14.sp, FontWeight.SemiBold), color = ZodiaryColors.Purple)
                Spacer(Modifier.height(8.dp))
                if (entry == null) {
                    Text("Every story starts with a blank page.", style = displayStyle(21.sp), color = ZodiaryColors.Ink, lineHeight = 27.sp)
                    Spacer(Modifier.height(7.dp))
                    Text("No entry saved for this day yet.", style = bodyStyle(14.sp), color = ZodiaryColors.Muted)
                    TextButton(onClick = { model.openJournal(model.selectedDate) }, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                        Text("Write for this day  →", color = ZodiaryColors.Purple)
                    }
                } else {
                    if (entry.mood.isNotBlank()) Text("Mood · ${entry.mood}", style = bodyStyle(14.sp, FontWeight.Medium), color = ZodiaryColors.DeepPurple)
                    Spacer(Modifier.height(7.dp))
                    Text(entry.text, style = bodyStyle(15.sp), color = ZodiaryColors.Ink, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = { model.openJournal(model.selectedDate) }, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                        Text("Open reflection  →", color = ZodiaryColors.Purple)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
internal fun ProfileScreen(model: ZodiaryScreenModel) {
    var showSignPicker by remember { mutableStateOf(false) }
    AppScrollColumn {
        Text("Your space", style = displayStyle(34.sp), color = ZodiaryColors.Purple)
        Spacer(Modifier.height(7.dp))
        Text("Make this journal feel like yours.", style = bodyStyle(15.sp), color = ZodiaryColors.Muted)
        Spacer(Modifier.height(24.dp))
        Surface(shape = RoundedCornerShape(25.dp), color = ZodiaryColors.DeepPurple, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                SignMedallion(model.sign, Modifier.size(74.dp), compact = true)
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    Text("Your sign", style = bodyStyle(13.sp), color = ZodiaryColors.Lavender)
                    Text(model.sign.name, style = displayStyle(24.sp), color = ZodiaryColors.White)
                }
                TextButton(onClick = { showSignPicker = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Change", color = ZodiaryColors.Gold)
                }
            }
        }
        Spacer(Modifier.height(15.dp))
        ProfileSetting(
            title = "Your interests",
            detail = model.topics.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "General prompts",
            action = "Edit",
            onClick = model::editPreferences,
        )
        Spacer(Modifier.height(12.dp))
        ProfileSetting(
            title = "Journal focus",
            detail = model.focus,
            action = "Edit",
            onClick = model::editPreferences,
        )
        Spacer(Modifier.height(12.dp))
        ProfileSetting(
            title = "Daily reminder",
            detail = model.reminder,
            action = "Edit",
            onClick = model::editPreferences,
        )
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(23.dp), color = ZodiaryColors.Lavender, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(19.dp)) {
                Text("A note to keep", style = bodyStyle(14.sp, FontWeight.SemiBold), color = ZodiaryColors.DeepPurple)
                Spacer(Modifier.height(8.dp))
                Text("The stars offer a perspective; you decide what resonates.", style = displayStyle(21.sp), color = ZodiaryColors.DeepPurple, lineHeight = 27.sp)
                Spacer(Modifier.height(10.dp))
                Text("Readings are for reflection, not prediction or professional advice.", style = bodyStyle(14.sp), color = ZodiaryColors.Ink)
            }
        }
        Spacer(Modifier.height(15.dp))
        AccountPanel(model)
        Spacer(Modifier.height(7.dp))
        Text("Reminder timing is saved, but notifications aren't active yet.", style = bodyStyle(13.sp), color = ZodiaryColors.Muted)
        Spacer(Modifier.height(20.dp))
    }
    if (showSignPicker) SignPickerDialog(
        selected = model.sign,
        onDismiss = { showSignPicker = false },
        onSelect = { model.selectSign(it); showSignPicker = false },
    )
}

@Composable
private fun AccountPanel(model: ZodiaryScreenModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val canSubmit = email.contains('@') && password.length >= 8 && !model.isAccountBusy && !model.isAccountInitializing

    Spacer(Modifier.height(9.dp))
    Surface(shape = RoundedCornerShape(23.dp), color = ZodiaryColors.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("Your account", style = bodyStyle(16.sp, FontWeight.SemiBold), color = ZodiaryColors.DeepPurple)
            Spacer(Modifier.height(6.dp))

            when {
                model.isAccountInitializing -> {
                    Text("Checking your account…", style = bodyStyle(14.sp), color = ZodiaryColors.Muted)
                }
                !model.isSupabaseConfigured -> {
                    Text(
                        model.supabaseConfigurationError ?: "Your journal is saved on this device. Connect Supabase to turn on account sync.",
                        style = bodyStyle(14.sp), color = ZodiaryColors.Muted, lineHeight = 20.sp,
                    )
                }
                model.accountEmail != null -> {
                    Text("Signed in as ${model.accountEmail}", style = bodyStyle(14.sp), color = ZodiaryColors.Ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Your entries and preferences sync when saved. A local copy stays on this device.",
                        style = bodyStyle(13.sp), color = ZodiaryColors.Muted, lineHeight = 19.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(model.accountMessage, style = bodyStyle(13.sp), color = ZodiaryColors.Purple, lineHeight = 19.sp)
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TextButton(onClick = model::syncAccount, enabled = !model.isAccountBusy) {
                            Text(if (model.isAccountBusy) "Syncing…" else "Sync now", color = ZodiaryColors.Purple)
                        }
                        TextButton(onClick = model::signOutAccount, enabled = !model.isAccountBusy) {
                            Text("Sign out", color = ZodiaryColors.Purple)
                        }
                    }
                }
                else -> {
                    Text(
                        "Start as a guest, then sign in when you want your journal backed up and available on another device.",
                        style = bodyStyle(14.sp), color = ZodiaryColors.Muted, lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(11.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        singleLine = true,
                        enabled = !model.isAccountBusy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        enabled = !model.isAccountBusy,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Use at least 8 characters.", style = bodyStyle(12.sp), color = ZodiaryColors.Muted)
                    Spacer(Modifier.height(6.dp))
                    Text(model.accountMessage, style = bodyStyle(13.sp), color = ZodiaryColors.Purple, lineHeight = 19.sp)
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { model.createAccount(email, password) },
                            enabled = canSubmit,
                            colors = ButtonDefaults.buttonColors(containerColor = ZodiaryColors.Purple),
                        ) { Text(if (model.isAccountBusy) "Please wait…" else "Create account") }
                        TextButton(
                            onClick = { model.signIn(email, password) },
                            enabled = canSubmit,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text("Sign in", color = ZodiaryColors.Purple) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSetting(title: String, detail: String, action: String, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(21.dp), color = ZodiaryColors.White, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = bodyStyle(15.sp, FontWeight.SemiBold), color = ZodiaryColors.Ink)
                Spacer(Modifier.height(3.dp))
                Text(detail, style = bodyStyle(13.sp), color = ZodiaryColors.Muted)
            }
            TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) { Text(action, color = ZodiaryColors.Purple) }
        }
    }
}

@Composable
internal fun ReadingScreen(model: ZodiaryScreenModel, onBack: () -> Unit) {
    val reading = model.currentDailyReading
    AppScrollColumn {
        BackRow(onBack = onBack, backLabel = "Home")
        Spacer(Modifier.height(28.dp))
        if (reading == null) {
            Text("Today's reading", style = displayStyle(34.sp), color = ZodiaryColors.Purple)
            Spacer(Modifier.height(10.dp))
            Text("Your journal is still here.", style = bodyStyle(17.sp), color = ZodiaryColors.Muted)
            Spacer(Modifier.height(22.dp))
            Surface(shape = RoundedCornerShape(24.dp), color = ZodiaryColors.White, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(21.dp)) {
                    Text(
                        if (model.isDailyLoading) "Finding your daily reading…" else "The daily reading isn't available right now.",
                        style = displayStyle(24.sp), color = ZodiaryColors.DeepPurple, lineHeight = 29.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("We couldn't load the reading. Your journal is still here.", style = bodyStyle(16.sp), color = ZodiaryColors.Ink, lineHeight = 24.sp)
                    if (!model.isDailyLoading) {
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { model.refreshDaily(force = true) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Try again  ↻", color = ZodiaryColors.Purple)
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Text("The stars offer a perspective; you decide what resonates.", style = bodyStyle(14.sp), color = ZodiaryColors.Muted)
                    Spacer(Modifier.height(7.dp))
                    Text("Readings are for reflection, not prediction or professional advice.", style = bodyStyle(14.sp), color = ZodiaryColors.Muted)
                }
            }
        } else {
            Text(readingTitle(reading), style = displayStyle(34.sp), color = ZodiaryColors.Purple, lineHeight = 38.sp)
            Spacer(Modifier.height(9.dp))
            Text("A general reading for ${reading.sign}.", style = bodyStyle(16.sp), color = ZodiaryColors.Muted)
            Spacer(Modifier.height(5.dp))
            Text("Content date · ${formatProviderDate(reading.providerDate)}", style = bodyStyle(14.sp, FontWeight.SemiBold), color = ZodiaryColors.DeepPurple)
            reading.generatedAt?.let {
                Text("Provider generated · ${formatInstant(it)}", style = bodyStyle(13.sp), color = ZodiaryColors.Muted)
            }
            model.dailyFetchedAt?.let {
                Text("Fetched · ${formatInstant(it)}", style = bodyStyle(13.sp), color = ZodiaryColors.Muted)
            }
            if (model.dailyRefreshError != null) {
                Spacer(Modifier.height(10.dp))
                Text("The update isn't available. You're viewing the saved reading for ${formatProviderDate(reading.providerDate)}.", style = bodyStyle(14.sp), color = ZodiaryColors.DeepPurple)
            } else if (model.isDailyRefreshing) {
                Spacer(Modifier.height(7.dp))
                Text("Checking for a newer edition…", style = bodyStyle(13.sp), color = ZodiaryColors.Muted)
            }
            Spacer(Modifier.height(19.dp))
            reading.sections.forEach { section ->
                if (section.title != null) {
                    Text(section.title, style = bodyStyle(18.sp, FontWeight.SemiBold), color = ZodiaryColors.Purple)
                    Spacer(Modifier.height(6.dp))
                }
                if (section.body.isNotBlank()) {
                    Text(section.body, style = bodyStyle(16.sp), color = ZodiaryColors.Ink, lineHeight = 24.sp)
                    Spacer(Modifier.height(17.dp))
                }
            }
            reading.closing?.takeIf(String::isNotBlank)?.let { closing ->
                Text("A closing thought", style = bodyStyle(18.sp, FontWeight.SemiBold), color = ZodiaryColors.Purple)
                Spacer(Modifier.height(6.dp))
                Text(closing, style = bodyStyle(16.sp), color = ZodiaryColors.Ink, lineHeight = 24.sp)
            }
            Spacer(Modifier.height(12.dp))
            CosmyDayAttribution(onDark = false)
            Spacer(Modifier.height(11.dp))
            Text("The stars offer a perspective; you decide what resonates.", style = bodyStyle(14.sp), color = ZodiaryColors.Muted)
            Spacer(Modifier.height(7.dp))
            Text("Readings are for reflection, not prediction or professional advice.", style = bodyStyle(14.sp), color = ZodiaryColors.Muted)
        }
        Spacer(Modifier.height(22.dp))
        PrimaryAction("Write today's page", onClick = onBack)
    }
}

@Composable
private fun OnboardingColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp, vertical = 18.dp),
        content = content,
    )
}

@Composable
private fun AppScrollColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
        content = content,
    )
}

@Composable
private fun BrandWordmark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.zodiary_logo),
        contentDescription = "Zodiary",
        modifier = modifier.width(138.dp).height(32.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun BackRow(onBack: () -> Unit, backLabel: String = "Back") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("‹  $backLabel", color = ZodiaryColors.Ink, style = bodyStyle(15.sp))
        }
    }
}

@Composable
private fun WelcomeArtwork() {
    Box(
        modifier = Modifier.fillMaxWidth().height(210.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(200.dp)) {
            drawCircle(color = ZodiaryColors.Lavender.copy(alpha = 0.45f), radius = size.minDimension * 0.48f)
            drawCircle(color = ZodiaryColors.Purple.copy(alpha = 0.35f), radius = size.minDimension * 0.37f, style = Stroke(width = 1.5.dp.toPx()))
            drawCircle(color = ZodiaryColors.Gold.copy(alpha = 0.8f), radius = size.minDimension * 0.025f, center = androidx.compose.ui.geometry.Offset(size.width * 0.74f, size.height * 0.3f))
            drawCircle(color = ZodiaryColors.Purple.copy(alpha = 0.65f), radius = size.minDimension * 0.018f, center = androidx.compose.ui.geometry.Offset(size.width * 0.23f, size.height * 0.68f))
        }
        Text("✧", color = ZodiaryColors.Purple, fontSize = 80.sp, fontFamily = FontFamily.Serif)
    }
}

@Composable
private fun SignMedallion(sign: ZodiacSign, modifier: Modifier = Modifier, compact: Boolean = false) {
    Box(
        modifier = modifier.semantics { contentDescription = "Zodiac sign ${sign.name}" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val signAccent = Color(sign.accent.toInt())
            drawCircle(color = signAccent.copy(alpha = 0.22f), radius = size.minDimension * 0.48f)
            drawCircle(color = ZodiaryColors.Lavender.copy(alpha = 0.45f), radius = size.minDimension * 0.37f, style = Stroke(width = 1.4.dp.toPx()))
            drawArc(
                color = signAccent.copy(alpha = 0.88f),
                startAngle = -50f,
                sweepAngle = 115f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(size.width * 0.82f, size.height * 0.82f),
                topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.09f, size.height * 0.09f),
            )
            drawCircle(color = ZodiaryColors.Gold, radius = size.minDimension * 0.025f, center = androidx.compose.ui.geometry.Offset(size.width * 0.77f, size.height * 0.25f))
        }
        Text(
            sign.symbol,
            color = ZodiaryColors.DeepPurple,
            fontSize = if (compact) 32.sp else 76.sp,
            fontFamily = FontFamily.Serif,
        )
    }
}

@Composable
private fun ChoiceSection(title: String, choices: List<String>, selected: Set<String>, onSelect: (String) -> Unit) {
    Column {
        Text(title, style = bodyStyle(15.sp, FontWeight.SemiBold), color = ZodiaryColors.Ink)
        Spacer(Modifier.height(8.dp))
        ChoiceGrid(choices, selected, onSelect, columns = 2)
    }
}

@Composable
private fun ChoiceGrid(choices: List<String>, selected: Set<String>, onSelect: (String) -> Unit, columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        choices.chunked(columns).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { choice ->
                    Box(Modifier.weight(1f)) {
                        FilterChip(
                            selected = choice in selected,
                            onClick = { onSelect(choice) },
                            label = {
                                Text(choice, maxLines = 1, overflow = TextOverflow.Ellipsis, style = bodyStyle(13.sp))
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            leadingIcon = if (choice in selected) {
                                { Text("✓", color = ZodiaryColors.Purple, fontSize = 13.sp) }
                            } else null,
                        )
                    }
                }
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ZodiaryColors.Purple,
            contentColor = ZodiaryColors.White,
            disabledContainerColor = ZodiaryColors.Purple.copy(alpha = 0.4f),
            disabledContentColor = ZodiaryColors.White,
        ),
    ) {
        Text(text, style = bodyStyle(16.sp, FontWeight.SemiBold))
    }
}

@Composable
private fun SignPickerDialog(selected: ZodiacSign, onDismiss: () -> Unit, onSelect: (ZodiacSign) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose your sign", style = displayStyle(23.sp), color = ZodiaryColors.Purple) },
        text = {
            Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState())) {
                ZodiaryViewModel.zodiacSigns.forEach { sign ->
                    TextButton(onClick = { onSelect(sign) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("${sign.symbol}   ${sign.name}${if (selected.name == sign.name) "  ✓" else ""}", modifier = Modifier.fillMaxWidth(), color = ZodiaryColors.Ink, style = bodyStyle(16.sp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = ZodiaryColors.Purple) } },
        containerColor = ZodiaryColors.Canvas,
    )
}

private fun displayStyle(size: androidx.compose.ui.unit.TextUnit) = androidx.compose.ui.text.TextStyle(
    fontFamily = FrauncesFontFamily,
    fontSize = size,
    fontWeight = FontWeight.Medium,
    lineHeight = size * 1.18f,
)

private fun bodyStyle(size: androidx.compose.ui.unit.TextUnit, weight: FontWeight = FontWeight.Normal) = androidx.compose.ui.text.TextStyle(
    fontFamily = DmSansFontFamily,
    fontSize = size,
    fontWeight = weight,
    lineHeight = size * 1.35f,
)

private fun readingTitle(reading: DailyReading): String = DailyReadingLabels.title(reading, LocalDate.now())

private fun formatProviderDate(date: LocalDate): String = date.format(
    DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()),
)

private fun formatInstant(instant: Instant): String = DateTimeFormatter.ofPattern(
    "MMM d, h:mm a", Locale.getDefault(),
).withZone(ZoneId.systemDefault()).format(instant)
