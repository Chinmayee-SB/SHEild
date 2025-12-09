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
            val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean(Prefs.KEY_IS_LOGGED_IN, false)

            val destIntent = if (isLoggedIn) {
                Intent(this@SplashActivity, MainActivity::class.java)
            } else {
                Intent(this@SplashActivity, LoginActivity::class.java)
            }

            val options = ActivityOptions.makeCustomAnimation(
                this@SplashActivity,
                R.anim.fade_in,
                R.anim.fade_out
            )
            startActivity(destIntent, options.toBundle())
            finish()
        }, 1500) // optional shorter delay
    }
}
