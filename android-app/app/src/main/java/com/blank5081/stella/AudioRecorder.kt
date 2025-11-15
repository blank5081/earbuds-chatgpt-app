package com.blank5081.stella

import android.content.Context
import android.media.MediaRecorder
import java.io.File

object AudioRecorder {
    private var recorder: MediaRecorder? = null
    private var lastFile: File? = null

    fun startRecording(context: Context, fileName: String): String {
        stopRecording()
        val file = File(context.cacheDir, fileName)
        lastFile = file

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(file.absolutePath)

            try {
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return file.absolutePath
    }

    fun stopRecording(): String? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            lastFile?.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
