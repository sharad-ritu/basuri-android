package np.basuri.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate

/** Achievements are derived from saved progress, so they never disagree with it. */
enum class Badge(val title: String, val detail: String, val icon: ImageVector, val goal: Int, val value: (Progress) -> Int) {
    FirstSound("First sound", "Finish your first lesson", Icons.Rounded.MusicNote, 1, { it.completed.size }),
    Streak3("Three in a row", "Practice 3 days in a row", Icons.Rounded.LocalFireDepartment, 3, { it.bestStreak }),
    PitchPerfect("Pitch perfect", "Match every note in a lesson with the microphone", Icons.Rounded.GraphicEq, 1, { it.perfectLessons }),
    PathDone("First notes", "Complete Unit 1", Icons.Rounded.Route, lessons.count { it.unit == 1 }, { p -> lessons.count { it.unit == 1 && it.id in p.completed } }),
    LongBreath("Long breath", "Hold one steady long tone for 15 seconds", Icons.Rounded.Air, 15, { (it.bestHoldMs / 1000).toInt() }),
    Streak7("Full week", "Practice 7 days in a row", Icons.Rounded.Whatshot, 7, { it.bestStreak }),
    Sessions10("Ten sessions", "Complete 10 lessons in total", Icons.Rounded.Repeat, 10, { it.totalSessions }),
    FullRiyaz("Full riyaz", "Warm up, finish a lesson, and keep time on 5 days", Icons.Rounded.SelfImprovement, 5, { it.fullRiyazDays }),
    Days14("Two weeks of practice", "Practice on 14 different days", Icons.Rounded.CalendarMonth, 14, { it.practiceDays }),
    Streak30("Month of breath", "Practice 30 days in a row", Icons.Rounded.EmojiEvents, 30, { it.bestStreak }),
    SteadyPulse("Steady pulse", "Practice 15 minutes with the metronome", Icons.Rounded.AvTimer, 15, { (it.rhythmSeconds / 60).toInt() }),
    Sessions50("Fifty sessions", "Complete 50 lessons in total", Icons.Rounded.MilitaryTech, 50, { it.totalSessions }),
    AllLessons("Whole course", "Complete every lesson on the Learn path", Icons.Rounded.WorkspacePremium, lessons.size, { it.completed.size });

    fun earned(progress: Progress) = value(progress) >= goal
    fun progressOf(progress: Progress) = value(progress).coerceAtMost(goal)
}

/** What one completed lesson earned. Stored as primitives so it survives process recreation. */
data class Reward(
    val lines: List<Pair<String, Int>> = emptyList(),
    val streak: Int = 0,
    val streakExtended: Boolean = false,
    val newBadges: List<Badge> = emptyList(),
    val levelBefore: Int = 1,
    val freezeEarned: Boolean = false,
) {
    val xp: Int get() = lines.sumOf { it.second }
}

object Rewards {
    const val LESSON = 10
    const val FIRST_TIME = 20
    const val MATCHED = 5
    const val SELF_GUIDED = 2
    const val DAILY_GOAL = 15
    const val FIRST_TODAY = 10

    /** Applies a completed lesson to [before] and returns the saved progress with what it earned. */
    fun complete(before: Progress, lessonId: String, matched: Int, total: Int, today: LocalDate = LocalDate.now()): Pair<Progress, Reward> {
        val countBefore = before.sessions[today] ?: 0
        val lines = buildList {
            add("Lesson complete" to LESSON)
            if (lessonId !in before.completed) add("First time through" to FIRST_TIME)
            if (matched > 0) add("$matched pitch-matched ${if (matched == 1) "note" else "notes"}" to matched * MATCHED)
            if (total - matched > 0) add("${total - matched} self-guided ${if (total - matched == 1) "note" else "notes"}" to (total - matched) * SELF_GUIDED)
            if (countBefore == 0) add("First lesson today" to FIRST_TODAY)
            if (countBefore + 1 >= before.dailyGoal && today !in before.goalBonusDays) add("Daily goal reached" to DAILY_GOAL)
        }
        var after = before.copy(
            completed = before.completed + lessonId,
            sessions = before.sessions + (today to countBefore + 1),
            xp = before.xp + lines.sumOf { it.second },
            perfectLessons = before.perfectLessons + if (total > 0 && matched == total) 1 else 0,
            goalBonusDays = if (countBefore + 1 >= before.dailyGoal) before.goalBonusDays + today else before.goalBonusDays,
        )
        val streak = after.streak
        val freezeEarned = countBefore == 0 && streak > 0 && streak % 7 == 0 && after.freezes < Progress.MAX_FREEZES
        after = after.copy(bestStreak = maxOf(after.bestStreak, streak), freezes = after.freezes + if (freezeEarned) 1 else 0)
        val newBadges = Badge.entries.filter { it.earned(after) && !it.earned(before) }
        return after to Reward(lines, streak, countBefore == 0, newBadges, before.level, freezeEarned)
    }
}
