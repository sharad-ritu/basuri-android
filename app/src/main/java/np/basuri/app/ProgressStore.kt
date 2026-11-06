package np.basuri.app

import android.content.Context
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** All saved data stays on this device. No recordings or pitch samples are stored. */
data class Progress(
    val onboarded: Boolean = false,
    val flute: Flute = Flute(),
    val dailyGoal: Int = 1,
    val completed: Set<String> = emptySet(),
    val sessions: Map<LocalDate, Int> = emptyMap(),
    val xp: Int = 0,
    val bestStreak: Int = 0,
    /** Unused streak freezes. One is earned every seven streak days, up to [MAX_FREEZES]. */
    val freezes: Int = 0,
    /** Missed days that a streak freeze covered. They keep a streak alive but are not practice. */
    val frozen: Set<LocalDate> = emptySet(),
    /** Lessons finished with every note pitch-matched by the microphone. */
    val perfectLessons: Int = 0,
    /** Total seconds of metronome practice. It earns an achievement but does not keep a lesson streak. */
    val rhythmSeconds: Long = 0,
    /** Daily reminder time in minutes after midnight, or -1 when reminders are off. */
    val reminderMinutes: Int = -1,
    /** A late-evening nudge when a streak would end tonight. */
    val streakSaver: Boolean = true,
    /** A Saturday evening summary of the week (Sunday to Saturday, as in Nepal). */
    val weeklyRecap: Boolean = true,
    /** Seconds of metronome practice per day, kept for the daily plan and the weekly summary. */
    val rhythmDays: Map<LocalDate, Long> = emptyMap(),
    /** Days with a long-tone warm-up of at least [WARMUP_MS]. */
    val warmupDays: Set<LocalDate> = emptySet(),
    /** Longest steady long tone, in milliseconds. */
    val bestHoldMs: Long = 0,
    /** The completion screen offered a reminder once; it does not ask again. */
    val reminderOffered: Boolean = false,
    /** Days whose daily-goal XP bonus was already paid, so changing the goal can't pay it twice. */
    val goalBonusDays: Set<LocalDate> = emptySet(),
    /** Sa came from the microphone, or the learner declined the offer to tune; either way, don't ask again. */
    val tunePromptDone: Boolean = false,
) {
    val today: LocalDate get() = LocalDate.now()
    val warmedUp: Boolean get() = today in warmupDays
    val rhythmToday: Long get() = rhythmDays[today] ?: 0
    val rhythmDone: Boolean get() = rhythmToday >= RHYTHM_GOAL_SECONDS
    val goalDone: Boolean get() = todayCount >= dailyGoal
    /** Warm-up, the daily lesson goal, and rhythm practice, all on one day. */
    fun fullRiyaz(date: LocalDate) = date in warmupDays && (sessions[date] ?: 0) > 0 && (rhythmDays[date] ?: 0) >= RHYTHM_GOAL_SECONDS
    val fullRiyazDays: Int get() = warmupDays.count(::fullRiyaz)
    val nextLessonIndex: Int get() = lessons.indexOfFirst { it.id !in completed }
    fun unlocked(index: Int) = index == 0 || lessons[index - 1].id in completed
    /** Notes from lessons the learner has reached, always including Sa, Re, and Ga. */
    val unlockedNotes: List<Swar> get() {
        val reached = lessons.filterIndexed { i, _ -> unlocked(i) }.flatMap { it.notes }.toSet() + setOf(Swar.Sa, Swar.Re, Swar.Ga)
        return Swar.entries.filter { it in reached }
    }
    val todayCount: Int get() = sessions[LocalDate.now()] ?: 0
    val totalSessions: Int get() = sessions.values.sum()
    val practiceDays: Int get() = sessions.size
    fun practiced(date: LocalDate) = (sessions[date] ?: 0) > 0
    fun keptStreak(date: LocalDate) = practiced(date) || date in frozen
    /** Consecutive kept days ending today, or ending yesterday while today is still open. */
    val streak: Int get() {
        var date = LocalDate.now()
        if (!keptStreak(date)) date = date.minusDays(1)
        var days = 0
        while (keptStreak(date)) { days++; date = date.minusDays(1) }
        return days
    }
    /** The streak is alive but today has no lesson yet. */
    val streakAtRisk: Boolean get() = streak > 0 && !practiced(LocalDate.now())
    val level: Int get() = Levels.levelFor(xp)

    /** Uses saved freezes to cover missed days since the last kept day, when there are enough. */
    fun withFreezesApplied(today: LocalDate = LocalDate.now()): Progress {
        val last = (sessions.keys + frozen).filter { it < today }.maxOrNull() ?: return this
        val missed = ChronoUnit.DAYS.between(last, today).toInt() - 1
        if (missed < 1 || missed > freezes) return this
        return copy(freezes = freezes - missed, frozen = frozen + (1..missed).map { last.plusDays(it.toLong()) })
    }

    companion object {
        const val MAX_FREEZES = 2
        const val RHYTHM_GOAL_SECONDS = 120L
        const val WARMUP_MS = 5000L
    }
}

/** Levels grow a little longer each time: 50 XP to reach level 2, then 60 more, 70 more, and so on. */
object Levels {
    fun xpToReach(level: Int): Int = (1 until level).sumOf { 40 + 10 * it }
    fun levelFor(xp: Int): Int { var level = 1; while (xp >= xpToReach(level + 1)) level++; return level }
    /** XP earned inside the current level, and the size of that level. */
    fun within(xp: Int): Pair<Int, Int> { val l = levelFor(xp); return (xp - xpToReach(l)) to (xpToReach(l + 1) - xpToReach(l)) }
    fun title(level: Int): String = when {
        level <= 1 -> "First breath"
        level == 2 -> "Steady Sa"
        level == 3 -> "Finding Re"
        level == 4 -> "Gentle Ga"
        level <= 6 -> "Daily player"
        level <= 9 -> "Devoted student"
        else -> "Basuri regular"
    }
}

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("basuri", Context.MODE_PRIVATE)
    private fun dates(key: String) = prefs.getString(key, "").orEmpty().split(';').mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
    fun load(): Progress {
        val sessions = prefs.getString("sessions", "").orEmpty().split(';').mapNotNull { entry ->
            val parts = entry.split('=')
            if (parts.size != 2) return@mapNotNull null
            val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val count = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            date to count
        }.toMap()
        val completed = prefs.getStringSet("completed", emptySet()).orEmpty().filter { id -> lessons.any { it.id == id } }.toSet()
        val frozen = dates("frozen")
        // Progress saved before XP existed earns the base and first-completion XP it would have received.
        val xp = if (prefs.contains("xp")) prefs.getInt("xp", 0) else sessions.values.sum() * Rewards.LESSON + completed.size * Rewards.FIRST_TIME
        val loaded = Progress(
            prefs.getBoolean("onboarded", false),
            Flute(prefs.getInt("pitchClass", 7).coerceIn(0, 11), prefs.getInt("octave", 4).coerceIn(3, 5),
                prefs.getInt("tuneCents", 0).coerceIn(-Flute.MAX_CENTS, Flute.MAX_CENTS)),
            prefs.getInt("goal", 1).coerceIn(1, 3),
            completed, sessions, xp.coerceAtLeast(0),
            maxOf(prefs.getInt("bestStreak", 0), longestRun(sessions.keys + frozen)),
            prefs.getInt("freezes", 0).coerceIn(0, Progress.MAX_FREEZES), frozen,
            prefs.getInt("perfect", 0).coerceAtLeast(0),
            prefs.getLong("rhythmSeconds", 0).coerceAtLeast(0),
            prefs.getInt("reminder", -1).let { if (it in 0 until 24 * 60) it else -1 },
            prefs.getBoolean("streakSaver", true), prefs.getBoolean("weeklyRecap", true),
            daySeconds(prefs.getString("rhythmDays", "")), dates("warmupDays"),
            prefs.getLong("bestHold", 0).coerceAtLeast(0), prefs.getBoolean("reminderOffered", false),
            dates("goalBonusDays"), prefs.getBoolean("tunePromptDone", false),
        )
        return loaded.withFreezesApplied().also { if (it != loaded) save(it) }
    }
    fun save(progress: Progress) {
        prefs.edit().putBoolean("onboarded", progress.onboarded)
            .putInt("pitchClass", progress.flute.pitchClass).putInt("octave", progress.flute.octave).putInt("tuneCents", progress.flute.cents)
            .putInt("goal", progress.dailyGoal).putStringSet("completed", progress.completed)
            .putString("sessions", progress.sessions.entries.joinToString(";") { "${it.key}=${it.value}" })
            .putInt("xp", progress.xp).putInt("bestStreak", progress.bestStreak).putInt("freezes", progress.freezes)
            .putString("frozen", progress.frozen.joinToString(";")).putInt("perfect", progress.perfectLessons)
            .putLong("rhythmSeconds", progress.rhythmSeconds).putInt("reminder", progress.reminderMinutes)
            .putBoolean("streakSaver", progress.streakSaver).putBoolean("weeklyRecap", progress.weeklyRecap)
            // One short entry per practice day; kept in full because achievements are computed from it.
            .putString("rhythmDays", progress.rhythmDays.entries.joinToString(";") { "${it.key}=${it.value}" })
            .putString("goalBonusDays", progress.goalBonusDays.filter { it > LocalDate.now().minusDays(7) }.joinToString(";"))
            .putString("warmupDays", progress.warmupDays.joinToString(";")).putLong("bestHold", progress.bestHoldMs)
            .putBoolean("reminderOffered", progress.reminderOffered).putBoolean("tunePromptDone", progress.tunePromptDone)
            .apply()
    }
    private fun daySeconds(raw: String?) = raw.orEmpty().split(';').mapNotNull { entry ->
        val parts = entry.split('=')
        val date = parts.getOrNull(0)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null
        parts.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0 }?.let { date to it }
    }.toMap()
    /** Keeps the flute and settings; clears lessons, history, XP, and achievements. */
    fun reset(progress: Progress): Progress = Progress(onboarded = true, flute = progress.flute, dailyGoal = progress.dailyGoal,
        reminderMinutes = progress.reminderMinutes, streakSaver = progress.streakSaver, weeklyRecap = progress.weeklyRecap, reminderOffered = true,
        tunePromptDone = progress.tunePromptDone)
    private fun longestRun(days: Set<LocalDate>): Int {
        var best = 0
        days.forEach { day ->
            if (day.minusDays(1) in days) return@forEach
            var length = 0
            while (day.plusDays(length.toLong()) in days) length++
            best = maxOf(best, length)
        }
        return best
    }
}
