# Einkaufsliste

Eine einfache, private Android-Einkaufsliste für ein Gerät. Einträge können auf drei Arten ergänzt werden:

- **Manuell** – Artikel und optionale Menge/Hinweis eintippen
- **Per Foto** – Liste oder Notiz abfotografieren; mit Mistral-API-Schlüssel erkennt die KI Artikel mit Menge, sonst on-device per ML Kit
- **Aus Rezept** – Rezepttext oder Rezept-Link einfügen; die App liest bevorzugt die strukturierten Rezeptdaten der Seite (Schema.org/JSON-LD) mit exakten Mengenangaben
- **Tags (optional)** – Einträge lassen sich mit einem Tag versehen (z. B. „Grillabend“); über die Tag-Chips lassen sie sich priorisieren sowie ein- und ausblenden

## Technik

- Kotlin, Jetpack Compose, **Material 3 Expressive** im **Retro-Comic-Look** (eigene Farbpalette, Comic-Fonts „Bangers“ + „Baloo 2“)
- Room-Datenbank (lokal, kein Account)
- ML Kit Text Recognition (on-device) + Mistral API (optional, für Foto- und Rezept-Auswertung)
- Mindestens Android 8.0 (API 26), targetSdk 35

## APK herunterladen

Der GitHub-Actions-Workflow (`.github/workflows/apk.yml`) baut bei jedem Push auf `main` die Release-APK. Version, Tag-Name und APK-Name ergeben sich automatisch aus der App-Version (`versionName`); die Release-Notes stammen aus der obersten Sektion von `CHANGELOG.md`:

1. Tab **Releases** öffnen – dort liegt die aktuelle APK als Download
2. APK aufs Handy übertragen und installieren („Unbekannte Quellen“ erlauben)

## Release-Signierung

Die APK wird mit dem hinterlegten Release-Keystore signiert. Dafür sind folgende GitHub-Secrets nötig:

- `ANDROID_KEYSTORE_BASE64` – Base64-kodierte Keystore-Datei (`base64 -w0 release.keystore`)
- `ANDROID_KEYSTORE_PASSWORD` – Passwort des Keystores
- `ANDROID_KEY_ALIAS` – Alias des Schlüssels
- `ANDROID_KEY_PASSWORD` – Passwort des Schlüssels (falls identisch mit Store-Passwort, trotzdem setzen)

Ohne Keystore schlägt der Build bewusst fehl, damit nie versehentlich eine APK mit abweichender Signatur veröffentlicht wird.

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
├── MistralApi.kt            # KI-Auswertung (Foto + Rezepttext)
├── WebRecipeFetcher.kt      # Rezept-Webseiten: strukturierte Daten + Textextraktion
├── RecipeParser.kt          # Lokaler Zutaten-Parser (Offline-Fallback)
├── ApiKeyStore.kt           # Verschlüsselte Speicherung des API-Schlüssels
├── DraftItem.kt             # Entwurf für neue Einträge
├── data/                    # Room: Entity, DAO, Datenbank
└── ui/                      # Screens, Dialoge, Theme
```

## Tests

```bash
gradle :app:testDebugUnitTest
```

prüft u. a. den `RecipeParser` (Mengen mit/ohne Einheit, Überschriften, Zubereitungsschritte, Aufzählungszeichen, Duplikate).
