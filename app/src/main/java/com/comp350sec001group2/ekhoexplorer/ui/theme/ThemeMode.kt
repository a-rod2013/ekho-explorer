package com.comp350sec001group2.ekhoexplorer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

// Using enum in preparation for settings menu
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
