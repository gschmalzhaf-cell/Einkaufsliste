package de.romerlap.einkaufsliste.ui.v2

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val deco2: Color,
    val onCycleSeason: () -> Unit = {}
)

val LocalSeason = staticCompositionLocalOf {
    SeasonColors(
        Season.current(), Color(0xFF5B7FB5), Color(0xFFF2B950), Color(0xFFF2F5FA),
        Color(0xFFDDE6F2), Color.White, Color(0xFF374252), Color(0xFF71809B),
        Color.White, Color(0xFFBFD0E6)
    )
}

/**
 * Saisonales Cozy-Theme: leitet Material-Farben und Typografie aus der
 * aktiven Jahreszeit ab (mit weichem 600-ms-Crossfade beim Wechsel).
 * Auf Android 17 (API 37+) werden die runden Baloo-2-Schriften auf die
 * Android-Standardschriften reduziert; alle Farben und Effekte bleiben.
 */
@Composable
fun SeasonTheme(
    season: Season = Season.current(),
    content: @Composable () -> Unit
) {
    var activeSeason by remember(season) { mutableStateOf(season) }
    val dark = isSystemInDarkTheme()
    val raw = activeSeason.toRawColors()
    val spec = tween<Color>(durationMillis = 550)

    val primary by animateColorAsState(raw.primary, spec, label = "primary")
    val secondary by animateColorAsState(raw.secondary, spec, label = "secondary")
    val background by animateColorAsState(
        if (dark) Color(0xFF231F1B) else raw.background,
        spec,
        label = "bg"
    )
    val sky by animateColorAsState(
        if (dark) Color(0xFF2D2722) else raw.sky,
        spec,
        label = "sky"
    )
    val surface by animateColorAsState(
        if (dark) Color(0xFF302B25) else raw.surface,
        spec,
        label = "surface"
    )
    val ink by animateColorAsState(
        if (dark) raw.sky else raw.ink,
        spec,
        label = "ink"
    )
    val soft by animateColorAsState(raw.soft, spec, label = "soft")
    val deco by animateColorAsState(raw.deco, spec, label = "deco")
    val deco2 by animateColorAsState(raw.deco2, spec, label = "deco2")

    val colors = SeasonColors(
        season = activeSeason,
        primary = primary,
        secondary = secondary,
        background = background,
        sky = sky,
        surface = surface,
        ink = ink,
        soft = soft,
        deco = deco,
        deco2 = deco2,
        onCycleSeason = {
            val all = Season.entries
            activeSeason = all[(activeSeason.ordinal + 1) % all.size]
        }
    )

    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.primary,
            secondary = colors.secondary,
            background = colors.background,
            surface = colors.surface,
            onPrimary = Color.White,
            onBackground = colors.ink,
            onSurface = colors.ink,
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

private fun Season.toRawColors() = SeasonColors(
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

