package com.limpamidia.app.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Toca um áudio por vez, para você ouvir antes de decidir apagar. */
class AudioPlayer(context: Context) {

    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null

    var playingId by mutableStateOf<String?>(null)
        private set

    fun toggle(item: MediaItem) {
        if (playingId == item.id) {
            stop()
            return
        }
        stop()
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mp.setDataSource(appContext, item.uri)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { stop() }
            mp.setOnErrorListener { _, _, _ ->
                stop()
                true
            }
            mp.prepareAsync()
            player = mp
            playingId = item.id
        } catch (_: Exception) {
            mp.release()
            player = null
            playingId = null
        }
    }

    fun stop() {
        val current = player
        player = null
        playingId = null
        if (current != null) {
            try {
                current.stop()
            } catch (_: Exception) {
            }
            current.release()
        }
    }
}
