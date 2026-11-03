# Changelog

Alle Änderungen an der Einkaufsliste-App werden hier pro Version dokumentiert.
Die GitHub-Release-Notes werden automatisch aus der obersten Version-Sektion dieser Datei erzeugt – der Changelog steht damit immer vollständig im jeweiligen Release-Tag.

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
