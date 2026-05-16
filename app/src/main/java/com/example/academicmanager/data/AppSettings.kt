package com.example.academicmanager.data

import android.content.Context

// Tema ve dil ayarları — SharedPreferences ile saklanır (synchronous okuma için DataStore yerine)
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_DARK   = "DARK"
        const val THEME_LIGHT  = "LIGHT"
        const val LANG_TR      = "tr"
        const val LANG_EN      = "en"
    }

    // Tema tercihi: SYSTEM (sistem teması), DARK (koyu), LIGHT (açık)
    var themeMode: String
        get()       = prefs.getString("theme", THEME_SYSTEM) ?: THEME_SYSTEM
        set(value)  { prefs.edit().putString("theme", value).apply() }

    // Dil tercihi: tr (Türkçe), en (İngilizce)
    var language: String
        get()       = prefs.getString("language", LANG_TR) ?: LANG_TR
        set(value)  { prefs.edit().putString("language", value).apply() }
}
