# YEM1N Android: Arbeitsregeln

Android-Version der Übungs-App für Cems Kinder (Yemin, Klasse 3, und Evîn, Vorschule).
Kotlin mit Jetpack Compose. Gegenstück zur iOS-App im Repo `cembot90/YEM1N`.
Das JSON-Format der Arbeiten ist in beiden Apps gleich und kommt aus den Claude-Projekten.

## Aufgabenteilung

Cem fasst den Kotlin-Code nicht an. Alle Codeänderungen laufen über dieses Repo.
Cem macht alles, wofür Zugriff auf seinen Mac, ein Gerät oder ein Konto nötig ist:
Android Studio, Emulator, Tests auf echten Geräten, Play Console, Firebase.

## Zweige

`main` ist der geprüfte Stand. Größere Eingriffe kommen erst auf `entwicklung`,
Cem holt sie mit `git pull`, baut sie in Android Studio und probiert sie aus.
Danach werden `entwicklung` und `main` per Fast-Forward gleichgezogen.
Die Historie wird nicht umgeschrieben. Kleine Änderungen (Texte, Farben, kleine
Korrekturen) legt Claude direkt auf `main` ab, wenn der Code sehr wahrscheinlich baut.

## Prüfen

GitHub Actions (`.github/workflows/tests.yml`) führt bei jedem Push die Unit-Tests aus
und baut die Debug-App. In Android Studio laufen die Tests mit Rechtsklick auf
`app/src/test`, Run Tests. Neue Logik bekommt Tests, reine Oberfläche nicht.

## Aufbau

- `daten/`: JSON-Format und fertige Modelle (reines Kotlin, ohne Android)
- `logik/`: Antwortprüfung, Runde, Zahlenmauer, Fortschritt (reines Kotlin, ohne Android)
- `speicher/`: Dateien auf dem Gerät (Beispielarbeiten aus `assets/pakete`, eingefügte Arbeiten, Fortschritt)
- `ui/`: Oberfläche mit Compose, Farben und Theme in `ui/theme`

Reines Kotlin bleibt getrennt von Android, damit es sich als JVM-Test ohne Emulator prüfen lässt.
Die Regeln für Antworten entsprechen der iOS-App (`Antworten.swift`, `MatheUeben.swift`):
Zahlen werden als Zahlen verglichen, bei Texten zählt Groß und Klein, "Lösung zeigen"
zählt weder als richtig noch als falsch.

## Schreibweise

Keine Gedankenstriche, weder im Code noch in Texten noch in Commit-Nachrichten.
Minus ist ein normaler Bindestrich, Mal ist `·`, Geteilt ist `:`.
Oberflächentexte und Kommentare auf Deutsch, kindgerecht und ohne Fachjargon.
Farben: Lacivert und Sarı (Navy `#00205B`, Gelb `#FFED00`), wie bei iOS.

## Cloud-Entscheidung

Keine eigene Android-Cloud (kein Firebase). iOS nutzt nur die öffentliche CloudKit-Datenbank.
Android soll über die CloudKit-Web-Services-REST-API an dieselbe Datenbank, also eine Cloud für beide Systeme.
Entschieden (21.10.2026, nach Problemen mit der Apple-Anmeldung auf Android): Android-Nutzer brauchen keine Apple-ID.
Lesen geht direkt bei CloudKit mit dem API-Token (`speicher/CloudClient.kt`, `logik/JokerCloud.kt`).
Schreiben und Mitteilungen laufen über einen kleinen Server auf Cloudflare Workers (`server/`) mit CloudKit-Server-Schlüssel.
Mitteilungen auf Android kommen über Googles FCM (nur als Postbote, keine Firebase-Datenbank).
Plan und Grenzen: `cloud/SERVER-KONZEPT.md`. Die Apple-Anmeldung in den Einstellungen bleibt vorerst als Zweitweg.
Records vom Server gehören dem Entwickler: iOS kann sie lesen und neue anlegen, aber nicht ändern.
Deshalb werden nur Joker, Antworten, Daumen, Ergebnisse und Duelle geteilt (alles nur neu anlegen). Haustier und Pakete bleiben je System getrennt.

## Stand und nächste Schritte

Meilenstein 1: Arbeiten laden, üben (alle Aufgabenarten), Fortschritt speichern, Arbeit einfügen.
Meilenstein 2a: Lob wie iOS, Münzen, Serie mit freiem Tag, Tagesziel, Fehlerheft mit Wiederholplan,
Einstellungen (Name, Tagesziel, Arbeiten löschen), Fach-Filter.
Meilenstein 2b-1: Aufgaben-Generator (Port von MatheGenerator), Probearbeit mit Uhr und Note,
Training (zwei schwächste Bereiche), Rosa-Modus, JSON-Datei öffnen.
Meilenstein 2b-2: Erfolge-Bildschirm (Serie, Tagesziel, Wochenziel mit Belohnung, 24 Abzeichen),
JSON aus anderen Apps annehmen (Teilen und Öffnen).
Meilenstein 2c: Vorschule (alle 23 Spiele plus Memory, Zahlenraum bis 5/10/20, Schulstart-Pass),
Vorlesen mit der Sprachausgabe des Geräts (Schalter in den Einstellungen).
Meilenstein 4a: Spiele-Bereich (15 Münzen pro Spiel, Tageslimit, Bestwerte, Eltern-Schalter) Einhorn-Flug und Zahlenlauf (Spiele laufen im Querformat).
Alle fünf Spiele sind da: Einhorn-Flug, Zahlenlauf, Münzjagd (Stick links, Sprungknopf rechts), Elfmeterschießen (Wischen, Torwart wird besser) und Münz-Hockey (Finger führt den Schläger, Computer wird pro Level besser).
Die Spiele haben selbst erzeugte Töne (`logik/Klang.kt`) und Vibration (`ui/SpielEffekte.kt`), beides in den Einstellungen abschaltbar. Partikel gab es schon.
Meilenstein 4b: Natürliche Stimmen (Piper über sherpa-onnx 1.13.8, Pakete aus dem iOS-Release `stimmen-1`, Laden und Probehören in den Einstellungen, danach offline). Das Projekt steht deshalb unter GPL-3.0 (`LICENSE`, `THIRD-PARTY.md`).
Vorschule kann Englisch und Türkisch anbieten (Schalter in den Einstellungen, Umschalter in der Vorschule).
Englisch für Kleine (8 Themen, anhören und spielen) ist da, sobald Englisch in den Einstellungen an ist.
Konfetti gibt es auf den Vorschul-Ergebnisseiten ab einem Stern.
Eigene Vorschul-Pakete (Arbeiten mit Fach "Vorschule" und Aufgaben der Art "wahl") erscheinen in der Vorschule unter "Eigene Pakete" und laufen wie die Spiele (`logik/VorschulPaket.kt`).
Meilenstein 5: Haustier (Ei wählen, schlüpft nach der ersten Runde, wächst in 5 Stufen, Hunger und Laune, Leckerli 5, Spielen 8, 6 Hüte, Tagesgeschenk, Meldung auf der Ergebnisseite). Alles bleibt auf dem Gerät. Erinnerung um 17 Uhr, wenn das Tier hungrig würde (Schalter in den Einstellungen, Mitteilungen ab Android 13 nach Frage). Man kann ein anderes Haustier aussuchen. Ein kleines Widget für den Startbildschirm zeigt Haustier und Serie.
Veröffentlichung: `store/VEROEFFENTLICHUNG.md` sammelt Checkliste, Data-Safety-Antworten und Store-Texte für Cem.
Meilenstein 3: Info (Anleitung per WebView mit Kurzfassung ohne Internet, Datenschutz, Impressum,
Beta und Haftung, Feedback per Mail, Versionsinfo).
Meilenstein 6 (Cloud ohne Apple-ID): Server läuft auf Cloudflare (`server/`, Adresse in `speicher/ServerClient.kt`), Schreiben mit Server-Schlüssel und Lesen mit API-Token sind bestätigt. Joker-Seite (Familiencode, Frage, Antwort, Daumen) und stilles Senden der Rundenergebnisse sind da (`ui/JokerScreen.kt`, `logik/JokerCloud.kt`, `logik/ServerAnfrage.kt`).
Noch offen: Elternansicht der Ergebnisse, Mitteilungen über FCM, Duell, Wechsel auf die Production-Umgebung (eigener API-Token, Server-Schlüssel und Schema dort).
Danach: Stimmen, CloudKit-Anbindung, Joker, Haustier, Spiele, Widget.

## Designüberarbeitung (begonnen)

Schon da: Glas-Karten als Verlauf mit feiner Kante (`ui/theme/Glas.kt`, ohne echte Unschärfe, weil das auf Kinder-Tablets zu viel Leistung braucht), Lichtflecken im Hintergrund, federnde Karten auf dem Startbildschirm, Fortschrittsring für das Tagesziel, Sparmodus (Schalter in den Einstellungen, an bei schwachen Geräten).
Noch offen: Federn und Ringe auch in den übrigen Bildschirmen.

Ursprünglicher Plan:

Moderneres Aussehen wie die iOS-App: Glas-Karten (Unschärfe und feine Kante, ab Android 12 per Blur,
darunter flache Karten), weichere Hintergründe mit Lichtflecken, federnde Knöpfe, Fortschrittsringe,
Sparmodus für schwache Geräte. Erst nach den Funktionen, damit nicht doppelt gebaut wird.
