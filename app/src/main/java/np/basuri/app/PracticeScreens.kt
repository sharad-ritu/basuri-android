package np.basuri.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.*


@Composable internal fun LessonScreen(vm: BasuriViewModel, progress: Progress, onExit: () -> Unit) {
    val index by vm.lessonIndex.collectAsStateWithLifecycle()
    val step by vm.step.collectAsStateWithLifecycle()
    val guideIndex by vm.guideIndex.collectAsStateWithLifecycle()
    val matched by vm.matched.collectAsStateWithLifecycle()
    val selfGuided by vm.selfGuided.collectAsStateWithLifecycle()
    val audio by vm.audio.collectAsStateWithLifecycle()
    val lesson = lessons[index]
    val listen = rememberListenAction(vm)
    val scroll = rememberScrollState()
    var help by remember { mutableStateOf(false) }
    var selfGuidedDialog by remember { mutableStateOf(false) }
    LaunchedEffect(index, step, guideIndex) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ if (step == 0 && guideIndex > 0) vm.previousGuide() else onExit() }) {
                Icon(if (step == 0 && guideIndex > 0) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Close,
                    if (step == 0 && guideIndex > 0) "Previous instruction" else "Leave lesson")
            }
            LinearProgressIndicator(progress = { (if (step == 0) guideIndex else lesson.guides.size + step - 1).toFloat() / (lesson.guides.size + lesson.notes.size) },
                color = Indigo, trackColor = Mist, drawStopIndicator = {}, modifier = Modifier.weight(1f).padding(horizontal = 10.dp).height(6.dp))
            Text(if (step == 0) "Guide ${guideIndex + 1}/${lesson.guides.size}" else "Note $step/${lesson.notes.size}", color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 8.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp)) {
            if (step == 0) {
                val guide = lesson.guides[guideIndex]
                Text(lesson.title, color = Indigo, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                Text(guide.title, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(24.dp))
                GuideArt(guide.picture)
                Text(guide.cue, style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, lineHeight = 29.sp), modifier = Modifier.padding(top = 24.dp))
                TextButton({ help = true }, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 48.dp).padding(top = 8.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.HelpOutline, null, Modifier.size(18.dp))
                    Text("More guidance", modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                val note = lesson.notes[step - 1]
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("Play ", style = MaterialTheme.typography.headlineLarge)
                            SwarLabel(note, MaterialTheme.typography.headlineLarge)
                        }
                        Text(if (selfGuided) "Self-guided practice" else "Listen, then match the note", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
                    }
                    IconButton({ help = true }) { Icon(Icons.AutoMirrored.Rounded.HelpOutline, "Practice guidance", tint = Muted) }
                }
                if (lesson.notes.size > 1) {
                    NotePhrase(lesson.notes, step - 1)
                    Spacer(Modifier.height(12.dp))
                }
                AudioError(audio)
                PracticeCard(progress.flute, note, audio, matched, selfGuided)
                Text(lesson.practiceTip, color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp, bottom = 20.dp))
            }
        }
        Surface(color = Paper) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                when {
                    step == 0 -> {
                        PrimaryButton(if (guideIndex < lesson.guides.lastIndex) "Continue" else "Start practice", vm::advanceGuide)
                    }
                    matched -> {
                        PrimaryButton(if (step == lesson.notes.size) "Finish lesson" else "Next note", vm::nextStep)
                        TextButton(vm::playReference, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, Modifier.size(18.dp))
                            Text("Hear ${vm.currentNote.spoken} again", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    selfGuided -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HearButton(vm.currentNote, audio, vm::playReference, vm::stopAudio, Modifier.weight(.8f))
                            PrimaryButton("I've practiced ${vm.currentNote.spoken}", vm::nextStep, Modifier.weight(1.3f))
                        }
                        TextButton(listen, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Use microphone pitch feedback", style = MaterialTheme.typography.bodyMedium) }
                    }
                    else -> {
                        ListeningControls(vm.currentNote, audio, vm::playReference, vm::stopAudio, listen)
                        TextButton({ vm.stopAudio(); selfGuidedDialog = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("Practice without microphone", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
    if (selfGuidedDialog) AlertDialog(onDismissRequest = { selfGuidedDialog = false },
        title = { Text("Practice self-guided?") }, text = { Text("Hear each reference and try the note on your flute. Confirm each note yourself. This lesson will count toward your goal, with no microphone pitch result for those steps.") },
        confirmButton = { TextButton({ selfGuidedDialog = false; vm.useSelfGuided() }) { Text("Use self-guided practice") } },
        dismissButton = { TextButton({ selfGuidedDialog = false }) { Text("Back") } })
    if (help) HelpSheet(if (step == 0) lesson.guides[guideIndex].title else "Practice guidance", { help = false }) {
        if (step == 0) Text(lesson.guides[guideIndex].detail, style = MaterialTheme.typography.bodyLarge)
        else {
            Text(lesson.practiceTip, style = MaterialTheme.typography.bodyLarge)
            PracticeExplanation(progress.flute, audio, vm.currentNote)
            lesson.guides.forEach { guide ->
                Text(guide.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp))
                Text(guide.cue, color = Muted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

@Composable private fun GuideArt(picture: GuidePicture) {
    when (picture) {
        GuidePicture.Flute -> Surface(color = Mist, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                FluteArt(Modifier.fillMaxWidth().height(150.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Blowing hole", style = MaterialTheme.typography.labelMedium, color = Muted)
                    Text("Six finger holes", style = MaterialTheme.typography.labelMedium, color = Muted)
                }
            }
        }
        GuidePicture.Breath -> Surface(color = Mist, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                BreathArt(Modifier.fillMaxWidth().height(160.dp))
                Text("Air across the far edge", style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
        is GuidePicture.Fingers -> Surface(color = Color.White, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp)) {
                SwarLabel(picture.note, MaterialTheme.typography.displayLarge, Indigo, Modifier.padding(bottom = 16.dp))
                Fingering(picture.note)
                Text("${fingeringCue(picture.note)}. Leave the rest open.", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
            }
        }
        is GuidePicture.Phrase -> Surface(color = Mist, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("Your ${picture.notes.size}-note phrase", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 20.dp))
                NotePhrase(picture.notes, 0)
                if (picture.caption.isNotEmpty()) Text(picture.caption, color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 20.dp))
            }
        }
    }
}

/** Up to five notes share the width; longer phrases scroll sideways and keep the current note in view. */
@Composable private fun NotePhrase(notes: List<Swar>, current: Int) {
    val scroll = rememberScrollState()
    val fits = notes.size <= 5
    val density = androidx.compose.ui.platform.LocalDensity.current
    LaunchedEffect(current) { if (!fits) scroll.animateScrollTo(with(density) { ((current - 1).coerceAtLeast(0) * 63).dp.roundToPx() }) }
    Row(Modifier.fillMaxWidth().then(if (fits) Modifier else Modifier.horizontalScroll(scroll))
        .clearAndSetSemantics { contentDescription = "${notes.joinToString(", ") { it.spoken }}. Current note ${current + 1} of ${notes.size}." }, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        notes.forEachIndexed { index, note ->
            Box((if (fits) Modifier.weight(1f) else Modifier.width(56.dp)).background(if (index == current) Indigo else if (index < current) GreenWash else Mist, RoundedCornerShape(11.dp)).padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                SwarLabel(note, MaterialTheme.typography.labelLarge, if (index == current) Color.White else Ink)
            }
        }
    }
}

@Composable internal fun StudioScreen(vm: BasuriViewModel, progress: Progress, onSettings: () -> Unit) {
    val note by vm.practiceNote.collectAsStateWithLifecycle()
    val audio by vm.audio.collectAsStateWithLifecycle()
    val matched by vm.matched.collectAsStateWithLifecycle()
    val longTone by vm.longTone.collectAsStateWithLifecycle()
    val lastHold by vm.lastHold.collectAsStateWithLifecycle()
    val listen = rememberListenAction(vm)
    var help by remember { mutableStateOf(false) }
    val notes = progress.unlockedNotes
    LaunchedEffect(notes) { if (note !in notes) vm.chooseNote(Swar.Sa) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Practice", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f).semantics { heading() })
            IconButton({ help = true }) { Icon(Icons.AutoMirrored.Rounded.HelpOutline, "Practice help", tint = Muted) }
        }
        Text(if (longTone) "Hold one note as steadily as you can. Rest between breaths." else "Choose a note, hear it, then find it on your flute.",
            color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
        Row(Modifier.fillMaxWidth().background(Mist, RoundedCornerShape(14.dp)).padding(4.dp)) {
            listOf(false to "Find a note", true to "Long tones").forEach { (mode, label) ->
                Surface(onClick = { vm.setLongTone(mode) }, color = if (longTone == mode) Color.White else Color.Transparent, shape = RoundedCornerShape(11.dp),
                    shadowElevation = if (longTone == mode) 1.dp else 0.dp, modifier = Modifier.weight(1f).heightIn(min = 44.dp).semantics { selected = longTone == mode; role = Role.Tab }) {
                    Box(contentAlignment = Alignment.Center) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (longTone == mode) Ink else Muted) }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        NotePicker(notes, note) { vm.chooseNote(it) }
        if (notes.size < Swar.entries.size) Text("More notes appear as you reach them on the Learn path.", color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(14.dp))
        AudioError(audio)
        if (longTone) LongToneCard(progress, note, audio, lastHold) else PracticeCard(progress.flute, note, audio, matched)
        Spacer(Modifier.height(16.dp))
        ListeningControls(note, audio, vm::playReference, vm::stopAudio, listen)
        TextButton(onSettings, modifier = Modifier.heightIn(min = 48.dp).align(Alignment.CenterHorizontally)) {
            Icon(Icons.Rounded.Tune, null, Modifier.size(17.dp))
            Text("Sa ${progress.flute.label} · Change flute", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 7.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
    if (help) HelpSheet("Your practice room", { help = false }) { PracticeExplanation(progress.flute, audio, note) }
}

/** Notes grouped by octave so low, middle, and high Sa are never confused. */
@Composable private fun NotePicker(notes: List<Swar>, selected: Swar, onChoose: (Swar) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        notes.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { swar ->
                    val chosen = swar == selected
                    Surface(onClick = { onChoose(swar) }, color = if (chosen) Indigo else Color.White, shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (chosen) Indigo else Color(0xFFD9DEEB)),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp).semantics { this.selected = chosen }) {
                        Box(contentAlignment = Alignment.Center) { SwarLabel(swar, MaterialTheme.typography.labelLarge, if (chosen) Color.White else Ink, Modifier.padding(vertical = 10.dp)) }
                    }
                }
                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A long-tone warm-up: time how long one note stays within 50 cents, with short wobbles forgiven. */
@Composable private fun LongToneCard(progress: Progress, note: Swar, audio: AudioStatus, lastHold: Long) {
    val listening = audio as? AudioStatus.Listening
    val reading = listening?.reading
    val cents = (reading as? PitchReading.Tone)?.let { 1200 * ln(it.hz / progress.flute.frequency(note)) / ln(2.0) }
    val hold = listening?.holdMs ?: 0
    val steady = hold > 0
    val color by animateColorAsState(if (steady) Green else Indigo, label = "Long tone color")
    Surface(color = Color.White, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, if (steady) Green.copy(.5f) else Color(0xFFE0E5F1))) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SwarLabel(note, MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp, lineHeight = 52.sp), Ink, Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Reference", style = MaterialTheme.typography.labelMedium, color = Muted)
                    Text(notePitchLabel(progress.flute, note), style = MaterialTheme.typography.titleLarge, color = Indigo)
                }
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { (hold / BasuriViewModel.LONG_TONE_GOAL.toFloat()).coerceIn(0f, 1f) }, color = color, trackColor = Mist,
                    strokeWidth = 7.dp, modifier = Modifier.size(120.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clearAndSetSemantics {
                    contentDescription = if (listening == null) "Long tone timer, not listening" else "Holding ${"%.1f".format(hold / 1000f)} seconds"
                }) {
                    Text("%.1f".format(hold / 1000f), fontFamily = Rounded, fontSize = 40.sp, color = if (steady) Green else Ink)
                    Text("seconds", color = Muted, style = MaterialTheme.typography.labelMedium)
                }
            }
            PitchMeter(cents, color)
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Last hold", if (lastHold > 0) "%.1f s".format(lastHold / 1000f) else "—")
                Stat("Best ever", if (progress.bestHoldMs > 0) "%.1f s".format(progress.bestHoldMs / 1000f) else "—")
                Stat("Warm-up", if (progress.warmedUp) "Done today" else "5 s to go", if (progress.warmedUp) Green else Muted)
            }
            Text(when {
                listening == null -> "Start listening, then play ${note.spoken} softly and steadily. Aim for 8 seconds; 5 seconds counts as today's warm-up."
                reading is PitchReading.Tone && !steady -> "Find the center of the meter, then hold it."
                steady -> "Steady. Keep the air relaxed and even."
                else -> "Play one comfortable note."
            }, color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable private fun Stat(label: String, value: String, color: Color = Ink) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
    }
}

@Composable private fun PracticeCard(flute: Flute, note: Swar, audio: AudioStatus, matched: Boolean, selfGuided: Boolean = false) {
    val reading = (audio as? AudioStatus.Listening)?.reading
    val cents = (reading as? PitchReading.Tone)?.let { 1200 * ln(it.hz / flute.frequency(note)) / ln(2.0) }
    val held = if (matched) 1f else (audio as? AudioStatus.Listening)?.held ?: 0f
    val listening = audio is AudioStatus.Listening
    val onTarget = cents != null && abs(cents) <= 35
    val color by animateColorAsState(if (matched || onTarget) Green else Indigo, label = "Pitch feedback color")
    val title = when {
        audio is AudioStatus.Playing -> "Listen to the reference"
        selfGuided -> "Practice at your own pace"
        matched -> "You matched ${note.spoken}"
        reading is PitchReading.Tone && onTarget -> "On target. Hold it here."
        reading is PitchReading.Tone && abs(cents ?: 0.0) > 900 -> "Check your Sa octave"
        reading is PitchReading.Tone && (cents ?: 0.0) < 0 -> "Below the target"
        reading is PitchReading.Tone -> "Above the target"
        reading is PitchReading.Unclear -> "No clear pitch yet"
        listening -> "Listening for ${note.spoken}"
        audio is AudioStatus.Error -> "Audio needs attention"
        else -> "Ready when you are"
    }
    val detail = when {
        audio is AudioStatus.Playing -> "The microphone is off during playback."
        selfGuided -> "Confirm below when you've tried the note."
        matched -> "A steady pitch near your reference."
        reading is PitchReading.Tone && abs(cents ?: 0.0) > 900 -> "The octave must match your flute setup."
        reading is PitchReading.Tone -> "Compare with the reference. Rest and try again."
        reading is PitchReading.Unclear -> "Try one note in a quieter room."
        listening -> "No pitch yet. Play one comfortable note."
        audio is AudioStatus.Error -> "Try the audio help or practice self-guided."
        else -> "Hear the note, then start listening."
    }
    Surface(color = Color.White, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, if (matched) Green.copy(.5f) else Color(0xFFE0E5F1))) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SwarLabel(note, MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp, lineHeight = 54.sp), Ink, Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Reference", style = MaterialTheme.typography.labelMedium, color = Muted)
                    Text(notePitchLabel(flute, note), style = MaterialTheme.typography.titleLarge, color = Indigo)
                }
            }
            Text(fingeringCue(note), color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
            Fingering(note)
            if (!selfGuided) {
                Spacer(Modifier.height(14.dp))
                PitchMeter(cents, color)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(when { matched -> Icons.Rounded.CheckCircle; selfGuided -> Icons.Rounded.Spa; listening -> Icons.Rounded.Mic; audio is AudioStatus.Playing -> Icons.Rounded.Hearing; else -> Icons.Rounded.MusicNote },
                    null, tint = color, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f).padding(start = 9.dp)) {
                    Text(title, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { if (matched) liveRegion = LiveRegionMode.Polite })
                    Text(detail, color = Muted, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (selfGuided) {
                Text("Microphone off · No pitch check", color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
            } else {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { held }, color = color, trackColor = Mist, drawStopIndicator = {}, modifier = Modifier.fillMaxWidth().height(5.dp))
                Text(if (matched) "Pitch match complete" else "Hold near the center for 1.5 seconds", color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 7.dp))
            }
        }
    }
}

@Composable private fun PitchMeter(cents: Double?, color: Color) {
    val position by animateFloatAsState(((cents ?: 0.0).coerceIn(-100.0, 100.0) / 100).toFloat(), label = "Pitch position")
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        contentDescription = if (cents == null) "Pitch meter, no measurement" else "Pitch ${cents.roundToInt()} cents from reference"
    }) {
        Canvas(Modifier.fillMaxWidth().height(30.dp)) {
            val center = size.width / 2
            val half = center - 8.dp.toPx()
            val y = size.height / 2
            drawRoundRect(GreenWash, Offset(center - half * .35f, 0f), androidx.compose.ui.geometry.Size(half * .7f, size.height), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
            drawLine(Color(0xFFB4BED6), Offset(8.dp.toPx(), y), Offset(size.width - 8.dp.toPx(), y), 2.dp.toPx())
            (-2..2).forEach { tick ->
                val x = center + tick * half / 2
                drawLine(if (tick == 0) Green else Muted.copy(.6f), Offset(x, y - 4.dp.toPx()), Offset(x, y + 4.dp.toPx()), 1.dp.toPx())
            }
            if (cents != null) {
                val x = center + position * half
                drawCircle(color, 6.dp.toPx(), Offset(x, y))
                drawCircle(Color.White, 2.dp.toPx(), Offset(x, y))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Lower", color = Muted, style = MaterialTheme.typography.labelMedium)
            Text("Target", color = Green, style = MaterialTheme.typography.labelMedium)
            Text("Higher", color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable private fun rememberListenAction(vm: BasuriViewModel): () -> Unit {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (vm.screen.value == Screen.Practice || (vm.screen.value == Screen.Lesson && vm.step.value > 0)) {
            if (granted) vm.listen() else vm.permissionDenied()
        }
    }
    return {
        vm.usePitchFeedback()
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.listen()
        else { vm.stopAudio(); permission.launch(Manifest.permission.RECORD_AUDIO) }
    }
}

@Composable private fun ListeningControls(note: Swar, audio: AudioStatus, play: () -> Unit, stop: () -> Unit, listen: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HearButton(note, audio, play, stop, Modifier.weight(.85f))
        PrimaryButton(if (audio is AudioStatus.Listening) "Stop listening" else "Start listening", { if (audio is AudioStatus.Listening) stop() else listen() },
            Modifier.weight(1.2f).semantics { stateDescription = if (audio is AudioStatus.Listening) "Microphone on" else "Microphone off" })
    }
}

@Composable private fun HearButton(note: Swar, audio: AudioStatus, play: () -> Unit, stop: () -> Unit, modifier: Modifier) {
    OutlinedButton({ if (audio is AudioStatus.Playing) stop() else play() }, modifier.heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)) {
        Icon(if (audio is AudioStatus.Playing) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp, null, Modifier.size(19.dp))
        Text(if (audio is AudioStatus.Playing) "Stop tone" else "Hear ${note.spoken}", style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(start = 7.dp))
    }
}

@Composable private fun AudioError(audio: AudioStatus) {
    if (audio !is AudioStatus.Error) return
    val context = LocalContext.current
    Surface(color = Color(0xFFFCECEF), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(vertical = 12.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(audio.message, color = Color(0xFF85283B), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) TextButton({
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }) { Text("Open app settings") }
        }
    }
}

@Composable private fun PracticeExplanation(flute: Flute, audio: AudioStatus, note: Swar) {
    val reading = (audio as? AudioStatus.Listening)?.reading
    Text("Listen to the reference, then play one note. The microphone opens after playback stops. A pitch match needs 1.5 seconds within ±35 cents of the target. A brief flicker in the reading pauses the timer rather than restarting it.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
    Surface(color = Mist, shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(top = 16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Pitch details", style = MaterialTheme.typography.titleMedium)
            Text("Sa ${flute.label}${if (flute.cents != 0) " (${flute.tuningLabel})" else ""} · Target ${notePitchLabel(flute, note)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            Text("Reference ${"%.1f".format(flute.frequency(note))} Hz", style = MaterialTheme.typography.bodyMedium)
            Text(if (reading is PitchReading.Tone) "Heard ${"%.1f".format(reading.hz)} Hz · ${"%+.0f".format(1200 * ln(reading.hz / flute.frequency(note)) / ln(2.0))} cents" else "No pitch measurement at the moment", style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
    Text("No reading? Try one note in a quieter room. Check microphone permission and Android's microphone privacy switch. Basuri cannot tell why an unclear sound was missed.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
    Text("This meter checks pitch only. It cannot identify your instrument or assess your embouchure, finger placement, tone quality, rhythm, or smoothness. Free practice does not add a lesson completion.", style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(top = 16.dp))
}

private fun notePitchLabel(flute: Flute, note: Swar): String = flute.pitchLabel(note)
