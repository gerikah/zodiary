package com.zodiary.app

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zodiary.app.data.DailyReading
import com.zodiary.app.data.DailyReadingLabels
import com.zodiary.app.data.MoonSnapshot
import com.zodiary.app.ui.theme.DmSansFontFamily
import com.zodiary.app.ui.theme.FrauncesFontFamily
import com.zodiary.app.ui.theme.ZodiaryColors
import com.zodiary.app.ui.theme.ZodiaryShapes
import com.zodiary.app.ui.theme.ZodiarySpacing
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

internal object ZodiaryReferenceScreens {
    @Composable
    fun Splash(onComplete: () -> Unit) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(900)
            onComplete()
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.72f))
            BrandLogo(width = 204.dp)
            Spacer(Modifier.height(49.dp))
            Text(
                "Dear universe,\nit’s me.",
                style = displayStyle(36.sp).copy(fontStyle = FontStyle.Italic, lineHeight = 41.sp),
                color = ZodiaryColors.DeepPurple,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(17.dp))
            Text("Your personal cosmic journal", style = bodyStyle(15.sp), color = ZodiaryColors.Ink)
            Spacer(Modifier.weight(1.28f))
        }
    }

    @Composable
    fun Welcome(model: ZodiaryScreenModel) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = ZodiarySpacing.ScreenInset),
        ) {
            Row(Modifier.fillMaxWidth().height(42.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TopTextAction("Skip →", onClick = { model.navigate(Destination.Birthday) })
            }
            Spacer(Modifier.height(75.dp))
            Text(
                "The universe is\nlistening. What’s\nyour story?",
                style = displayStyle(39.sp).copy(lineHeight = 40.sp),
                color = ZodiaryColors.Purple,
            )
            Spacer(Modifier.height(14.dp))
            Text("The stars inspire. You write.", style = bodyStyle(16.sp), color = ZodiaryColors.Ink)
            Spacer(Modifier.weight(1f))
            ProgressStars(active = 0)
            Spacer(Modifier.height(26.dp))
            GradientCta("Begin your story", onClick = { model.navigate(Destination.Birthday) })
            Spacer(Modifier.height(44.dp))
        }
    }

    @Composable
    fun Birthday(model: ZodiaryScreenModel) {
        var date by remember(model.birthDate) { mutableStateOf(model.birthDate ?: LocalDate.now()) }
        var touched by remember(model.birthDate) { mutableStateOf(model.birthDate != null) }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = ZodiarySpacing.ScreenInset),
        ) {
            TopTextAction("Back", onClick = { model.navigate(Destination.Welcome) }, modifier = Modifier.height(42.dp))
            Spacer(Modifier.height(58.dp))
            Text("Let’s find\nyour sign.", style = displayStyle(39.sp).copy(lineHeight = 40.sp), color = ZodiaryColors.Purple)
            Spacer(Modifier.height(8.dp))
            Text("Your birthday is where your cosmic\nstory begins.", style = bodyStyle(15.sp).copy(lineHeight = 19.sp), color = ZodiaryColors.Muted)
            Spacer(Modifier.height(34.dp))
            Text("When is your birthday?", style = bodyStyle(15.sp, FontWeight.SemiBold), color = ZodiaryColors.Ink)
            Spacer(Modifier.height(11.dp))
            WheelDatePicker(
                date = date,
                onDateChange = { date = it; touched = true },
            )
            Spacer(Modifier.height(12.dp))
            Text("We’ll use this to find your zodiac sign. Your full birthday isn’t saved.", style = bodyStyle(13.sp), color = ZodiaryColors.Muted)
            Spacer(Modifier.weight(1f))
            ProgressStars(active = 1)
            Spacer(Modifier.height(22.dp))
            GradientCta(
                "Discover my sign",
                onClick = {
                    model.discoverSignFromBirthday(date)
                    model.navigate(Destination.SignReveal)
                },
                enabled = touched,
            )
            Spacer(Modifier.height(43.dp))
        }
    }

    @Composable
    fun SignReveal(model: ZodiaryScreenModel) {
        var showSignPicker by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp).height(43.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TopTextAction("Back", onClick = { model.navigate(Destination.Birthday) })
                TopTextAction("Skip →", onClick = model::finishOnboarding, quiet = true)
            }
            Spacer(Modifier.height(57.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = ZodiarySpacing.ScreenInset)) {
                Text("You’re a ${model.sign.name}!", style = displayStyle(37.sp), color = ZodiaryColors.Purple)
                Spacer(Modifier.height(7.dp))
                Text("Your Zodiary just took on your colors.", style = bodyStyle(15.sp), color = ZodiaryColors.Muted)
            }
            Spacer(Modifier.weight(1f))
            SignArtwork(model.sign)
            Spacer(Modifier.weight(0.72f))
            Column(Modifier.fillMaxWidth().padding(horizontal = ZodiarySpacing.ScreenInset)) {
                ProgressStars(active = 2)
                Spacer(Modifier.height(24.dp))
                GradientCta("Explore my sign", onClick = { model.navigate(Destination.Preferences) })
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { showSignPicker = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                ) {
                    Text("Not quite right? Change my sign.", style = bodyStyle(14.sp), color = ZodiaryColors.Muted)
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        if (showSignPicker) {
            SignPickerDialog(
                selected = model.sign,
                onDismiss = { showSignPicker = false },
                onSelect = { model.selectSign(it); showSignPicker = false },
            )
        }
    }

    @Composable
    fun Preferences(model: ZodiaryScreenModel) {
        Column(Modifier.fillMaxSize().padding(horizontal = ZodiarySpacing.ScreenInset)) {
            Spacer(Modifier.height(112.dp))
            Text("Make it personal.", style = displayStyle(35.sp), color = ZodiaryColors.Purple)
            Spacer(Modifier.height(7.dp))
            Text("Choose what matters most to you.", style = bodyStyle(15.sp), color = ZodiaryColors.Muted)
            Spacer(Modifier.height(22.dp))
            PreferencePillGroup("What would you like to explore?", model.topicOptions, model.topics, model::changeTopic)
            Spacer(Modifier.height(17.dp))
            PreferencePillGroup("What brings you here?", model.focusOptions, setOf(model.focus), model::changeFocus)
            Spacer(Modifier.height(17.dp))
            PreferencePillGroup("Want a daily reminder?", model.reminderOptions, setOf(model.reminder), model::changeReminder)
            Spacer(Modifier.weight(1f))
            ProgressStars(active = 3)
            Spacer(Modifier.height(22.dp))
            GradientCta(
                if (model.preferencesReturnTo == Destination.Profile) "Save preferences" else "Save my preferences",
                onClick = model::savePreferences,
            )
            Spacer(Modifier.height(43.dp))
        }
    }

    @Composable
    fun Trust(onContinue: () -> Unit) {
        Column(Modifier.fillMaxSize().padding(horizontal = ZodiarySpacing.ScreenInset)) {
            Spacer(Modifier.height(44.dp))
            BrandLogo(width = 142.dp)
            Spacer(Modifier.height(56.dp))
            Text("A note before\nwe begin.", style = displayStyle(38.sp), color = ZodiaryColors.Purple)
            Spacer(Modifier.height(24.dp))
            LavenderCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Text("The stars offer a perspective; you decide what resonates.", style = displayStyle(23.sp), color = ZodiaryColors.DeepPurple)
                    Spacer(Modifier.height(14.dp))
                    Text("Readings are for reflection, not prediction or professional advice.", style = bodyStyle(15.sp), color = ZodiaryColors.Ink)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("Your journal is ready for its first page.", style = bodyStyle(15.sp), color = ZodiaryColors.Muted)
            Spacer(Modifier.weight(1f))
            GradientCta("Open my journal", onClick = onContinue)
            Spacer(Modifier.height(48.dp))
        }
    }

    @Composable
    fun Home(model: ZodiaryScreenModel) {
        val reading = model.currentDailyReading
        val overview = reading?.sections?.firstOrNull { it.title == "Overview" }?.body ?: reading?.content.orEmpty()
        val readingLines = reading?.content.orEmpty().trim().lines().map(String::trim).filter(String::isNotEmpty)
        val providerTitle = readingLines.firstOrNull()?.takeIf { line ->
            readingLines.size > 1 && line.length <= 48 &&
                !line.endsWith('.') && !line.endsWith('!') && !line.endsWith('?') && !line.endsWith(':') &&
                !line.startsWith("overview", ignoreCase = true)
        }
        val readingSummary = if (providerTitle != null) readingLines.drop(1).joinToString(" ") else overview
        Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 390.dp)
                    .background(ZodiaryColors.Purple, ZodiaryShapes.Hero)
                    .padding(horizontal = 25.dp, vertical = 22.dp),
            ) {
                Spacer(Modifier.height(75.dp))
                Text("Today’s reading", style = bodyStyle(14.sp, FontWeight.Bold), color = ZodiaryColors.Gold)
                Spacer(Modifier.height(11.dp))
                Text(
                    when {
                        providerTitle != null -> providerTitle
                        reading != null -> "A reading for ${reading.sign}"
                        else -> "Today’s reading"
                    },
                    style = displayStyle(28.sp).copy(lineHeight = 33.sp),
                    color = ZodiaryColors.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(7.dp))
                when {
                    reading != null -> {
                        Text(readingSummary, style = bodyStyle(14.sp).copy(lineHeight = 18.sp), color = ZodiaryColors.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(5.dp))
                        TextButton(
                            onClick = { model.navigate(Destination.Reading) },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                            modifier = Modifier.heightIn(min = 34.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = ZodiaryColors.Gold),
                        ) {
                            Text("Read today’s reading", style = bodyStyle(13.sp, FontWeight.SemiBold))
                        }
                        if (model.dailyRefreshError != null) {
                            Text("Update unavailable · showing the saved reading", style = bodyStyle(12.sp), color = ZodiaryColors.Lavender)
                        } else if (model.isDailyRefreshing) {
                            Text("Checking for a newer edition…", style = bodyStyle(12.sp), color = ZodiaryColors.Lavender)
                        }
                        CosmyDayAttribution(onDark = true)
                    }
                    model.isDailyLoading -> {
                        Text("Finding your daily reading…", style = bodyStyle(15.sp), color = ZodiaryColors.Lavender)
                        Spacer(Modifier.height(8.dp))
                        Text("Your journal is ready while we check the sky.", style = bodyStyle(14.sp), color = ZodiaryColors.White)
                    }
                    else -> {
                        Text("The daily reading isn’t available right now. Your journal is still here.", style = bodyStyle(14.sp), color = ZodiaryColors.White)
                        TextButton(onClick = { model.refreshDaily(force = true) }) {
                            Text("Try again", color = ZodiaryColors.Lavender, style = bodyStyle(14.sp, FontWeight.SemiBold))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                LavenderAction(label = "Write today’s page", onClick = { model.openJournal(LocalDate.now()) })
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = ZodiarySpacing.ScreenInset)) {
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SummaryPill("Sign · ${model.sign.name}", Modifier.weight(1f))
                    SummaryPill("Mood · ${model.mood.ifBlank { "Not set" }}", Modifier.weight(1f))
                    SummaryPill("Entries · ${model.entries.size}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(29.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                    LavenderCard(Modifier.weight(0.82f).heightIn(min = 88.dp)) {
                        Column(Modifier.fillMaxSize().padding(14.dp)) {
                            Text("Your sign", style = bodyStyle(11.sp, FontWeight.SemiBold), color = ZodiaryColors.DeepPurple)
                            Spacer(Modifier.height(7.dp))
                            Text("${model.sign.symbol} ${model.sign.name}", style = displayStyle(16.sp), color = ZodiaryColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    LavenderCard(Modifier.weight(1.25f).heightIn(min = 88.dp)) {
                        Column(Modifier.fillMaxSize().padding(14.dp)) {
                            Text("A prompt to begin with", style = bodyStyle(11.sp, FontWeight.SemiBold), color = ZodiaryColors.DeepPurple)
                            Spacer(Modifier.height(5.dp))
                            Text(model.currentPrompt.text, style = bodyStyle(12.sp).copy(lineHeight = 15.sp), color = ZodiaryColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = model::nextPrompt, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 32.dp)) {
                                Text("Try another prompt", style = bodyStyle(11.sp, FontWeight.SemiBold), color = ZodiaryColors.Purple)
                            }
                        }
                    }
                    LavenderCard(Modifier.weight(0.82f).heightIn(min = 88.dp)) {
                        Column(Modifier.fillMaxSize().padding(14.dp)) {
                            Text("Journal focus", style = bodyStyle(11.sp, FontWeight.SemiBold), color = ZodiaryColors.DeepPurple)
                            Spacer(Modifier.height(7.dp))
                            Text(model.focus, style = displayStyle(15.sp), color = ZodiaryColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.height(31.dp))
                LavenderCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                        Text("WORDS TO CARRY TODAY", style = bodyStyle(12.sp, FontWeight.Bold), color = ZodiaryColors.DeepPurple, letterSpacing = 0.4.sp)
                        Spacer(Modifier.height(10.dp))
                        Text("I can meet this day one honest moment at a time.", style = displayStyle(22.sp).copy(lineHeight = 27.sp), color = ZodiaryColors.DeepPurple)
                    }
                }
                reading?.moon?.let { moon ->
                    Spacer(Modifier.height(16.dp))
                    MoonSnapshotCard(reading.providerDate, moon)
                }
                Spacer(Modifier.height(25.dp))
            }
        }
    }

    @Composable
    fun Journal(model: ZodiaryScreenModel) {
        val isToday = model.journalDate == LocalDate.now()
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth()
                    .background(ZodiaryColors.Purple, ZodiaryShapes.Hero)
                    .padding(horizontal = 24.dp, vertical = 23.dp)
                    .heightIn(min = 174.dp),
            ) {
                Text(if (isToday) "TODAY’S PAGE" else "YOUR PAGE", style = bodyStyle(13.sp, FontWeight.Bold), color = ZodiaryColors.Gold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(8.dp))
                Text(if (isToday) "Today’s page" else "Your page", style = displayStyle(34.sp), color = ZodiaryColors.White)
                Spacer(Modifier.height(4.dp))
                Text(model.journalDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)), style = bodyStyle(14.sp), color = ZodiaryColors.Lavender)
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(21.dp))
                Text("How’s your inner universe?", style = bodyStyle(16.sp, FontWeight.SemiBold), color = ZodiaryColors.Ink)
                Spacer(Modifier.height(3.dp))
                Text("No right or wrong feelings. Just yours.", style = bodyStyle(13.sp), color = ZodiaryColors.Muted)
                Spacer(Modifier.height(12.dp))
                PillGrid(model.moodOptions, setOf(model.mood), model::changeMood, columns = 3, gold = true)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                    LavenderCard(Modifier.weight(1.18f).heightIn(min = 176.dp)) {
                        Column(Modifier.fillMaxSize().padding(16.dp)) {
                            Text(model.currentPrompt.sourceLabel(), style = bodyStyle(11.sp, FontWeight.SemiBold), color = ZodiaryColors.Purple)
                            Spacer(Modifier.height(10.dp))
                            Text(model.currentPrompt.text, style = displayStyle(16.sp).copy(lineHeight = 20.sp), color = ZodiaryColors.Ink, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = model::nextPrompt, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 36.dp)) {
                                Text("Try another prompt →", style = bodyStyle(12.sp, FontWeight.SemiBold), color = ZodiaryColors.Purple)
                            }
                        }
                    }
                    LavenderCard(Modifier.weight(0.78f).heightIn(min = 176.dp)) {
                        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
                            Text("A moment, kept", style = bodyStyle(11.sp, FontWeight.SemiBold), color = ZodiaryColors.DeepPurple)
                            Spacer(Modifier.height(9.dp))
                            Text(
                                when {
                                    model.entries.containsKey(model.journalDate) -> "This day has a saved reflection."
                                    model.journalText.isNotBlank() -> "Your draft stays on this device as you write."
                                    else -> "Every story starts with a blank page."
                                },
                                style = displayStyle(15.sp).copy(lineHeight = 20.sp),
                                color = ZodiaryColors.Ink,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                LavenderCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(17.dp)) {
                        Text("YOUR REFLECTION", style = bodyStyle(11.sp, FontWeight.Bold), color = ZodiaryColors.DeepPurple, letterSpacing = 0.4.sp)
                        Spacer(Modifier.height(9.dp))
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 168.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(ZodiaryColors.White)
                                .padding(15.dp),
                        ) {
                            BasicTextField(
                                value = model.journalText,
                                onValueChange = model::editJournalText,
                                modifier = Modifier.fillMaxSize(),
                                textStyle = bodyStyle(15.sp).copy(color = ZodiaryColors.Ink, lineHeight = 23.sp),
                                cursorBrush = Brush.verticalGradient(listOf(ZodiaryColors.Purple, ZodiaryColors.DeepPurple)),
                                decorationBox = { innerTextField ->
                                    if (model.journalText.isBlank()) {
                                        Text("Dear universe, today I…", style = bodyStyle(15.sp), color = ZodiaryColors.Muted)
                                    }
                                    innerTextField()
                                },
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            model.saveMessage.ifBlank { "Draft saved on this device as you write." },
                            style = bodyStyle(12.sp),
                            color = ZodiaryColors.Muted,
                        )
                        Spacer(Modifier.height(14.dp))
                        GradientCta("Save entry", onClick = model::saveEntry, enabled = model.journalText.isNotBlank())
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }
    }

    @Composable
    fun Calendar(model: ZodiaryScreenModel) {
        var month by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
        val firstDay = month.withDayOfMonth(1)
        val leadingDays = firstDay.dayOfWeek.value % 7
        val dates = (0 until 42).map { firstDay.plusDays((it - leadingDays).toLong()) }
        Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth()
                    .background(ZodiaryColors.Purple, ZodiaryShapes.Hero)
                    .padding(horizontal = 24.dp)
                    .padding(top = 25.dp, bottom = 24.dp),
            ) {
                Spacer(Modifier.height(22.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    shape = ZodiaryShapes.CardLarge,
                    color = ZodiaryColors.FormLavender,
                    shadowElevation = 5.dp,
                ) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 15.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(month.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)), modifier = Modifier.weight(1f), style = displayStyle(24.sp), color = ZodiaryColors.Ink)
                            IconButton(onClick = {
                                month = month.minusMonths(1)
                                model.selectCalendarDate(month)
                            }, modifier = Modifier.size(42.dp)) {
                                Text("‹", style = displayStyle(31.sp), color = ZodiaryColors.Purple, modifier = Modifier.semantics { contentDescription = "Previous month" })
                            }
                            IconButton(onClick = {
                                val next = month.plusMonths(1)
                                if (!next.isAfter(LocalDate.now().withDayOfMonth(1))) {
                                    month = next
                                    model.selectCalendarDate(month)
                                }
                            }, enabled = month.isBefore(LocalDate.now().withDayOfMonth(1)), modifier = Modifier.size(42.dp)) {
                                Text("›", style = displayStyle(31.sp), color = ZodiaryColors.Purple, modifier = Modifier.semantics { contentDescription = "Next month" })
                            }
                        }
                        Text(month.year.toString(), style = bodyStyle(12.sp), color = ZodiaryColors.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 9.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                                Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = bodyStyle(12.sp, FontWeight.SemiBold), color = ZodiaryColors.Muted)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        dates.chunked(7).forEach { week ->
                            Row(Modifier.fillMaxWidth()) {
                                week.forEach { date ->
                                    CalendarDateCell(
                                        date = date,
                                        month = month,
                                        selected = date == model.selectedDate,
                                        today = date == LocalDate.now(),
                                        entry = model.entries[date],
                                        enabled = !date.isAfter(LocalDate.now()),
                                        onClick = { model.selectCalendarDate(date) },
                                        modifier = Modifier.weight(1f).height(49.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text("Marked days hold a saved reflection.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = bodyStyle(11.sp), color = ZodiaryColors.Muted)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(23.dp))
                val entry = model.entries[model.selectedDate]
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                    LavenderCard(Modifier.weight(0.78f).heightIn(min = 156.dp)) {
                        Column(Modifier.fillMaxSize().padding(16.dp)) {
                            Text("YOUR MOOD", style = bodyStyle(11.sp, FontWeight.Bold), color = ZodiaryColors.DeepPurple)
                            Spacer(Modifier.height(10.dp))
                            Text(entry?.mood?.takeIf(String::isNotBlank) ?: "No mood saved", style = displayStyle(20.sp), color = ZodiaryColors.Ink)
                            Spacer(Modifier.height(5.dp))
                            Text(model.selectedDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)), style = bodyStyle(12.sp), color = ZodiaryColors.Muted)
                        }
                    }
                    LavenderCard(Modifier.weight(1.22f).heightIn(min = 156.dp)) {
                        Column(Modifier.fillMaxSize().padding(16.dp)) {
                            Text("YOUR REFLECTION", style = bodyStyle(11.sp, FontWeight.Bold), color = ZodiaryColors.DeepPurple)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                entry?.text ?: "Every story starts with a blank page.",
                                style = bodyStyle(13.sp).copy(lineHeight = 18.sp),
                                color = ZodiaryColors.Ink,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { model.openJournal(model.selectedDate) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 34.dp)) {
                                Text(if (entry == null) "Write for this day →" else "Open reflection →", style = bodyStyle(11.sp, FontWeight.SemiBold), color = ZodiaryColors.Purple)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }
    }

    @Composable
    fun Profile(model: ZodiaryScreenModel) {
        var showSignPicker by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth()
                    .height(205.dp)
                    .background(ZodiaryColors.Purple, ZodiaryShapes.Hero)
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("YOUR SPACE", style = bodyStyle(13.sp, FontWeight.Bold), color = ZodiaryColors.Gold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(7.dp))
                Text("Your space", style = displayStyle(34.sp), color = ZodiaryColors.White)
                Text("Make this journal feel like yours.", style = bodyStyle(14.sp), color = ZodiaryColors.Lavender)
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(23.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                    LavenderCard(Modifier.weight(0.76f).heightIn(min = 188.dp)) {
                        Column(Modifier.fillMaxSize().padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("YOUR SIGN", style = bodyStyle(10.sp, FontWeight.Bold), color = ZodiaryColors.DeepPurple)
                            Spacer(Modifier.height(12.dp))
                            SignGlyph(model.sign, Modifier.size(58.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(model.sign.name, style = displayStyle(19.sp), color = ZodiaryColors.Ink)
                            TextButton(onClick = { showSignPicker = true }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.heightIn(min = 34.dp)) {
                                Text("Change sign", style = bodyStyle(11.sp, FontWeight.SemiBold), color = ZodiaryColors.Purple)
                            }
                        }
                    }
                    LavenderCard(Modifier.weight(1.24f).heightIn(min = 188.dp)) {
                        Column(Modifier.fillMaxSize().padding(15.dp)) {
                            Text("YOUR PREFERENCES", style = bodyStyle(10.sp, FontWeight.Bold), color = ZodiaryColors.DeepPurple)
                            Spacer(Modifier.height(12.dp))
                            Text("Topics", style = bodyStyle(11.sp, FontWeight.SemiBold), color = ZodiaryColors.Muted)
                            Text(model.topics.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "General prompts", style = bodyStyle(13.sp), color = ZodiaryColors.Ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(9.dp))
                            Text("Journal focus", style = bodyStyle(11.sp, FontWeight.SemiBold), color = ZodiaryColors.Muted)
                            Text(model.focus, style = bodyStyle(13.sp), color = ZodiaryColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = model::editPreferences, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 34.dp)) {
                                Text("Edit preferences →", style = bodyStyle(11.sp, FontWeight.SemiBold), color = ZodiaryColors.Purple)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(17.dp))
                LavenderCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("DAILY REMINDER", style = bodyStyle(11.sp, FontWeight.Bold), color = ZodiaryColors.DeepPurple)
                                Spacer(Modifier.height(4.dp))
                                Text(model.reminder, style = displayStyle(19.sp), color = ZodiaryColors.Ink)
                            }
                            TextButton(onClick = model::editPreferences) {
                                Text("Edit", style = bodyStyle(12.sp, FontWeight.SemiBold), color = ZodiaryColors.Purple)
                            }
                        }
                        Text("Reminder timing is saved as a preference; notifications aren’t active yet.", style = bodyStyle(12.sp), color = ZodiaryColors.Muted)
                    }
                }
                Spacer(Modifier.height(17.dp))
                AccountPanel(
                    model = model,
                    onLogin = { model.navigate(Destination.Login) },
                    onRegister = { model.navigate(Destination.Register) },
                )
                Spacer(Modifier.height(25.dp))
            }
        }
        if (showSignPicker) {
            SignPickerDialog(model.sign, onDismiss = { showSignPicker = false }) {
                model.selectSign(it)
                showSignPicker = false
            }
        }
    }

    @Composable
    fun Reading(model: ZodiaryScreenModel, onBack: () -> Unit) {
        val reading = model.currentDailyReading
        Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().background(ZodiaryColors.Purple, ZodiaryShapes.Hero).padding(horizontal = 24.dp, vertical = 19.dp),
            ) {
                TopTextAction("‹  Home", onClick = onBack, color = ZodiaryColors.White)
                Spacer(Modifier.height(29.dp))
                Text("TODAY’S READING", style = bodyStyle(12.sp, FontWeight.Bold), color = ZodiaryColors.Gold)
                Spacer(Modifier.height(8.dp))
                Text(reading?.let { DailyReadingLabels.title(it, LocalDate.now()) } ?: "Today’s reading", style = displayStyle(31.sp), color = ZodiaryColors.White)
                Spacer(Modifier.height(9.dp))
                Text("A fresh perspective for ${reading?.sign ?: model.sign.name}.", style = bodyStyle(15.sp), color = ZodiaryColors.Lavender)
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(20.dp))
                if (reading == null) {
                    LavenderCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text(if (model.isDailyLoading) "Finding your daily reading…" else "The daily reading isn’t available right now.", style = displayStyle(22.sp), color = ZodiaryColors.DeepPurple)
                            Spacer(Modifier.height(10.dp))
                            Text("Your journal remains available while you’re offline.", style = bodyStyle(14.sp), color = ZodiaryColors.Muted)
                            if (!model.isDailyLoading) TextButton(onClick = { model.refreshDaily(force = true) }) { Text("Try again", color = ZodiaryColors.Purple) }
                        }
                    }
                } else {
                    Text("Content date · ${reading.providerDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH))}", style = bodyStyle(13.sp, FontWeight.SemiBold), color = ZodiaryColors.DeepPurple)
                    reading.generatedAt?.let { Text("Provider generated · ${DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH).withZone(java.time.ZoneId.systemDefault()).format(it)}", style = bodyStyle(12.sp), color = ZodiaryColors.Muted) }
                    model.dailyFetchedAt?.let { Text("Fetched · ${DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH).withZone(java.time.ZoneId.systemDefault()).format(it)}", style = bodyStyle(12.sp), color = ZodiaryColors.Muted) }
                    if (model.dailyRefreshError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("The update isn’t available. You’re viewing the saved reading for ${reading.providerDate}.", style = bodyStyle(13.sp), color = ZodiaryColors.DeepPurple)
                    }
                    Spacer(Modifier.height(19.dp))
                    reading.sections.forEach { section ->
                        section.title?.let { Text(it, style = displayStyle(22.sp), color = ZodiaryColors.Purple) }
                        if (section.title != null) Spacer(Modifier.height(5.dp))
                        Text(section.body, style = bodyStyle(16.sp).copy(lineHeight = 24.sp), color = ZodiaryColors.Ink)
                        Spacer(Modifier.height(17.dp))
                    }
                    reading.closing?.takeIf(String::isNotBlank)?.let {
                        Text("A closing thought", style = displayStyle(21.sp), color = ZodiaryColors.Purple)
                        Spacer(Modifier.height(5.dp))
                        Text(it, style = bodyStyle(16.sp).copy(lineHeight = 24.sp), color = ZodiaryColors.Ink)
                    }
                    Spacer(Modifier.height(10.dp))
                    CosmyDayAttribution(onDark = false)
                    Text("The stars offer a perspective; you decide what resonates.", style = bodyStyle(14.sp), color = ZodiaryColors.Muted)
                    Spacer(Modifier.height(6.dp))
                    Text("Readings are for reflection, not prediction or professional advice.", style = bodyStyle(14.sp), color = ZodiaryColors.Muted)
                }
                Spacer(Modifier.height(20.dp))
                GradientCta("Write today’s page", onClick = { model.openJournal(LocalDate.now()) })
                Spacer(Modifier.height(25.dp))
            }
        }
    }

    @Composable
    fun Login(model: ZodiaryScreenModel) = AuthScreen(model, registering = false)

    @Composable
    fun Register(model: ZodiaryScreenModel) = AuthScreen(model, registering = true)

    @Composable
    fun Navigation(destination: Destination, onNavigate: (Destination) -> Unit) {
        val selected = when (destination) {
            Destination.Journal -> Destination.Journal
            Destination.Calendar -> Destination.Calendar
            Destination.Profile -> Destination.Profile
            else -> Destination.Home
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(ZodiaryColors.White).navigationBarsPadding().height(66.dp).padding(horizontal = 25.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationItem("Home", selected == Destination.Home, Icons.Outlined.Home) { onNavigate(Destination.Home) }
            NavigationItem("Journal", selected == Destination.Journal, Icons.Outlined.Edit) { onNavigate(Destination.Journal) }
            NavigationItem("Calendar", selected == Destination.Calendar, Icons.Outlined.CalendarMonth) { onNavigate(Destination.Calendar) }
            NavigationItem("Profile", selected == Destination.Profile, Icons.Outlined.PersonOutline) { onNavigate(Destination.Profile) }
        }
    }
}

@Composable
private fun AuthScreen(model: ZodiaryScreenModel, registering: Boolean) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    val emailValid = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())
    val passwordValid = password.length >= 8
    val confirmationValid = !registering || confirmPassword == password
    val canSubmit = emailValid && passwordValid && confirmationValid && model.isSupabaseConfigured && !model.isAccountBusy && !model.isAccountInitializing

    LaunchedEffect(model.accountEmail) {
        if (model.accountEmail != null) model.navigate(Destination.Profile)
    }

    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TopTextAction("Back", onClick = { model.navigate(Destination.Profile) })
                if (registering) TopTextAction("Skip →", onClick = { model.navigate(Destination.Profile) }, quiet = true)
            }
            Spacer(Modifier.height(if (registering) 48.dp else 25.dp))
            Text(if (registering) "Start somewhere" else "Welcome back!", style = displayStyle(37.sp), color = ZodiaryColors.Purple)
            Spacer(Modifier.height(6.dp))
            Text(
                if (registering) "A space for your stars, thoughts, and everything in between." else "Your journal is right where you left it.",
                style = bodyStyle(13.sp),
                color = ZodiaryColors.Muted,
            )
            Spacer(Modifier.height(25.dp))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 29.dp, topEnd = 29.dp),
            color = ZodiaryColors.FormLavender,
            shadowElevation = 5.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 23.dp)) {
                Text(if (registering) "Sign up" else "Login", style = displayStyle(23.sp), color = ZodiaryColors.Purple)
                Spacer(Modifier.height(17.dp))
                AuthField("Email address", email, { email = it }, "you@example.com", email = true)
                if (submitted && !emailValid) AuthValidation("Enter a valid email address.")
                Spacer(Modifier.height(13.dp))
                AuthField(
                    "Password",
                    password,
                    { password = it },
                    if (registering) "Create password" else "Enter your password",
                    passwordVisible = passwordVisible,
                    onTogglePassword = { passwordVisible = !passwordVisible },
                )
                if (registering) {
                    if (submitted && !passwordValid) AuthValidation("Use at least 8 characters.")
                    Spacer(Modifier.height(13.dp))
                    AuthField(
                        "Confirm password",
                        confirmPassword,
                        { confirmPassword = it },
                        "Enter your password again",
                        passwordVisible = confirmVisible,
                        onTogglePassword = { confirmVisible = !confirmVisible },
                    )
                    if (submitted && !confirmationValid) AuthValidation("Passwords don’t match.")
                } else {
                    Spacer(Modifier.height(5.dp))
                    Text("Password recovery isn’t available yet.", style = bodyStyle(11.sp), color = ZodiaryColors.Muted)
                    if (submitted && !passwordValid) AuthValidation("Enter your password.")
                }
                if (!model.isSupabaseConfigured) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        model.supabaseConfigurationError ?: "Account sync isn’t configured. Guest journaling remains available on this device.",
                        style = bodyStyle(13.sp).copy(lineHeight = 18.sp),
                        color = ZodiaryColors.Muted,
                    )
                }
                if (model.isAccountInitializing) {
                    Spacer(Modifier.height(10.dp))
                    Text("Checking your account…", style = bodyStyle(12.sp), color = ZodiaryColors.Muted)
                }
                if (model.accountMessage.isNotBlank() && model.accountMessage != "Your journal is saved on this device. Connect an account whenever you’re ready to sync it.") {
                    Spacer(Modifier.height(11.dp))
                    Text(
                        model.accountMessage,
                        style = bodyStyle(12.sp).copy(lineHeight = 17.sp),
                        color = if (looksLikeError(model.accountMessage)) Color(0xFF9C3E4B) else ZodiaryColors.DeepPurple,
                    )
                }
                Spacer(Modifier.height(19.dp))
                GradientCta(
                    if (model.isAccountBusy) "Please wait…" else if (registering) "Register" else "Login",
                    onClick = {
                        submitted = true
                        if (canSubmit) {
                            if (registering) model.createAccount(email.trim(), password) else model.signIn(email.trim(), password)
                        }
                    },
                    enabled = canSubmit,
                )
                Spacer(Modifier.height(13.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (registering) "Already have an account?" else "Not registered yet?", style = bodyStyle(12.sp), color = ZodiaryColors.Ink)
                    TextButton(onClick = { model.navigate(if (registering) Destination.Login else Destination.Register) }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text(if (registering) "Log In" else "Sign Up", style = bodyStyle(12.sp, FontWeight.SemiBold), color = ZodiaryColors.Purple)
                    }
                }
                TextButton(onClick = { model.navigate(Destination.Profile) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Continue as a guest", style = bodyStyle(12.sp), color = ZodiaryColors.Muted)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AccountPanel(model: ZodiaryScreenModel, onLogin: () -> Unit, onRegister: () -> Unit) {
    Surface(shape = ZodiaryShapes.CardLarge, color = ZodiaryColors.FormLavender, modifier = Modifier.fillMaxWidth(), shadowElevation = 3.dp) {
        Column(Modifier.padding(18.dp)) {
            Text("YOUR ACCOUNT", style = bodyStyle(11.sp, FontWeight.Bold), color = ZodiaryColors.DeepPurple)
            Spacer(Modifier.height(8.dp))
            when {
                model.isAccountInitializing -> Text("Checking your account…", style = bodyStyle(14.sp), color = ZodiaryColors.Muted)
                model.accountEmail != null -> {
                    Text("Signed in as ${model.accountEmail}", style = bodyStyle(14.sp, FontWeight.SemiBold), color = ZodiaryColors.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text("Your entries and preferences sync when saved. A local copy stays on this device.", style = bodyStyle(12.sp), color = ZodiaryColors.Muted)
                    if (model.accountMessage.isNotBlank()) Text(model.accountMessage, style = bodyStyle(12.sp), color = ZodiaryColors.Purple)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = model::syncAccount, enabled = !model.isAccountBusy) { Text(if (model.isAccountBusy) "Syncing…" else "Sync now", color = ZodiaryColors.Purple) }
                        TextButton(onClick = model::signOutAccount, enabled = !model.isAccountBusy) { Text("Sign out", color = ZodiaryColors.Purple) }
                    }
                }
                !model.isSupabaseConfigured -> Text(
                    model.supabaseConfigurationError ?: "Your journal is saved on this device. Connect Supabase to turn on account sync.",
                    style = bodyStyle(13.sp).copy(lineHeight = 18.sp), color = ZodiaryColors.Muted,
                )
                else -> {
                    Text("Start as a guest, then sign in when you want your journal synced across devices.", style = bodyStyle(13.sp).copy(lineHeight = 18.sp), color = ZodiaryColors.Muted)
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        GradientCta("Sign in", onClick = onLogin, modifier = Modifier.weight(1f))
                        GradientCta("Create account", onClick = onRegister, modifier = Modifier.weight(1.25f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    email: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
) {
    Text(label, style = bodyStyle(13.sp, FontWeight.Medium), color = ZodiaryColors.Ink)
    Spacer(Modifier.height(5.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 53.dp),
        singleLine = true,
        placeholder = { Text(placeholder, style = bodyStyle(13.sp), color = ZodiaryColors.Muted) },
        leadingIcon = {
            Icon(if (email) Icons.Outlined.Email else Icons.Outlined.Lock, contentDescription = null, tint = ZodiaryColors.Muted, modifier = Modifier.size(19.dp))
        },
        trailingIcon = onTogglePassword?.let {
            {
                IconButton(onClick = it) {
                    Icon(
                        if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = ZodiaryColors.Muted,
                    )
                }
            }
        },
        visualTransformation = if (onTogglePassword != null && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (email) KeyboardType.Email else KeyboardType.Password),
        shape = ZodiaryShapes.Input,
        textStyle = bodyStyle(14.sp).copy(color = ZodiaryColors.Ink),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ZodiaryColors.White,
            unfocusedContainerColor = ZodiaryColors.White,
            focusedBorderColor = ZodiaryColors.Purple,
            unfocusedBorderColor = Color(0xFFEAE5EF),
            cursorColor = ZodiaryColors.Purple,
        ),
    )
}

@Composable
private fun AuthValidation(message: String) {
    Text(message, modifier = Modifier.padding(top = 4.dp), style = bodyStyle(11.sp), color = Color(0xFF9C3E4B))
}

@Composable
private fun WheelDatePicker(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    val years = remember { (LocalDate.now().year downTo LocalDate.now().year - 120).toList() }
    val months = remember { (1..12).toList() }
    val days = (1..YearMonth.of(date.year, date.monthValue).lengthOfMonth()).toList()
    val dateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
    Surface(
        modifier = Modifier.fillMaxWidth().height(176.dp),
        shape = RoundedCornerShape(21.dp),
        color = ZodiaryColors.FormLavender,
        shadowElevation = 5.dp,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 13.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WheelColumn(
                label = "Month",
                values = months,
                selectedIndex = date.monthValue - 1,
                modifier = Modifier.weight(1.15f),
                valueLabel = { month -> LocalDate.of(2000, month, 1).format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)) },
                onSelectIndex = { index -> onDateChange(date.withMonth(index + 1).coerceDayOfMonth()) },
            )
            WheelColumn(
                label = "Day",
                values = days,
                selectedIndex = date.dayOfMonth - 1,
                modifier = Modifier.weight(0.72f),
                valueLabel = Int::toString,
                onSelectIndex = { index -> onDateChange(date.withDayOfMonth(index + 1)) },
            )
            WheelColumn(
                label = "Year",
                values = years,
                selectedIndex = years.indexOf(date.year).coerceAtLeast(0),
                modifier = Modifier.weight(1.08f),
                valueLabel = Int::toString,
                onSelectIndex = { index -> onDateChange(date.withYear(years[index]).coerceDayOfMonth()) },
            )
        }
    }
    Text(date.format(dateFormat), modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center, style = bodyStyle(11.sp), color = ZodiaryColors.Purple)
}

@Composable
private fun <T> WheelColumn(
    label: String,
    values: List<T>,
    selectedIndex: Int,
    modifier: Modifier,
    valueLabel: (T) -> String,
    onSelectIndex: (Int) -> Unit,
) {
    val latestSelection by rememberUpdatedState(selectedIndex)
    val latestOnSelect by rememberUpdatedState(onSelectIndex)
    val dragThreshold = with(LocalDensity.current) { 25.dp.toPx() }
    Box(
        modifier = modifier.height(152.dp)
            .semantics { contentDescription = "$label picker, ${valueLabel(values[selectedIndex.coerceIn(values.indices)])}" }
            .pointerInput(values.size) {
                var accumulated = 0f
                detectVerticalDragGestures(
                    onDragEnd = { accumulated = 0f },
                    onDragCancel = { accumulated = 0f },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        accumulated += amount
                        var current = latestSelection
                        while (accumulated >= dragThreshold) {
                            current = (current - 1).coerceAtLeast(0)
                            latestOnSelect(current)
                            accumulated -= dragThreshold
                        }
                        while (accumulated <= -dragThreshold) {
                            current = (current + 1).coerceAtMost(values.lastIndex)
                            latestOnSelect(current)
                            accumulated += dragThreshold
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            WheelValue(values.getOrNull(selectedIndex - 1)?.let(valueLabel), secondary = true, onClick = { onSelectIndex((selectedIndex - 1).coerceAtLeast(0)) })
            Box(
                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(13.dp)).background(ZodiaryColors.White.copy(alpha = 0.86f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(valueLabel(values[selectedIndex.coerceIn(values.indices)]), style = displayStyle(22.sp), color = ZodiaryColors.DeepPurple, maxLines = 1)
            }
            WheelValue(values.getOrNull(selectedIndex + 1)?.let(valueLabel), secondary = true, onClick = { onSelectIndex((selectedIndex + 1).coerceAtMost(values.lastIndex)) })
        }
    }
}

@Composable
private fun WheelValue(value: String?, secondary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(43.dp).clickable(enabled = value != null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        value?.let {
            Text(it, style = bodyStyle(17.sp, FontWeight.Medium), color = ZodiaryColors.DeepPurple.copy(alpha = if (secondary) 0.56f else 1f), maxLines = 1)
        }
    }
}

@Composable
private fun SignArtwork(sign: ZodiacSign) {
    if (sign.name == "Libra") {
        BoxWithConstraints(Modifier.fillMaxWidth().height(204.dp).clipToBounds().semantics { contentDescription = "Illustrated Libra mascot" }) {
            val scale = maxWidth / 390f
            Image(
                painter = painterResource(R.drawable.zodiary_sign_reveal_reference),
                contentDescription = null,
                modifier = Modifier.width(maxWidth).height(844.dp * (maxWidth.value / 390f)).offset(y = -396.dp * (maxWidth.value / 390f)),
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
            )
        }
    } else {
        Box(Modifier.fillMaxWidth().height(204.dp), contentAlignment = Alignment.Center) {
            SignGlyph(sign, Modifier.size(184.dp))
        }
    }
}

@Composable
private fun SignGlyph(sign: ZodiacSign, modifier: Modifier = Modifier) {
    Box(modifier.semantics { contentDescription = "${sign.name} zodiac sign" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val accent = Color(sign.accent.toInt())
            drawCircle(accent.copy(alpha = 0.18f), radius = size.minDimension * 0.48f)
            drawCircle(ZodiaryColors.Lavender.copy(alpha = 0.7f), radius = size.minDimension * 0.37f, style = Stroke(width = 1.5.dp.toPx()))
            drawArc(accent, -48f, 118f, false, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round), size = androidx.compose.ui.geometry.Size(size.width * 0.82f, size.height * 0.82f), topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.09f, size.height * 0.09f))
            drawCircle(ZodiaryColors.Gold, radius = size.minDimension * 0.025f, center = androidx.compose.ui.geometry.Offset(size.width * 0.76f, size.height * 0.24f))
        }
        Text(sign.symbol, style = displayStyle(58.sp), color = ZodiaryColors.DeepPurple)
    }
}

@Composable
private fun CalendarDateCell(
    date: LocalDate,
    month: LocalDate,
    selected: Boolean,
    today: Boolean,
    entry: JournalEntry?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inMonth = date.month == month.month
    Column(
        modifier = modifier
            .padding(1.dp)
            .clip(CircleShape)
            .then(if (today && !selected) Modifier.border(1.dp, ZodiaryColors.Purple.copy(alpha = 0.55f), CircleShape) else Modifier)
            .background(if (selected) ZodiaryColors.Purple else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button)
            .semantics {
                contentDescription = date.format(DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH)) + if (entry != null) ", saved journal entry" else ""
                if (selected) stateDescription = "Selected"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = bodyStyle(13.sp, if (today || entry != null || selected) FontWeight.SemiBold else FontWeight.Normal),
            color = when {
                selected -> ZodiaryColors.White
                !inMonth -> ZodiaryColors.Muted.copy(alpha = 0.4f)
                !enabled -> ZodiaryColors.Muted.copy(alpha = 0.45f)
                else -> ZodiaryColors.Ink
            },
        )
        Box(Modifier.padding(top = 1.dp).size(4.dp).clip(CircleShape).background(when {
            selected && entry != null -> ZodiaryColors.Gold
            entry != null -> ZodiaryColors.Purple
            else -> Color.Transparent
        }))
    }
}

@Composable
private fun PreferencePillGroup(title: String, options: List<String>, selected: Set<String>, onSelect: (String) -> Unit) {
    Text(title, style = bodyStyle(14.sp, FontWeight.SemiBold), color = ZodiaryColors.Ink)
    Spacer(Modifier.height(8.dp))
    PillGrid(options, selected, onSelect, columns = 3, gold = false)
}

@Composable
private fun PillGrid(options: List<String>, selected: Set<String>, onSelect: (String) -> Unit, columns: Int, gold: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        options.chunked(columns).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { option ->
                    SelectionPill(
                        option = option,
                        selected = option in selected,
                        onClick = { onSelect(option) },
                        modifier = Modifier.weight(1f),
                        gold = gold,
                    )
                }
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SelectionPill(option: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, gold: Boolean = false) {
    val fill = when {
        gold -> if (selected) ZodiaryColors.Gold else ZodiaryColors.Gold.copy(alpha = 0.48f)
        selected -> ZodiaryColors.Lavender
        else -> ZodiaryColors.FormLavender
    }
    Surface(
        modifier = modifier.heightIn(min = if (gold) 37.dp else 33.dp),
        onClick = onClick,
        enabled = true,
        shape = ZodiaryShapes.Pill,
        color = fill,
        border = if (selected && !gold) BorderStroke(1.4.dp, ZodiaryColors.Purple) else null,
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
            Text(option, style = bodyStyle(if (gold) 11.sp else 11.sp, if (selected) FontWeight.SemiBold else FontWeight.Medium), color = ZodiaryColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ProgressStars(active: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { index ->
            Text(
                if (index == active) "✦" else "✧",
                style = bodyStyle(if (index == active) 18.sp else 17.sp),
                color = if (index == active) ZodiaryColors.Gold else ZodiaryColors.Lavender.copy(alpha = 0.9f),
                modifier = Modifier.padding(end = 8.dp).semantics { contentDescription = if (index == active) "Step ${index + 1} of 4" else "" },
            )
        }
    }
}

@Composable
private fun BrandLogo(width: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.zodiary_logo),
        contentDescription = "Zodiary",
        modifier = modifier.width(width).height(width * 0.23f),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
    )
}

@Composable
private fun PurpleHero(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(ZodiaryColors.Purple, ZodiaryShapes.Hero).padding(horizontal = 24.dp, vertical = 20.dp),
        content = content,
    )
}

@Composable
private fun LavenderCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = ZodiaryShapes.CardLarge,
        color = ZodiaryColors.Lavender,
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun GradientCta(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = ZodiaryShapes.Pill
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(Color(0xFFB49387), Color(0xFF765994), ZodiaryColors.DeepPurple))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFFBCA9A2), Color(0xFF827A8D)))
    }
    Box(
        modifier = modifier.fillMaxWidth().height(ZodiarySpacing.ActionHeight).shadow(if (enabled) 3.dp else 0.dp, shape)
            .clip(shape).background(brush).border(3.dp, ZodiaryColors.Purple, shape)
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = bodyStyle(15.sp, FontWeight.SemiBold), color = ZodiaryColors.White)
    }
}

@Composable
private fun LavenderAction(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(62.dp).clip(CircleShape).background(ZodiaryColors.Lavender)
            .clickable(onClick = onClick, role = Role.Button).semantics { contentDescription = label },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = bodyStyle(14.sp, FontWeight.SemiBold), color = ZodiaryColors.DeepPurple)
        Spacer(Modifier.width(10.dp))
        Text("↗", style = displayStyle(23.sp), color = ZodiaryColors.DeepPurple)
    }
}

@Composable
private fun SummaryPill(text: String, modifier: Modifier = Modifier) {
    Surface(modifier.heightIn(min = 37.dp), shape = CircleShape, color = ZodiaryColors.Gold) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
            Text(text, style = bodyStyle(10.sp, FontWeight.SemiBold), color = ZodiaryColors.DeepPurple, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MoonSnapshotCard(date: LocalDate, moon: MoonSnapshot) {
    LavenderCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(17.dp)) {
            Text("MOON SNAPSHOT · ${date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)).uppercase(Locale.ENGLISH)}", style = bodyStyle(11.sp, FontWeight.Bold), color = ZodiaryColors.DeepPurple)
            Spacer(Modifier.height(6.dp))
            moon.sign?.let { Text("Moon in $it", style = displayStyle(19.sp), color = ZodiaryColors.Ink) }
            val phase = moon.phaseName ?: moon.phase?.replace('_', ' ')?.replaceFirstChar { it.titlecase(Locale.ENGLISH) }
            phase?.let { Text(it, style = bodyStyle(13.sp), color = ZodiaryColors.Ink) }
            moon.illumination?.let { Text("${"%.1f".format(Locale.ENGLISH, it)}% illuminated", style = bodyStyle(12.sp), color = ZodiaryColors.Muted) }
            CosmyDayAttribution(onDark = false)
        }
    }
}

@Composable
private fun CosmyDayAttribution(onDark: Boolean) {
    val uriHandler = LocalUriHandler.current
    TextButton(
        onClick = { runCatching { uriHandler.openUri("https://cosmyday.com") } },
        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp),
        modifier = Modifier.heightIn(min = 31.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = if (onDark) ZodiaryColors.Lavender else ZodiaryColors.Purple),
    ) {
        Text("Astrology data by CosmyDay", style = bodyStyle(11.sp, FontWeight.SemiBold))
    }
}

@Composable
private fun TopTextAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, quiet: Boolean = false, color: Color = ZodiaryColors.Ink) {
    Box(
        modifier.heightIn(min = 42.dp).widthIn(min = 48.dp).clickable(onClick = onClick, role = Role.Button).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = bodyStyle(14.sp, if (quiet) FontWeight.Normal else FontWeight.Medium), color = if (quiet) ZodiaryColors.Muted else color)
    }
}

@Composable
private fun NavigationItem(label: String, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(ZodiarySpacing.NavItemSize)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) ZodiaryColors.Lavender else Color.Transparent)
            .clickable(onClick = onClick, role = Role.Tab)
            .semantics { contentDescription = label; if (selected) stateDescription = "Selected" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ZodiaryColors.Ink, modifier = Modifier.size(25.dp))
    }
}

@Composable
private fun SignPickerDialog(selected: ZodiacSign, onDismiss: () -> Unit, onSelect: (ZodiacSign) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose your sign", style = displayStyle(23.sp), color = ZodiaryColors.Purple) },
        text = {
            LazyColumn(Modifier.heightIn(max = 430.dp)) {
                itemsIndexed(ZodiaryViewModel.zodiacSigns) { _, sign ->
                    TextButton(onClick = { onSelect(sign) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("${sign.symbol}   ${sign.name}${if (selected.name == sign.name) "  ✓" else ""}", modifier = Modifier.fillMaxWidth(), style = bodyStyle(15.sp), color = ZodiaryColors.Ink)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = ZodiaryColors.Purple) } },
        containerColor = ZodiaryColors.Canvas,
    )
}

private fun displayStyle(size: androidx.compose.ui.unit.TextUnit) = TextStyle(
    fontFamily = FrauncesFontFamily,
    fontSize = size,
    fontWeight = FontWeight.Medium,
    lineHeight = size * 1.16f,
)

private fun bodyStyle(size: androidx.compose.ui.unit.TextUnit, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = DmSansFontFamily,
    fontSize = size,
    fontWeight = weight,
    lineHeight = size * 1.34f,
)

private fun looksLikeError(message: String): Boolean {
    val normalized = message.lowercase(Locale.ROOT)
    return listOf("failed", "couldn’t", "couldn't", "invalid", "unavailable", "error", "didn’t finish", "didn't finish").any(normalized::contains)
}

private fun LocalDate.coerceDayOfMonth(): LocalDate = withDayOfMonth(min(dayOfMonth, YearMonth.from(this).lengthOfMonth()))
