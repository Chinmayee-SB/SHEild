package com.example.sheild

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import android.os.Looper

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@SplashActivity, LoginActivity::class.java)

            // ✅ Use ActivityOptions for modern transitions
            val options = ActivityOptions.makeCustomAnimation(
                this@SplashActivity,   // context
                R.anim.fade_in,        // enter animation
                R.anim.fade_out        // exit animation
            )

            startActivity(intent, options.toBundle())  // <-- options.toBundle() works here
            finish()
        }, 3000)

    }
}