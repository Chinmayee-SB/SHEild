package com.example.sheild

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class RegisterActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.register)

        val etFullName = findViewById<EditText>(R.id.etFullName) // ensure this exists in layout
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            var valid = true
            if (fullName.isEmpty()) { etFullName.error = "Enter name"; valid = false }
            if (email.isEmpty()) { etEmail.error = "Enter email"; valid = false }
            if (password.isEmpty()) { etPassword.error = "Enter password"; valid = false }
            if (!valid) return@setOnClickListener

            doRegister(fullName, email, password)
        }

        tvLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
            startActivity(intent, options.toBundle())
            finish()
        }
    }

    private fun doRegister(fullName: String, email: String, password: String) {
        val json = JSONObject()
        json.put("full_name", fullName)
        json.put("email", email)
        json.put("password", password)

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url(Prefs.REGISTER_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val s = it.body?.string()
                    if (!it.isSuccessful || s == null) {
                        runOnUiThread {
                            Toast.makeText(this@RegisterActivity, "Registration failed", Toast.LENGTH_LONG).show()
                        }
                        return
                    }

                    try {
                        val obj = JSONObject(s)
                        val ok = obj.optBoolean("ok", false)
                        if (ok) {
                            val user = obj.optJSONObject("user")
                            val id = user?.optString("id") ?: ""
                            val name = user?.optString("full_name") ?: fullName
                            val emailResp = user?.optString("email") ?: email

                            val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
                            prefs.edit()
                                .putBoolean(Prefs.KEY_IS_LOGGED_IN, true)
                                .putString(Prefs.KEY_USER_ID, id)
                                .putString(Prefs.KEY_USER_NAME, name)
                                .putString(Prefs.KEY_USER_EMAIL, emailResp)
                                .apply()

                            runOnUiThread {
                                Toast.makeText(this@RegisterActivity, "Registered successfully", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                                val options = ActivityOptions.makeCustomAnimation(this@RegisterActivity, R.anim.fade_in, R.anim.fade_out)
                                startActivity(intent, options.toBundle())
                                finish()
                            }
                        } else {
                            val message = obj.optString("message", "Registration failed")
                            runOnUiThread {
                                Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (ex: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@RegisterActivity, "Registration error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        })
    }
}
