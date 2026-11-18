package de.romerlap.einkaufsliste.ui.v2

import java.time.LocalDate

/**
 * SeasonEngine: bestimmt die Jahreszeit automatisch per Datum
 * (meteorologische Jahreszeiten) und liefert die passende
 * Cozy-Design-Palette samt Stimmungstext.
 */
enum class Season(
    val label: String,
    val emoji: String,
    val mood: String,
    val emptyHint: String,
    val primary: Long,
    val secondary: Long,
    val background: Long,
    val sky: Long,
    val surface: Long,
    val ink: Long,
    val soft: Long,
    val deco: Long,
    val deco2: Long,
    val particle: ParticleKind
) {
    SPRING(
        "Frühling",
        "🌸",
        "Blütenduft & frischer Markt",
        "Dein Frühlingskorb ist noch leer 🌸\nTippe unten auf „Hinzufügen“ für frische Ideen.",
        0xFFE87A9E, 0xFF7FA86B, 0xFFFDF1EC, 0xFFFBE9EE, 0xFFFFFFFF,
        0xFF4A3B44, 0xFF8A7480, 0xFFF5B8CE, 0xFF9CC48B, ParticleKind.PETAL
    ),
    SUMMER(
        "Sommer",
        "☀️",
        "Sonnentag & kühle Erfrischung",
        "Noch nichts für den Sommertag geplant ☀️\nTippe unten auf „Hinzufügen“.",
        0xFF2E8FA8, 0xFFF2A93B, 0xFFF4FBEF, 0xFFCDEBF2, 0xFFFFFFFF,
        0xFF26404A, 0xFF6E838B, 0xFFF5D77A, 0xFF7CC5B2, ParticleKind.SUN
    ),
    AUTUMN(
        "Herbst",
        "🍂",
        "Goldenes Laub & warme Küche",
        "Die Herbstküche wartet auf Zutaten 🍂\nTippe unten auf „Hinzufügen“.",
        0xFFC96F2F, 0xFF9C4A2F, 0xFFFBF3E6, 0xFFF3E3C8, 0xFFFFFFFF,
        0xFF4E362A, 0xFF8A6F5C, 0xFFD9915E, 0xFFB08040, ParticleKind.LEAF
    ),
    WINTER(
        "Winter",
        "❄️",
        "Schneegestöber & gemütlicher Vorrat",
        "Alles bereit für gemütliche Wintertage ❄️\nTippe unten auf „Hinzufügen“.",
        0xFF5B7FB5, 0xFFF2B950, 0xFFF2F5FA, 0xFFDDE6F2, 0xFFFFFFFF,
        0xFF374252, 0xFF71809B, 0xFFFFFFFF, 0xFFBFD0E6, ParticleKind.SNOW
    );

    /** Art der Saison-Partikel für den Motion-Layer. */
    enum class ParticleKind { PETAL, SUN, LEAF, SNOW }

    companion object {
        /** Aktuelle Jahreszeit nach meteorologischem Kalender. */
        fun current(date: LocalDate = LocalDate.now()): Season {
            val m = date.monthValue
            return when (m) {
                3, 4, 5 -> SPRING
                6, 7, 8 -> SUMMER
                9, 10, 11 -> AUTUMN
                else -> WINTER
            }
        }
    }
}

