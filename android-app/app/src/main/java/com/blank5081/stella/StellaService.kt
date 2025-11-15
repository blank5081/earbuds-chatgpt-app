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
import okhttp3.*
import java.io.File
import java.io.IOException

class StellaService : Service() {

    private val TAG = "StellaService"
    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Stella service created")
        startForegroundService()
    }

    private fun startForegroundService() {
        val channelId = Config.NOTIFICATION_CHANNEL_ID
        val channelName = "Stella Assistant"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Stella is active")
            .setContentText("Listening for earbud gestures…")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(Config.NOTIFICATION_ID, notification)
    }

    companion object {

        fun startRecording(context: Context) {
            Log.d("StellaService", "Recording started…")

            val filePath = AudioRecorder.startRecording(
                context,
                Config.RECORD_FILENAME
            )

            // Wait a short time then stop and send audio
            Thread {
                Thread.sleep(1500)
                val finalFile = AudioRecorder.stopRecording()
                if (finalFile != null) {
                    sendToServer(context, File(finalFile))
                }
            }.start()
        }

        private fun sendToServer(context: Context, audioFile: File) {
            Log.d("StellaService", "Sending audio to server…")

            val client = OkHttpClient()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "audio",
                    audioFile.name,
                    RequestBody.create(MediaType.parse("audio/3gp"), audioFile)
                )
                .build()

            val request = Request.Builder()
                .url(Config.SERVER_URL)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("StellaService", "Error sending audio", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val audioBytes = response.body()?.bytes()
                    if (audioBytes != null) {
                        playAudio(context, audioBytes)
                    }
                }
            })
        }

        private fun playAudio(context: Context, audio: ByteArray) {
            try {
                val tempFile = File(context.cacheDir, "response.mp3")
                tempFile.writeBytes(audio)

                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    setDataSource(tempFile.absolutePath)
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
