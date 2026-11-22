# Gideon's Einkaufsliste

Eine private, intelligente Android-Einkaufsliste für ein Gerät im **„Cozy Seasons“**-Design – komplett ohne Account oder Cloud-Zwang, lokal in Room gespeichert und optional mit **Mistral-KI** für Foto-, Rezept- und Menü-Erkennung.

---

## Highlights & Funktionen

### Intelligente Einkaufs-Helfer (Neu in v2.4)
- **Supermarkt-Gang-Sortierung (`AisleClassifier`)**: Ordnet Artikel offline automatisch 11 deutschen Supermarkt-Abteilungen zu (🥬 Obst & Gemüse, 🥖 Backwaren, 🧀 Kühlregal, 🥩 Fleisch & Fisch, 🍝 Vorrat, 🧂 Backen & Gewürze, 🧊 Tiefkühl, 🥤 Getränke, 🍫 Snacks, 🧼 Haushalt, 📦 Sonstiges) – per Toggle-Icon in der Kopfzeile umschaltbar.
- **Automatische Duplikat- & Mengen-Zusammenführung (`QuantityMerger`)**: Fügst du z. B. `200 g Mehl` hinzu, obwohl `300 g Mehl` schon offen auf der Liste steht, addiert die App kompatible Einheiten automatisch zu `500 g` (inkl. `g ↔ kg`, `ml ↔ l`, Stückzahlen) und reaktiviert erledigte Artikel bei Bedarf.
- **Smart-Schnelleingabe (`SmartInputParser`) & Autocomplete**: Tippe einfach `2 kg Kartoffeln #Sonntag` oder `3x Paprika` – Menge, Artikelname, `#Tag`, Abteilung und handelsübliche Packungsgröße werden sofort erkannt. Beim Tippen erscheinen passende Artikelvorschläge aus dem deutschen Supermarkt-Katalog (`RetailCatalog`).
- **KI- & Saison-Assistent (`AiAssistSheet`)**:
  - **Mit Mistral-API-Schlüssel**: Nenne ein Gericht oder einen Anlass (z. B. *„Lasagne für 4 Personen“* oder *„Grillabend“*) – die KI erstellt eine passende Liste inkl. Mengen, Packungsgrößen und Tag-Vorschlag und berücksichtigt dabei Artikel, die schon auf deiner Liste stehen.
  - **Ohne Schlüssel (Offline)**: Bietet sofort nutzbare saisonale Ideen und Wochen-Basics.

### Vier Wege, Einträge zu ergänzen
1. **Manuell (Smart-Eingabe)** – mit Live-Vorschlägen, Mengen-Parsing, Packungsgrößen-Hinweis und optionaler KI-Recherche
2. **KI- / Saison-Assistent** – komplette Einkaufslisten aus einem Gericht oder Anlass generieren
3. **Per Foto** – Zettel oder Produkte abfotografieren (Mistral Vision erkennt Artikel + Menge + Packungsgröße; offline per ML Kit Text Recognition)
4. **Aus Rezept oder Link** – Rezepttext oder URL einfügen; liest bevorzugt strukturierte Schema.org/JSON-LD-Rezeptdaten mit exakten Mengen

### Komfort & Cozy Seasons Design
- **Einträge nachträglich bearbeiten**: Tipp direkt auf eine Karte oder das Stift-Symbol öffnet den Bearbeiten-Dialog für Name, Menge, Tag und Packungsgröße.
- **Cozy Seasons UI**: Automatische Jahreszeiten-Paletten (Frühling, Sommer, Herbst, Winter) mit Himmels-Verlauf, saisonalem Kamera-Loch-Anker, handgezeichneten Partikeln, Feder-Physik beim Abhaken, Konfetti-Burst beim Abschließen und tippbarer Saison-Pille zum Live-Wechseln (550-ms-Crossfade).
- **Tags & Sofort-Filter**: Einträge mit `#Tag` versehen, über runde Chips filtern und priorisieren („Ohne Tag“ immer griffbereit).
- **Mistral-API-Schlüssel dauerhaft & sichtbar**: Verschlüsselte Speicherung mit automatischem Fallback, **„✨ KI aktiv“**-Badge in der Kopfzeile und Status-Vorschau in den Einstellungen.
- **Diagnose-Bericht**: Integriertes Ereignis- & Absturzprotokoll, als Markdown über *Einstellungen → Diagnose-Bericht* kopier- oder teilbar.

---

## APK herunterladen & installieren

Bei jedem Push auf `main` baut der GitHub-Actions-Workflow (`.github/workflows/apk.yml`) automatisch die signierte Release-APK und veröffentlicht sie mit den aktuellen Release-Notes aus `CHANGELOG.md`:

1. Oben den Tab **Releases** öffnen
2. Die aktuelle `Einkaufsliste-v*.apk` herunterladen
3. Auf dem Android-Gerät installieren (ggf. „Aus dieser Quelle zulassen“ aktivieren)

---

## Technik & Architektur

- **UI**: Kotlin, Jetpack Compose, Material 3, Custom Canvas Motion-Layer (`ui/v2`)
- **Daten**: Room (lokal, SQLite, Schema-Version 3 mit automatischen Migrationen, Hintergrund-Vorwärmung beim Start)
- **KI & Erkennung**: Mistral API (`mistral-small-latest` inkl. Vision), Google ML Kit Text Recognition (On-Device-Fallback), `WebRecipeFetcher` (JSON-LD + HTML-Extraktion), lokaler `RecipeParser`
- **Plattform**: Android 8.0+ (`minSdk 26`), `compileSdk 36`, `targetSdk 35`, inkl. Android-17-Stabilitäts-Fixes (kompositionsfreier Ladebildschirm, kein `Surface`-Root, System-Schrift-Fallback ab API 37)
- **Design-Quelle**: [`design/v2/design-system.md`](design/v2/design-system.md) mit importierbaren SVG-Screens unter `design/v2/screens/`

### Projektstruktur

```text
app/src/main/java/de/romerlap/einkaufsliste/
├── MainActivity.kt          # Einstieg, saisonaler Ladebildschirm, Compose-Start & Watchdog
├── EinkaufslisteApp.kt      # Application-Klasse mit Hintergrund-DB-Vorwärmung
├── ListViewModel.kt         # Listen-Zustand (Room-Flow) + intelligente Duplikat-Zusammenführung
├── SmartIntelligence.kt     # AisleClassifier (Supermarkt-Gänge), SmartInputParser, QuantityMerger
├── MistralApi.kt            # Mistral-KI: Foto-Vision, Rezept-Analyse, Packungsgrößen & KI-Assistent
├── RetailCatalog.kt         # Offline-Katalog handelsüblicher Packungsgrößen + Autocomplete
├── WebRecipeFetcher.kt      # Rezept-Webseiten: Schema.org/JSON-LD + Textextraktion
├── RecipeParser.kt          # Lokaler Zutaten-Parser (Offline-Fallback)
├── ApiKeyStore.kt           # Verschlüsselte API-Key-Speicherung mit Selbstreparatur & Fallback
├── ReportLog.kt             # Persistentes Diagnose- & Absturzprotokoll
├── DraftItem.kt             # Entwurf für neue / importierte Einträge
├── data/                    # Room: ShoppingItem, ItemDao, AppDatabase
└── ui/
    ├── v2/                  # Cozy Seasons: HomeScreen, Season, SeasonTheme, ParticleLayer
    ├── AiAssistSheet.kt     # KI- & Saison-Assistent
    ├── ManualAddDialog.kt   # Hinzufügen & Bearbeiten mit Smart-Parsing + Autocomplete
    ├── PhotoResultSheet.kt  # Foto-Auswertung (Mistral Vision / ML Kit)
    ├── RecipeSheet.kt       # Rezepttext- & Link-Import
    ├── SettingsSheet.kt     # Einstellungen mit API-Key-Status
    ├── ApiKeyDialog.kt      # Schlüssel-Verwaltung mit Vorschau & Sichtbarkeits-Toggle
    └── ReportDialog.kt      # Diagnose-Bericht anzeigen, kopieren, teilen
```

---

## Lokal bauen & testen

Projekt in **Android Studio** (JDK 17) öffnen oder per CLI bauen:

```bash
# Debug-APK bauen
gradle :app:assembleDebug

# Unit-Tests ausführen (RecipeParser + SmartIntelligence)
gradle :app:testDebugUnitTest
```

### Release-Signierung (GitHub Secrets)

Die Release-APK wird in GitHub Actions mit dem hinterlegten Release-Keystore signiert:

- `ANDROID_KEYSTORE_BASE64` – Base64-kodierte Keystore-Datei (`base64 -w0 release.keystore`)
- `ANDROID_KEYSTORE_PASSWORD` – Passwort des Keystores
- `ANDROID_KEY_ALIAS` – Alias des Schlüssels
- `ANDROID_KEY_PASSWORD` – Passwort des Schlüssels

Ohne gültigen Keystore bricht der Workflow bewusst ab, damit nie eine APK mit abweichender Signatur veröffentlicht wird.

---

## Ideen & Roadmap – Vorschläge für nächste Features & Optimierungen

Die folgenden Ideen bauen direkt auf dem aktuellen Stand (**v2.4.0**) auf und sind nach Alltagsnutzen priorisiert:

### Hoher Alltagsnutzen (Quick Wins)
- [ ] **Swipe-Gesten auf Karten**: Nach rechts wischen = abhaken, nach links wischen = löschen (mit **„Rückgängig“-Snackbar**, falls man sich vertippt hat).
- [ ] **Verlauf & Häufige Artikel („Oft gekauft“)**: Die App merkt sich lokal, welche Artikel regelmäßig gekauft werden (z. B. Milch, Brot, Eier) und zeigt sie als 1-Tipp-Chips zum schnellen Nachlegen an.
- [ ] **Liste teilen / exportieren**: Die aktuelle Einkaufsliste formatiert nach Supermarkt-Abteilungen als WhatsApp-/Signal-Nachricht oder Markdown teilen.
- [ ] **Portions-Umrechner beim Rezept-Import**: Im `RecipeSheet` direkt per `− / +` die Personenzahl skalieren (z. B. Rezept für 2 → auf 4 Personen verdoppeln), bevor die Zutaten übernommen werden.

### Noch mehr Intelligenz
- [ ] **Spracheingabe (Mikrofon-Button)**: Per Android-Spracherkennung frei diktieren (*„Zwei Liter Milch, ein Kilo Äpfel und Butter“*) – `SmartInputParser` oder Mistral trennt die Artikel automatisch in einzelne Karten.
- [ ] **Eigene Supermarkt-Reihenfolge (Lernmodus)**: Die Gang-Reihenfolge (`Aisle.sortOrder`) pro Lieblingsmarkt (z. B. *Mein Rewe*, *Mein Aldi*) per Drag-and-Drop anpassen – oder die App lernt die Reihenfolge aus dem Abhak-Verhalten.
- [ ] **Barcode-Scanner (ML Kit Barcode + OpenFoodFacts)**: Leere Packung in der Küche scannen → Produktname, Menge und Abteilung werden automatisch erkannt.
- [ ] **Kühlschrank-/Vorrats-Check per KI**: Ein Foto vom offenen Kühlschrank machen → Mistral schlägt fehlende Zutaten für ein passendes Abendessen vor.
- [ ] **Intelligente Preis- & Budget-Schätzung**: Optionale grobe Kostenschätzung für den aktuellen Einkaufswagen (offline Richtwerte oder KI-Schätzung).

### UX, System & Technik-Optimierungen
- [ ] **Home-Screen-Widget (Jetpack Glance)**: Einkaufsliste direkt auf dem Android-Startbildschirm abhaken und per `+` schnell Artikel ergänzen.
- [ ] **Dark-Mode-Feinschliff & manuelle Saison-Sperre**: Gewählte Wunsch-Jahreszeit in den Einstellungen dauerhaft speichern (statt nur temporär per Tipp auf die Saison-Pille).
- [ ] **Veraltete V1-Überreste aufräumen**: Nicht mehr genutzte V1-Komponenten (`ui/ListScreen.kt`, `ui/theme/ComicStyle.kt`) entfernen, sobald sie nicht mehr als Referenz benötigt werden – das hält die APK noch schlanker.
- [ ] **Automatisierte CI-Tests**: Im GitHub-Actions-Workflow vor `assembleRelease` zusätzlich `gradle :app:testDebugUnitTest` ausführen, damit jeder Release-Build automatisch durch alle Unit-Tests abgesichert ist.
- [ ] **GitHub Actions auf aktuelle Versionen heben**: `actions/setup-java@v5` und aktuelle Node-24-kompatible Actions verwenden, um Deprecation-Warnungen im Build-Log zu beseitigen.
