package np.basuri.app

/** How a vibhag (section) of a taal begins, in Bhatkhande notation. */
enum class TaalMark(val symbol: String, val label: String) {
    Sam("X", "Sam"), Khali("0", "Khali"), Tali("", "Tali")
}

/** One vibhag: its first beat's mark, the tali number shown for [TaalMark.Tali], and its theka bols. */
data class Vibhag(val mark: TaalMark, val bols: List<String>, val taliNumber: Int = 0) {
    val symbol: String get() = if (mark == TaalMark.Tali) "$taliNumber" else mark.symbol
}

/**
 * A rhythm cycle shared by Nepali and Hindustani music. Shared by the metronome and the notation page so both describe
 * the same structure. Bols are the common tabla theka; they are a reference, not a lesson.
 */
data class Taal(val id: String, val name: String, val vibhags: List<Vibhag>, val note: String = "") {
    val beats: Int get() = vibhags.sumOf { it.bols.size }
    val bols: List<String> get() = vibhags.flatMap { it.bols }
    val grouping: String get() = vibhags.joinToString("+") { "${it.bols.size}" }
    /** The mark at a 0-based beat index, or null when that beat is not the start of a vibhag. */
    fun markAt(beat: Int): Vibhag? {
        var start = 0
        vibhags.forEach { if (start == beat) return it; start += it.bols.size }
        return null
    }
}

val taals = listOf(
    Taal("teentaal", "Teentaal", listOf(
        Vibhag(TaalMark.Sam, listOf("Dha", "Dhin", "Dhin", "Dha")),
        Vibhag(TaalMark.Tali, listOf("Dha", "Dhin", "Dhin", "Dha"), 2),
        Vibhag(TaalMark.Khali, listOf("Dha", "Tin", "Tin", "Ta")),
        Vibhag(TaalMark.Tali, listOf("Ta", "Dhin", "Dhin", "Dha"), 3),
    ), "The most common cycle in Nepali folk songs and Hindustani music."),
    Taal("keherwa", "Keherwa", listOf(
        Vibhag(TaalMark.Sam, listOf("Dha", "Ge", "Na", "Ti")),
        Vibhag(TaalMark.Khali, listOf("Na", "Ka", "Dhi", "Na")),
    ), "A light eight-beat cycle used in folk and film songs."),
    Taal("dadra", "Dadra", listOf(
        Vibhag(TaalMark.Sam, listOf("Dha", "Dhi", "Na")),
        Vibhag(TaalMark.Khali, listOf("Dha", "Ti", "Na")),
    ), "A six-beat cycle with a lilting feel."),
    Taal("rupak", "Rupak", listOf(
        Vibhag(TaalMark.Khali, listOf("Tin", "Tin", "Na")),
        Vibhag(TaalMark.Tali, listOf("Dhi", "Na"), 1),
        Vibhag(TaalMark.Tali, listOf("Dhi", "Na"), 2),
    ), "Seven beats. Its first beat, sam, is also khali."),
    Taal("jhaptaal", "Jhaptaal", listOf(
        Vibhag(TaalMark.Sam, listOf("Dhi", "Na")),
        Vibhag(TaalMark.Tali, listOf("Dhi", "Dhi", "Na"), 2),
        Vibhag(TaalMark.Khali, listOf("Ti", "Na")),
        Vibhag(TaalMark.Tali, listOf("Dhi", "Dhi", "Na"), 3),
    ), "Ten beats in uneven sections of 2, 3, 2, and 3."),
    Taal("ektaal", "Ektaal", listOf(
        Vibhag(TaalMark.Sam, listOf("Dhin", "Dhin")),
        Vibhag(TaalMark.Khali, listOf("DhaGe", "TiRaKiTa")),
        Vibhag(TaalMark.Tali, listOf("Tu", "Na"), 2),
        Vibhag(TaalMark.Khali, listOf("Kat", "Ta")),
        Vibhag(TaalMark.Tali, listOf("DhaGe", "TiRaKiTa"), 3),
        Vibhag(TaalMark.Tali, listOf("Dhi", "Na"), 4),
    ), "Twelve beats in six sections of two."),
)
