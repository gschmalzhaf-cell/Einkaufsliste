package de.romerlap.einkaufsliste.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Comic-Tuschefarbe für Konturen. */
val Ink = Color(0xFF1D1D1B)

/**
 * Karte im Retro-Comic-Stil: Fläche mit dicker Kontur und hartem,
 * versetztem Schatten („Sticker-Look"). Kontur, Kartenfarbe und
 * Schattenfarbe passen sich automatisch an Hell-/Dunkelmodus an,
 * damit der Text immer gut lesbar bleibt.
 */
@Composable
fun ComicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    shadowOffset: Dp = 4.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit
) {
    val outline = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(cornerRadius)
    Box(modifier = modifier) {
        Surface(
            color = outline,
            shape = shape,
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
        ) {}
        Surface(
            color = containerColor,
            shape = shape,
            border = BorderStroke(2.dp, outline),
            content = content
        )
    }
}

/** Dicke Kontur für Chips, Buttons und Textfelder im Comic-Look (theme-abhängig). */
@Composable
fun Modifier.comicOutline(cornerRadius: Dp = 12.dp): Modifier {
    val outline = MaterialTheme.colorScheme.outline
    return this.border(width = 2.dp, color = outline, shape = RoundedCornerShape(cornerRadius))
}

/** Gelber „Sticker"-Hintergrund für Überschriften. */
fun Modifier.comicSticker(): Modifier =
    this
        .background(Color(0xFFFFC800), RoundedCornerShape(6.dp))
        .border(2.dp, Ink, RoundedCornerShape(6.dp))
