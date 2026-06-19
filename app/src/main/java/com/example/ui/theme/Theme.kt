package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BriarLightColorScheme = lightColorScheme(
    primary = BriarGreenNeon,          // Color(0xFF6750A4)
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BriarGreenLight, // Color(0xFFD0BCFF)
    onPrimaryContainer = BriarGreenDark, // Color(0xFF21005D)
    secondary = BriarGreenLight,
    onSecondary = Color(0xFF1D1B20),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF21005D),
    background = BriarDarkBack,         // Color(0xFFFEF7FF)
    onBackground = BriarTextPrimary,     // Color(0xFF1D1B20)
    surface = BriarCardBack,            // Color(0xFFFFFFFF)
    onSurface = BriarTextPrimary,        // Color(0xFF1D1B20)
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = BriarTextSecondary, // Color(0xFF49454F)
    outline = Color(0xFF79747E),
    error = BriarError
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BriarLightColorScheme,
        typography = Typography,
        content = content
    )
}

