package np.basuri.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

val Ember = Color(0xFFD9692B)
val EmberWash = Color(0xFFFCEBDD)
val Frost = Color(0xFF3F7FB8)
val FrostWash = Color(0xFFE3EEF8)
private val Line = Color(0xFFE3E7F1)

private fun days(n: Int) = "$n ${if (n == 1) "day" else "days"}"

/** Compact streak count for the home header. Grey while today's lesson is still to do. */
@Composable internal fun StreakChip(progress: Progress) {
    val lit = progress.practiced(LocalDate.now())
    Surface(color = if (lit) EmberWash else Mist, shape = CircleShape, modifier = Modifier.semantics(mergeDescendants = true) {
        contentDescription = if (progress.streak == 0) "No streak yet" else "${progress.streak}-day streak" + if (lit) "" else ", practice today to keep it"
    }) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(18.dp), tint = if (lit) Ember else Muted)
            Text("${progress.streak}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (lit) Ember else Muted,
                modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable internal fun StreakCard(progress: Progress) {
    val today = LocalDate.now()
    val lit = progress.practiced(today)
    var calendar by remember { mutableStateOf(false) }
    if (calendar) PracticeCalendarSheet(progress) { calendar = false }
    Surface(onClick = { calendar = true }, color = Color.White, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Line)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(if (lit) EmberWash else Mist, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = if (lit) Ember else Muted, modifier = Modifier.size(28.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(if (progress.streak == 0) "Start a streak" else "${progress.streak}-day streak", style = MaterialTheme.typography.titleLarge)
                    Text(when {
                        progress.streak == 0 -> "Finish a lesson today to light your first day."
                        lit -> "Today is done. Best: ${days(progress.bestStreak)}."
                        else -> "Finish a lesson today to keep it going."
                    }, color = if (progress.streakAtRisk) Ember else Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 2.dp))
                }
                Icon(Icons.Rounded.CalendarMonth, "Open practice calendar", tint = Indigo, modifier = Modifier.padding(end = if (progress.freezes > 0) 8.dp else 0.dp).size(20.dp))
                if (progress.freezes > 0) Surface(color = FrostWash, shape = CircleShape, modifier = Modifier.semantics {
                    contentDescription = "${progress.freezes} streak ${if (progress.freezes == 1) "freeze" else "freezes"} saved"
                }) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AcUnit, null, tint = Frost, modifier = Modifier.size(16.dp))
                        Text("${progress.freezes}", color = Frost, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            WeekStrip(progress, Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(14.dp))
            LevelBar(progress.xp)
        }
    }
}

@Composable private fun PracticeCalendarSheet(progress: Progress, onDismiss: () -> Unit) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    val first = (progress.sessions.keys + progress.frozen).minOrNull()?.let(YearMonth::from) ?: YearMonth.now()
    HelpSheet("Your practice", onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton({ month = month.minusMonths(1) }, enabled = month > first) { Icon(Icons.Rounded.ChevronLeft, "Previous month") }
            Text("${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}", style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite })
            IconButton({ month = month.plusMonths(1) }, enabled = month < YearMonth.now()) { Icon(Icons.Rounded.ChevronRight, "Next month") }
        }
        MonthGrid(progress, month)
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Legend(Ember, "Practiced"); Legend(FrostWash, "Freeze"); Legend(Mist, "No lesson")
        }
        Spacer(Modifier.height(20.dp))
        listOf(
            Triple(Icons.Rounded.LocalFireDepartment, "Current streak", days(progress.streak)),
            Triple(Icons.Rounded.EmojiEvents, "Best streak", days(progress.bestStreak)),
            Triple(Icons.Rounded.CalendarMonth, "Practice days", "${progress.practiceDays}"),
            Triple(Icons.Rounded.Check, "Lessons completed", "${progress.totalSessions}"),
            Triple(Icons.Rounded.AvTimer, "Metronome practice", "${progress.rhythmSeconds / 60} min"),
            Triple(Icons.Rounded.AcUnit, "Streak freezes saved", "${progress.freezes} of ${Progress.MAX_FREEZES}"),
            Triple(Icons.Rounded.Star, "Total XP", "${progress.xp}"),
        ).chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (icon, label, value) ->
                    Surface(color = Color.White, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Line),
                        modifier = Modifier.weight(1f).fillMaxHeight().semantics(mergeDescendants = true) {}) {
                        Column(Modifier.padding(12.dp)) {
                            Icon(icon, null, tint = Indigo, modifier = Modifier.size(18.dp))
                            Text(value, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 6.dp))
                            Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Text("Streaks count days with at least one completed lesson. A freeze is earned every 7 streak days, up to ${Progress.MAX_FREEZES}, and covers a missed day automatically. Metronome practice earns an achievement but does not keep a streak.",
            color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 5.dp))
    }
}

@Composable private fun MonthGrid(progress: Progress, month: YearMonth) {
    val today = LocalDate.now()
    val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val lead = Math.floorMod(month.atDay(1).dayOfWeek.value - firstDay.value, 7)
    val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        (0 until 7).forEach { i ->
            Text(firstDay.plus(i.toLong()).getDisplayName(TextStyle.NARROW, Locale.getDefault()), color = Muted, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f).clearAndSetSemantics {})
        }
    }
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            week.forEach { day ->
                Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                    if (day != null) {
                        val practiced = progress.practiced(day)
                        val frozen = day in progress.frozen
                        Box(Modifier.fillMaxSize().background(when { practiced -> Ember; frozen -> FrostWash; else -> if (day > today) Color.Transparent else Mist }, CircleShape)
                            .then(if (day == today) Modifier.border(2.dp, Indigo, CircleShape) else Modifier)
                            .clearAndSetSemantics {
                                contentDescription = "${day.dayOfMonth} ${day.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}: " +
                                    when { practiced -> "${progress.sessions[day]} ${if (progress.sessions[day] == 1) "lesson" else "lessons"}"; frozen -> "streak freeze"; day > today -> "upcoming"; else -> "no lesson" }
                            }, contentAlignment = Alignment.Center) {
                            Text("${day.dayOfMonth}", style = MaterialTheme.typography.labelMedium, fontWeight = if (day == today) FontWeight.Bold else FontWeight.Medium,
                                color = when { practiced -> Color.White; frozen -> Frost; day > today -> Muted.copy(.5f); else -> Ink })
                        }
                    }
                }
            }
            repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** The last seven days, ending today: practiced, covered by a freeze, missed, or still open. */
@Composable internal fun WeekStrip(progress: Progress, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val week = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val summary = week.joinToString(". ") { day ->
        val name = day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        "$name: " + when { progress.practiced(day) -> "practiced"; day in progress.frozen -> "streak freeze"; day == today -> "not yet"; else -> "no practice" }
    }
    Row(modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "Last seven days. $summary" }, horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEach { day ->
            val practiced = progress.practiced(day)
            val frozen = day in progress.frozen
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), color = if (day == today) Ink else Muted,
                    style = MaterialTheme.typography.labelMedium, fontWeight = if (day == today) FontWeight.Bold else FontWeight.Medium)
                Box(Modifier.padding(top = 6.dp).size(32.dp)
                    .background(when { practiced -> Ember; frozen -> FrostWash; else -> Mist }, CircleShape)
                    .then(if (day == today && !practiced) Modifier.border(2.dp, Indigo, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center) {
                    when {
                        practiced -> Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        frozen -> Icon(Icons.Rounded.AcUnit, null, tint = Frost, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable internal fun LevelBar(xp: Int, animateFrom: Int = xp) {
    val shown = remember { Animatable(animateFrom.toFloat()) }
    LaunchedEffect(xp) { shown.animateTo(xp.toFloat(), tween(if (animateFrom == xp) 0 else 1100, delayMillis = 250, easing = FastOutSlowInEasing)) }
    val value = shown.value.toInt()
    val level = Levels.levelFor(value)
    val (into, size) = Levels.within(value)
    Column(Modifier.semantics(mergeDescendants = true) {
        contentDescription = "Level ${Levels.levelFor(xp)}, ${Levels.title(Levels.levelFor(xp))}. ${Levels.within(xp).first} of ${Levels.within(xp).second} XP to the next level. $xp XP in total."
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).background(Indigo, CircleShape), contentAlignment = Alignment.Center) {
                Text("$level", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Text(Levels.title(level), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
            Text("$into / $size XP", color = Muted, style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(progress = { into.toFloat() / size }, color = Indigo, trackColor = Mist, drawStopIndicator = {},
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(8.dp))
    }
}

@Composable internal fun BadgeTile(badge: Badge, progress: Progress, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val earned = badge.earned(progress)
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(48.dp).background(if (earned) Bamboo.copy(.35f) else Mist, CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (earned) badge.icon else Icons.Rounded.Lock, null, tint = if (earned) Ink else Muted.copy(.7f), modifier = Modifier.size(if (earned) 24.dp else 18.dp))
            }
            Text(badge.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                color = if (earned) Ink else Muted, modifier = Modifier.padding(top = 8.dp))
            if (!earned) Text("${badge.progressOf(progress)}/${badge.goal}", style = MaterialTheme.typography.labelMedium, color = Muted)
        }
    }
    val description = "${badge.title}. ${badge.detail}. " + if (earned) "Earned" else "${badge.progressOf(progress)} of ${badge.goal}"
    if (onClick != null) Surface(onClick = onClick, color = Color.White, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line),
        modifier = modifier.clearAndSetSemantics { contentDescription = description; role = Role.Button; onClick(null) { onClick(); true } }) { content() }
    else Surface(color = Color.White, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line),
        modifier = modifier.clearAndSetSemantics { contentDescription = description }) { content() }
}

@Composable internal fun AchievementsSection(progress: Progress) {
    var open by remember { mutableStateOf<Badge?>(null) }
    val earned = Badge.entries.count { it.earned(progress) }
    Row(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
        Text("Achievements", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
        Text("$earned of ${Badge.entries.size}", color = Muted, style = MaterialTheme.typography.labelMedium)
    }
    var all by remember { mutableStateOf(false) }
    // Earned badges first, then the ones closest to being earned.
    val shown = Badge.entries.sortedWith(compareByDescending<Badge> { it.earned(progress) }.thenByDescending { it.progressOf(progress).toFloat() / it.goal }).take(3)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        shown.forEach { BadgeTile(it, progress, Modifier.weight(1f).fillMaxHeight()) { open = it } }
    }
    TextButton({ all = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 4.dp)) { Text("See all ${Badge.entries.size} achievements") }
    if (all) HelpSheet("Achievements", { all = false }) {
        Text("$earned of ${Badge.entries.size} earned", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
        Badge.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { BadgeTile(it, progress, Modifier.weight(1f).fillMaxHeight()) { open = it } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    open?.let { badge ->
        HelpSheet(badge.title, { open = null }) {
            Text(badge.detail, style = MaterialTheme.typography.bodyLarge)
            Text(if (badge.earned(progress)) "Earned. Nice work." else "Progress: ${badge.progressOf(progress)} of ${badge.goal}.",
                color = if (badge.earned(progress)) Green else Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
            if (badge in setOf(Badge.Streak3, Badge.Streak7, Badge.Streak30)) Text("Streaks count days with at least one completed lesson. A streak freeze covers one missed day; you earn one every 7 streak days, up to ${Progress.MAX_FREEZES}.",
                color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

/** The top of the completion screen: the streak moment when today is newly lit, otherwise a check. */
@Composable internal fun CompletionHero(reward: Reward) {
    val pop = remember { Animatable(.4f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = .45f, stiffness = Spring.StiffnessLow)) }
    if (reward.streakExtended && reward.streak > 0) {
        Box(Modifier.size(96.dp).scale(pop.value).background(EmberWash, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.LocalFireDepartment, null, tint = Ember, modifier = Modifier.size(56.dp))
        }
        Text("${reward.streak}", fontFamily = Rounded, fontSize = 44.sp, color = Ember, modifier = Modifier.padding(top = 10.dp))
        Text(if (reward.streak == 1) "Day one of your streak" else "${days(reward.streak)} in a row", style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    } else {
        Box(Modifier.size(72.dp).scale(pop.value).background(GreenWash, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = Green, modifier = Modifier.size(36.dp))
        }
        Text("Lesson complete", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 20.dp))
    }
}

@Composable internal fun RewardCard(reward: Reward, progress: Progress) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("XP earned", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("+${reward.xp}", fontFamily = Rounded, fontSize = 26.sp, color = Indigo)
            }
            reward.lines.forEach { (label, xp) ->
                Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.weight(1f))
                    Text("+$xp", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Line)
            LevelBar(progress.xp, animateFrom = (progress.xp - reward.xp).coerceAtLeast(0))
            if (progress.level > reward.levelBefore) Surface(color = Mist, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 14.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.KeyboardDoubleArrowUp, null, tint = Indigo)
                    Text("Level ${progress.level} reached: ${Levels.title(progress.level)}", style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (reward.freezeEarned) Surface(color = FrostWash, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 10.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AcUnit, null, tint = Frost)
                    Text("Streak freeze earned. It covers one missed day automatically.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
    if (reward.newBadges.isNotEmpty()) {
        Text(if (reward.newBadges.size == 1) "New achievement" else "New achievements", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp))
        reward.newBadges.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { BadgeTile(it, progress, Modifier.weight(1f).fillMaxHeight()) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
