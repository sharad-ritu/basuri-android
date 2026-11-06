package np.basuri.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.Process
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin

internal sealed interface NotationPlayback {
    data class Stopped(val message: String? = null) : NotationPlayback
    data class Starting(val exampleIndex: Int) : NotationPlayback
    data class Playing(val exampleIndex: Int) : NotationPlayback
    data class Error(val exampleIndex: Int, val message: String) : NotationPlayback
}

/** One worker owns one streaming track and its focus, including the final audible samples. */
internal class NotationPlayer(context: Context) {
    private val manager = context.applicationContext.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val lock = Any()
    private val mutableStatus = MutableStateFlow<NotationPlayback>(NotationPlayback.Stopped())
    val status = mutableStatus.asStateFlow()
    private var session: Session? = null

    private data class Note(val start: Long, val end: Long, val hz: Double)
    private class AudioFailure(message: String) : Exception(message)
    private class Session(
        val exampleIndex: Int,
        val beatFrames: LongArray,
        val notes: List<Note>,
        val taal: Taal,
    ) {
        val end: Long get() = beatFrames.last()
        @Volatile var cancelled = false
        var track: AudioTrack? = null // Read, pause and release only under lock.
        lateinit var worker: Thread
        var started = false
        var previousHead = 0L
        var wraps = 0L
        var stopMessage: String? = null
    }

    fun start(exampleIndex: Int, example: NotationExample, flute: Flute, bpm: Int) {
        require(bpm in 30..240 && example.beats.isNotEmpty())
        stop()
        // Round absolute boundaries, keeping fractional frames instead of accumulating drift.
        val framesPerBeat = RATE * 60.0 / bpm
        val beatFrames = LongArray(example.beats.size + 1) { (it * framesPerBeat).roundToLong() }
        val notes = mutableListOf<Note>()
        example.beats.forEachIndexed { index, beat ->
            when (beat) {
                NotationBeat.Hold -> if (notes.isNotEmpty()) {
                    // Extend the same oscillator/envelope across every Hold, with no re-attack.
                    notes[notes.lastIndex] = notes.last().copy(end = beatFrames[index + 1])
                }
                is NotationBeat.Notes -> {
                    val tones = beat.tones
                    tones.forEachIndexed { part, tone ->
                        val start = ((index + part.toDouble() / tones.size) * framesPerBeat).roundToLong()
                        val end = ((index + (part + 1.0) / tones.size) * framesPerBeat).roundToLong()
                        notes += Note(start, end, flute.frequency(tone.pitch.semitones + tone.octave.offset))
                    }
                }
            }
        }
        synchronized(lock) {
            val run = Session(exampleIndex, beatFrames, notes, notationKeherwa)
            session = run
            mutableStatus.value = NotationPlayback.Starting(exampleIndex)
            run.worker = Thread({ play(run) }, "Basuri-notation")
            run.worker.start()
        }
    }

    fun stop() {
        val run = synchronized(lock) { session } ?: return
        stop(run)
    }

    /** Pause unblocks a pending write; join finishes release before the next example can start. */
    private fun stop(run: Session, message: String? = null) {
        synchronized(lock) {
            if (session !== run) return
            run.cancelled = true
            run.stopMessage = message
            runCatching { run.track?.pause() }
        }
        run.worker.interrupt()
        // Bounded so a stalled device can't freeze the UI; the worker still releases everything itself.
        if (Thread.currentThread() != run.worker) run.worker.join(400)
    }

    /** Zero-based sounding beat. Frame callbacks only sample this value; they never advance it. */
    fun position(): Int = synchronized(lock) {
        val run = session ?: return -1
        if (!run.started || run.cancelled) return -1
        // AudioTrack is a platform boundary; the worker reports any failed device operation.
        runCatching {
            val frame = playedFrames(run)
            if (frame == 0L || frame >= run.end) -1
            else run.beatFrames.indexOfLast { it <= frame }
        }.getOrDefault(-1)
    }

    // Called under lock, also protecting playback-head reads from a concurrent release.
    private fun playedFrames(run: Session): Long {
        val track = run.track ?: return 0
        val head = track.playbackHeadPosition.toLong() and 0xffffffffL
        if (head < run.previousHead) run.wraps += 1L shl 32
        run.previousHead = head
        return run.wraps + head
    }

    private fun play(run: Session) {
        var failure: String? = null
        val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener({ change ->
                if (change < 0) stop(run, "Playback stopped because another app needs audio. Tap Play when you're ready.")
            }, Handler(Looper.getMainLooper())).build()
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                throw AudioFailure("Audio is busy. Pause other audio, then tap Play again.")
            }
            if (run.cancelled) return
            val minimum = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minimum <= 0) throw AudioFailure("This device couldn't open reference audio. Tap Play to try again.")
            val track = AudioTrack.Builder().setAudioAttributes(attributes)
                .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STREAM).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setBufferSizeInBytes(maxOf(minimum, BLOCK * 4)).build()
            synchronized(lock) { run.track = track }
            if (track.state != AudioTrack.STATE_INITIALIZED) throw AudioFailure("Reference audio couldn't start. Tap Play to try again.")
            track.setBufferSizeInFrames(maxOf(BLOCK * 2, RATE / 25))
            val clicks = Click.entries.associateWith(::synthesizeClick)
            val buffer = ShortArray(BLOCK)
            var written = 0L
            var beatIndex = 0
            var noteIndex = 0
            var click = ShortArray(0)
            var clickIndex = 0
            synchronized(lock) {
                if (run.cancelled) return
                track.play()
                run.started = true
                mutableStatus.value = NotationPlayback.Playing(run.exampleIndex)
            }
            while (!run.cancelled) {
                for (i in buffer.indices) {
                    val frame = written + i
                    if (beatIndex < run.beatFrames.lastIndex && frame == run.beatFrames[beatIndex]) {
                        val mark = run.taal.markAt(beatIndex % run.taal.beats)?.mark
                        click = clicks.getValue(when (mark) {
                            TaalMark.Sam -> Click.Sam
                            TaalMark.Tali -> Click.Tali
                            TaalMark.Khali -> Click.Khali
                            null -> Click.Beat
                        })
                        clickIndex = 0
                        beatIndex++
                    }
                    while (noteIndex < run.notes.size && frame >= run.notes[noteIndex].end) noteIndex++
                    val note = run.notes.getOrNull(noteIndex)
                    val melody = if (note != null && frame >= note.start) {
                        val age = frame - note.start
                        val remaining = note.end - frame - 1
                        val envelope = min(1.0, min(age / (RATE * .03), remaining / (RATE * .08)))
                        val phase = 2 * PI * note.hz * age / RATE
                        (sin(phase) + .12 * sin(2 * phase)) * envelope * 6500
                    } else 0.0
                    val tick = if (clickIndex < click.size) click[clickIndex++].toInt() else 0
                    buffer[i] = if (frame < run.end) (melody + tick).toInt().toShort() else 0
                }
                var offset = 0
                while (offset < buffer.size && !run.cancelled) {
                    val count = track.write(buffer, offset, buffer.size - offset, AudioTrack.WRITE_BLOCKING)
                    if (run.cancelled) break
                    if (count <= 0) throw AudioFailure("Reference audio was interrupted. Tap Play to try again.")
                    offset += count
                }
                written += offset
                // Keep streaming silence until the last note has really sounded, then release.
                if (synchronized(lock) { playedFrames(run) >= run.end }) break
            }
        } catch (error: Exception) {
            if (!run.cancelled) failure = if (error is AudioFailure) error.message
                else "Reference audio couldn't play. Pause other audio, then tap Play again."
        } finally {
            synchronized(lock) {
                run.track?.let { track ->
                    runCatching { track.stop() }
                    runCatching { track.release() }
                    run.track = null
                }
            }
            runCatching { manager.abandonAudioFocusRequest(focus) }
            synchronized(lock) {
                if (session === run) {
                    session = null
                    mutableStatus.value = failure?.let { NotationPlayback.Error(run.exampleIndex, it) }
                        ?: NotationPlayback.Stopped(run.stopMessage)
                }
            }
        }
    }

    private enum class Click(val hz: Double, val gain: Int, val milliseconds: Int) {
        Sam(1900.0, 4000, 18), Tali(1400.0, 3200, 14), Khali(650.0, 1600, 20), Beat(1000.0, 2600, 12)
    }

    private fun synthesizeClick(click: Click): ShortArray {
        val count = RATE * click.milliseconds / 1000
        return ShortArray(count) { i ->
            val edge = sin(PI * i / (count - 1))
            val envelope = edge * edge * exp(-3.0 * i / count)
            val phase = 2 * PI * click.hz * i / RATE
            ((sin(phase) + .18 * sin(2 * phase)) * envelope * click.gain).toInt().toShort()
        }
    }

    private companion object {
        const val RATE = 48000
        const val BLOCK = 240
    }
}
