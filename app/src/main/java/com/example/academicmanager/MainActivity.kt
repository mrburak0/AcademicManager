package com.example.academicmanager

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.example.academicmanager.data.AppSettings
import com.example.academicmanager.ui.MainScreen
import com.example.academicmanager.ui.theme.AcademicManagerTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.util.Locale
class MainActivity : ComponentActivity() {

    // Uygulama başlamadan önce dil locale'ini uygula
    override fun attachBaseContext(newBase: Context) {
        val settings = AppSettings(newBase)
        val locale   = Locale(settings.language)
        val config   = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Firestore çevrimdışı kalıcılığını etkinleştir
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        val appSettings = AppSettings(this)

        setContent {
            // Tema tercihi: değişince anında uygulanır (activity restart gerekmez)
            var themeMode by remember { mutableStateOf(appSettings.themeMode) }

            val isDark = when (themeMode) {
                AppSettings.THEME_DARK  -> true
                AppSettings.THEME_LIGHT -> false
                else                    -> isSystemInDarkTheme() // SYSTEM
            }

            AcademicManagerTheme(darkTheme = isDark) {
                MainScreen(
                    appSettings   = appSettings,
                    currentTheme  = themeMode,
                    onThemeChange = { newTheme ->
                        appSettings.themeMode = newTheme
                        themeMode = newTheme
                    }
                )
            }
        }
    }
}
