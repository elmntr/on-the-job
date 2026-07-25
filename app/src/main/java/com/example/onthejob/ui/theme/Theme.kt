package com.example.onthejob.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// DESIGN.md specifies one fixed paper-and-ink palette with no dark-mode variant.
// This theme is intentionally light-only — dark mode is out of scope until
// DESIGN.md defines one, so isSystemInDarkTheme() is not used here on purpose.

// DESIGN.md doesn't define an error color. Using a muted brick tone that fits
// the sage/ink palette rather than Material's default red — flag if you want
// something else; easy to swap.
private val ErrorTone = Color(0xFFB3564A)
private val ErrorToneBg = Color(0xFFF3DEDB)

private val OnTheJobColorScheme = lightColorScheme(
    primary = Amber,
    onPrimary = Ink,
    primaryContainer = Amber,
    onPrimaryContainer = Ink,

    secondary = AmberDark,
    onSecondary = Paper,

    background = Paper,
    onBackground = Ink,

    surface = CardSurface,
    onSurface = Ink2,
    surfaceVariant = Paper2,
    onSurfaceVariant = Muted,

    error = ErrorTone,
    onError = Paper,
    errorContainer = ErrorToneBg,
    onErrorContainer = ErrorTone,

    outline = Muted,
    outlineVariant = Muted,
)

@Composable
fun OnTheJobTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OnTheJobColorScheme,
        typography = OnTheJobTypography,
        content = content,
    )
}