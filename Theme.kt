package com.limpamidia.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B7A6E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F0E3),
    onPrimaryContainer = Color(0xFF00201B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FD1B5),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005046),
    onPrimaryContainer = Color(0xFFB8F0E3),
)

@Composable
fun LimpaMidiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
