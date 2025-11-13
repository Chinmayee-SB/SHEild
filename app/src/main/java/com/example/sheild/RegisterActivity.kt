package com.example.sheild

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.register)

        // ✅ Get view references
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        // ✅ When Register button is clicked
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // You can add validation logic here later
            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Navigate to LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
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

        // ✅ When "Here Before? Login" text is clicked
        tvLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(
                this,
                R.anim.fade_in,
                R.anim.fade_out
            )
            startActivity(intent, options.toBundle())
            finish()
        }









    }
}