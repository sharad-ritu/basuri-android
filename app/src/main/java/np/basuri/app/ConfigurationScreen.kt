package np.basuri.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.text.format.DateFormat
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Onboarding (two pages) and the My flute tab share the Sa and goal controls. */
@Composable internal fun Configuration(progress: Progress, onboarding: Boolean, onBack: () -> Unit, vm: BasuriViewModel, onSave: (Flute, Int, Boolean) -> Unit) {
    var pitchClass by rememberSaveable(progress.flute) { mutableIntStateOf(progress.flute.pitchClass) }
    var octave by rememberSaveable(progress.flute) { mutableIntStateOf(progress.flute.octave) }
    var cents by rememberSaveable(progress.flute) { mutableIntStateOf(progress.flute.cents) }
    /** The pitch the microphone last heard, shown until the learner changes the setting by hand. */
    var heard by rememberSaveable(progress.flute) { mutableStateOf<Double?>(null) }
    val detection by vm.saDetection.collectAsStateWithLifecycle()
    val audio by vm.audio.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.detectSa() else vm.saPermissionDenied()
    }
    val detect = {
        heard = null
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.detectSa()
        else { vm.stopAudio(); micPermission.launch(Manifest.permission.RECORD_AUDIO) }
    }
    LaunchedEffect(detection) {
        val found = detection as? SaDetection.Found ?: return@LaunchedEffect
        pitchClass = found.flute.pitchClass; octave = found.flute.octave; cents = found.flute.cents; heard = found.hz
        vm.clearSaDetection()
    }
    DisposableEffect(Unit) { onDispose { vm.clearSaDetection() } }
    /** A hand-picked setting wins: stop any detection still listening so its result can't overwrite it. */
    val manual: (() -> Unit) -> Unit = { change ->
        if (detection is SaDetection.Listening) vm.stopAudio()
        vm.clearSaDetection(); heard = null; change()
    }
    var goal by rememberSaveable(progress.dailyGoal) { mutableIntStateOf(progress.dailyGoal) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var help by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    val flute = Flute(pitchClass, octave, cents)
    // A fresh detection that confirms the saved Sa still needs a Save, to record that the flute is tuned.
    val dirty = flute != progress.flute || goal != progress.dailyGoal || (heard != null && !progress.tunePromptDone)
    val scroll = rememberScrollState()
    LaunchedEffect(page) { scroll.scrollTo(0) }
    BackHandler(onboarding && page > 0) { page = 0 }
    Column(Modifier.fillMaxSize()) {
        if (onboarding) TopLine("Set up your practice", { if (page > 0) page = 0 else onBack() }, "${page + 1} of 2")
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp)) {
            if (!onboarding) {
                Text("My flute", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 16.dp).semantics { heading() })
                Text("Your Sa, daily goal, and reminders.", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp, bottom = 20.dp))
            }
            if (!onboarding || page == 0) {
                if (onboarding) {
                    Text("Find your flute's Sa", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 8.dp))
                    Text("Sa is the note with the three holes nearest the blowing hole closed.", color = Muted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
                    Fingering(Swar.Sa)
                    Spacer(Modifier.height(20.dp))
                }
                SettingsCard(if (onboarding) null else "Sa reference", if (onboarding) null else Icons.Rounded.MusicNote) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Sa note", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(flute.label, color = Indigo, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(12.dp))
                    SaDetector(detection, heard, flute, onboarding || flute != progress.flute || !progress.tunePromptDone, onboarding, detect, vm::stopAudio)
                    Text("Or choose it yourself", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
                    pitchNames.chunked(4).forEachIndexed { row, names ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            names.forEachIndexed { col, label ->
                                val value = row * 4 + col
                                Choice(label, value == pitchClass, { manual { pitchClass = value } }, Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text("Octave", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (3..5).forEach { value -> Choice("$value", value == octave, { manual { octave = value } }, Modifier.weight(1f)) }
                    }
                    FineTuning(cents) { manual { cents = it } }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Sa ${flute.label} · ${"%.1f".format(flute.frequency(Swar.Sa))} Hz", color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        val playing = audio is AudioStatus.Playing
                        OutlinedButton({ if (playing) vm.stopAudio() else vm.playTone(flute.frequency(Swar.Sa)) }, shape = RoundedCornerShape(14.dp),
                            enabled = detection !is SaDetection.Listening, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icon(if (playing) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp, null, Modifier.size(18.dp))
                            Text(if (playing) "Stop" else "Hear Sa", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    TextButton({ help = true }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.HelpOutline, null, Modifier.size(18.dp))
                        Text("Not sure which note to choose?", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (onboarding) Text("Can't play a steady note yet? Keep G4 for now. After your first lesson, Basuri will offer to tune to your flute.", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp))
            }
            if (!onboarding || page == 1) {
                if (onboarding) {
                    Text("Make a little time", style = MaterialTheme.typography.headlineLarge)
                    Text("Choose a goal that fits your day. You can change it whenever you like.", color = Muted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
                } else Spacer(Modifier.height(16.dp))
                SettingsCard(if (onboarding) null else "Daily goal", if (onboarding) null else Icons.Rounded.WbSunny) {
                    (1..3).forEach { value ->
                        val selected = goal == value
                        Surface(onClick = { goal = value }, color = if (selected) Mist else Color.White,
                            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Indigo else Color(0xFFDDE2EE)), shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).semantics { this.selected = selected }) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked, null, tint = if (selected) Indigo else Muted)
                                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                    Text("$value ${if (value == 1) "lesson" else "lessons"} a day", style = MaterialTheme.typography.titleMedium)
                                    Text(when (value) { 1 -> "About 3–5 minutes"; 2 -> "About 8–10 minutes"; else -> "About 12–15 minutes" }, color = Muted, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    Text("Repeated and self-guided lessons count toward your goal.", color = Muted, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(16.dp))
                SettingsCard("Reminders", Icons.Rounded.NotificationsActive) { ReminderSettings(progress, vm, compact = onboarding) }
                if (!onboarding) {
                    Spacer(Modifier.height(16.dp))
                    SettingsCard("Your data", Icons.Rounded.Lock) {
                        Text("Audio is processed live, never saved or uploaded. The microphone runs only when you choose to listen. No account or network access.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton({ reset = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(top = 12.dp).heightIn(min = 48.dp)) {
                            Icon(Icons.Rounded.RestartAlt, null, Modifier.size(18.dp))
                            Text("Reset progress", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    val context = LocalContext.current
                    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() }
                    Text("Basuri ${version.orEmpty()} · For a six-hole Nepali basuri", color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 20.dp))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        AnimatedVisibility(onboarding || dirty) {
            Surface(color = Paper) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    PrimaryButton(if (!onboarding) "Save changes" else if (page == 0) "Continue" else "Start my first lesson", {
                        if (onboarding && page == 0) { vm.stopAudio(); page = 1 } else onSave(flute, goal, heard != null)
                    })
                }
            }
        }
    }
    if (help) HelpSheet("Choosing your Sa", { help = false }) {
        Text("Use a six-hole Nepali basuri. Count from the blowing hole: Sa closes holes 1–3, Re closes 1–2, and Ga closes only hole 1.", style = MaterialTheme.typography.bodyLarge)
        Text("Printed flute labels can refer to an all-closed pipe note. Here, choose the actual sound of your three-hole Sa. Ask your maker or teacher if the label is unclear.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
        Text("The easiest way is to let Basuri listen: close holes 1–3 and play one long, gentle note. It sets the nearest note and octave, and fine-tunes every reference to your flute.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
        Text("Middle C is C4. The octave matters for pitch checking. References start from A4 = 440 Hz with equal temperament, shifted by your fine tuning. Many handmade flutes sit 10–30 cents away from concert pitch. Seven-hole variants and Carnatic venu need different guidance.", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
    }
    if (reset) AlertDialog(onDismissRequest = { reset = false },
        title = { Text("Reset all progress?") },
        text = { Text("This clears completed lessons, streaks, XP, achievements, and practice history on this device. Your Sa, goal, and reminder settings stay. This cannot be undone.") },
        confirmButton = { TextButton({ reset = false; vm.resetProgress() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Reset progress") } },
        dismissButton = { TextButton({ reset = false }) { Text("Keep my progress") } })
}

@Composable private fun SettingsCard(title: String?, icon: ImageVector?, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFFE3E7F1))) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            if (title != null) Row(Modifier.padding(bottom = 12.dp).semantics(mergeDescendants = true) { heading() }, verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) Icon(icon, null, tint = Indigo, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            }
            content()
        }
    }
}

/**
 * Returns a function that applies a reminder time, first asking for notification permission on
 * Android 13 and later. -1 turns reminders off without asking.
 */
@Composable internal fun rememberReminderChooser(onChange: (Int) -> Unit): (Int) -> Unit {
    val context = LocalContext.current
    // Saveable: the permission dialog can outlive a rotation, and the chosen time must survive it.
    var pending by rememberSaveable { mutableIntStateOf(-1) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (pending >= 0) onChange(if (granted) pending else -1)
        pending = -1
    }
    return { value ->
        if (value >= 0 && !Reminders.canNotify(context) && Build.VERSION.SDK_INT >= 33) {
            pending = value; permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else onChange(value)
    }
}

/** Reminder changes save immediately, because they depend on a system permission dialog rather than the Save button. */
@Composable private fun ReminderSettings(progress: Progress, vm: BasuriViewModel, compact: Boolean) {
    val context = LocalContext.current
    val choose = rememberReminderChooser(vm::setReminder)
    val minutes = progress.reminderMinutes
    val on = minutes >= 0
    // Re-checked on resume: the learner may have just changed it in system settings.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { resumes++; onPauseOrDispose {} }
    val blocked = remember(on, resumes) { on && !Reminders.canNotify(context) }
    val offChannel = remember(on, resumes, progress.streakSaver, progress.weeklyRecap) { if (on && !blocked) Reminders.blockedChannel(context, progress) else null }
    var picking by rememberSaveable { mutableStateOf(false) }
    if (picking) ReminderTimeDialog(if (on) minutes else Reminders.presets[2].first, { picking = false }) { picking = false; choose(it) }
    ToggleRow("Daily reminder", if (on) "Around ${Reminders.label(minutes)}, only if no lesson is done yet." else "A gentle nudge to keep your streak going.", on) {
        choose(if (it) Reminders.presets[2].first else -1)
    }
    if (on) {
        Reminders.presets.chunked(2).forEachIndexed { index, row ->
            Row(Modifier.fillMaxWidth().padding(top = if (index == 0) 12.dp else 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, name) -> Choice("$name · ${Reminders.label(value)}", value == minutes, { choose(value) }, Modifier.weight(1f)) }
            }
        }
        val custom = Reminders.presets.none { it.first == minutes }
        Choice(if (custom) "Custom · ${Reminders.label(minutes)}" else "Custom time…", custom, { picking = true }, Modifier.fillMaxWidth().padding(top = 8.dp))
        if (!compact) {
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Color(0xFFE3E7F1))
            ToggleRow("Streak saver", "At 21:30 if your streak would end tonight.", progress.streakSaver, vm::setStreakSaver)
            Spacer(Modifier.height(10.dp))
            ToggleRow("Weekly summary", "Saturday evening: your week's days, lessons, and best streak.", progress.weeklyRecap, vm::setWeeklyRecap)
            TextButton(vm::sendTestReminder, enabled = !blocked, modifier = Modifier.heightIn(min = 48.dp).padding(top = 6.dp), contentPadding = PaddingValues(horizontal = 0.dp)) {
                Icon(Icons.Rounded.NotificationsActive, null, Modifier.size(18.dp))
                Text("Send a test reminder", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    if (blocked) {
        Text("Notifications are off for Basuri, so reminders can't appear.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
        TextButton({
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }, contentPadding = PaddingValues(horizontal = 0.dp)) { Text("Open notification settings") }
    }
    if (offChannel != null) {
        Text("\u201c${offChannel.second}\u201d notifications are turned off in Android settings, so they can't appear.", color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
        TextButton({
            context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, offChannel.first))
        }, contentPadding = PaddingValues(horizontal = 0.dp)) { Text("Turn on ${offChannel.second.lowercase()}") }
    }
    Text("Saved right away. Scheduled on this device only.", color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 10.dp))
}

@Composable private fun ToggleRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = Indigo, checkedBorderColor = Indigo))
    }
}

/** Microphone Sa detection: play the three-hole Sa and Basuri sets the nearest note, octave, and tuning. */
@Composable private fun SaDetector(detection: SaDetection, heard: Double?, flute: Flute, needsSave: Boolean, onboarding: Boolean, onDetect: () -> Unit, onStop: () -> Unit) {
    Surface(color = Mist, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(Modifier.padding(14.dp)) {
            if (detection is SaDetection.Listening) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Mic, null, tint = Indigo)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text("Listening for your Sa", style = MaterialTheme.typography.titleMedium)
                        Text(detection.hz?.let { "Hearing ${"%.0f".format(it)} Hz. Hold it steady." } ?: "Close holes 1–3 and play one long, gentle note.",
                            color = Muted, style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onStop, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stop") }
                }
                LinearProgressIndicator(progress = { detection.steady }, color = Indigo, trackColor = Color.White, drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(5.dp))
                return@Column
            }
            Text("Let Basuri listen", style = MaterialTheme.typography.titleMedium)
            when {
                detection is SaDetection.Failed -> Text(detection.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite })
                heard != null -> Text("Heard ${"%.1f".format(heard)} Hz: Sa is ${flute.label}" + (if (flute.cents != 0) ", ${flute.tuningLabel}" else "") +
                    if (!needsSave) ". That matches your saved Sa." else ". ${if (onboarding) "Continue" else "Save changes"} to keep it.", color = Green, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite })
                else -> Text("Play your three-hole Sa and Basuri will find its note and tune every reference to your flute.", color = Muted,
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
            OutlinedButton(onDetect, shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(top = 10.dp).heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.Mic, null, Modifier.size(18.dp))
                Text(if (heard != null || detection is SaDetection.Failed) "Listen again" else "Detect my Sa", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** A ±50 cent offset so every reference matches a flute that isn't at concert pitch. */
@Composable private fun FineTuning(cents: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Fine tuning", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(if (cents == 0) "Concert pitch, A4 = 440 Hz" else if (cents > 0) "Your flute is a little sharp" else "Your flute is a little flat",
                color = Muted, style = MaterialTheme.typography.labelMedium)
        }
        IconButton({ onChange((cents - 5).coerceAtLeast(-Flute.MAX_CENTS)) }, enabled = cents > -Flute.MAX_CENTS) { Icon(Icons.Rounded.Remove, "Lower by 5 cents") }
        Text(if (cents == 0) "0¢" else "%+d¢".format(cents), style = MaterialTheme.typography.titleMedium, color = Indigo, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 48.dp).semantics { liveRegion = LiveRegionMode.Polite; contentDescription = if (cents == 0) "Tuning 0 cents" else "Tuning ${"%+d".format(cents)} cents" })
        IconButton({ onChange((cents + 5).coerceAtMost(Flute.MAX_CENTS)) }, enabled = cents < Flute.MAX_CENTS) { Icon(Icons.Rounded.Add, "Raise by 5 cents") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ReminderTimeDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(initial / 60, initial % 60, DateFormat.is24HourFormat(context))
    AlertDialog(onDismissRequest = onDismiss, containerColor = Paper,
        title = { Text("Reminder time") },
        text = { TimePicker(state) },
        confirmButton = { TextButton({ onConfirm(state.hour * 60 + state.minute) }) { Text("Set reminder") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}
