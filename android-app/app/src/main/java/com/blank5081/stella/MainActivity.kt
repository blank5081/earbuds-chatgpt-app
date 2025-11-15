package com.blank5081.stella

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.content.Intent

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.statusText)
        val button = findViewById<Button>(R.id.testButton)

        // Start Stella background service so earbud gestures work
        startService(Intent(this, StellaService::class.java))

        button.setOnClickListener {
            status.text = "Recording…"
            // Manually trigger recording for testing
            StellaService.startRecording(this)
        }
    }
}
