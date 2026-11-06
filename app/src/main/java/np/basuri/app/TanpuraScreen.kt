package np.basuri.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@Composable internal fun TanpuraTool(flute: Flute, focus: PracticeAudioFocus) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val engine = remember(context, focus) { TanpuraEngine(context, focus) }
    val status by engine.status.collectAsStateWithLifecycle()
    val active = status is TanpuraStatus.Starting || status is TanpuraStatus.Playing
    KeepScreenOn(active)
    var stringName by rememberSaveable { mutableStateOf(TanpuraString.Pa.name) }
    val string = TanpuraString.valueOf(stringName)
    var lowerOctave by rememberSaveable { mutableStateOf(false) }
    var cycle by rememberSaveable { mutableFloatStateOf(4.5f) }
    var volume by rememberSaveable { mutableFloatStateOf(.55f) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val droneSa = if (lowerOctave) flute.copy(octave = flute.octave - 1) else flute

    DisposableEffect(engine, owner, flute) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) engine.stop()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); engine.stop() }
    }

    PracticeToolPanel {
        Text("Tanpura", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("Sa = ${flute.label}", style = MaterialTheme.typography.headlineMedium, color = Indigo,
            modifier = Modifier.padding(top = 10.dp))
        Text("${string.label} · Sa · Sa · low Sa", color = Ink, style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp).semantics { contentDescription = "Four-string cycle: low ${string.label}, Sa, Sa, low Sa" })
        Text(if (lowerOctave) "Drone Sa = ${droneSa.label}, one octave below your flute." else "Drone Sa matches your flute.",
            color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
        PrimaryButton(if (active) "Stop tanpura" else "Start tanpura", {
            if (active) engine.stop() else engine.start(flute.frequency(Swar.Sa), string, lowerOctave, cycle, volume)
        }, modifier = Modifier.semantics {
            stateDescription = when (status) {
                TanpuraStatus.Starting -> "Starting"
                TanpuraStatus.Playing -> "Playing"
                else -> "Stopped"
            }
        })
        when (val state = status) {
            is TanpuraStatus.Error -> Text(state.message, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp).semantics { liveRegion = LiveRegionMode.Polite })
            is TanpuraStatus.Stopped -> state.message?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp).semantics { liveRegion = LiveRegionMode.Polite }) }
            else -> Unit
        }
        Text("Drone volume · ${(volume * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium,
            color = Muted, modifier = Modifier.padding(top = 14.dp))
        Slider(value = volume, onValueChange = { volume = it; engine.setVolume(it) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                contentDescription = "Drone volume"; stateDescription = "${(volume * 100).roundToInt()} percent"
            })
        TextButton(onClick = { settingsOpen = !settingsOpen }, contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                stateDescription = if (settingsOpen) "Expanded" else "Collapsed"
            }) { Text(if (settingsOpen) "Hide drone settings" else "Drone settings · ${if (cycle == 6f) "Slow" else if (cycle == 3f) "Fast" else "Medium"}") }
        if (settingsOpen) {
            Text("First string", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TanpuraString.entries.forEach { choice ->
                    DroneChoice(choice.label, string == choice, !active, "${choice.label} first string") { stringName = choice.name }
                }
            }
            Text("Drone register", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DroneChoice("Same Sa", !lowerOctave, !active) { lowerOctave = false }
                DroneChoice("Octave lower", lowerOctave, !active) { lowerOctave = true }
            }
            if (active) Text("Stop the drone to change its strings or register.", color = Muted, style = MaterialTheme.typography.labelMedium)
            Text("Plucking pace", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Slow" to 6f, "Medium" to 4.5f, "Fast" to 3f).forEach { (label, seconds) ->
                    DroneChoice(label, cycle == seconds, true, "$label, $seconds seconds per four-string cycle") {
                        cycle = seconds; engine.setCycle(seconds)
                    }
                }
            }
            Text("${if (cycle == 4.5f) "4.5" else cycle.toInt()} seconds per four-string cycle.", color = Muted, style = MaterialTheme.typography.labelMedium)
        }
        Text("A gentle synthesized drone. You can add the metronome below.", color = Muted,
            style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable private fun DroneChoice(label: String, chosen: Boolean, enabled: Boolean, description: String = label, onClick: () -> Unit) {
    FilterChip(selected = chosen, onClick = onClick, enabled = enabled, label = { Text(label) }, shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Mist, selectedLabelColor = Indigo),
        modifier = Modifier.heightIn(min = 48.dp).semantics {
            contentDescription = description; stateDescription = if (chosen) "Selected" else "Not selected"
        })
}
