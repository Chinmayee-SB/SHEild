package com.example.sheild

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OngoingCallActivity : AppCompatActivity() {

    private var seconds = 0
    private var running = true
    private lateinit var timerText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ongoing_call)

        val callerName = findViewById<TextView>(R.id.callerName)
        timerText = findViewById(R.id.callTimer)
        val btnEnd = findViewById<Button>(R.id.btnEndCall)

        // Set fake caller name (could pass via Intent later)
        val callerNameText = intent.getStringExtra("callerName") ?: "Caller"
        callerName.text = callerNameText

        // Start timer
        runTimer()

        btnEnd.setOnClickListener {
            running = false
            finish()
        }
    }

    private fun runTimer() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                val minutes = seconds / 60
                val secs = seconds % 60
                val time = String.format("%02d:%02d", minutes, secs)
                timerText.text = time

                if (running) {
                    seconds++
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }
}
