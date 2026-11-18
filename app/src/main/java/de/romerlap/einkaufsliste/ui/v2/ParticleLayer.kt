package de.romerlap.einkaufsliste.ui.v2

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Motion-Layer: sanfte Saison-Partikel (Blueten, Sonnenstaub, Laub, Schnee)
 * als Canvas hinter dem Inhalt. Pausiert automatisch, wenn die App in den
 * Hintergrund geht (withFrameNanos-Laufe endet mit der Komposition).
 */
@Composable
fun ParticleLayer(modifier: Modifier = Modifier) {
    val season = LocalSeason.current
    val kind = season.season.particle
    val rand = remember { Random(season.season.ordinal + 42) }
    val particles = remember(kind) {
        List(26) { i ->
            Particle(
                x = rand.nextFloat(),
                y = rand.nextFloat(),
                size = 4f + rand.nextFloat() * 8f,
                drift = 0.004f + rand.nextFloat() * 0.012f,
                sway = rand.nextFloat() * (2f * Math.PI.toFloat()),
                swaySpeed = 0.4f + rand.nextFloat() * 1.2f,
                rotation = rand.nextFloat() * 360f,
                rotationSpeed = 8f + rand.nextFloat() * 40f,
                alpha = 0.35f + rand.nextFloat() * 0.5f,
                colorA = if (i % 2 == 0) season.deco else season.deco2,
                depth = 0.6f + rand.nextFloat() * 0.8f
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
        for (p in particles) {
            val y = (p.y + time * p.drift * p.depth) % 1f
            val sway = sin(p.sway + time * p.swaySpeed)
            val x = p.x + (sway * 0.03f * p.depth)
            val px = x * w
            val py = y * h
            when (kind) {
                Season.ParticleKind.SNOW -> drawCircle(p.colorA, p.size / 2, Offset(px, py), p.alpha)
                Season.ParticleKind.PETAL -> {
                    rotate(p.rotation + time * p.rotationSpeed, Offset(px, py)) {
                        drawCircle(p.colorA, p.size / 2, Offset(px, py), p.alpha * 0.9f)
                        drawCircle(p.colorA.copy(alpha = p.alpha * 0.5f), p.size / 3, Offset(px + p.size, py + p.size / 2), p.alpha * 0.7f)
                    }
                }
                Season.ParticleKind.LEAF -> {
                    rotate(p.rotation + time * p.rotationSpeed, Offset(px, py)) {
                        drawCircle(p.colorA, p.size / 2.2f, Offset(px, py), p.alpha * 0.85f)
                    }
                }
                Season.ParticleKind.SUN -> {
                    // Sommer: warmer Sonnenstaub, sehr langsam und leuchtend
                    drawCircle(p.colorA, p.size / 3, Offset(px, py), p.alpha * 0.6f)
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
