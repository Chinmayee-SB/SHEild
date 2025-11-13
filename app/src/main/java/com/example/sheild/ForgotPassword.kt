package com.example.sheild

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.AlertDialog
import android.widget.Button
import android.widget.EditText


class ForgotPassword : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)
        val btnResetPassword = findViewById<Button>(R.id.btnResetPassword)
        val etForgotEmail = findViewById<EditText>(R.id.ForgotEmail) // Reference the email field

        // 2. Set the click listener on the button
        btnResetPassword.setOnClickListener {
            val email = etForgotEmail.text.toString().trim()

            if (email.isNotEmpty()) {
                // Call the function to show the custom pop-up
                showNotificationSentDialog()
            } else {
                etForgotEmail.error = "Please enter your email"
            }
        }
    }

    // 3. Function to create and show the custom AlertDialog
    private fun showNotificationSentDialog() {
        // Build the AlertDialog
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog) // We'll define CustomAlertDialog style below

        // Use an existing layout for simplicity, or create a custom one if needed.
        // For this example, we'll use a basic setup that you can theme to match the image.
        builder.setTitle("Notification Sent!!")
        // No message needed since the title is expressive

        // Set the "Ok" button action
        builder.setPositiveButton("Ok") { dialog, which ->
            // Action to perform when 'Ok' is clicked (e.g., navigate back to login)
            finish()
        }

        // Create and show the AlertDialog
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)

        dialog.show()
        }
    }
