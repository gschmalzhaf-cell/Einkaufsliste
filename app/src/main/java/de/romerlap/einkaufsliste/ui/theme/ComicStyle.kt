package de.romerlap.einkaufsliste.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Comic-Tuschefarbe für Konturen. */
val Ink = Color(0xFF1D1D1B)

/** Comic-Papierton für Karten. */
val ComicPaper = Color(0xFFFFFDF5)

/**
 * Karte im Retro-Comic-Stil: cremefarbene Fläche mit dicker schwarzer
 * Kontur und hartem, versetztem Schatten („Sticker-Look").
 */
@Composable
fun ComicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    shadowOffset: Dp = 4.dp,
    containerColor: Color = ComicPaper,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(modifier = modifier) {
        Surface(
            color = Ink,
            shape = shape,
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
        ) {}
        Surface(
            color = containerColor,
            shape = shape,
            border = BorderStroke(2.dp, Ink),
            content = content
        )
    }
}

/** Dicke schwarze Kontur für Chips, Buttons und Textfelder im Comic-Look. */
fun Modifier.comicOutline(cornerRadius: Dp = 12.dp): Modifier =
    this.border(width = 2.dp, color = Ink, shape = RoundedCornerShape(cornerRadius))

/** Gelber „Sticker"-Hintergrund für Überschriften. */
fun Modifier.comicSticker(): Modifier =
    this
        .background(Color(0xFFFFC800), RoundedCornerShape(6.dp))
        .border(2.dp, Ink, RoundedCornerShape(6.dp))
