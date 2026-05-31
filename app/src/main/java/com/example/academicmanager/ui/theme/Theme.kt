package com.example.academicmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary         = EmeraldGreen,
    secondary       = IndigoAccent,
    background      = Slate900,
    surface         = Slate800,
    onPrimary       = TextPrimary,
    onSecondary     = TextPrimary,
    onBackground    = TextPrimary,
    onSurface       = TextPrimary,
    surfaceVariant  = Slate700,
    error           = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary         = EmeraldGreen,
    secondary       = IndigoAccent,
    background      = Color(0xFFF1F5F9),
    surface         = Color(0xFFFFFFFF),
    onPrimary       = Color.White,
    onSecondary     = Color.White,
    onBackground    = Color(0xFF0F172A),
    onSurface       = Color(0xFF0F172A),
    surfaceVariant  = Color(0xFFE2E8F0),
    error           = ErrorRed
)

@Composable
fun AcademicManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors   = if (darkTheme) DarkAppColors   else LightAppColors

    // AppColorState singleton'ı güncelle — okuyan composable'lar recompose olur
    SideEffect { AppColorState.updateColors(appColors) }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}
