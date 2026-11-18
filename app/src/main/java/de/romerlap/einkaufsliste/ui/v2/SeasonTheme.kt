package de.romerlap.einkaufsliste.ui.v2

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import de.romerlap.einkaufsliste.ui.theme.Baloo2
import de.romerlap.einkaufsliste.ui.theme.ComicTypography

/** Saison-Daten für alle Komponenten der V2-UI. */
data class SeasonColors(
    val season: Season,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val sky: Color,
    val surface: Color,
    val ink: Color,
    val soft: Color,
    val deco: Color,
    val deco2: Color
)

val LocalSeason = staticCompositionLocalOf {
    SeasonColors(
        Season.current(), Color(0xFF5B7FB5), Color(0xFFF2B950), Color(0xFFF2F5FA),
        Color(0xFFDDE6F2), Color.White, Color(0xFF374252), Color(0xFF71809B),
        Color.White, Color(0xFFBFD0E6)
    )
}

/**
 * Saisonales Cozy-Theme: aleitete Material-Farben und Typografie aus der
 * aktiven Jahreszeit ab. Auf Android 17 (API 37+) werden - als Lehre aus
 * dem weißen-Bildschirm-Problem - die runden Baloo-2-Schriften auf die
 * Android-Standardschriften reduziert; alle Farben und Effekte bleiben.
 */
@Composable
fun SeasonTheme(
    season: Season = Season.current(),
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val colors = season.toColors()
    // Im Dunkelmodus: Hintergrund und Himmel dunkler, Textfarben heller halten.
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.primary,
            secondary = colors.secondary,
            background = Color(0xFF26221C),
            surface = Color(0xFF302B24),
            onPrimary = Color.White,
            onBackground = colors.sky,
            onSurface = colors.sky,
            onSurfaceVariant = colors.soft
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            secondary = colors.secondary,
            background = colors.background,
            surface = colors.surface,
            onPrimary = Color.White,
            onBackground = colors.ink,
            onSurface = colors.ink,
            onSurfaceVariant = colors.soft
        )
    }
    val typography = if (Build.VERSION.SDK_INT >= 37) Typography() else ComicTypography
    CompositionLocalProvider(LocalSeason provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            content = content
        )
    }
}

private fun Season.toColors() = SeasonColors(
    season = this,
    primary = Color(primary),
    secondary = Color(secondary),
    background = Color(background),
    sky = Color(sky),
    surface = Color(surface),
    ink = Color(ink),
    soft = Color(soft),
    deco = Color(deco),
    deco2 = Color(deco2)
)

/** Baloo-2-Fontfamily als Lokal-Export für cozy Texte (nicht API 37+). */
val CozyFont = Baloo2
