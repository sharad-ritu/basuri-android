package np.basuri.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private val Line = Color(0xFFE3E7F1)

/** The lesson to suggest today: the next new lesson, or a rotating review once the course is done. */
internal fun todaysLesson(progress: Progress): Int = progress.nextLessonIndex.takeIf { it >= 0 } ?: (LocalDate.now().dayOfYear % lessons.size)

@Composable internal fun HomeScreen(progress: Progress, onLesson: (Int) -> Unit, onSettings: () -> Unit, onWarmup: () -> Unit, onTools: () -> Unit) {
    val next = progress.nextLessonIndex
    val currentUnit = lessons.getOrNull(next)?.unit ?: courses.last().id
    // Keyed on the current unit so finishing a unit opens the next one, even with Home's state kept between tabs.
    var expanded by rememberSaveable(currentUnit) { mutableStateOf("$currentUnit") }
    val open = expanded.split(',').mapNotNull { it.toIntOrNull() }.toSet()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_launcher), null, tint = Color.Unspecified, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)))
            Text("basuri", fontFamily = Rounded, fontSize = 25.sp, modifier = Modifier.padding(start = 9.dp))
            Spacer(Modifier.weight(1f))
            StreakChip(progress)
            Spacer(Modifier.width(8.dp))
            Surface(onClick = onSettings, color = Mist, shape = CircleShape) {
                Row(Modifier.padding(horizontal = 13.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Tune, null, Modifier.size(16.dp), tint = Indigo)
                    Text("Sa ${progress.flute.label}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        TodayCard(progress, { onLesson(todaysLesson(progress)) }, onWarmup, onTools)
        Spacer(Modifier.height(12.dp))
        StreakCard(progress)
        Row(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
            Text("Learn path", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f).semantics { heading() })
            Text("${progress.completed.size} of ${lessons.size} lessons", color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 6.dp))
        }
        courses.forEach { course ->
            val members = lessons.withIndex().filter { it.value.unit == course.id }
            if (members.isEmpty()) return@forEach
            UnitHeader(course, members.count { it.value.id in progress.completed }, members.size, progress.unlocked(members.first().index),
                course.id in open) { expanded = (if (course.id in open) open - course.id else open + course.id).joinToString(",") }
            AnimatedVisibility(course.id in open) {
                Column(Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                    members.forEachIndexed { position, (index, lesson) -> LessonRow(progress, index, lesson, index == next, position == members.lastIndex, onLesson) }
                }
            }
        }
        if (next < 0) Surface(color = GreenWash, shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(top = 8.dp)) {
            Text("You've completed every lesson. Repeat any lesson, or keep warming up and keeping time each day.", color = Green, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        }
        AchievementsSection(progress)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable private fun UnitHeader(course: Course, done: Int, total: Int, unlocked: Boolean, open: Boolean, onToggle: () -> Unit) {
    val turn by animateFloatAsState(if (open) 180f else 0f, label = "Unit chevron")
    Surface(onClick = onToggle, color = Color.Transparent, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        .semantics { stateDescription = if (open) "Expanded" else "Collapsed" }) {
        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                if (!unlocked) Box(Modifier.size(42.dp).background(Mist, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Lock, null, tint = Muted, modifier = Modifier.size(18.dp)) }
                else {
                    CircularProgressIndicator(progress = { done.toFloat() / total }, modifier = Modifier.fillMaxSize(), color = if (done == total) Green else Indigo, trackColor = Mist, strokeWidth = 3.dp)
                    if (done == total) Icon(Icons.Rounded.Check, null, tint = Green, modifier = Modifier.size(18.dp))
                    else Text("$done/$total", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("UNIT ${course.id}", color = if (unlocked) Indigo else Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(course.title, style = MaterialTheme.typography.titleLarge, color = if (unlocked) Ink else Muted)
                Text(if (unlocked) course.subtitle else "Finish Unit ${course.id - 1} to unlock", color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            Icon(Icons.Rounded.ExpandMore, if (open) "Collapse ${course.title}" else "Expand ${course.title}", tint = Muted, modifier = Modifier.rotate(turn))
        }
    }
}

@Composable private fun LessonRow(progress: Progress, index: Int, lesson: Lesson, current: Boolean, last: Boolean, onLesson: (Int) -> Unit) {
    val done = lesson.id in progress.completed
    val available = progress.unlocked(index)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(42.dp).fillMaxHeight().padding(top = if (current) 16.dp else 12.dp)) {
            Box(Modifier.size(32.dp).background(when { done -> GreenWash; current -> Indigo; else -> Mist }, CircleShape), contentAlignment = Alignment.Center) {
                when {
                    done -> Icon(Icons.Rounded.Check, null, tint = Green, modifier = Modifier.size(18.dp))
                    !available -> Icon(Icons.Rounded.Lock, null, tint = Muted, modifier = Modifier.size(14.dp))
                    else -> Text("${index + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (current) Color.White else Ink)
                }
            }
            if (!last) Box(Modifier.padding(top = 6.dp).width(2.dp).weight(1f).background(if (done) Green.copy(.35f) else Mist))
        }
        Spacer(Modifier.width(10.dp))
        if (current) {
            Surface(color = Ink, shape = RoundedCornerShape(22.dp), modifier = Modifier.weight(1f).padding(bottom = 12.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(if (progress.completed.isEmpty()) "Start here" else "Up next", color = Bamboo, style = MaterialTheme.typography.labelMedium)
                    Text(lesson.title, color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 5.dp))
                    Text(lesson.subtitle, color = Color(0xFFD5DCEF), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                    NoteDots(lesson.notes.distinct(), Color.White)
                    Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${lesson.minutes} min", color = Color(0xFFD5DCEF), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        Button({ onLesson(index) }, colors = ButtonDefaults.buttonColors(containerColor = Bamboo, contentColor = Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Start lesson", fontWeight = FontWeight.Bold)
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(start = 8.dp).size(18.dp))
                        }
                    }
                }
            }
        } else {
            Surface(onClick = { onLesson(index) }, enabled = available, color = Color.Transparent, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).padding(bottom = 6.dp)) {
                Row(Modifier.padding(vertical = 10.dp, horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(lesson.title, style = MaterialTheme.typography.titleMedium, color = if (available) Ink else Muted)
                        Text(when { done -> "Completed · Practice again"; available -> "${lesson.minutes} min · ${lesson.subtitle}"; else -> "${lesson.minutes} min · Locked" },
                            color = if (done) Green else Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 3.dp))
                    }
                    if (available) Icon(Icons.Rounded.ChevronRight, if (done) "Practice ${lesson.title} again" else "Open ${lesson.title}", tint = Muted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** The notes a lesson uses, as small labels with octave dots. */
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun NoteDots(notes: List<Swar>, color: Color) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = "Notes: ${notes.joinToString { it.spoken }}" }) {
        notes.forEach { note ->
            Box(Modifier.background(color.copy(.14f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                SwarLabel(note, MaterialTheme.typography.labelLarge, color)
            }
        }
    }
}

/** Today's riyaz: a short daily routine that adds up to steady progress. */
@Composable private fun TodayCard(progress: Progress, onLesson: () -> Unit, onWarmup: () -> Unit, onTools: () -> Unit) {
    val lessonIndex = todaysLesson(progress)
    val lesson = lessons[lessonIndex]
    val review = progress.nextLessonIndex < 0
    val items = listOf(
        TodayItem(Icons.Rounded.Air, "Warm up with long tones", if (progress.warmedUp) "Done today" else "Hold one note steadily for 5 seconds", progress.warmedUp, onWarmup),
        TodayItem(Icons.Rounded.Route, (if (review) "Review: " else "Lesson: ") + lesson.title,
            if (progress.goalDone) "Daily goal complete · ${progress.todayCount} today" else "${progress.todayCount} of ${progress.dailyGoal} today · ${lesson.minutes} min", progress.goalDone, onLesson),
        TodayItem(Icons.Rounded.AvTimer, "Keep time for 2 minutes",
            if (progress.rhythmDone) "Done today" else "With the metronome · ${clock(progress.rhythmToday)} of 2:00", progress.rhythmDone, onTools),
    )
    val count = items.count { it.done }
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Line)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
            Row(Modifier.padding(horizontal = 16.dp).semantics(mergeDescendants = true) { heading() }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Today's riyaz", style = MaterialTheme.typography.titleLarge)
                    Text(if (count == items.size) "Full practice today. Wonderful." else "About ${lesson.minutes + 4} minutes in all", color = if (count == items.size) Green else Muted,
                        style = MaterialTheme.typography.labelMedium)
                }
                Text("$count of ${items.size}", color = if (count == items.size) Green else Indigo, style = MaterialTheme.typography.labelLarge)
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { Box(Modifier.weight(1f).height(5.dp).background(if (it.done) Green else Mist, RoundedCornerShape(3.dp))) }
            }
            items.forEach { item ->
                Surface(onClick = item.action, color = Color.Transparent, modifier = Modifier.fillMaxWidth().semantics {
                    stateDescription = if (item.done) "Done" else "Not done"
                }) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 9.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(if (item.done) GreenWash else Mist, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(if (item.done) Icons.Rounded.Check else item.icon, null, tint = if (item.done) Green else Indigo, modifier = Modifier.size(19.dp))
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = if (item.done) Muted else Ink)
                            Text(item.detail, color = if (item.done) Green else Muted, style = MaterialTheme.typography.labelMedium)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private data class TodayItem(val icon: ImageVector, val title: String, val detail: String, val done: Boolean, val action: () -> Unit)
private fun clock(seconds: Long) = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

@Composable internal fun GoalCard(progress: Progress) {
    val complete = progress.goalDone
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { (progress.todayCount.toFloat() / progress.dailyGoal).coerceAtMost(1f) }, color = if (complete) Green else Indigo,
                    trackColor = Mist, strokeWidth = 3.dp, modifier = Modifier.fillMaxSize())
                Icon(if (complete) Icons.Rounded.Check else Icons.Rounded.WbSunny, null, tint = if (complete) Green else Indigo, modifier = Modifier.size(16.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(if (complete) "Daily goal complete" else "A little practice today", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(if (complete) "${progress.todayCount} ${if (progress.todayCount == 1) "lesson" else "lessons"} today" else "${progress.todayCount} of ${progress.dailyGoal} ${if (progress.dailyGoal == 1) "lesson" else "lessons"}", color = Muted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable internal fun CompletionScreen(progress: Progress, lesson: Lesson, summary: SessionSummary, onHome: () -> Unit, onNext: () -> Unit, isLast: Boolean,
                                         onReminder: (Int) -> Unit, onDeclineReminder: () -> Unit, onTune: () -> Unit, onDeclineTune: () -> Unit) {
    val index = lessons.indexOf(lesson)
    val nextLesson = lessons.getOrNull(index + 1)
    val newUnit = nextLesson != null && nextLesson.unit != lesson.unit
    val chooseReminder = rememberReminderChooser(onReminder)
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(36.dp))
            CompletionHero(summary.reward)
            Text(if (summary.reward.streakExtended) "Lesson complete · ${lesson.title}" else lesson.title, style = MaterialTheme.typography.bodyLarge, color = Muted,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 28.dp))
            if (summary.reward.lines.isNotEmpty()) { RewardCard(summary.reward, progress); Spacer(Modifier.height(16.dp)) }
            Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Your practice", style = MaterialTheme.typography.titleMedium)
                    ResultRow("Pitch-matched notes", "${summary.checked} of ${summary.total}", Icons.Rounded.GraphicEq)
                    ResultRow("Self-guided notes", "${summary.total - summary.checked} of ${summary.total}", Icons.Rounded.Spa)
                    Text("Saved on this device. Self-guided notes have no microphone result.", color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                }
            }
            // One prompt at a time: tuning first (it makes every pitch check fairer), then the reminder offer.
            if (!progress.tunePromptDone) {
                Surface(color = Mist, shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(top = 16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Mic, null, tint = Indigo)
                            Text("Tune Basuri to your flute", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
                        }
                        Text("Now that you can play Sa, let the microphone find its exact pitch. Every reference and pitch check will then match your flute.",
                            color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onTune, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                                modifier = Modifier.heightIn(min = 48.dp)) { Text("Tune to my flute") }
                            TextButton(onDeclineTune, modifier = Modifier.heightIn(min = 48.dp)) { Text("Not now") }
                        }
                    }
                }
            } else if (progress.reminderMinutes < 0 && !progress.reminderOffered) {
                Surface(color = Mist, shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(top = 16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.NotificationsActive, null, tint = Indigo)
                            Text("Practice again tomorrow?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
                        }
                        Text("A gentle evening reminder helps a streak grow. It stays quiet on days you've already practiced.", color = Muted,
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button({ chooseReminder(Reminders.presets[2].first) }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                                modifier = Modifier.heightIn(min = 48.dp)) { Text("Remind me at ${Reminders.label(Reminders.presets[2].first)}") }
                            TextButton(onDeclineReminder, modifier = Modifier.heightIn(min = 48.dp)) { Text("Not now") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            if (nextLesson != null) {
                if (newUnit) Surface(color = Bamboo.copy(.3f), shape = RoundedCornerShape(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    Text("New unit unlocked: ${courseOf(nextLesson).title}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                Text("Up next", color = Muted, style = MaterialTheme.typography.labelMedium)
                Text(nextLesson.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 5.dp))
            } else Text("You've finished every lesson.\nKeep your flute warm with daily review.", textAlign = TextAlign.Center, color = Muted, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            PrimaryButton(if (isLast) "Go to practice" else "Continue learning", onNext)
            TextButton(onHome, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Back to my lessons") }
        }
    }
}

@Composable private fun ResultRow(label: String, value: String, icon: ImageVector) {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Indigo, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
