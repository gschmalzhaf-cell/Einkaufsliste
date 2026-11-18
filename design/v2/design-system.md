# Einkaufsliste 2 – Cozy Seasons Design-System

Der Design-entwurf für Version 2 (Neuaufbau). Die SVG-Screens unter `screens/` sind direkt in
Pixso/Figma importierbar. Sobald das Figma-MCP-Limit frei ist, wird dieser Stand zusätzlich in die
Figma-Datei „Einkaufsliste 2 – Cozy Seasons Design System" synchronisiert.

## Konzept

- **Cozy Illustrationen**: weiche runde Formen, warme Töne, Picturebook-Stimmung
- **Saisonale Engine**: Frühling / Sommer / Herbst / Winter – automatisch per Datum (meteorologisch:
  Frühling ab 1.3., Sommer ab 1.6., Herbst ab 1.9., Winter ab 1.12.)
- **Pixel-9-Pro-Spielwiese**: Kamera-Loch als saisonaler Anker (Ring um das Cutout), Edge-to-Edge mit
  abgerundeten Karten (Radius 26px), Insets-sauber
- **Motion: alles an** – Saison-Partikel (Canvas), Feder-Physik beim Abhaken, Konfetti beim Ablösen
  erledigter Einträge, Parallax beim Scrollen, weiche Spring-Übergänge

## Saison-Paletten (Light)

| Rolle | Frühling | Sommer | Herbst | Winter |
|---|---|---|---|---|
| primary | `#E87A9E` | `#2E8FA8` | `#C96F2F` | `#5B7FB5` |
| secondary | `#7FA86B` | `#F2A93B` | `#9C4A2F` | `#F2B950` |
| background | `#FDF1EC` | `#F4FBEF` | `#FBF3E6` | `#F2F5FA` |
| sky (Kopfbereich) | `#FBE9EE` | `#CDEBF2` | `#F3E3C8` | `#DDE6F2` |
| surface (Karten) | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` |
| ink (Text) | `#4A3B44` | `#26404A` | `#4E362A` | `#374252` |
| soft (Sekundärtext) | `#8A7480` | `#6E838B` | `#8A6F5C` | `#71809B` |
| deco (Partikel 1) | `#F5B8CE` | `#F5D77A` | `#D9915E` | `#FFFFFF` |
| deco2 (Partikel 2) | `#9CC48B` | `#7CC5B2` | `#B08040` | `#BFD0E6` |

## Kamera-Loch-Konzept (saisonaler Anker)

| Saison | Behandlung |
|---|---|
| Frühling | Blütenreigen rotiert langsam um das Loch (rosa Ring) |
| Sommer | Sonnenscheibe mit Strahlen wandert tagsüber um das Loch |
| Herbst | Herbstblätter wirbeln am Loch vorbei (sanfte Bahnkurven) |
| Winter | Schneeflocken sammeln sich am Loch-Rand (Blaugrau-Ring) |

## Komponenten

- **CozyCard**: Radius 26 px, weiß, weicher Schatten (Ink 10 %), Checkbox-Kreis 26 px, Tag in Saison-Primary
- **Chips**: Pille Radius 16 px; selected = Saison-Primary
- **FAB**: zentrierte Pille (Radius 28 px) in Saison-Primary mit Schatten-Offset
- **Titelzeile**: „Einkaufsliste" 30 px + Saison-Kicker (14 px, Sperr-Satz) in Saison-Soft

## Motion-Spezifikation (Umsetzung in V2)

- Saison-Partikel: Canvas-Layer hinter allem, ~20–40 Partikel, 30–60 fps, Akku-schonend (pausiert im Hintergrund)
- Abhaken: Federphysik (spring damping 0.6), Checkbox füllt sich mit Bounce
- „Erledigte entfernen": Konfetti-Burst aus Karten heraus
- Saisonwechsel (automatisch beim Überschreiten der Datumsgrenze): Crossfade 600 ms aller Farb-Tokens
