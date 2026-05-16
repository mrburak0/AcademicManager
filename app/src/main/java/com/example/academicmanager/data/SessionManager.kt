package com.example.academicmanager.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SessionManager {
    private const val PREFS_NAME = "academic_secure_session"
    private const val KEY_USERNAME = "session_username"

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSession(context: Context, username: String) {
        prefs(context).edit().putString(KEY_USERNAME, username).apply()
    }

    fun getUsername(context: Context): String? {
        return try {
            prefs(context).getString(KEY_USERNAME, null)
        } catch (_: Exception) { null }
    }

    fun clearSession(context: Context) {
        try {
            prefs(context).edit().clear().apply()
        } catch (_: Exception) { }
    }
}
