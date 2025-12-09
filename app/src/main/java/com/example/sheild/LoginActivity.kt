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
class LoginActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
//        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                doLogin(email, password)
            } else {
                if (email.isEmpty()) etEmail.error = "Enter email"
                if (password.isEmpty()) etPassword.error = "Enter password"
            }
        }

//        tvForgotPassword.setOnClickListener {
//            val intent = Intent(this, ForgotPassword::class.java)
//            val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
//            startActivity(intent, options.toBundle())
//        }

        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
            startActivity(intent, options.toBundle())
        }
    }

    private fun doLogin(email: String, password: String) {
        val json = JSONObject()
        json.put("email", email)
        json.put("password", password)

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url(Prefs.LOGIN_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val s = it.body?.string()
                    if (!it.isSuccessful || s == null) {
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Login failzed", Toast.LENGTH_LONG).show()
                        }
                        return
                    }

                    try {
                        val obj = JSONObject(s)
                        val ok = obj.optBoolean("ok", false)
                        if (ok) {
                            // server returns user object
                            val user = obj.optJSONObject("user")
                            val id = user?.optString("id") ?: ""
                            val name = user?.optString("full_name") ?: user?.optString("full_name") ?: ""
                            val emailResp = user?.optString("email") ?: email

                            // save prefs
                            val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
                            prefs.edit()
                                .putBoolean(Prefs.KEY_IS_LOGGED_IN, true)
                                .putString(Prefs.KEY_USER_ID, id)
                                .putString(Prefs.KEY_USER_NAME, name)
                                .putString(Prefs.KEY_USER_EMAIL, emailResp)
                                .apply()

                            runOnUiThread {
                                Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                val options = ActivityOptions.makeCustomAnimation(this@LoginActivity, R.anim.fade_in, R.anim.fade_out)
                                startActivity(intent, options.toBundle())
                                finish()
                            }
                        } else {
                            val message = obj.optString("message", "Invalid credentials")
                            runOnUiThread {
                                Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (ex: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Login error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        })
    }
}
