package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CfcBlueDark,
    secondary = CfcPurple,
    tertiary = CfcGold,
    background = CfcNavyDark,
    surface = CfcSurfaceDark,
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color.White,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    error = CfcRed
)

private val LightColorScheme = lightColorScheme(
    primary = CfcBlueLight,
    secondary = CfcPurple,
    tertiary = CfcGold,
    background = CfcBackgroundLight,
    surface = CfcSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = CfcNavyLight,
    onSurface = CfcNavyLight,
    error = CfcRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
