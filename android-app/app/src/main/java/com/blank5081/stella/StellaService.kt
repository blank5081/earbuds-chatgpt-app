package com.blank5081.stella

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.io.File

class StellaService : Service() {

    private val TAG = "StellaService"
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification("Stella ready")
        Log.d(TAG, "StellaService created")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceWithNotification(text: String) {
        val channelId = Config.NOTIFICATION_CHANNEL_ID
        val channelName = "Stella Voice Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }

        val notif: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Stella")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(Config.NOTIFICATION_ID, notif)
    }

    companion object {
        fun startRecording(context: Context) {
            val i = Intent(context, StellaService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }

            Thread {
                try {
                    WakeLockManager.acquire(context, 8_000L)

                    val filePath = AudioRecorder.startRecording(context, Config.RECORD_FILENAME)
                    Thread.sleep(1600)
                    val finalPath = AudioRecorder.stopRecording() ?: return@Thread

                    val json: JSONObject = ApiClient.uploadAudio(Config.SERVER_URL, finalPath)
                    handleServerResponse(context, json)

                    try { File(finalPath).delete() } catch (_: Exception) {}
                } catch (e: Exception) {
                    Log.e("StellaService", "Error in flow", e)
                } finally {
                    WakeLockManager.release()
                }
            }.start()
        }

        private fun handleServerResponse(context: Context, json: JSONObject) {
            try {
                if (json.has("audio_path")) {
                    Mp3Player.playFromPath(context, json.getString("audio_path"))
                    return
                }

                if (json.has("audio_url")) {
                    streamAndPlay(context, json.getString("audio_url"))
                    return
                }

                if (json.has("text")) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(
                        Config.NOTIFICATION_ID,
                        NotificationCompat.Builder(context, Config.NOTIFICATION_CHANNEL_ID)
                            .setContentTitle("Stella")
                            .setContentText(json.getString("text"))
                            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                            .build()
                    )
                }
            } catch (e: Exception) {
                Log.e("StellaService", "Response handling error", e)
            }
        }

        private fun streamAndPlay(context: Context, url: String) {
            try {
                val mp = MediaPlayer()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                mp.setDataSource(url)
                mp.setOnPreparedListener { it.start() }
                mp.setOnCompletionListener { it.release() }
                mp.prepareAsync()
            } catch (e: Exception) {
                Log.e("StellaService", "Streaming error", e)
            }
        }
    }
}
