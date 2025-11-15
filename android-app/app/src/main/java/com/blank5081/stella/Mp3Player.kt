package com.blank5081.stella

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.File

object Mp3Player {
    private var player: MediaPlayer? = null
    private val TAG = "Mp3Player"

    fun playFromFile(context: Context, file: File, onDone: (() -> Unit)? = null) {
        stop()
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    onDone?.invoke()
                    stop()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MP3 playback error: $what / $extra")
                    stop()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "playFromFile error", e)
        }
    }

    fun playFromPath(context: Context, path: String, onDone: (() -> Unit)? = null) {
        playFromFile(context, File(path), onDone)
    }

    fun stop() {
        try {
            player?.stop()
            player?.release()
            player = null
        } catch (e: Exception) {
            // ignore
        }
    }
}
