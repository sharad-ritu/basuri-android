package np.basuri.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.math.*

sealed interface PitchReading {
    data object Quiet : PitchReading
    data object Unclear : PitchReading
    data class Tone(val hz: Double) : PitchReading
}

/** One coroutine owns one audio resource. Cancel and join it before starting another. */
class AudioEngine(context: Context, onInterrupted: () -> Unit) {
    private val app = context.applicationContext
    private val manager = app.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener { if (it < 0) onInterrupted() }.build()

    private fun acquireFocus() {
        check(manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            "Audio is busy. Pause other audio and try again."
        }
    }

    suspend fun play(frequency: Double) {
        acquireFocus()
        var track: AudioTrack? = null
        try {
            val rate = 44100
            val count = (rate * 1.8).toInt()
            val samples = ShortArray(count) { i ->
                val t = i.toDouble() / rate
                val envelope = min(1.0, min(t / .06, (1.8 - t) / .16)).coerceAtLeast(0.0)
                val phase = 2 * PI * frequency * t
                ((sin(phase) + .12 * sin(2 * phase)) * envelope * 6500).toInt().toShort()
            }
            track = AudioTrack.Builder().setAudioAttributes(attributes)
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(count * 2).build()
            check(track.state != AudioTrack.STATE_UNINITIALIZED) { "Playback could not start. Try again." }
            check(track.write(samples, 0, samples.size) == samples.size) { "Could not load the reference tone." }
            check(track.state == AudioTrack.STATE_INITIALIZED) { "Playback could not load. Try again." }
            track.play()
            delay(1850)
        } finally {
            track?.let { runCatching { it.stop() }; it.release() }
            manager.abandonAudioFocusRequest(focus)
        }
    }

    suspend fun listen(onReading: suspend (PitchReading) -> Unit) {
        check(app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "Microphone permission is off. Enable it or practice without a pitch check."
        }
        acquireFocus()
        var recorder: AudioRecord? = null
        try {
            // Allow the speaker's reference tone to decay before opening the microphone.
            delay(350)
            val rate = 24000
            val minimum = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(minimum > 0) { "This device does not support microphone capture at 24 kHz." }
            recorder = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())
                .setBufferSizeInBytes(max(minimum, 8192)).build()
            check(recorder.state == AudioRecord.STATE_INITIALIZED) { "Microphone could not open. Close other recording apps and retry." }
            recorder.startRecording()
            check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone could not start." }
            val samples = ShortArray(2048)
            while (true) {
                currentCoroutineContext().ensureActive()
                var offset = 0
                while (offset < samples.size) {
                    currentCoroutineContext().ensureActive()
                    val count = recorder.read(samples, offset, samples.size - offset, AudioRecord.READ_BLOCKING)
                    check(count > 0) { "Microphone input stopped. Try listening again." }
                    offset += count
                }
                onReading(PitchDetector.detect(samples, rate))
            }
        } finally {
            recorder?.let { runCatching { it.stop() }; it.release() }
            manager.abandonAudioFocusRequest(focus)
        }
    }
}

/** YIN difference function with a fixed comparison window and parabolic lag refinement. */
private object PitchDetector {
    fun detect(samples: ShortArray, rate: Int): PitchReading {
        val mean = samples.sumOf { it.toDouble() } / samples.size
        val data = DoubleArray(samples.size) { (samples[it] - mean) / 32768.0 }
        val rms = sqrt(data.sumOf { it * it } / data.size)
        if (rms < .006) return PitchReading.Quiet
        if (samples.count { abs(it.toInt()) > 32000 } > samples.size / 50) return PitchReading.Unclear
        // About 75–2100 Hz: low Pa on a bass flute through high Sa on a small one.
        val minLag = rate / 2100
        val maxLag = rate / 75
        val window = samples.size - maxLag
        val difference = DoubleArray(maxLag + 1)
        for (lag in 1..maxLag) {
            var sum = 0.0
            for (i in 0 until window) { val d = data[i] - data[i + lag]; sum += d * d }
            difference[lag] = sum
        }
        var running = 0.0
        val normalized = DoubleArray(maxLag + 1) { 1.0 }
        for (lag in 1..maxLag) {
            running += difference[lag]
            normalized[lag] = if (running == 0.0) 1.0 else difference[lag] * lag / running
        }
        var lag = minLag
        while (lag < maxLag - 1) {
            if (normalized[lag] < .12) {
                while (lag + 1 < maxLag && normalized[lag + 1] < normalized[lag]) lag++
                if (normalized[lag] >= .12 || lag >= maxLag) return PitchReading.Unclear
                val left = normalized[lag - 1]
                val middle = normalized[lag]
                val right = normalized[lag + 1]
                val denominator = 2 * (2 * middle - right - left)
                val refined = lag + if (abs(denominator) > 1e-10) (right - left) / denominator else 0.0
                return PitchReading.Tone(rate / refined)
            }
            lag++
        }
        return PitchReading.Unclear
    }
}
