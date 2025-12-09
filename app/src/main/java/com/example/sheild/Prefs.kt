package com.example.sheild

object Prefs {
    const val NAME = "sheild_prefs"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_EMAIL = "user_email"

    // <-- update SERVER_BASE_URL to your server's address (including http/https and port if needed)

    // http://192.168.29.118:8000 - lava mobile phone
    const val SERVER_BASE_URL = "http://172.20.10.3:8000"
    const val LOGIN_URL = "$SERVER_BASE_URL/login"
    const val REGISTER_URL = "$SERVER_BASE_URL/register"
}