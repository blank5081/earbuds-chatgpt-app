package com.blank5081.stella

import android.content.Context
import android.os.PowerManager
import android.util.Log

object WakeLockManager {
    private var wakeLock: PowerManager.WakeLock? = null
    private val TAG = "WakeLockManager"

    fun acquire(context: Context, timeoutMs: Long = 10_000L) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (wakeLock == null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Stella:WakeLock")
                wakeLock?.setReferenceCounted(false)
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(timeoutMs)
                Log.d(TAG, "WakeLock acquired for ${timeoutMs}ms")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wakelock", e)
        }
    }

    fun release() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            // ignore
        }
    }
}
