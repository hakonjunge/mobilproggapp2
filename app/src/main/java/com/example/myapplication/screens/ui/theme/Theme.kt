package com.example.myapplication.screens.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.preferences.ThemePreferences

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF956BFA), // Gold
    secondary = Color(0xFFB8AAE7), // Orange
    tertiary = Color(0xFF800194), // Deep Orange
    background = Color(0xFF000000), // Svart bakgrunn
    surface = Color(0xFF121212), // Mørk overflate
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White, // Hvit tekst på bakgrunn
    onSurface = Color.White // Hvit tekst på overflater
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00A4FF), // Gold
    secondary = Color(0xFF02E1FF), // Orange
    tertiary = Color(0xFF00A4FF), // Deep Orange

    background = Color(0xFFFFFFFF), // White
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // Observer brukerens lagrede preferanse for mørk modus
    val isDarkModeEnabled = ThemePreferences.isDarkModeEnabled(context).collectAsState(initial = false)

    MaterialTheme(
        // Velg fargeskjema basert på brukerens preferanse
        colorScheme = if (isDarkModeEnabled.value) DarkColorScheme else LightColorScheme,
        typography = com.example.myapplication.ui.theme.Typography,
        content = content
    )
}