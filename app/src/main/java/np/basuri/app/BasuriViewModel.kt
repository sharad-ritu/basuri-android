package np.basuri.app

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ln

enum class Screen { Welcome, Setup, Home, Lesson, Complete, Practice, Metronome, Notation, Settings }
/** Screens reachable from the bottom navigation, other than Settings. */
val tabScreens = setOf(Screen.Home, Screen.Practice, Screen.Metronome, Screen.Notation)
sealed interface AudioStatus {
    data object Idle : AudioStatus
    data object Playing : AudioStatus
    /** [held] is progress toward a match (or the long-tone goal); [holdMs] is the current steady hold. */
    data class Listening(val reading: PitchReading = PitchReading.Quiet, val held: Float = 0f, val holdMs: Long = 0) : AudioStatus
    data class Error(val message: String) : AudioStatus
}

/** Listening for the learner's three-hole Sa in setup or My flute. */
sealed interface SaDetection {
    data object Idle : SaDetection
    /** [steady] is progress toward a confident reading; [hz] is the latest pitch heard. */
    data class Listening(val steady: Float = 0f, val hz: Double? = null) : SaDetection
    data class Found(val flute: Flute, val hz: Double) : SaDetection
    data class Failed(val message: String) : SaDetection
}

data class SessionSummary(val checked: Int, val total: Int, val reward: Reward = Reward())

class BasuriViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val store = ProgressStore(application)
    private val _progress = MutableStateFlow(store.load())
    val progress = _progress.asStateFlow()
    val screen = saved.getStateFlow("screen", if (_progress.value.onboarded) Screen.Home else Screen.Welcome)
    val lessonIndex = saved.getStateFlow("lesson", 0)
    val step = saved.getStateFlow("step", 0)
    val guideIndex = saved.getStateFlow("guide", 0)
    val selfGuided = saved.getStateFlow("selfGuided", false)
    val practiceNote = saved.getStateFlow("note", Swar.Sa)
    val matched = saved.getStateFlow("matched", false)
    /** Practice room mode: a steady long tone instead of a 1.5-second match. */
    val longTone = saved.getStateFlow("longTone", false)
    /** The most recent finished long tone in this session, in milliseconds. */
    private val _lastHold = MutableStateFlow(0L)
    val lastHold = _lastHold.asStateFlow()
    private var liveHold = 0L
    private val _audio = MutableStateFlow<AudioStatus>(AudioStatus.Idle)
    val audio = _audio.asStateFlow()
    private val _saDetection = MutableStateFlow<SaDetection>(SaDetection.Idle)
    val saDetection = _saDetection.asStateFlow()
    private val _summary = MutableStateFlow(SessionSummary(0, 0))
    val summary = _summary.asStateFlow()
    private val audioMutex = Mutex()
    private var audioJob: Job? = null
    private var generation = 0
    private val engine = AudioEngine(application) { viewModelScope.launch { stopAudio() } }
    val currentLesson: Lesson get() = lessons[lessonIndex.value]
    val currentNote: Swar get() = if (screen.value == Screen.Practice) practiceNote.value else currentLesson.notes[(step.value - 1).coerceIn(currentLesson.notes.indices)]
    /** Every save first applies any streak freezes due today, so a stale in-memory day can't undo them. */
    private fun persist(progress: Progress) { val p = progress.withFreezesApplied(); store.save(p); _progress.value = p }
    fun refreshDay() { _progress.value = store.load() }
    fun configure(flute: Flute, goal: Int, finishOnboarding: Boolean) {
        stopAudio()
        persist(_progress.value.copy(flute = flute, dailyGoal = goal, onboarded = _progress.value.onboarded || finishOnboarding))
        navigate(if (finishOnboarding) Screen.Home else saved.get<Screen>("settingsReturn") ?: Screen.Home)
    }
    /** Metronome sessions shorter than ten seconds are treated as a test tap, not practice. */
    fun addRhythmPractice(seconds: Long) {
        if (seconds < 10) return
        val p = _progress.value
        // A session that crossed midnight is split between the two days it touched.
        val now = java.time.LocalDateTime.now()
        val today = now.toLocalDate()
        val sinceMidnight = java.time.Duration.between(today.atStartOfDay(), now).seconds
        val todayPart = minOf(seconds, sinceMidnight)
        var days = p.rhythmDays + (today to (p.rhythmDays[today] ?: 0) + todayPart)
        if (seconds > todayPart) { val y = today.minusDays(1); days = days + (y to (days[y] ?: 0) + seconds - todayPart) }
        persist(p.copy(rhythmSeconds = p.rhythmSeconds + seconds, rhythmDays = days))
    }
    fun setReminder(minutes: Int) = updateReminders(_progress.value.copy(reminderMinutes = minutes, reminderOffered = true))
    fun setStreakSaver(on: Boolean) = updateReminders(_progress.value.copy(streakSaver = on))
    fun setWeeklyRecap(on: Boolean) = updateReminders(_progress.value.copy(weeklyRecap = on))
    fun declineReminder() = persist(_progress.value.copy(reminderOffered = true))
    private fun updateReminders(progress: Progress) { persist(progress); Reminders.scheduleAll(getApplication(), progress) }
    fun sendTestReminder() = Reminders.notify(getApplication(), _progress.value, test = true)
    fun resetProgress() { stopAudio(); persist(store.reset(_progress.value)); navigate(Screen.Home) }
    /** Opens the next lesson from a notification action. */
    fun openFromNotification(target: String?) {
        if (!_progress.value.onboarded || target == null) return
        // Action buttons don't auto-cancel their notification.
        Reminders.dismiss(getApplication())
        when (target) {
            Reminders.OPEN_LESSON -> beginLesson(_progress.value.nextLessonIndex.takeIf { it >= 0 } ?: 0)
            Reminders.OPEN_WARMUP -> { saved["longTone"] = true; navigate(Screen.Practice) }
            else -> navigate(Screen.Home)
        }
    }
    fun setLongTone(on: Boolean) { stopAudio(); saved["longTone"] = on; _lastHold.value = 0 }
    private fun recordHold(ms: Long) {
        _lastHold.value = ms
        if (ms < 1000) return
        val p = _progress.value
        val warm = if (ms >= Progress.WARMUP_MS) p.warmupDays + LocalDate.now() else p.warmupDays
        if (warm != p.warmupDays || ms > p.bestHoldMs) persist(p.copy(warmupDays = warm, bestHoldMs = maxOf(p.bestHoldMs, ms)))
    }
    fun openSettings() {
        if (screen.value == Screen.Settings) return
        saved["settingsReturn"] = if (screen.value in tabScreens) screen.value else Screen.Home
        navigate(Screen.Settings)
    }
    fun closeSettings() { navigate(saved.get<Screen>("settingsReturn") ?: Screen.Home) }
    fun navigate(target: Screen) {
        stopAudio(); saved["matched"] = false; saved["screen"] = target
    }
    fun beginLesson(index: Int) {
        stopAudio(); saved["lesson"] = index; saved["step"] = 0; saved["checked"] = 0
        saved["guide"] = 0; saved["selfGuided"] = false
        saved["matched"] = false; saved["screen"] = Screen.Lesson
    }
    fun nextStep() {
        if (screen.value != Screen.Lesson) return
        if (step.value > 0 && !matched.value && !selfGuided.value) return
        stopAudio()
        if (step.value > 0 && matched.value) saved["checked"] = (saved.get<Int>("checked") ?: 0) + 1
        saved["matched"] = false
        if (step.value < currentLesson.notes.size) saved["step"] = step.value + 1
        else {
            val checked = saved.get<Int>("checked") ?: 0
            val (after, reward) = Rewards.complete(_progress.value.withFreezesApplied(), currentLesson.id, checked, currentLesson.notes.size)
            persist(after)
            // Today's reminder or streak saver is out of date once a lesson is done.
            Reminders.dismiss(getApplication())
            _summary.value = SessionSummary(checked, currentLesson.notes.size, reward)
            saveSummary(_summary.value)
            saved["screen"] = Screen.Complete
        }
    }
    fun advanceGuide() {
        if (guideIndex.value < currentLesson.guides.lastIndex) saved["guide"] = guideIndex.value + 1
        else nextStep()
    }
    fun previousGuide() { if (guideIndex.value > 0) saved["guide"] = guideIndex.value - 1 }
    fun useSelfGuided() { stopAudio(); saved["matched"] = false; saved["selfGuided"] = true }
    fun usePitchFeedback() { saved["selfGuided"] = false }
    private fun saveSummary(summary: SessionSummary) {
        val r = summary.reward
        saved["summaryChecked"] = summary.checked
        saved["summaryTotal"] = summary.total
        saved["rewardLines"] = ArrayList(r.lines.map { "${it.second}|${it.first}" })
        saved["rewardBadges"] = ArrayList(r.newBadges.map { it.name })
        saved["rewardStreak"] = r.streak
        saved["rewardExtended"] = r.streakExtended
        saved["rewardLevelBefore"] = r.levelBefore
        saved["rewardFreeze"] = r.freezeEarned
    }
    init {
        // Alarms can be lost to a force stop; re-arming is idempotent.
        Reminders.scheduleAll(application, _progress.value)
        val lines = saved.get<ArrayList<String>>("rewardLines").orEmpty().mapNotNull { line ->
            val (xp, label) = line.split('|', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
            xp.toIntOrNull()?.let { label to it }
        }
        val badges = saved.get<ArrayList<String>>("rewardBadges").orEmpty().mapNotNull { name -> Badge.entries.firstOrNull { it.name == name } }
        _summary.value = SessionSummary(saved.get<Int>("summaryChecked") ?: 0, saved.get<Int>("summaryTotal") ?: 0,
            Reward(lines, saved.get<Int>("rewardStreak") ?: 0, saved.get<Boolean>("rewardExtended") ?: false, badges,
                saved.get<Int>("rewardLevelBefore") ?: _progress.value.level, saved.get<Boolean>("rewardFreeze") ?: false))
    }
    fun chooseNote(note: Swar) { stopAudio(); saved["note"] = note; saved["matched"] = false }
    fun permissionDenied() {
        stopAudio()
        _audio.value = AudioStatus.Error("Microphone access is off. In app settings, choose Permissions, then Microphone. You can also practice self-guided.")
    }
    fun stopAudio() {
        if (liveHold > 0) { val ms = liveHold; liveHold = 0; recordHold(ms) }
        generation++
        audioJob?.cancel()
        _audio.value = AudioStatus.Idle
        if (_saDetection.value is SaDetection.Listening) _saDetection.value = SaDetection.Idle
    }
    fun clearSaDetection() { if (_saDetection.value !is SaDetection.Listening) _saDetection.value = SaDetection.Idle }
    fun saPermissionDenied() {
        stopAudio()
        _saDetection.value = SaDetection.Failed("Microphone access is off, so Basuri can't hear your flute. Choose your Sa below instead.")
    }

    /**
     * Listens until one pitch holds steady (within 40 cents of its own median) for about 1.4 seconds,
     * then reports the nearest Sa setting and its offset in cents. Gives up after 15 seconds.
     */
    fun detectSa() {
        val token = ++generation
        audioJob?.cancel()
        _audio.value = AudioStatus.Idle
        _saDetection.value = SaDetection.Listening()
        viewModelScope.launch {
            audioMutex.withLock {
                audioJob?.cancelAndJoin()
                if (token != generation) return@withLock
                audioJob = viewModelScope.launch(Dispatchers.IO) {
                    val run = ArrayList<Double>()
                    var gap = 0
                    val started = SystemClock.elapsedRealtime()
                    fun median() = run.sorted().let { (it[(it.size - 1) / 2] + it[it.size / 2]) / 2 }
                    try {
                        engine.listen { reading ->
                            val hz = (reading as? PitchReading.Tone)?.hz?.takeIf { it in 120.0..1050.0 }
                            if (hz != null) {
                                gap = 0
                                if (run.isNotEmpty() && abs(1200 * ln(hz / median()) / ln(2.0)) > 40) run.clear()
                                run += hz
                            } else if (++gap > 2) run.clear()
                            val result = if (run.size >= SA_FRAMES) median() else null
                            val timedOut = SystemClock.elapsedRealtime() - started > 15_000
                            withContext(Dispatchers.Main) {
                                if (token != generation) return@withContext
                                _saDetection.value = when {
                                    result != null -> Flute.fromFrequency(result)?.let { SaDetection.Found(it, result) }
                                        ?: SaDetection.Failed("Heard ${"%.0f".format(result)} Hz, which is outside C3–B5. Check that you played the three-hole Sa without overblowing.")
                                    timedOut -> SaDetection.Failed("No steady note yet. Close holes 1–3, blow gently, and hold the note for two seconds.")
                                    else -> { SaDetection.Listening((run.size / SA_FRAMES.toFloat()).coerceAtMost(1f), hz); return@withContext }
                                }
                                generation++
                                audioJob?.cancel()
                            }
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            if (token == generation) _saDetection.value = SaDetection.Failed(e.message ?: "The microphone could not start. Please try again.")
                        }
                    }
                }
            }
        }
    }

    /** Plays a reference outside a lesson, such as an unsaved Sa in My flute. */
    fun playTone(frequency: Double) = startAudio(false, frequency)

    private fun startAudio(listening: Boolean, frequencyOverride: Double? = null) {
        // Switching from listening to the reference (or restarting) must not drop a hold in progress.
        if (liveHold > 0) { val ms = liveHold; liveHold = 0; recordHold(ms) }
        val token = ++generation
        audioJob?.cancel()
        val frequency = frequencyOverride ?: _progress.value.flute.frequency(currentNote)
        val sustain = listening && screen.value == Screen.Practice && longTone.value
        viewModelScope.launch {
            audioMutex.withLock {
                audioJob?.cancelAndJoin()
                if (token != generation) return@withLock
                _audio.value = if (listening) AudioStatus.Listening() else AudioStatus.Playing
                audioJob = viewModelScope.launch(Dispatchers.IO) {
                    try {
                        if (listening) {
                            var held = 0L
                            var previous = 0L
                            var drift = 0L
                            engine.listen { reading ->
                                val now = SystemClock.elapsedRealtime()
                                val cents = if (reading is PitchReading.Tone) 1200 * ln(reading.hz / frequency) / ln(2.0) else Double.NaN
                                val elapsed = now - previous
                                val steady = abs(cents) <= (if (sustain) 50 else 35) && previous != 0L && elapsed <= 250
                                var ended = 0L
                                if (steady) { held += elapsed; drift = 0 }
                                // A brief wobble, breath noise, or octave misread pauses the hold instead of
                                // resetting it: up to 400 ms for a long tone, about two frames for a match.
                                else if (held > 0 && previous != 0L) {
                                    drift += elapsed
                                    if (drift > (if (sustain) 400 else 200)) { if (sustain) ended = held; held = 0; drift = 0 }
                                }
                                else held = 0
                                previous = now
                                withContext(Dispatchers.Main) {
                                    if (token == generation) {
                                        if (sustain) {
                                            liveHold = held
                                            if (ended > 0) recordHold(ended)
                                            _audio.value = AudioStatus.Listening(reading, (held / LONG_TONE_GOAL.toFloat()).coerceIn(0f, 1f), held)
                                        } else {
                                            _audio.value = AudioStatus.Listening(reading, (held / 1500f).coerceIn(0f, 1f), held)
                                            if (held >= 1500) {
                                                saved["matched"] = true
                                                stopAudio()
                                            }
                                        }
                                    }
                                }
                            }
                        } else engine.play(frequency)
                        withContext(Dispatchers.Main) { if (token == generation) _audio.value = AudioStatus.Idle }
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            if (token == generation) _audio.value = AudioStatus.Error(e.message ?: "Audio could not start. Please try again.")
                        }
                    }
                }
            }
        }
    }
    fun playReference() = startAudio(false)
    companion object {
        const val LONG_TONE_GOAL = 8000L
        /** About 1.4 seconds of 2048-sample frames at 24 kHz. */
        private const val SA_FRAMES = 16
    }
    fun listen() {
        if (screen.value == Screen.Practice) saved["matched"] = false
        startAudio(true)
    }
}
