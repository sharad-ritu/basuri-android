package np.basuri.app

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal enum class TanpuraString(val label: String, val semitones: Int) {
    Pa("Pa", -5), Ma("Ma", -7), Ni("Ni", -1)
}

internal sealed interface TanpuraStatus {
    data class Stopped(val message: String? = null) : TanpuraStatus
    data object Starting : TanpuraStatus
    data object Playing : TanpuraStatus
    data class Error(val message: String) : TanpuraStatus
}

/** Precomputed, band-limited plucks; the audio loop only sums overlapping table samples. */
internal class TanpuraEngine(context: Context, private val focus: PracticeAudioFocus = PracticeAudioFocus(context)) {
    private val lock = Any()
    private val mutableStatus = MutableStateFlow<TanpuraStatus>(TanpuraStatus.Stopped())
    val status = mutableStatus.asStateFlow()
    @Volatile private var cycleSeconds = 4.5f
    @Volatile private var volume = .55f
    private var session: Session? = null

    private class AudioFailure(message: String) : Exception(message)
    private class Voice(val pluck: FloatArray, var age: Int = 0)
    private class Session(val sa: Double, val string: TanpuraString) {
        @Volatile var cancelled = false
        @Volatile var stopping = false
        @Volatile var stopDeadline = Long.MAX_VALUE
        // Track operations from the UI and worker are protected by lock.
        var track: AudioTrack? = null
        var started = false
        var message: String? = null
        lateinit var worker: Thread
    }

    fun setCycle(seconds: Float) { cycleSeconds = seconds.coerceIn(3f, 6f) }
    fun setVolume(value: Float) { volume = value.coerceIn(0f, 1f) }

    fun start(saHz: Double, string: TanpuraString, lowerOctave: Boolean, cycle: Float, gain: Float) {
        require(saHz.isFinite() && saHz > 0)
        synchronized(lock) {
            if (session != null) return
            setCycle(cycle)
            setVolume(gain)
            val run = Session(if (lowerOctave) saHz / 2 else saHz, string)
            session = run
            mutableStatus.value = TanpuraStatus.Starting
            run.worker = Thread({ play(run) }, "Basuri-tanpura")
            run.worker.start()
        }
    }

    fun stop(message: String? = null) {
        val run = synchronized(lock) {
            session?.also {
                it.message = message
                it.stopping = true
                it.stopDeadline = SystemClock.elapsedRealtime() + 300
                if (!it.started) it.cancelled = true
            }
        } ?: return
        run.worker.interrupt()
        if (Thread.currentThread() != run.worker) {
            // Normally drains the 120 ms release. The bounded wait keeps a stalled device from freezing the UI;
            // the worker still releases the track and focus on its own.
            run.worker.join(450)
        }
    }

    private fun play(run: Session) {
        var failure: String? = null
        try {
            if (!focus.acquire(run) {
                if (synchronized(lock) { session === run }) {
                    stop("Drone stopped because another app needs audio. Tap Start when you're ready.")
                }
            }) throw AudioFailure("Audio is busy. Pause other audio and try again.")
            if (run.cancelled) return
            val frequencies = doubleArrayOf(
                run.sa * 2.0.pow(run.string.semitones / 12.0), run.sa * .9997, run.sa * 1.0003, run.sa / 2,
            )
            val plucks = frequencies.map { hz ->
                if (run.cancelled) return
                makePluck(hz, run)
            }
            if (run.cancelled) return
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val minimum = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minimum <= 0) throw AudioFailure("This device couldn't open drone audio. Try again.")
            val track = AudioTrack.Builder().setAudioAttributes(focus.attributes)
                .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STREAM).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setBufferSizeInBytes(maxOf(minimum, BLOCK * 4)).build()
            synchronized(lock) { run.track = track }
            if (track.state != AudioTrack.STATE_INITIALIZED) throw AudioFailure("Drone audio couldn't start. Try again.")
            track.setBufferSizeInFrames(maxOf(BLOCK * 2, RATE / 25))
            val buffer = ShortArray(BLOCK)
            val voices = ArrayList<Voice>(16)
            var frame = 0L
            var nextPluck = 0L
            var stringIndex = 0
            var gate = 0f
            var smoothVolume = volume
            var fadeEnd: Long? = null
            var previousHead = 0L
            var wraps = 0L
            synchronized(lock) {
                if (run.cancelled) return
                track.play()
                run.started = true
                mutableStatus.value = TanpuraStatus.Playing
            }
            while (!run.cancelled && SystemClock.elapsedRealtime() < run.stopDeadline) {
                val targetVolume = volume
                for (i in buffer.indices) {
                    if (!run.stopping && frame >= nextPluck) {
                        voices.add(Voice(plucks[stringIndex]))
                        stringIndex = (stringIndex + 1) % 4
                        nextPluck = frame + (cycleSeconds * RATE / 4).toLong()
                    }
                    var sample = 0f
                    var voiceIndex = 0
                    while (voiceIndex < voices.size) {
                        val voice = voices[voiceIndex++]
                        if (voice.age < voice.pluck.size) sample += voice.pluck[voice.age++]
                    }
                    gate = if (run.stopping) (gate - 1f / RELEASE).coerceAtLeast(0f)
                        else (gate + 1f / ATTACK).coerceAtMost(1f)
                    smoothVolume += (targetVolume - smoothVolume) * .001f
                    buffer[i] = (sample * gate * smoothVolume * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    if (run.stopping && gate == 0f && fadeEnd == null) fadeEnd = frame + 1
                    frame++
                }
                // Every table ends at zero. Reap finished voices once per block, without allocating per sample.
                voices.removeAll { it.age >= it.pluck.size }
                var offset = 0
                while (offset < buffer.size && !run.cancelled && SystemClock.elapsedRealtime() < run.stopDeadline) {
                    // A nonblocking write makes cancellation safe even if the output device stalls.
                    val count = track.write(buffer, offset, buffer.size - offset, AudioTrack.WRITE_NON_BLOCKING)
                    if (count < 0) throw AudioFailure("Drone audio was interrupted. Tap Start to try again.")
                    offset += count
                    if (count == 0) {
                        try { Thread.sleep(2) } catch (_: InterruptedException) { /* Recheck cancellation. */ }
                    }
                }
                if (fadeEnd != null) {
                    val head = track.playbackHeadPosition.toLong() and 0xffffffffL
                    if (head < previousHead) wraps += 1L shl 32
                    previousHead = head
                    if (wraps + head >= fadeEnd) break
                }
            }
        } catch (error: Exception) {
            if (!run.cancelled && !run.stopping) failure = if (error is AudioFailure) error.message
                else "Drone audio couldn't start. Pause other audio and try again."
        } finally {
            synchronized(lock) {
                run.track?.let {
                    runCatching { it.stop() }
                    runCatching { it.release() }
                }
                run.track = null
            }
            runCatching { focus.release(run) }
            synchronized(lock) {
                session = null
                mutableStatus.value = failure?.let { TanpuraStatus.Error(it) } ?: TanpuraStatus.Stopped(run.message)
            }
        }
    }

    private fun makePluck(hz: Double, run: Session): FloatArray {
        val size = 4096
        // The bright table adds softly weighted upper partials. Keep every partial below 9 kHz.
        val harmonics = min(14, (9000 / hz).toInt()).coerceAtLeast(1)
        val dark = FloatArray(size)
        val bright = FloatArray(size)
        val normalization = (1..harmonics).sumOf { 1.0 / it.toDouble().pow(1.45) }
        for (i in 0 until size) {
            val phase = 2 * PI * i / size
            dark[i] = ((sin(phase) + .22 * sin(2 * phase)) / 1.22).toFloat()
            bright[i] = ((1..harmonics).sumOf { harmonic ->
                sin(phase * harmonic) / harmonic.toDouble().pow(1.45)
            } / normalization).toFloat()
        }
        val pluck = FloatArray(RATE * 9)
        val step = hz * size / RATE
        var phase = 0.0
        // Evaluate slow controls at 750 Hz and interpolate them for smooth sample-rate envelopes.
        val controls = pluck.size / 64 + 1
        val envelopes = DoubleArray(controls)
        val brightness = DoubleArray(controls)
        for (i in 0 until controls) {
            val t = i * 64.0 / RATE
            envelopes[i] = min(1.0, t / .018) * min(1.0, (9 - t) / .8) * exp(-t / 2.8) * .19
            val sweep = sin(PI * .7 * t)
            // A slow brightness swell suggests the moving jawari contact of a tanpura bridge.
            brightness[i] = .18 + .38 * sweep * sweep * exp(-t / 4)
        }
        for (i in pluck.indices) {
            if (i % 4096 == 0 && run.cancelled) return pluck
            val control = i / 64
            val blend = (i % 64) / 64.0
            val envelope = envelopes[control] + (envelopes[control + 1] - envelopes[control]) * blend
            val sheen = brightness[control] + (brightness[control + 1] - brightness[control]) * blend
            val index = phase.toInt()
            val next = (index + 1) % size
            val fraction = (phase - index).toFloat()
            val body = dark[index] + (dark[next] - dark[index]) * fraction
            val buzz = bright[index] + (bright[next] - bright[index]) * fraction
            pluck[i] = ((body + (buzz - body) * sheen) * envelope).toFloat()
            phase = (phase + step) % size
        }
        // Soften the exact endpoints as well, so dropping a spent voice cannot click.
        for (i in 0 until 128) {
            pluck[i] *= i / 127f
            pluck[pluck.lastIndex - i] *= i / 127f
        }
        return pluck
    }

    private companion object {
        const val RATE = 48000
        const val BLOCK = 240
        const val ATTACK = RATE * .15f
        const val RELEASE = RATE * .12f
    }
}
