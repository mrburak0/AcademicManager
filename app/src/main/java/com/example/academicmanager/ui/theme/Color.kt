package com.example.academicmanager.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

// Sabit marka renkleri (her iki temada da aynı)
val EmeraldGreen = Color(0xFF10B981)
val IndigoAccent = Color(0xFF6366F1)
val ErrorRed     = Color(0xFFEF4444)
val SuccessGreen = Color(0xFF10B981)

// Koyu tema sabitleri (geriye uyumluluk + PDF gibi composable-dışı alanlar için)
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val TextPrimary   = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

// ─── Tema renklerini tutan veri sınıfı ─────────────────────────
data class AppColors(
    val background   : Color,
    val surface      : Color,
    val surface2     : Color,
    val textPrimary  : Color,
    val textSecondary: Color,
    val border       : Color,
    val inputBg      : Color,
    val isDark       : Boolean
)

val DarkAppColors = AppColors(
    background    = Slate900,
    surface       = Slate800,
    surface2      = Slate700,
    textPrimary   = TextPrimary,
    textSecondary = TextSecondary,
    border        = Slate700,
    inputBg       = Slate700,
    isDark        = true
)

val LightAppColors = AppColors(
    background    = Color(0xFFF1F5F9),
    surface       = Color(0xFFFFFFFF),
    surface2      = Color(0xFFE2E8F0),
    textPrimary   = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    border        = Color(0xFFCBD5E1),
    inputBg       = Color(0xFFF8FAFC),
    isDark        = false
)

val LocalAppColors = compositionLocalOf { DarkAppColors }

// ─── Singleton — her composable'dan @Composable olmadan okunabilir ──
// mutableStateOf kullanıldığı için okuyan composable'lar değişince recompose olur
private val _appColors = mutableStateOf(DarkAppColors)

object AppColorState {
    val background   : Color   get() = _appColors.value.background
    val surface      : Color   get() = _appColors.value.surface
    val surface2     : Color   get() = _appColors.value.surface2
    val textPrimary  : Color   get() = _appColors.value.textPrimary
    val textSecondary: Color   get() = _appColors.value.textSecondary
    val border       : Color   get() = _appColors.value.border
    val inputBg      : Color   get() = _appColors.value.inputBg
    val isDark       : Boolean get() = _appColors.value.isDark

    fun updateColors(c: AppColors) { _appColors.value = c }
}
