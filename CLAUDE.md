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
Android soll später über die CloudKit-Web-Services-REST-API an dieselbe Datenbank, also eine Cloud für beide Systeme.

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
Offen bei den Spielen: Elfmeterschießen, Münzjagd, Münz-Hockey.
Offen in Vorschule: Englisch und Türkisch für Kleine, eigene Vorschul-Pakete, Konfetti, bessere Stimmen.
Meilenstein 3: Info (Anleitung per WebView mit Kurzfassung ohne Internet, Datenschutz, Impressum,
Beta und Haftung, Feedback per Mail, Versionsinfo).
Danach: Stimmen, CloudKit-Anbindung, Joker, Haustier, Spiele, Widget.

## Geplant nach allen Meilensteinen: Designüberarbeitung

Moderneres Aussehen wie die iOS-App: Glas-Karten (Unschärfe und feine Kante, ab Android 12 per Blur,
darunter flache Karten), weichere Hintergründe mit Lichtflecken, federnde Knöpfe, Fortschrittsringe,
Sparmodus für schwache Geräte. Erst nach den Funktionen, damit nicht doppelt gebaut wird.
