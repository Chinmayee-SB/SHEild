package com.example.sheild

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class FakeCallActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_call)

        // Play ringtone sound
        mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_RINGTONE_URI)
        mediaPlayer?.start()

        // Buttons
        val btnAccept = findViewById<Button>(R.id.btnAccept)
        val btnDecline = findViewById<Button>(R.id.btnDecline)

        btnDecline.setOnClickListener {
            mediaPlayer?.stop()
            finish() // close fake call
        }

        btnAccept.setOnClickListener {
            mediaPlayer?.stop()
            val intent = Intent(this, OngoingCallActivity::class.java)
            intent.putExtra("callerName", "Mom") // or dynamic
            startActivity(intent)
            // Later: Show fake ongoing call screen
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
