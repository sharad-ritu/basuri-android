package np.basuri.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

private enum class NotationSection(val label: String) {
    Swaras("Swaras"), Octaves("Octaves"), Rhythm("Rhythm"), Fingering("Fingering"), Examples("Examples")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun NotationScreen(flute: Flute) {
    var sectionIndex by rememberSaveable { mutableIntStateOf(0) }
    val section = NotationSection.entries[sectionIndex]
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 20.dp)) {
        Text("Notation", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 16.dp).semantics { heading() })
        Text("Read sargam, one beat at a time.", color = Muted, style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            NotationSection.entries.forEachIndexed { index, entry ->
                FilterChip(selected = section == entry, onClick = {
                    sectionIndex = index
                    scope.launch { scroll.scrollTo(0) }
                }, label = { Text(entry.label) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        when (section) {
            NotationSection.Swaras -> NotationSwaras(flute)
            NotationSection.Octaves -> NotationOctaves(flute)
            NotationSection.Rhythm -> NotationRhythm()
            NotationSection.Fingering -> NotationFingerings(flute)
            NotationSection.Examples -> NotationExamples(flute)
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable private fun NotationHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

@Composable private fun NotationBody(text: String) {
    Text(text, color = Muted, style = MaterialTheme.typography.bodyLarge)
}

@Composable private fun NotationCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable private fun NotationSwaras(flute: Flute) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(color = Mist, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your Sa = ${flute.label}", style = MaterialTheme.typography.titleMedium, color = Indigo)
                Text("The three-hole Sa chosen in My flute sets every pitch below. The same sargam can sound in a different key on another flute.",
                    style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
        }
        NotationHeading("The seven swaras")
        NotationBody("Sargam names a note relative to Sa. These are the seven shuddh forms. Sa and Pa are achal: they have no komal or tivra form, but still move with your chosen Sa.")
        NotationCard {
            notationShuddh.forEachIndexed { index, pitch ->
                val swar = pitch.swar
                val tone = NotationTone(pitch)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NotationGlyph(tone)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${swar.sargam} · ${swar.devanagari}", style = MaterialTheme.typography.titleMedium)
                        Text(swar.fullName + if (swar.achal) " · Achal" else "", color = Muted, style = MaterialTheme.typography.bodyMedium)
                        Text("${pitch.semitones} semitones from Sa · ${flute.notationPitch(tone)}", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (index < notationShuddh.lastIndex) HorizontalDivider(color = Mist)
            }
        }
        NotationHeading("Komal and tivra")
        NotationBody("Komal Re, Ga, Dha and Ni sit one semitone below their shuddh forms. An underline marks them. Tivra Ma sits one semitone above shuddh Ma and has a short vertical tick above it.")
        NotationCard {
            NotationHeading("Twelve notes in an octave")
            Text("The numbers count semitones above Sa. The next Sa is at 12.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            NotationPitch.entries.forEachIndexed { index, pitch ->
                val tone = NotationTone(pitch)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NotationGlyph(tone)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(pitch.spokenName.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                        Text("+${pitch.semitones} semitones · ${flute.notationPitch(tone)}", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (index < NotationPitch.entries.lastIndex) HorizontalDivider(color = Mist)
            }
        }
        Text("Pitch names use Basuri's equal-tempered reference, A4 = 440 Hz. Classical and folk intonation is more flexible; these pitches are a beginner guide, not a rule for every raga.",
            color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun NotationOctaves(flute: Flute) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NotationHeading("Octaves · saptak")
        NotationBody("A dot changes the octave. It does not change the swar name. Read the dot together with its note.")
        NotationOctave.entries.forEach { octave ->
            val tone = NotationTone(NotationPitch.Sa, octave)
            NotationCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NotationGlyph(tone)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        NotationHeading(octave.title)
                        Text(octave.description.replaceFirstChar { it.uppercase() }, color = Muted, style = MaterialTheme.typography.bodyMedium)
                        Text("Sa = ${flute.notationPitch(tone)}", style = MaterialTheme.typography.titleMedium, color = Indigo)
                    }
                }
                NotationBody(when (octave) {
                    NotationOctave.Mandra -> "A dot below. One octave below middle Sa."
                    NotationOctave.Madhya -> "No octave mark. This is the register of your chosen Sa."
                    NotationOctave.Taar -> "A dot above. One octave above middle Sa."
                })
            }
        }
        NotationCard {
            NotationHeading("Two marks can belong to one note")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NotationGlyph(NotationTone(NotationPitch.KomalNi, NotationOctave.Mandra))
                Text("Komal Ni in mandra saptak. The underline lowers Ni by a semitone; the dot moves it down an octave.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }
        Text("Mandra and taar are relative to Sa, not to the octave number in a pitch name. These are reading examples; not every octave is practical on every basuri.", color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun NotationRhythm() {
    val taal = notationKeherwa
    val teentaal = taals.first { it.id == "teentaal" }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NotationHeading("Read notes against a taal")
        NotationBody("A taal is a repeating beat cycle. Each grid column is one matra, or beat. A vertical bar | ends a vibhag, a section within that cycle.")
        NotationCard {
            NotationHeading("Marks at section starts")
            NotationBody("X marks sam, the first beat of the cycle. 0 marks khali, the wave. Numbers such as 2 and 3 mark other tali, or clap, sections. Khali is not a rest; keep playing the melody.")
            Text("In ${teentaal.name} the section starts read ${teentaal.vibhags.joinToString(", ") { it.symbol }}. In ${taal.name} they read ${taal.vibhags.joinToString(", ") { it.symbol }}.", style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        NotationCard {
            NotationHeading("${taal.name} · ${taal.beats} beats")
            Text("${taal.grouping} grouping, just as in the metronome. The small words below the notes are tabla bols, not sargam.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            NotationTaalGrid(taal, notationExamples[3].beats)
            NotationBody("Read each section left to right, then continue on the next row. Beat 5 begins khali. After beat 8, return to sam on beat 1.")
        }
        NotationCard {
            NotationHeading("A dash means continue")
            NotationBeatGlyph(NotationBeat.Hold, NotationScript.Symbols)
            NotationBody("Each - here continues the previous note for one more beat. Sa followed by a dash lasts two beats. Keep the sound going without starting the note again.")
        }
        NotationCard {
            NotationHeading("More than one note in a beat")
            NotationBeatGlyph(NotationBeat.Notes(NotationTone(NotationPitch.Sa), listOf(NotationTone(NotationPitch.Re))), NotationScript.Symbols)
            NotationBody("A curve below groups notes inside one beat. In this pair, give Sa half the beat and Re the other half. The next column starts on the next beat.")
        }
        NotationCard {
            NotationHeading("Pauses and other editions")
            NotationBody("The avagraha sign ऽ, sometimes typed like S, can extend a sung syllable. It is not a universal silence sign. On this page the letter S always means Sa.")
            NotationBody("Rest conventions vary between books. A rest may be left blank or use a mark explained by that edition. These examples contain no rests. Check a score's key before treating a blank or dash as silence.")
        }
        NotationSource("Notation guide · Raag Hindustani", "https://raag-hindustani.com/Notation.html")
        NotationSource("Symbol reference · Swar Sudha Trust", "https://swarsudhatrust.org/Notation.html")
    }
}

@Composable private fun NotationFingerings(flute: Flute) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NotationHeading("Six holes, counted from the breath")
        NotationBody("Count finger holes 1–6 from the blowing hole toward the open end, left to right here. The blowing hole is separate. These positions follow Basuri's three-hole Sa convention.")
        NotationHoleLegend()
        notationFingerings.forEach { position ->
            NotationCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NotationGlyph(position.tone)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(position.tone.pitch.swar.sargam + if (position.tone.octave == NotationOctave.Mandra) " · Mandra" else " · Madhya", style = MaterialTheme.typography.titleMedium)
                        Text(flute.notationPitch(position.tone), color = Indigo, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                NotationFingering(position.holes)
                Text(position.cue, color = Muted, style = MaterialTheme.typography.bodyLarge)
            }
        }
        NotationCard {
            NotationHeading("What half covered means")
            NotationFingering(listOf(NotationHole.Closed, NotationHole.Closed, NotationHole.Half, NotationHole.Open, NotationHole.Open, NotationHole.Open))
            Text("Illustration only, not an assigned note. A half-filled circle shows a partly covered hole. The amount of coverage is adjusted by listening; it need not be exactly half.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            NotationBody("Komal notes are generally played by half-holing. Exact fingerings for komal notes, Ma, tivra Ma and taar saptak vary by flute and playing technique. Upper notes also involve changes in air and overblowing. Check a reference tone and a teacher's chart for your flute.")
        }
        NotationSource("Fingering reference · Bansuri Academy", "https://bansuri.com/learn-bansuri/swaras-symbols/")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun NotationExamples(flute: Flute) {
    var scriptIndex by rememberSaveable { mutableIntStateOf(0) }
    var bpm by rememberSaveable { mutableIntStateOf(70) }
    val script = NotationScript.entries[scriptIndex]
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val player = remember(context, flute) { NotationPlayer(context) }
    val status by player.status.collectAsStateWithLifecycle()
    val activeExample = when (val state = status) {
        is NotationPlayback.Starting -> state.exampleIndex
        is NotationPlayback.Playing -> state.exampleIndex
        else -> null
    }
    var currentBeat by remember(player) { mutableIntStateOf(-1) }
    val examples = remember { notationExamples }

    DisposableEffect(player, owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.stop()
        }
        owner.lifecycle.addObserver(observer)
        // Leaving Examples or the Notation tab disposes this player as well.
        onDispose { owner.lifecycle.removeObserver(observer); player.stop() }
    }
    LaunchedEffect(player, status) {
        currentBeat = -1
        if (status is NotationPlayback.Playing) {
            while (true) withFrameNanos { currentBeat = player.position() }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NotationHeading("Read along slowly")
        NotationBody("Try saying the notes while counting. These are reading exercises in Keherwa, not prescribed raga compositions. Take a comfortable breath whenever you need one.")
        Text("Show notes as", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            NotationScript.entries.forEachIndexed { index, entry ->
                FilterChip(selected = index == scriptIndex, onClick = { scriptIndex = index },
                    label = { Text(entry.label) }, modifier = Modifier.heightIn(min = 48.dp).semantics {
                        contentDescription = when (entry) {
                            NotationScript.Symbols -> "Latin note letters"
                            NotationScript.Names -> "Sargam names"
                            NotationScript.Devanagari -> "Devanagari notes"
                        }
                    })
            }
        }
        NotationHeading("Play along")
        NotationBody("Hear the phrase at your flute's pitch. These are synthesized reference tones, not a recording of a basuri.")
        Text("Sa = ${flute.label} · Soft clicks mark each beat, with sam accented and khali quieter.",
            color = Indigo, style = MaterialTheme.typography.bodyMedium)
        Text("Tempo · beats per minute", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(50, 70, 90).forEach { tempo ->
                FilterChip(selected = bpm == tempo, onClick = { bpm = tempo }, enabled = activeExample == null,
                    label = { Text("$tempo BPM") }, modifier = Modifier.heightIn(min = 48.dp).semantics {
                        contentDescription = "$tempo beats per minute"
                    })
            }
        }
        Text("Each Play runs the phrase once. Stop playback to change the tempo.", color = Muted, style = MaterialTheme.typography.bodyMedium)
        when (val state = status) {
            is NotationPlayback.Stopped -> state.message?.let {
                Text(it, color = Muted, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            else -> Unit
        }
        examples.forEachIndexed { index, example ->
            val active = activeExample == index
            NotationCard {
                NotationHeading(example.title)
                Text("${notationKeherwa.name} · ${notationKeherwa.grouping}", color = Indigo, style = MaterialTheme.typography.bodyMedium)
                PrimaryButton(if (active) "Stop" else "Play along", {
                    currentBeat = -1
                    if (active) player.stop() else player.start(index, example, flute, bpm)
                }, modifier = Modifier.semantics {
                    contentDescription = if (active) "Stop ${example.title}" else "Play ${example.title} along with ${notationKeherwa.name}"
                    stateDescription = when {
                        status is NotationPlayback.Starting && active -> "Starting"
                        active -> "Playing"
                        else -> "Stopped"
                    }
                })
                val error = status as? NotationPlayback.Error
                if (error?.exampleIndex == index) {
                    Text(error.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                NotationTaalGrid(notationKeherwa, example.beats, script, if (active) currentBeat else -1)
                NotationBody(example.explanation)
            }
        }
    }
}

@Composable private fun NotationSource(label: String, url: String) {
    val uriHandler = LocalUriHandler.current
    TextButton(onClick = { uriHandler.openUri(url) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
