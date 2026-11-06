package np.basuri.app

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

/** Compatibility entry point until the host switches its tab to ToolsScreen. */
@Composable internal fun MetronomeScreen(onPracticed: (Long) -> Unit = {}) {
    val context = LocalContext.current
    val focus = remember(context) { PracticeAudioFocus(context) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        MetronomeTool(onPracticed, focus)
    }
}

/** [onPracticed] receives each finished session's audible seconds exactly once. */
@Composable internal fun MetronomeTool(onPracticed: (Long) -> Unit, focus: PracticeAudioFocus) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val report by rememberUpdatedState(onPracticed)
    val engine = remember(context, focus) { MetronomeEngine(context, focus) { report(it) } }
    val status by engine.status.collectAsStateWithLifecycle()
    val active = status is MetronomeStatus.Starting || status is MetronomeStatus.Playing
    KeepScreenOn(active)
    var bpm by rememberSaveable { mutableIntStateOf(80) }
    var taalMode by rememberSaveable { mutableStateOf(false) }
    var simpleBeats by rememberSaveable { mutableIntStateOf(4) }
    var taalId by rememberSaveable { mutableStateOf("teentaal") }
    var subdivisions by rememberSaveable { mutableIntStateOf(1) }
    // Two minutes matches the daily riyaz item, so the metronome stops itself when that task is done.
    var timerMinutes by rememberSaveable { mutableIntStateOf(2) }
    var trainerStep by rememberSaveable { mutableIntStateOf(0) }
    var sessionSettingsOpen by rememberSaveable { mutableStateOf(false) }
    val taal = taals.firstOrNull { it.id == taalId } ?: taals.first()
    var position by remember { mutableStateOf(MetronomePosition()) }
    val taps = remember { ArrayDeque<Long>() }
    var tapCount by remember { mutableIntStateOf(0) }
    val changeTempo: (Int) -> Unit = { bpm = it.coerceIn(30, 240); engine.setTempo(bpm) }

    DisposableEffect(engine, owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { engine.stop() }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); engine.stop() }
    }
    LaunchedEffect(engine, active) {
        if (active) {
            while (true) withFrameNanos { position = engine.position(); if (trainerStep > 0) bpm = engine.currentTempo() }
        } else { position = engine.position() }
    }

    Column(Modifier.fillMaxWidth()) {
        MetronomePanel {
            Text("Metronome", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$bpm", fontFamily = Rounded, fontSize = 48.sp, lineHeight = 54.sp, color = Ink,
                    modifier = Modifier.semantics { contentDescription = "$bpm beats per minute" })
                Text("beats per minute", style = MaterialTheme.typography.bodyMedium, color = Muted)
                Text(when {
                    bpm < 60 -> "Approx. laya · Vilambit (slow)"
                    bpm <= 140 -> "Approx. laya · Madhya (medium)"
                    else -> "Approx. laya · Drut (fast)"
                }, style = MaterialTheme.typography.bodyMedium, color = Indigo,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            }
            Slider(value = bpm.toFloat(), onValueChange = { changeTempo(it.roundToInt()) }, valueRange = 30f..240f,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                    contentDescription = "Tempo in beats per minute"; stateDescription = "$bpm beats per minute"
                })
            FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = if (LocalDensity.current.fontScale > 1.3f) 2 else 4,
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(-5, -1, 1, 5).forEach { step ->
                    OutlinedButton(onClick = { changeTempo(bpm + step) }, enabled = if (step < 0) bpm > 30 else bpm < 240,
                        shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                            contentDescription = "${if (step < 0) "Decrease" else "Increase"} tempo by ${kotlin.math.abs(step)} beats per minute"
                        }) { Text(if (step < 0) "−${-step}" else "+$step", textAlign = TextAlign.Center) }
                }
            }
            OutlinedButton(onClick = {
                val now = SystemClock.elapsedRealtime()
                if (taps.isNotEmpty() && now - taps.last() > 2000) taps.clear()
                taps.addLast(now)
                while (taps.size > 6) taps.removeFirst()
                tapCount = taps.size
                if (taps.size >= 2) {
                    val average = (taps.last() - taps.first()).toDouble() / (taps.size - 1)
                    if (average > 0) changeTempo((60000 / average).roundToInt())
                }
            }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 48.dp)) {
                Text("Tap tempo", style = MaterialTheme.typography.labelLarge)
            }
            Text(if (tapCount == 1) "Keep tapping to set the pulse." else "Tap steady beats to set tempo. Pause 2 seconds to reset.",
                style = MaterialTheme.typography.labelMedium, color = Muted, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
            PrimaryButton(if (active) "Stop metronome" else "Start metronome", {
                if (active) engine.stop() else {
                    position = MetronomePosition()
                    engine.start(bpm, if (taalMode) MetronomePattern.taal(taal) else MetronomePattern.simple(simpleBeats), subdivisions, timerMinutes, trainerStep)
                }
            }, modifier = Modifier.semantics {
                contentDescription = if (active) "Stop metronome" else "Start metronome"
                stateDescription = when (status) {
                    MetronomeStatus.Starting -> "Starting"
                    MetronomeStatus.Playing -> "Playing"
                    else -> "Stopped"
                }
            })
            when (val state = status) {
                is MetronomeStatus.Error -> Text(state.message, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Polite })
                is MetronomeStatus.Stopped -> state.message?.let {
                    Text(it, color = Green, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Polite })
                }
                else -> Unit
            }
        }

        Spacer(Modifier.height(16.dp))
        MetronomePanel {
            Text(if (taalMode) taal.name else "$simpleBeats beats per bar", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            FlowRow(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Cycle ${position.cycle}", style = MaterialTheme.typography.bodyMedium, color = Indigo)
                Text("Practice ${metronomeTime(position.elapsedSeconds)}", style = MaterialTheme.typography.bodyMedium, color = Muted)
                if (timerMinutes > 0) Text("${metronomeTime((timerMinutes * 60L - position.elapsedSeconds).coerceAtLeast(0))} remaining",
                    style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
            Text("Choose your cycle", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetronomeChoice("Simple", !taalMode, !active) { taalMode = false; position = MetronomePosition() }
                MetronomeChoice("Taal", taalMode, !active) { taalMode = true; position = MetronomePosition() }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (taalMode) {
                    taals.forEach { preset ->
                        MetronomeChoice("${preset.name} · ${preset.beats}", preset.id == taal.id, !active) {
                            taalId = preset.id; position = MetronomePosition()
                        }
                    }
                } else {
                    (1..12).forEach { count ->
                        MetronomeChoice("$count", count == simpleBeats, !active, "$count beats per bar") {
                            simpleBeats = count; position = MetronomePosition()
                        }
                    }
                }
            }
            Text(if (active) "Tempo stays adjustable. Stop to change the cycle or session settings."
                else "Swipe for more ${if (taalMode) "taals" else "beat counts"}.", color = Muted,
                style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            MetronomeBeats(if (taalMode) taal else null, simpleBeats, position, active)
            Text(if (taalMode) "X sam · 0 khali · numbers mark tali" else "The gold outline marks the accented first beat of each bar.",
                color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 10.dp))
            if (taalMode) Text("${taal.grouping} · ${taal.note}", color = Muted,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            Text("Clicks per beat", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp).semantics { heading() })
            Text("Subdivisions are quieter than the main beat.", color = Muted, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..4).forEach { count ->
                    MetronomeChoice(if (count == 2) "2 · Dugun" else "$count", count == subdivisions, !active,
                        "$count ${if (count == 1) "click" else "clicks"} per beat") { subdivisions = count }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        MetronomePanel {
            TextButton(onClick = { sessionSettingsOpen = !sessionSettingsOpen }, contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                    stateDescription = if (sessionSettingsOpen) "Expanded" else "Collapsed"
                }) { Text(if (sessionSettingsOpen) "Hide timer & speed trainer" else "Timer & speed trainer") }
            Text("${if (timerMinutes == 0) "No timer" else "$timerMinutes minute timer"} · ${if (trainerStep == 0) "Steady tempo" else "+$trainerStep BPM each minute"}",
                color = Muted, style = MaterialTheme.typography.bodyMedium)
            if (sessionSettingsOpen) {
                Text("Session timer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp).semantics { heading() })
                Text("The timer stops the metronome. The drone keeps playing.", color = Muted,
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 2, 5, 10, 15).forEach { minutes ->
                        MetronomeChoice(if (minutes == 0) "Off" else "$minutes min", timerMinutes == minutes, !active) {
                            timerMinutes = minutes; position = MetronomePosition()
                        }
                    }
                }
                Text("Speed trainer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp).semantics { heading() })
                Text("Raise the tempo each minute, up to 240 BPM.", color = Muted, style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 1, 2, 5).forEach { step ->
                        MetronomeChoice(if (step == 0) "Off" else "+$step BPM", trainerStep == step, !active) { trainerStep = step }
                    }
                }
            }
        }
        Text("Use a comfortable volume. The clicks guide your timing; they don't play tabla bols.", color = Muted,
            style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 16.dp, bottom = 24.dp))
    }
}

@Composable private fun MetronomePanel(content: @Composable ColumnScope.() -> Unit) {
    PracticeToolPanel(content)
}

@Composable private fun MetronomeChoice(
    label: String, chosen: Boolean, enabled: Boolean, description: String = label, onClick: () -> Unit
) {
    FilterChip(selected = chosen, onClick = onClick, enabled = enabled,
        label = { Text(label, textAlign = TextAlign.Center) }, shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Mist, selectedLabelColor = Indigo),
        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = description; selected = chosen; stateDescription = if (chosen) "Selected" else "Not selected" })
}

@Composable private fun MetronomeBeats(taal: Taal?, simpleBeats: Int, position: MetronomePosition, active: Boolean) {
    val groups = taal?.vibhags ?: listOf(Vibhag(TaalMark.Sam, List(simpleBeats) { "" }))
    val cellWidth = 58.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    var start = 0
    Column(Modifier.fillMaxWidth().semantics {
        contentDescription = "Beat indicator"
        stateDescription = when {
            active && position.beat >= 0 -> "Beat ${position.beat + 1} of ${taal?.beats ?: simpleBeats}, cycle ${position.cycle}"
            active -> "Starting"
            else -> "Stopped"
        }
    }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        groups.forEach { group ->
            val groupStart = start
            FlowRow(Modifier.fillMaxWidth().background(Paper, RoundedCornerShape(16.dp)).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                group.bols.forEachIndexed { index, bol ->
                    val beat = groupStart + index
                    val current = active && position.beat == beat
                    val sam = if (taal == null) beat == 0 else taal.markAt(beat)?.mark == TaalMark.Sam
                    val mark = if (index == 0) group.symbol else ""
                    val background = if (current) { if (sam) Bamboo else Indigo } else if (sam) Bamboo.copy(alpha = .16f) else Color.White
                    val foreground = if (current && !sam) Color.White else Ink
                    Column(Modifier.width(cellWidth).background(background, RoundedCornerShape(12.dp))
                        .border(BorderStroke(if (sam) 2.dp else 1.dp, if (sam) Bamboo else Color.Transparent), RoundedCornerShape(12.dp))
                        .padding(horizontal = 4.dp, vertical = 8.dp).clearAndSetSemantics {
                            contentDescription = "Beat ${beat + 1}${if (index == 0) ", ${group.mark.label}" else ""}${if (bol.isNotEmpty()) ", $bol" else ""}"
                            selected = current
                        }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(mark.ifEmpty { " " }, color = if (current) foreground else Muted,
                            style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                        Text("${beat + 1}", color = foreground, style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 2.dp))
                        if (taal != null) Text(bol, color = if (current) foreground else Muted,
                            style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                        Text(if (current) "Now" else " ", color = foreground, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            start += group.bols.size
        }
    }
}

private fun metronomeTime(seconds: Long): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
