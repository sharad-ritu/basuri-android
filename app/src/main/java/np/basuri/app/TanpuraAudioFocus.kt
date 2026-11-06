package np.basuri.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/** One focus lease for the Tools tab, retained until its last playing tool stops. */
internal class PracticeAudioFocus(context: Context) {
    private val manager = context.applicationContext.getSystemService(AudioManager::class.java)
    private val lock = Any()
    private val clients = mutableMapOf<Any, () -> Unit>()
    private var request: AudioFocusRequest? = null
    val attributes: AudioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()

    fun acquire(client: Any, onLoss: () -> Unit): Boolean = synchronized(lock) {
        if (request == null) {
            lateinit var focus: AudioFocusRequest
            focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener({ change ->
                    if (change < 0) loseFocus(focus)
                }, Handler(Looper.getMainLooper())).build()
            if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false
            request = focus
        }
        clients[client] = onLoss
        true
    }

    fun release(client: Any) = synchronized(lock) {
        clients.remove(client)
        if (clients.isEmpty()) {
            request?.let { manager.abandonAudioFocusRequest(it) }
            request = null
        }
    }

    private fun loseFocus(focus: AudioFocusRequest) {
        val callbacks = synchronized(lock) {
            if (request !== focus) return
            request = null
            val snapshot = clients.values.toList()
            clients.clear()
            manager.abandonAudioFocusRequest(focus)
            snapshot
        }
        // Never join an audio worker while holding the focus lock: its finally block releases its lease.
        callbacks.forEach { it() }
    }
}
