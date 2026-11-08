package de.romerlap.einkaufsliste.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Retro-Comic-Palette (hell): Creme-Papier, Comic-Rot, Sonnengelb, Himmelblau. */
private val LightColors = lightColorScheme(
    primary = Color(0xFFD62828),          // Comic-Rot
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFC800), // Sonnengelb
    onPrimaryContainer = Ink,
    secondary = Color(0xFF1D6FD1),        // Comic-Blau
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFA8D8FF),
    onSecondaryContainer = Ink,
    tertiary = Color(0xFFFFB703),         // Akzent-Gelb
    onTertiary = Ink,
    tertiaryContainer = Color(0xFFFFE3A3),
    onTertiaryContainer = Ink,
    background = Color(0xFFFFF6E5),       // Creme-Papier
    onBackground = Ink,
    surface = Color(0xFFFFF6E5),
    onSurface = Ink,
    surfaceVariant = Color(0xFFFFE9B3),
    onSurfaceVariant = Color(0xFF4A443A),
    surfaceContainer = Color(0xFFFFFDF5),
    surfaceContainerHigh = Color(0xFFFFF3D6),
    surfaceContainerHighest = Color(0xFFFFE9B3),
    outline = Ink,
    outlineVariant = Ink,
    error = Color(0xFFC1121F),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFD4D4),
    onErrorContainer = Ink
)

/** Retro-Comic-Palette (dunkel): warmes Braun-Papier mit knalligen Akzenten. */
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6B6B),
    onPrimary = Color(0xFF2B0B0B),
    primaryContainer = Color(0xFFB34700),
    onPrimaryContainer = Color(0xFFFFE3A3),
    secondary = Color(0xFF6FA8DC),
    onSecondary = Color(0xFF0B2238),
    secondaryContainer = Color(0xFF2C4A6E),
    onSecondaryContainer = Color(0xFFD6EBFF),
    tertiary = Color(0xFFFFC800),
    onTertiary = Color(0xFF2B2205),
    tertiaryContainer = Color(0xFF7A5D00),
    onTertiaryContainer = Color(0xFFFFE3A3),
    background = Color(0xFF26221C),
    onBackground = Color(0xFFFFF3D6),
    surface = Color(0xFF26221C),
    onSurface = Color(0xFFFFF3D6),
    surfaceVariant = Color(0xFF3B352B),
    onSurfaceVariant = Color(0xFFE8DCC0),
    surfaceContainer = Color(0xFF302B24),
    surfaceContainerHigh = Color(0xFF3B352B),
    surfaceContainerHighest = Color(0xFF474034),
    outline = Color(0xFFFFF3D6),
    outlineVariant = Color(0xFF8A7F6B),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF2B0B0B),
    errorContainer = Color(0xFF7A1F26),
    onErrorContainer = Color(0xFFFFD4D4)
)

@Composable
fun EinkaufslisteTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = ComicTypography,
        content = content
    )
}
