package com.example.sheild

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        // ✅ Now you can safely reference your TextView
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        // ✅ When Login button is clicked
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // You can add validation logic here later
            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Navigate to LoginActivity
                val intent = Intent(this, MainActivity::class.java)
                val options = ActivityOptions.makeCustomAnimation(
                    this,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
                startActivity(intent, options.toBundle())
                finish()
            } else {
                etEmail.error = "Enter email"
                etPassword.error = "Enter password"
            }
        }
        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPassword::class.java)

            // optional fade animation
            val options = ActivityOptions.makeCustomAnimation(
                this,
                R.anim.fade_in,
                R.anim.fade_out
            )
            startActivity(intent, options.toBundle())
        }

        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)

            // optional fade animation
            val options = ActivityOptions.makeCustomAnimation(
                this,
                R.anim.fade_in,
                R.anim.fade_out
            )
            startActivity(intent, options.toBundle())
        }
    }
}