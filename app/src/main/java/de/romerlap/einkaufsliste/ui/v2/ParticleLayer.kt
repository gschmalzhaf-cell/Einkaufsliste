package de.romerlap.einkaufsliste.ui.v2

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Motion-Layer: handgezeichnet wirkende Saison-Partikel (Blütenblätter,
 * leuchtender Sonnenstaub, echte Herbstblätter mit Stiel, Schneekristalle)
 * als Canvas hinter dem Inhalt. Pausiert automatisch im Hintergrund.
 */
@Composable
fun ParticleLayer(modifier: Modifier = Modifier) {
    val season = LocalSeason.current
    val kind = season.season.particle
    val rand = remember(season.season) { Random(season.season.ordinal + 42) }
    val particles = remember(kind, season.season) {
        List(28) { i ->
            Particle(
                x = rand.nextFloat(),
                y = rand.nextFloat(),
                size = 6f + rand.nextFloat() * 10f,
                drift = 0.005f + rand.nextFloat() * 0.013f,
                sway = rand.nextFloat() * (2f * Math.PI.toFloat()),
                swaySpeed = 0.5f + rand.nextFloat() * 1.1f,
                rotation = rand.nextFloat() * 360f,
                rotationSpeed = 12f + rand.nextFloat() * 38f,
                alpha = 0.35f + rand.nextFloat() * 0.45f,
                colorA = when (i % 3) {
                    0 -> season.deco
                    1 -> season.deco2
                    else -> season.primary.copy(alpha = 0.45f)
                },
                depth = 0.65f + rand.nextFloat() * 0.75f
            )
        }
    }
    var time by remember { mutableStateOf(0f) }

    androidx.compose.runtime.LaunchedEffect(kind) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) time += (now - last) / 1_000_000_000f
                last = now
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val leafPath = Path()
        for (p in particles) {
            val y = (p.y + time * p.drift * p.depth) % 1f
            val sway = sin(p.sway + time * p.swaySpeed)
            val x = p.x + (sway * 0.035f * p.depth)
            val px = x * w
            val py = y * h
            when (kind) {
                Season.ParticleKind.SNOW -> {
                    val r = p.size * 0.55f
                    drawCircle(p.colorA, r, Offset(px, py), p.alpha)
                    if (p.size > 11f) {
                        // Zarter Schneekristall-Schimmer bei größeren Flocken
                        rotate(p.rotation + time * p.rotationSpeed * 0.4f, Offset(px, py)) {
                            drawLine(
                                p.colorA,
                                Offset(px - r * 1.35f, py),
                                Offset(px + r * 1.35f, py),
                                strokeWidth = 1.6f,
                                alpha = p.alpha * 0.65f
                            )
                            drawLine(
                                p.colorA,
                                Offset(px, py - r * 1.35f),
                                Offset(px, py + r * 1.35f),
                                strokeWidth = 1.6f,
                                alpha = p.alpha * 0.65f
                            )
                        }
                    }
                }
                Season.ParticleKind.PETAL -> {
                    rotate(p.rotation + time * p.rotationSpeed, Offset(px, py)) {
                        drawOval(
                            color = p.colorA,
                            topLeft = Offset(px - p.size * 0.6f, py - p.size * 0.35f),
                            size = Size(p.size * 1.2f, p.size * 0.7f),
                            alpha = p.alpha * 0.9f
                        )
                        drawCircle(
                            color = Color.White,
                            radius = p.size * 0.16f,
                            center = Offset(px - p.size * 0.15f, py),
                            alpha = p.alpha * 0.55f
                        )
                    }
                }
                Season.ParticleKind.LEAF -> {
                    rotate(p.rotation + time * p.rotationSpeed, Offset(px, py)) {
                        val s = p.size * 0.85f
                        leafPath.reset()
                        leafPath.moveTo(px - s, py)
                        leafPath.quadraticTo(px, py - s * 0.85f, px + s, py)
                        leafPath.quadraticTo(px, py + s * 0.85f, px - s, py)
                        leafPath.close()
                        drawPath(leafPath, color = p.colorA, alpha = p.alpha * 0.85f)
                        // Kleiner Blattstiel / Mittelrippe
                        drawLine(
                            color = season.ink.copy(alpha = 0.25f),
                            start = Offset(px - s * 1.15f, py),
                            end = Offset(px + s * 0.5f, py),
                            strokeWidth = 1.4f
                        )
                    }
                }
                Season.ParticleKind.SUN -> {
                    // Sommer: warmer, pulsierender Sonnenstaub + kleine Lichtringe
                    val shimmer = 0.75f + 0.25f * sin(time * 2.2f + p.sway)
                    val r = p.size * 0.42f * shimmer
                    drawCircle(p.colorA, r * 1.7f, Offset(px, py), p.alpha * 0.22f)
                    drawCircle(p.colorA, r, Offset(px, py), p.alpha * 0.75f)
                }
            }
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val drift: Float,
    val sway: Float,
    val swaySpeed: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val alpha: Float,
    val colorA: Color,
    val depth: Float
)

