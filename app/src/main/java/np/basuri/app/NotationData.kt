package np.basuri.app

/** Relative note names, independent of the beginner lessons' three playable targets. */
internal enum class NotationSwar(
    val symbol: String, val sargam: String, val fullName: String,
    val devanagari: String, val semitones: Int,
) {
    Sa("S", "Sa", "Shadja", "सा", 0),
    Re("R", "Re", "Rishabh", "रे", 2),
    Ga("G", "Ga", "Gandhar", "ग", 4),
    Ma("M", "Ma", "Madhyam", "म", 5),
    Pa("P", "Pa", "Pancham", "प", 7),
    Dha("D", "Dha", "Dhaivat", "ध", 9),
    Ni("N", "Ni", "Nishad", "नि", 11);

    val achal: Boolean get() = this == Sa || this == Pa
}

internal enum class NotationAlteration(val description: String, val offset: Int) {
    Shuddh("shuddh", 0), Komal("komal", -1), Tivra("tivra", 1)
}

/** Enumerating the twelve forms prevents unsupported combinations such as komal Sa. */
internal enum class NotationPitch(
    val swar: NotationSwar, val alteration: NotationAlteration = NotationAlteration.Shuddh,
) {
    Sa(NotationSwar.Sa),
    KomalRe(NotationSwar.Re, NotationAlteration.Komal),
    Re(NotationSwar.Re),
    KomalGa(NotationSwar.Ga, NotationAlteration.Komal),
    Ga(NotationSwar.Ga),
    Ma(NotationSwar.Ma),
    TivraMa(NotationSwar.Ma, NotationAlteration.Tivra),
    Pa(NotationSwar.Pa),
    KomalDha(NotationSwar.Dha, NotationAlteration.Komal),
    Dha(NotationSwar.Dha),
    KomalNi(NotationSwar.Ni, NotationAlteration.Komal),
    Ni(NotationSwar.Ni);

    val semitones: Int get() = swar.semitones + alteration.offset
    val spokenName: String get() = if (swar.achal) swar.sargam else "${alteration.description} ${swar.sargam}"
}

internal enum class NotationOctave(val title: String, val description: String, val offset: Int) {
    Mandra("Mandra", "lower octave", -12),
    Madhya("Madhya", "middle octave", 0),
    Taar("Taar", "upper octave", 12)
}

internal data class NotationTone(
    val pitch: NotationPitch,
    val octave: NotationOctave = NotationOctave.Madhya,
) {
    val description: String get() = "${pitch.spokenName}, ${octave.description}"
}

internal enum class NotationScript(val label: String) {
    Symbols("S R G"), Names("Sa Re Ga"), Devanagari("सा रे ग");

    fun text(swar: NotationSwar): String = when (this) {
        Symbols -> swar.symbol
        Names -> swar.sargam
        Devanagari -> swar.devanagari
    }
}

internal fun Flute.notationPitch(tone: NotationTone): String {
    val noteMidi = midi + tone.pitch.semitones + tone.octave.offset
    return "${pitchNames[Math.floorMod(noteMidi, 12)]}${Math.floorDiv(noteMidi, 12) - 1}"
}

internal fun Swar.notationTone(): NotationTone = NotationTone(when (label) {
    "Sa" -> NotationPitch.Sa
    "Re" -> NotationPitch.Re
    "Ga" -> NotationPitch.Ga
    "Ma" -> NotationPitch.Ma
    "Pa" -> NotationPitch.Pa
    "Dha" -> NotationPitch.Dha
    else -> NotationPitch.Ni
}, when (octave) { Octave.Mandra -> NotationOctave.Mandra; Octave.Madhya -> NotationOctave.Madhya; Octave.Taar -> NotationOctave.Taar })

internal val notationShuddh = NotationPitch.entries.filter { it.alteration == NotationAlteration.Shuddh }

internal enum class NotationHole(val description: String) {
    Closed("closed"), Half("half covered"), Open("open")
}

internal data class NotationFingeringPosition(val tone: NotationTone, val closed: Int) {
    val holes: List<NotationHole> get() = List(6) { if (it < closed) NotationHole.Closed else NotationHole.Open }
    val cue: String get() = when (closed) {
        1 -> "Close hole 1. Leave holes 2–6 open."
        5 -> "Close holes 1–5. Leave hole 6 open."
        6 -> "Close all six holes."
        else -> "Close holes 1–$closed. Leave holes ${closed + 1}–6 open."
    }
}

// Three-hole Sa convention; Bansuri Academy: https://bansuri.com/learn-bansuri/swaras-symbols/
internal val notationFingerings = Swar.entries.sortedBy { -it.closed }.filter { !it.overblown && it.closed > 0 }
    .map { NotationFingeringPosition(it.notationTone(), it.closed) }

internal sealed interface NotationBeat {
    /** All of these notes share one beat; no empty note groups. */
    data class Notes(val first: NotationTone, val following: List<NotationTone> = emptyList()) : NotationBeat {
        val tones: List<NotationTone> get() = listOf(first) + following
    }
    data object Hold : NotationBeat
}

internal data class NotationExample(val title: String, val explanation: String, val beats: List<NotationBeat>)

internal val notationKeherwa: Taal get() = taals.first { it.id == "keherwa" }

internal val notationExamples: List<NotationExample> get() {
    val scale = notationShuddh.map { NotationTone(it) } + NotationTone(NotationPitch.Sa, NotationOctave.Taar)
    fun beat(pitch: NotationPitch) = NotationBeat.Notes(NotationTone(pitch))
    return listOf(
        NotationExample("Your lesson melody", "Sa, Re, Ga, Re, Sa. Play one note on each of beats 1–5, then continue the last Sa through beats 6–8. This adds a reading rhythm to lesson 4; the lesson itself does not check timing.",
            lessons.first { it.id == "changes" }.notes.map { NotationBeat.Notes(it.notationTone()) } + List(3) { NotationBeat.Hold }),
        NotationExample("Aroh · going up", "Read left to right, one note per beat. The dot on the last Sa places it one octave above your starting Sa. This is a shuddh scale exercise, not a complete raga.",
            scale.map { NotationBeat.Notes(it) }),
        NotationExample("Avroh · coming down", "Begin on upper Sa and step down to middle Sa. The octave dot belongs to the note, not to a beat or a pause.",
            scale.reversed().map { NotationBeat.Notes(it) }),
        NotationExample("Hold a note in Keherwa", "Begin Sa on beat 1 and continue it on beat 2. Play Re, Ga, then Re on beat 5. Hold Re on 6, return to Sa on 7, and hold it on 8.",
            listOf(beat(NotationPitch.Sa), NotationBeat.Hold, beat(NotationPitch.Re), beat(NotationPitch.Ga), beat(NotationPitch.Re), NotationBeat.Hold, beat(NotationPitch.Sa), NotationBeat.Hold)),
        NotationExample("Two notes inside one beat", "On beat 1, share the time equally between Sa and Re. The curve below groups them into one beat. Ga begins on beat 2. A curve below is a beat group, not an instruction to slide between pitches.",
            listOf(NotationBeat.Notes(NotationTone(NotationPitch.Sa), listOf(NotationTone(NotationPitch.Re))), beat(NotationPitch.Ga), beat(NotationPitch.Re), beat(NotationPitch.Sa), beat(NotationPitch.Re), beat(NotationPitch.Ga), beat(NotationPitch.Re), beat(NotationPitch.Sa))),
    )
}

// Bhatkhande marks and one-beat curves: https://raag-hindustani.com/Notation.html
// Dash/avagraha conventions: https://swarsudhatrust.org/Notation.html
// Rest symbols differ across transcriptions, so examples deliberately contain no rests.
