# Einkaufsliste

Eine einfache, private Android-Einkaufsliste für ein Gerät. Einträge können auf drei Arten ergänzt werden:

- **Manuell** – Artikel und optionale Menge/Hinweis eintippen
- **Per Foto** – handschriftliche Liste oder Notiz abfotografieren; Text wird on-device per ML Kit erkannt
- **Aus Rezept** – Rezepttext einfügen; ein deutscher Zutaten-Parser erkennt Mengen und Zutaten

## Technik

- Kotlin, Jetpack Compose, **Material 3 Expressive** (`MaterialExpressiveTheme`, dynamische Farben ab Android 12)
- Room-Datenbank (lokal, kein Account, kein Netzwerkzugriff)
- ML Kit Text Recognition (bundled, komplett offline)
- Mindestens Android 8.0 (API 26), targetSdk 35

## APK herunterladen

Der GitHub-Actions-Workflow (`.github/workflows/apk.yml`) baut bei jedem Push auf `main` automatisch die Release-APK und lädt sie als Artefakt hoch:

1. Tab **Actions** öffnen
2. Den Lauf **APK Build** anklicken
3. Unten unter **Artifacts** die Datei `Einkaufsliste-APK` herunterladen
4. ZIP entpacken – darin liegt `Einkaufsliste-v1.0.apk` – und auf dem Handy installieren („Unbekannte Quellen“ erlauben)

## Bauen

Projekt in **Android Studio** öffnen (JDK 17) und `app` ausführen bzw. `Build > Build APK(s)` wählen. Alternativ:

```bash
gradle :app:assembleDebug
```

## Projektstruktur

```
app/src/main/java/de/romerlap/einkaufsliste/
├── MainActivity.kt          # Einstieg + Compose-App
├── ListViewModel.kt         # Zustand der Liste (Room-Flow)
├── RecipeParser.kt          # Zutaten-Erkennung aus Rezepttext
├── DraftItem.kt             # Entwurf für neue Einträge
├── data/                    # Room: Entity, DAO, Datenbank
└── ui/                      # Screens, Dialoge, Theme
```

## Tests

```bash
gradle :app:testDebugUnitTest
```

prüft u. a. den `RecipeParser` (Mengen mit/ohne Einheit, Überschriften, Zubereitungsschritte, Aufzählungszeichen, Duplikate).
