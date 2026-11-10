# Retro-Comic Design-System – Einkaufsliste

Die verbindliche Design-Quelle für alle grafischen Updates, Korrekturen und Weiterentwicklungen.
Die SVG-Dateien in diesem Ordner lassen sich direkt in Pixso (oder Figma) importieren.

## Workflow

1. **Design zuerst**: Grafische Änderungen werden zuerst hier (bzw. in Pixso/Figma) entworfen
2. **Freigabe**: Das Design wird vom Owner geprüft
3. **Umsetzung**: Freigegebene Designs werden 1:1 in Jetpack Compose umgesetzt (`ui/theme/`)
4. **Release**: Direkt als neue Version mit APK

## Farb-Tokens

| Token | Hex (Light) | Verwendung |
|---|---|---|
| `primary` | `#D62828` | Comic-Rot: App-Titel, FAB, Buttons |
| `primaryContainer` | `#FFC800` | Sonnengelb: Section-Sticker, Chips (selected) |
| `secondary` | `#1D6FD1` | Comic-Blau: Tag-Label `#Tag` |
| `tertiary` | `#FFB703` | Akzent-Gelb |
| `background` | `#FFF6E5` | Creme-Papier: App-Hintergrund |
| `surfaceContainer` | `#FFFDF5` | Karten-Hintergrund (Light) |
| `Ink` | `#1D1D1B` | Tuschefarbe: Konturen, Text (Light) |
| `onSurfaceVariant` | `#4A443A` | Sekundärtext (Light) |

### Dark Mode

| Token | Hex (Dark) | Verwendung |
|---|---|---|
| `background` | `#26221C` | Warmes Braun-Papier |
| `surfaceContainer` | `#302B24` | Karten-Hintergrund (Dark) |
| `primary` | `#FF6B6B` | Roter Akzent |
| `outline` | `#FFF3D6` | Konturen und Text (Dark) |

## Typografie

| Stil | Font | Größe | Verwendung |
|---|---|---|---|
| Display | Bangers | 26–28 px | App-Titel, Section-Sticker, Tag-Label |
| Title | Bangers | 18 px | Dialog-/Sheet-Titel |
| Body | Baloo 2 | 16 px | Artikelnamen |
| Body Small | Baloo 2 | 12 px | Mengen-/Hinweis-Text |
| Label | Baloo 2 Bold | 12–14 px | Chips, Buttons |

Beide Fonts unterstützen Umlaute (latin-ext) und liegen als Ressourcen in `app/src/main/res/font/`.

## Komponenten

### ComicCard (Sticker-Karte)
- Hintergrund: `surfaceContainer`, Radius 14 px
- Kontur: 2 px `outline` (Light: Ink, Dark: Creme)
- **Harter Schatten**: identische Form, um 4 px versetzt, in Konturfarbe
- Inhalt: Checkbox | Name + `#Tag` + Detail | Löschen

### Tag-Chip
- Höhe 32 px, Radius 16 px (Pill)
- Kontur 2 px `outline`
- Selected: `primaryContainer` (Sonnengelb), sonst `background`
- Font: Baloo 2 Bold 12 px

### Section-Sticker
- `primaryContainer`-Fläche, Radius 6 px, Kontur 2 px
- Text: Bangers 20 px, `Ink`

### FAB (Hinzufügen)
- `primary`-Rot, Radius 16 px, Kontur 2 px, Textfarbe Weiß
- Text: Baloo 2 Bold 14 px

### App-Bar
- Hintergrund `background`, untere Kontur 2 px
- Titel: „EINKAUFSLISTE“, Bangers 26 px, `primary`

## Regeln

- Artikel ohne Tag stehen immer ganz oben (Priorität 1), danach ausgewählte Tags (2), Rest (3)
- Chip-Auswahl filtert sofort; „Alle“ setzt zurück
- Konturen und Kartenfarben sind immer theme-abhängig (Hell-/Dunkelmodus)
