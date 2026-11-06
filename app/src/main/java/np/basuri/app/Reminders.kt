package np.basuri.app

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Local practice notifications. All three use inexact alarms, so they need no exact-alarm permission
 * and may arrive a few minutes late. Nothing is sent over the network.
 *  - Daily reminder at the learner's chosen time, skipped once a lesson is done that day.
 *  - Streak saver at 21:30 when a streak would end at midnight.
 *  - Weekly summary on Saturday at 19:00, the end of the Nepali week (Sunday to Saturday).
 */
object Reminders {
    const val OPEN = "open"
    const val OPEN_LESSON = "lesson"
    const val OPEN_WARMUP = "warmup"
    private const val ACTION_DAILY = "np.basuri.app.PRACTICE_REMINDER"
    private const val ACTION_SAVER = "np.basuri.app.STREAK_SAVER"
    private const val ACTION_WEEKLY = "np.basuri.app.WEEKLY_SUMMARY"
    private const val ACTION_SNOOZE = "np.basuri.app.SNOOZE"
    private const val SAVER_MINUTES = 21 * 60 + 30
    private const val WEEKLY_MINUTES = 19 * 60
    /** Saturday is Nepal's weekly holiday and the last day of its week. */
    private val WEEKLY_DAY = DayOfWeek.SATURDAY
    private const val NOTIFY_DAILY = 1
    private const val NOTIFY_WEEKLY = 2

    private enum class Channel(val id: String, val title: String, val detail: String) {
        Daily("practice", "Daily reminder", "One reminder a day when no lesson is finished yet"),
        Saver("streak", "Streak saver", "A late-evening nudge when your streak would end tonight"),
        Weekly("weekly", "Weekly summary", "Your practice week, on Saturday evening"),
    }

    val presets = listOf(8 * 60 to "Morning", 12 * 60 + 30 to "Midday", 18 * 60 + 30 to "Evening", 21 * 60 to "Night")
    fun label(minutes: Int): String = "%d:%02d".format(minutes / 60, minutes % 60)

    private fun broadcast(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(context, code,
        Intent(context, ReminderReceiver::class.java).setAction(action), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun nextTrigger(minutes: Int, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        val today = now.with(LocalTime.of(minutes / 60, minutes % 60))
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    private const val SNOOZE_CODE = 5
    /** Alarm bookkeeping lives apart from progress so a receiver never races a whole-progress save. */
    private fun fired(context: Context) = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    private fun markFired(context: Context, action: String) = fired(context).edit().putString(action, LocalDate.now().toString()).apply()
    private fun firedToday(context: Context, action: String) = fired(context).getString(action, null) == LocalDate.now().toString()

    /**
     * The next time an alarm should fire. If today's time passed less than an hour ago and the alarm has
     * not fired yet (an inexact window still open, or the app rescheduled at the wrong moment), fire soon.
     */
    private fun due(context: Context, action: String, minutes: Int, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        val today = now.with(LocalTime.of(minutes / 60, minutes % 60))
        return when {
            today.isAfter(now) -> today
            !firedToday(context, action) && now.isBefore(today.plusHours(1)) -> now.plusMinutes(1)
            else -> today.plusDays(1)
        }
    }

    private fun set(context: Context, action: String, code: Int, at: ZonedDateTime?) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = broadcast(context, action, code)
        alarms.cancel(intent)
        if (at != null) alarms.setWindow(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), 10 * 60 * 1000L, intent)
    }

    /** Idempotent. Done up front so each reminder type has its own switch in Android settings from the start. */
    private fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        Channel.entries.forEach { manager.createNotificationChannel(NotificationChannel(it.id, it.title, NotificationManager.IMPORTANCE_DEFAULT).apply { description = it.detail }) }
    }

    /** The id and name of the first reminder the learner uses but has turned off in Android's per-type settings. */
    fun blockedChannel(context: Context, progress: Progress): Pair<String, String>? {
        val manager = context.getSystemService(NotificationManager::class.java)
        return listOfNotNull(Channel.Daily, Channel.Saver.takeIf { progress.streakSaver }, Channel.Weekly.takeIf { progress.weeklyRecap })
            .firstOrNull { manager.getNotificationChannel(it.id)?.importance == NotificationManager.IMPORTANCE_NONE }?.let { it.id to it.title }
    }

    /** Re-arms every alarm from saved settings. Safe to call at any time. */
    fun scheduleAll(context: Context, progress: Progress) {
        createChannels(context)
        val on = progress.reminderMinutes >= 0
        set(context, ACTION_DAILY, 1, if (on) due(context, ACTION_DAILY, progress.reminderMinutes) else null)
        if (!on) set(context, ACTION_DAILY, SNOOZE_CODE, null)
        // The saver is redundant when the daily reminder is already late in the evening.
        set(context, ACTION_SAVER, 2, if (on && progress.streakSaver && progress.reminderMinutes < SAVER_MINUTES - 60) due(context, ACTION_SAVER, SAVER_MINUTES) else null)
        val now = ZonedDateTime.now()
        val weekly = if (now.dayOfWeek == WEEKLY_DAY) due(context, ACTION_WEEKLY, WEEKLY_MINUTES, now)
            else now.with(TemporalAdjusters.next(WEEKLY_DAY)).with(LocalTime.of(WEEKLY_MINUTES / 60, WEEKLY_MINUTES % 60))
        set(context, ACTION_WEEKLY, 4, if (on && progress.weeklyRecap) (if (weekly.dayOfWeek == WEEKLY_DAY) weekly else weekly.with(TemporalAdjusters.next(WEEKLY_DAY))) else null)
    }

    /** False when the permission is missing (Android 13+) or notifications are blocked for the app in settings. */
    fun canNotify(context: Context) = (Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    private fun open(context: Context, target: String, code: Int) = PendingIntent.getActivity(context, code,
        Intent(context, MainActivity::class.java).putExtra(OPEN, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun post(context: Context, channel: Channel, id: Int, title: String, text: String, actions: Boolean) {
        if (!canNotify(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        createChannels(context)
        val builder = Notification.Builder(context, channel.id).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentText(text).setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open(context, if (actions) OPEN_LESSON else "home", 10 + id)).setAutoCancel(true).setColor(0xFF4C5FB5.toInt())
        if (actions) {
            builder.addAction(Notification.Action.Builder(null, "Start lesson", open(context, OPEN_LESSON, 20)).build())
            builder.addAction(Notification.Action.Builder(null, "Warm up", open(context, OPEN_WARMUP, 21)).build())
            builder.addAction(Notification.Action.Builder(null, "In 1 hour", broadcast(context, ACTION_SNOOZE, 3)).build())
        }
        manager.notify(id, builder.build())
    }

    /** Removes a posted daily reminder or streak saver (they share one id). */
    fun dismiss(context: Context) = context.getSystemService(NotificationManager::class.java).cancel(NOTIFY_DAILY)

    private fun nextLessonTitle(progress: Progress) = lessons.getOrNull(progress.nextLessonIndex)?.title

    /** The daily reminder. [test] posts it even if today is done, from the settings screen. */
    fun notify(context: Context, progress: Progress, test: Boolean = false) {
        if (!test && progress.practiced(LocalDate.now())) return
        val next = nextLessonTitle(progress)
        val (title, text) = when {
            progress.streak > 0 -> "Keep your ${progress.streak}-day streak going" to (next?.let { "Up next: $it. About ${lessons[progress.nextLessonIndex].minutes} minutes." } ?: "Repeat any lesson to keep your flame lit.")
            next != null -> "Time for a little flute practice" to "Up next: $next. About ${lessons[progress.nextLessonIndex].minutes} minutes."
            else -> "Time for a little flute practice" to "Repeat a favourite lesson or warm up with long tones."
        }
        post(context, Channel.Daily, NOTIFY_DAILY, title, text, actions = true)
    }

    private fun saver(context: Context, progress: Progress) {
        if (progress.practiced(LocalDate.now()) || progress.streak == 0) return
        val freeze = if (progress.freezes > 0) " A streak freeze would cover today, but a lesson keeps it for later." else ""
        post(context, Channel.Saver, NOTIFY_DAILY, "Your ${progress.streak}-day streak ends at midnight", "One short lesson keeps it going.$freeze", actions = true)
    }

    private fun weekly(context: Context, progress: Progress) {
        val today = LocalDate.now()
        val week = (0..6).map { today.minusDays(it.toLong()) }
        val days = week.count(progress::practiced)
        val lessonsDone = week.sumOf { progress.sessions[it] ?: 0 }
        val rhythm = week.sumOf { progress.rhythmDays[it] ?: 0 } / 60
        val text = if (days == 0) "No lessons this week. A fresh start is one short lesson away."
            else "$days practice ${if (days == 1) "day" else "days"} · $lessonsDone ${if (lessonsDone == 1) "lesson" else "lessons"}" +
                (if (rhythm > 0) " · $rhythm min with the metronome" else "") + ". Best streak: ${progress.bestStreak} days."
        post(context, Channel.Weekly, NOTIFY_WEEKLY, "Your practice week", text, actions = false)
    }

    internal fun handle(context: Context, intent: Intent, progress: Progress) {
        val on = progress.reminderMinutes >= 0
        when (intent.action) {
            ACTION_DAILY -> { markFired(context, ACTION_DAILY); if (on) notify(context, progress) }
            ACTION_SAVER -> { markFired(context, ACTION_SAVER); if (on && progress.streakSaver) saver(context, progress) }
            ACTION_WEEKLY -> { markFired(context, ACTION_WEEKLY); if (on && progress.weeklyRecap) weekly(context, progress) }
            ACTION_SNOOZE -> {
                dismiss(context)
                set(context, ACTION_DAILY, SNOOZE_CODE, ZonedDateTime.now().plusHours(1))
                return
            }
        }
        scheduleAll(context, progress)
    }
}

/** Shows practice notifications, and restores alarms after a reboot, app update, or clock change. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Reminders.handle(context, intent, ProgressStore(context).load())
}
