package np.basuri.app

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.Process
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToLong
import kotlin.math.sin

internal data class MetronomePattern(val marks: List<TaalMark?>) {
    val beats: Int get() = marks.size

    companion object {
        fun simple(beats: Int) = MetronomePattern(List(beats) { if (it == 0) TaalMark.Sam else null })
        fun taal(taal: Taal) = MetronomePattern(List(taal.beats) { taal.markAt(it)?.mark })
    }
}

internal sealed interface MetronomeStatus {
    data class Stopped(val message: String? = null) : MetronomeStatus
    data object Starting : MetronomeStatus
    data object Playing : MetronomeStatus
    data class Error(val message: String) : MetronomeStatus
}

internal data class MetronomePosition(val beat: Int = -1, val cycle: Long = 0, val elapsedSeconds: Long = 0)

/** One worker owns the track. Sound is scheduled only in PCM sample space. */
internal class MetronomeEngine(
    context: Context,
    private val focus: PracticeAudioFocus = PracticeAudioFocus(context),
    private val onFinished: (Long) -> Unit = {},
) {
    private val attributes = focus.attributes
    private val main = Handler(Looper.getMainLooper())
    private val lock = Any()
    private val tempo = AtomicInteger(80)
    private val mutableStatus = MutableStateFlow<MetronomeStatus>(MetronomeStatus.Stopped())
    val status = mutableStatus.asStateFlow()
    private var session: Session? = null
    private var lastPosition = MetronomePosition()

    private data class Beat(val frame: Long, val index: Int, val cycle: Long)
    private class AudioFailure(message: String) : Exception(message)
    private class Session(val pattern: MetronomePattern, val subdivisions: Int, val limit: Long?, val trainerStep: Int) {
        @Volatile var cancelled = false
        @Volatile var track: AudioTrack? = null
        lateinit var worker: Thread
        val beats = ArrayDeque<Beat>()
        var audibleBeat: Beat? = null
        var previousHead = 0L
        var wraps = 0L
        var stopMessage: String? = null
    }

    fun setTempo(bpm: Int) { tempo.set(bpm.coerceIn(30, 240)) }
    fun currentTempo(): Int = tempo.get()

    fun start(bpm: Int, pattern: MetronomePattern, subdivisions: Int, minutes: Int, trainerStep: Int = 0) {
        require(pattern.beats in 1..16 && subdivisions in 1..4 && minutes >= 0 && trainerStep in listOf(0, 1, 2, 5))
        setTempo(bpm)
        synchronized(lock) {
            if (session != null) return
            lastPosition = MetronomePosition()
            val run = Session(pattern, subdivisions, if (minutes == 0) null else minutes * 60L * RATE, trainerStep)
            session = run
            mutableStatus.value = MetronomeStatus.Starting
            run.worker = Thread({ play(run) }, "Basuri-metronome")
            run.worker.start()
        }
    }

    /** Pause unblocks a blocking write; joining also prevents overlap with a subsequent start. */
    fun stop(message: String? = null) {
        val run = synchronized(lock) {
            session?.also {
                it.cancelled = true
                it.stopMessage = message
                runCatching { it.track?.pause() }
            }
        } ?: return
        run.worker.interrupt()
        // Bounded so a stalled device can't freeze the UI; the worker still releases everything itself.
        if (Thread.currentThread() != run.worker) run.worker.join(400)
    }

    fun position(): MetronomePosition = synchronized(lock) {
        session?.let { readPosition(it) } ?: lastPosition
    }

    // All playback-head reads and releases share the lock. The unsigned 32-bit head may wrap.
    private fun readPosition(run: Session): MetronomePosition {
        val track = run.track ?: return lastPosition
        val head = track.playbackHeadPosition.toLong() and 0xffffffffL
        if (head < run.previousHead) run.wraps += 1L shl 32
        run.previousHead = head
        val frame = (run.wraps + head).let { if (run.limit == null) it else it.coerceAtMost(run.limit) }
        while (run.beats.isNotEmpty() && run.beats.first.frame <= frame) run.audibleBeat = run.beats.removeFirst()
        val beat = run.audibleBeat
        return MetronomePosition(beat?.index ?: -1, beat?.cycle ?: 0, frame / RATE).also { lastPosition = it }
    }

    private fun play(run: Session) {
        var failure: String? = null
        var completed = false
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            if (!focus.acquire(run) {
                if (synchronized(lock) { session === run }) {
                    stop("Metronome stopped because another app needs audio. Tap Start when you're ready.")
                }
            }) {
                throw AudioFailure("Audio is busy. Pause other audio and try again.")
            }
            if (run.cancelled) return
            val minimum = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minimum <= 0) throw AudioFailure("This device couldn't open metronome audio. Try again.")
            val track = AudioTrack.Builder().setAudioAttributes(attributes)
                .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STREAM).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setBufferSizeInBytes(maxOf(minimum, BLOCK * 4)).build()
            synchronized(lock) { run.track = track }
            if (track.state != AudioTrack.STATE_INITIALIZED) throw AudioFailure("Metronome audio couldn't start. Try again.")
            // Keep the queue short so a live tempo edit reaches the next unrendered beat promptly.
            track.setBufferSizeInFrames(maxOf(BLOCK * 2, RATE / 25))
            if (run.cancelled) return
            val buffer = ShortArray(BLOCK)
            val clicks = Click.entries.associateWith(::synthesize)
            var written = 0L
            var beatStart = 0.0
            var beatLength = RATE * 60.0 / tempo.get()
            var nextClick = 0L
            var nextIncrease = RATE * 60L
            var beat = 0
            var cycle = 1L
            var subdivision = 0
            var sound = clicks.getValue(Click.Sam)
            var soundIndex = sound.size
            synchronized(lock) {
                if (run.cancelled) return
                track.play()
                mutableStatus.value = MetronomeStatus.Playing
            }
            while (!run.cancelled) {
                for (i in buffer.indices) {
                    val frame = written + i
                    if ((run.limit == null || frame < run.limit) && frame == nextClick) {
                        val click = if (subdivision == 0) {
                            if (frame >= nextIncrease) {
                                if (run.trainerStep > 0) tempo.updateAndGet { (it + run.trainerStep).coerceAtMost(240) }
                                nextIncrease += RATE * 60L
                            }
                            beatLength = RATE * 60.0 / tempo.get()
                            synchronized(lock) { run.beats.addLast(Beat(frame, beat, cycle)) }
                            when (run.pattern.marks[beat]) {
                                TaalMark.Sam -> Click.Sam
                                TaalMark.Tali -> Click.Tali
                                TaalMark.Khali -> Click.Khali
                                null -> Click.Beat
                            }
                        } else Click.Subdivision
                        sound = clicks.getValue(click)
                        soundIndex = 0
                        subdivision++
                        if (subdivision == run.subdivisions) {
                            subdivision = 0
                            beatStart += beatLength
                            beat = (beat + 1) % run.pattern.beats
                            if (beat == 0) cycle++
                        }
                        // Retain fractional frames between beats instead of rounding each interval.
                        nextClick = (beatStart + beatLength * subdivision / run.subdivisions).roundToLong()
                    }
                    val sample = if (soundIndex < sound.size && (run.limit == null || frame < run.limit)) sound[soundIndex++].toInt() else 0
                    val fade = run.limit?.let { ((it - frame).toDouble() / BLOCK).coerceIn(0.0, 1.0) } ?: 1.0
                    buffer[i] = (sample * fade).toInt().toShort()
                }
                var offset = 0
                while (offset < buffer.size && !run.cancelled) {
                    val count = track.write(buffer, offset, buffer.size - offset, AudioTrack.WRITE_BLOCKING)
                    if (run.cancelled) break
                    if (count <= 0) throw AudioFailure("Metronome audio was interrupted. Tap Start to try again.")
                    offset += count
                }
                written += offset
                val played = synchronized(lock) {
                    readPosition(run)
                    run.wraps + run.previousHead
                }
                // After the deadline, write silence until the last audible samples have played.
                if (run.limit != null && played >= run.limit) { completed = true; break }
            }
        } catch (error: Exception) {
            if (!run.cancelled) failure = if (error is AudioFailure) error.message
                else "Metronome audio couldn't start. Pause other audio and try again."
        } finally {
            synchronized(lock) {
                run.track?.let { track ->
                    runCatching { readPosition(run) }
                    runCatching { track.stop() }
                    runCatching { track.release() }
                    run.track = null
                }
            }
            runCatching { focus.release(run) }
            val elapsed = synchronized(lock) {
                session = null
                mutableStatus.value = failure?.let { MetronomeStatus.Error(it) }
                    ?: MetronomeStatus.Stopped(if (completed) "Session complete. Take a breath, then practice again." else run.stopMessage)
                lastPosition.elapsedSeconds
            }
            // Capture this session's final playback head before another Start can reset it.
            // Deliver once on the UI thread, even after the tab has left composition.
            main.post { onFinished(elapsed) }
        }
    }

    private enum class Click(val hz: Double, val gain: Double, val milliseconds: Int) {
        Sam(1900.0, .75, 18), Tali(1400.0, .55, 14), Khali(650.0, .32, 20),
        Beat(1000.0, .40, 12), Subdivision(800.0, .17, 8)
    }

    private fun synthesize(click: Click): ShortArray {
        val count = RATE * click.milliseconds / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / RATE
            // Sine-squared edges start/end at zero; decay gives a short, soft-edged woodblock tick.
            val edge = sin(PI * i / (count - 1))
            val envelope = edge * edge * exp(-5.0 * i / count)
            val tone = sin(2 * PI * click.hz * t) + .18 * sin(4 * PI * click.hz * t)
            (tone * envelope * click.gain * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private companion object {
        const val RATE = 48000
        const val BLOCK = 240
    }
}
