package de.romerlap.einkaufsliste.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Build

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6C4A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA9F2C8),
    onPrimaryContainer = Color(0xFF002112),
    secondary = Color(0xFF4E6355),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8D6),
    onSecondaryContainer = Color(0xFF0B1F14),
    tertiary = Color(0xFF3D6373),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E8FB),
    onTertiaryContainer = Color(0xFF001F28)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DD5AD),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF005233),
    onPrimaryContainer = Color(0xFFA9F2C8),
    secondary = Color(0xFFB4CCBA),
    onSecondary = Color(0xFF203528),
    secondaryContainer = Color(0xFF364B3E),
    onSecondaryContainer = Color(0xFFD0E8D6),
    tertiary = Color(0xFFA5CCDF),
    onTertiary = Color(0xFF053543),
    tertiaryContainer = Color(0xFF234B5A),
    onTertiaryContainer = Color(0xFFC1E8FB)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EinkaufslisteTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColors else LightColors
    }
    MaterialExpressiveTheme(colorScheme = colorScheme, content = content)
}
