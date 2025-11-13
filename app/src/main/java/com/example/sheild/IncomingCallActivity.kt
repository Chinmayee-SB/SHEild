package com.example.sheild


import android.widget.TextView
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class IncomingCallActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var callerName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        // ⭐️ KEY CHANGE 1: Receive the caller name ⭐️
        callerName = intent.getStringExtra("callerName") ?: "Unknown"

        // Update the UI with the caller name
        val tvCallerName = findViewById<TextView>(R.id.callerName) // You need this TextView ID in your XML
        tvCallerName.text = callerName

        // Play ringtone sound
        mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_RINGTONE_URI)
        mediaPlayer?.isLooping = true // Ensure it plays until stopped
        mediaPlayer?.start()

        // Buttons
        val btnAccept = findViewById<Button>(R.id.btnAccept)
        val btnDecline = findViewById<Button>(R.id.btnDecline)

        btnDecline.setOnClickListener {
            mediaPlayer?.stop()
            finish() // close fake call screen
        }

        btnAccept.setOnClickListener {
            mediaPlayer?.stop()
            val intent = Intent(this, OngoingCallActivity::class.java)

            // ⭐️ KEY CHANGE 2: Pass the received callerName to the next activity ⭐️
            intent.putExtra("callerName", callerName)

            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
