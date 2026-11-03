# Changelog

Alle Änderungen an der Einkaufsliste-App werden hier pro Version dokumentiert.
Die GitHub-Release-Notes werden automatisch aus der obersten Version-Sektion dieser Datei erzeugt – der Changelog steht damit immer vollständig im jeweiligen Release-Tag.

## v1.6 – 2026-11-03

Neu: Zu jedem Eintrag wird jetzt eine handelsübliche Packungsgröße (Rewe, Aldi) angezeigt, mit der der Rezept-Bedarf abgedeckt ist.

### Neu
- **Packungsgrößen-Hinweis**: Unter jedem Eintrag steht die übliche Supermarkt-Packung, die die benötigte Menge abdeckt – z. B. „200 g“ Mehl → „Handelsüblich: 500 g oder 1 kg Packung“. So weiß man beim Einkauf genau, was man greifen muss
- **Rezept-Links und -texte**: Die KI liefert die passende Packungsgröße direkt für jede Zutat mit
- **Foto**: Auch hier recherchiert die KI pro Artikel die handelsübliche Packung
- **Manuell**: Beim Tippen erscheint sofort der Hinweis aus dem integrierten Katalog (rund 90 Grundzutaten mit Standard-Packungsgrößen von Rewe/Aldi); per Knopfdruck lässt sich die Packungsgröße zusätzlich per KI recherchieren
- Ohne API-Schlüssel greift die App auf den Offline-Katalog zurück

### Technisch
- Datenbank erweitert (Version 2, automatische Migration) – bestehende Einträge bleiben erhalten

## v1.5 – 2026-11-03

Behoben: Mengenangaben fehlten bei bestimmten Rezept-Links (u. a. Chefkoch und einfachbacken.de).

### Behoben
- **Chefkoch-Rezepte**: Chefkoch lädt die Zutatennamen erst per JavaScript nach – statisch geladene Seiten enthalten sie nicht. Die App liest Chefkoch-Rezepte jetzt direkt über die öffentliche Chefkoch-API aus, die alle Zutaten mit exakten Mengen liefert (z. B. „200 g Zucker“, „0,5 Pck. Backpulver“)
- **Seiten mit getrennten Mengen-Spalten** (z. B. einfachbacken.de): Wenn Menge und Artikel im Seitenlayout als eigene Elemente vorliegen, werden sie jetzt automatisch zu einer Zeile zusammengeführt („250 g“ + „weiche Butter“ → „250 g weiche Butter“)

## v1.4 – 2026-11-03

Behoben: Mengenangaben fehlten bei Rezepten, die per Link hinzugefügt wurden.

### Behoben
- **Mengen bei Rezept-Links**: Rezeptseiten werden jetzt zuerst nach strukturierten Daten (Schema.org JSON-LD) durchsucht, die praktisch alle Rezeptseiten für Suchmaschinen einbetten – dort stehen die Zutaten mit exakten Mengen (z. B. „200 g Mehl“). Erst wenn keine vorhanden sind, wird der normale Seitentext ausgewertet
- Zusätzlich wird das Microdata-Format (`itemprop="recipeIngredient"`) als weitere Quelle geprüft
- Längere Rezeptseiten werden vollständig berücksichtigt (bis 60.000 Zeichen statt 25.000)

### Verbessert
- Der Build veröffentlicht die Release-Version jetzt automatisch aus der App-Version – APK-Name, Tag und Titel stimmen immer überein

## v1.3 – 2026-11-03

Die App wird nun mit einem eigenen Release-Keystore signiert.

### Geändert
- **Echte Release-Signatur**: Die APK wird in GitHub Actions mit dem hinterlegten Keystore signiert (statt der Debug-Signatur) – Voraussetzung für saubere Updates und späteren Play-Store-Upload
- Die Signaturprüfung von Android wird damit geändert: **Einmaliges Neuinstallieren erforderlich** – die bestehende App deinstallieren und die neue APK installieren

### Hinweis
Fehlt der Keystore im Build (z. B. lokaler Build ohne Secrets), wird automatisch die Debug-Signierung verwendet.

## v1.2 – 2026-11-03

Rezepte können nun auch per Link hinzugefügt werden.

### Neu
- **Rezept per Link**: Rezept-URL (z. B. von einer Rezeptseite oder einem Food-Blog) einfach ins Rezept-Feld einfügen – die App lädt die Seite und die KI extrahiert die Zutaten mit Mengen direkt daraus
- Ohne API-Schlüssel wird der Seitentext mit dem lokalen Zutaten-Parser ausgewertet

### Hinweis
Manche Websites blockieren das automatische Laden ihrer Seiten – in dem Fall erscheint eine klare Fehlermeldung, und der Text kann stattdessen manuell kopiert und eingefügt werden.

## v1.1 – 2026-11-03

Fotos und Rezepte werden nun von einer KI (Mistral API) ausgewertet.

### Neu
- **Foto per KI**: Liste oder Notiz abfotografieren – die KI erkennt jeden Artikel mit der richtigen Menge (z. B. „Mehl – 200 g“) und trägt ihn direkt auf die Einkaufsliste
- **Rezept per KI**: Rezepttext einfügen – die KI extrahiert Zutaten und Mengen zuverlässiger als der bisherige Text-Parser
- **Einstellungen**: Über das Zahnrad-Symbol einen Mistral-API-Schlüssel hinterlegen (erhältlich auf console.mistral.ai). Der Schlüssel wird verschlüsselt ausschließlich auf dem Gerät gespeichert
- Ohne API-Schlüssel funktioniert die App wie bisher komplett offline (ML Kit Texterkennung bzw. lokaler Zutaten-Parser)

### Hinweis
Für die KI-Auswertung wird eine Internetverbindung benötigt; der Text geht zur Analyse an die Mistral-API. Alle Einkaufslisten-Daten bleiben lokal auf dem Gerät.

## v1.0 – 2026-11-03

Erste Version der Einkaufsliste-App.

### Neu
- Einträge **manuell** hinzufügen (Artikel + optionale Menge/Hinweis)
- Einträge **per Foto**: Liste oder Notiz abfotografieren, Text wird auf dem Gerät erkannt (ML Kit, komplett offline)
- Einträge **aus Rezept**: Rezepttext einfügen, Zutaten und Mengen werden automatisch erkannt (inkl. g, ml, EL, TL, Bund …)
- Einträge abhaken, einzeln löschen und alle Erledigten per Knopfdruck entfernen
- Material 3 Expressive Design mit dynamischen Farben (ab Android 12)
- Lokale Speicherung per Room-Datenbank – kein Konto, keine Netzwerkzugriffe

### Installation
`Einkaufsliste-v1.0.apk` herunterladen, aufs Handy übertragen und installieren („Unbekannte Quellen“ erlauben). Benötigt Android 8.0 (API 26) oder neuer.
