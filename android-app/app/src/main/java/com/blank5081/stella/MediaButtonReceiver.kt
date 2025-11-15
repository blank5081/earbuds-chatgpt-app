package com.blank5081.stella

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.KeyEvent

class MediaButtonReceiver : BroadcastReceiver() {
    private val TAG = "MediaButtonReceiver"

    override fun onReceive(context: Context, intent: Intent) {

        if (Intent.ACTION_MEDIA_BUTTON == intent.action) {
            val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)

            if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                val key = keyEvent.keyCode
                Log.d(TAG, "Media button pressed: $key")

                // Start Stella's recording function
                StellaService.startRecording(context)

                // Try to stop other apps (Google Assistant, etc.) from taking over
                abortBroadcast()
            }
        }
    }
}
