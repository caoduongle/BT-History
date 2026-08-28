package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BentoDarkColorScheme = darkColorScheme(
    primary = BentoPurpleLight,
    onPrimary = BentoPurplePrimary,
    primaryContainer = BentoPurplePrimary,
    onPrimaryContainer = BentoPurpleContainer,
    secondary = BentoPurpleLight,
    onSecondary = BentoBackground,
    secondaryContainer = BentoSurfaceVariant,
    onSecondaryContainer = BentoPurpleContainer,
    tertiary = BentoGreen,
    onTertiary = BentoBackground,
    tertiaryContainer = BentoGreenBg,
    onTertiaryContainer = BentoGreen,
    background = BentoBackground,
    onBackground = BentoTextPrimary,
    surface = BentoSurface,
    onSurface = BentoTextPrimary,
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = BentoTextSecondary,
    outline = BentoOutline,
    outlineVariant = BentoCardElevated,
    error = BentoRed,
    errorContainer = BentoRedBg,
    onError = BentoBackground,
    onErrorContainer = BentoRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BentoDarkColorScheme,
        typography = Typography,
        content = content
    )
}


