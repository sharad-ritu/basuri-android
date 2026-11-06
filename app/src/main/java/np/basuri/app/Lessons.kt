package np.basuri.app

import kotlin.math.pow

/** Register of a swar relative to the learner's chosen Sa. */
enum class Octave(val prefix: String) { Mandra("low "), Madhya(""), Taar("high ") }

/**
 * Notes on a standard six-hole Nepali basuri (the same fingering as a Hindustani bansuri), counting holes from the blowing hole, with
 * Sa as the top three holes closed. Mandra notes close more holes and need slower, warmer air.
 * Pa, Dha, Ni, and high Sa reuse a lower fingering with overblowing: a faster, narrower air stream.
 */
enum class Swar(val label: String, val semitones: Int, val closed: Int, val octave: Octave = Octave.Madhya) {
    MandraPa("Pa", -5, 6, Octave.Mandra), MandraDha("Dha", -3, 5, Octave.Mandra), MandraNi("Ni", -1, 4, Octave.Mandra),
    // Shuddh Ma is omitted: it needs a partly covered hole 1 (all six open sounds tivra Ma), which these diagrams cannot show yet.
    Sa("Sa", 0, 3), Re("Re", 2, 2), Ga("Ga", 4, 1),
    Pa("Pa", 7, 6), Dha("Dha", 9, 5), Ni("Ni", 11, 4), TaarSa("Sa", 12, 3, Octave.Taar);

    /** Played by overblowing a lower fingering. */
    val overblown: Boolean get() = semitones >= 7
    /** Words for TalkBack and prose, such as "low Pa" or "high Sa". */
    val spoken: String get() = octave.prefix + label
}
val pitchNames = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
/**
 * The learner's three-hole Sa. [cents] shifts every reference to match a flute that is not tuned
 * exactly to A4 = 440 Hz; pitch names stay those of the nearest equal-tempered note.
 */
data class Flute(val pitchClass: Int = 7, val octave: Int = 4, val cents: Int = 0) {
    val midi: Int get() = (octave + 1) * 12 + pitchClass
    val label: String get() = "${pitchNames[pitchClass]}$octave"
    /** "+12 cents", or an empty string when the flute is at concert pitch. */
    val tuningLabel: String get() = if (cents == 0) "" else "%+d cents".format(cents)
    fun frequency(swar: Swar): Double = frequency(swar.semitones)
    /** The reference [semitones] above (or below) Sa, including the fine tuning. */
    fun frequency(semitones: Int): Double = 440.0 * 2.0.pow((midi + semitones - 69 + cents / 100.0) / 12.0)
    companion object {
        const val MAX_CENTS = 50
        /** The nearest Sa setting for a heard pitch, or null when it falls outside C3–B5. */
        fun fromFrequency(hz: Double): Flute? {
            val exact = 69 + 12 * kotlin.math.log2(hz / 440.0)
            val nearest = kotlin.math.round(exact).toInt()
            val octave = Math.floorDiv(nearest, 12) - 1
            if (octave !in 3..5) return null
            return Flute(Math.floorMod(nearest, 12), octave, kotlin.math.round((exact - nearest) * 100).toInt().coerceIn(-MAX_CENTS, MAX_CENTS))
        }
    }
    fun pitchLabel(swar: Swar): String { val m = midi + swar.semitones; return "${pitchNames[Math.floorMod(m, 12)]}${Math.floorDiv(m, 12) - 1}" }
}
sealed interface GuidePicture {
    data object Flute : GuidePicture
    data object Breath : GuidePicture
    data class Fingers(val note: Swar) : GuidePicture
    /** A short written phrase, with an optional caption such as the fingering sequence. */
    data class Phrase(val notes: List<Swar>, val caption: String = "") : GuidePicture
}
data class Guide(val title: String, val cue: String, val detail: String, val picture: GuidePicture)
/** A themed group of lessons on the Learn path. */
data class Course(val id: Int, val title: String, val subtitle: String)
data class Lesson(
    val id: String, val title: String, val subtitle: String, val minutes: Int,
    val guides: List<Guide>, val notes: List<Swar>, val practiceTip: String, val unit: Int = 1,
)

fun fingeringCue(note: Swar): String = when (note.closed) {
    1 -> "Close hole 1"
    2 -> "Close holes 1 and 2"
    6 -> "Close all six holes"
    else -> "Close holes 1–${note.closed}"
} + if (note.overblown) " and overblow" else ""

fun lessonIndex(id: String) = lessons.indexOfFirst { it.id == id }
fun courseOf(lesson: Lesson) = courses.first { it.id == lesson.unit }
