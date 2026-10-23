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
Mitteilungen laufen über FCM und den Server (Abfrage jede Minute, höchstens etwa 5 Familien). Joker haben ein Tageslimit mit Nachschub und Freigabe durch die Eltern (`logik/JokerNachschub.kt`). Dazu kommen Elternansicht der Ergebnisse mit Details, Wochenbericht in Worten, Radar, Duell (Records `Duell` und `DuellAntwort`, gleiche wie iOS), Lernzeit mit täglicher Erinnerung und Notizblock.
Die CloudKit-Typen `JokerNachschub` und `JokerFreigabe` legt iOS selbst an (`anzahl` ist eine Zahl). `cloud/schema.ckdb` ist das ganze Schema, nur nötig, wenn Production fehlt.
Einwilligung der Eltern vor dem Familiencode (`logik/Einwilligung.kt`), ohne sie geht nichts in die Cloud. QR-Code für den Familiencode (zeigen und scannen, Format `yem1n://familie/CODE`).
Meilenstein 7 (Lernwelt): Aufgaben nach dem Lehrplan für Hessen, Bayern und NRW, Klassen 1 bis 4, Fächer Mathe, Deutsch, Englisch, Musik, Kunst, dazu Türkisch und Italienisch als Extras aus dem Sprachkatalog (`app/src/main/resources/sprachkatalog.json`, Wörter anhören und erkennen). Themen stehen in `logik/lehrplan/`, Bilder in `ui/AufgabenBild.kt`, Sterne je Thema bleiben erhalten. Bundesland und Klasse stellt man in der Lernwelt ein.
Meilenstein 8 (Erklären): Erklär-Freund (nach einer falschen Antwort "Warum war das falsch?", feste Sätze aus `logik/FehlerAnalyse.kt`, keine KI), Olli die Eule beim Haustier (`logik/Eule.kt`, Einführung und Tipps), Rechenweg als Bild (`logik/ErklaerBild.kt`, `ui/ErklaerBildView.kt`). Unter Einstellungen > Bereiche lassen sich Vorschule und Sprachen ausblenden.
Sprachen sind ein eigener Bereich (`ui/SprachenScreen.kt`, `logik/Sprachen.kt`): Karten, Paare, Fragen aus der Lernwelt. Haustier schläft von 20 bis 7 Uhr, Sonntagserinnerung zum Wochenbericht ist da.
Klassencode ist da (Einstellungen > Familie und Cloud): Pakete der Klasse holen, Ed25519-Unterschrift prüfen (`logik/Klassensiegel.kt`, Schlüssel wird beim ersten Mal gemerkt).
KI-Satz beim Erklär-Freund mit Gemini Nano über ML Kit GenAI (`speicher/KiText.kt`, `logik/KiPrompt.kt`, Beta, nur auf unterstützten Geräten, sonst feste Sätze, Zahlen werden geprüft).
Hauptmenü wie iOS (Kopf mit Knöpfen, Lernwelt, Extra, Mehr, Arbeiten nach Klasse und Fach), Einstellungen in sechs Gruppen. Sprachen mit Karteikasten (`logik/Karteikasten.kt`), Wiederholen und Elternbericht.
Stift und Handschrift sind da (`speicher/StiftLeser.kt`, `ui/StiftFeld.kt`, `logik/Handschrift.kt`, ML Kit Digital Ink, Modell lädt einmal nach Wunsch). Einrichtungsassistent beim ersten Start (`ui/EinrichtungScreen.kt`, `logik/Profil.kt`), Einstellungen > Technik mit Verbindungstest. Mathe-Editor (Plus > Eigene Arbeit bauen: Entwurf auf dem Gerät, Vorlagen, Formular mit Rechner, Prüfen, Speichern, JSON teilen; `logik/Editor.kt`, `ui/EditorScreen.kt`) und Vokabel-Editor (Sprachen > Eigene Wörter, `logik/EigeneThemen.kt`, `ui/VokabelEditorScreen.kt`, Datei `eigene-themen.json`) sind da. Eigene Themen haben keine Fragen aus der Lernwelt und keine Alternativen, nur Karten, Paare und Tippen.
Klassencode als Admin ist da (`logik/KlassenAdmin.kt`, Server `server/src/klasse.js`, Route `/v1/klasse`: Schlüssel anlegen, Pakete unterschreiben, senden, löschen; der Server prüft die Unterschrift). Nach Änderungen im `server/` muss Cem mit `npx wrangler deploy` neu veröffentlichen.
Sprachen: Tippen der Antworten (mit Sonderzeichen) und Auswahl der Lernsprachen sind da. Ergebnisse werden seitenweise geholt (`JokerCloud.alleSeiten`). Production-Schlüssel sind umgestellt (Development gelöscht).
Datenschutztext (`daten/InfoTexte.kt`) und `store/VEROEFFENTLICHUNG.md` beschreiben die Cloud mit Einwilligung. R8 ist im Release an (`app/proguard-rules.pro`), Cem testet vor dem Store-Upload ein signiertes Release.
Aufgaben beim Üben vorlesen (Lautsprecher-Knopf, Schalter "von selbst vorlesen", `logik/VorleseText.kt`). Der Server löscht Cloud-Einträge nach 365 Tagen (`server/src/loeschfrist.js`, täglich 03:00 UTC). "Animationen entfernen" in Android schaltet den Sparmodus ein. Abgleich mit iOS und offene Lücken: `docs/ABGLEICH-IOS.md`. Die iOS-Datenschutzseite hat einen Android-Abschnitt (3a).
Dokumentation pflegen: bei jeder Funktionsänderung `CLAUDE.md`, `docs/ARCHITEKTUR.md`, `docs/ELTERNANLEITUNG.md`, `InfoTexte.kt`, `store/VEROEFFENTLICHUNG.md` und die Testliste im Claude-Projekt mitziehen.

## Designüberarbeitung (begonnen)

Schon da: Glas-Karten als Verlauf mit feiner Kante (`ui/theme/Glas.kt`, ohne echte Unschärfe, weil das auf Kinder-Tablets zu viel Leistung braucht), Lichtflecken im Hintergrund, federnde Karten auf dem Startbildschirm, Fortschrittsring für das Tagesziel, Sparmodus (Schalter in den Einstellungen, an bei schwachen Geräten).
Federn und Ringe sind in Start, Sprachen und Lernwelt da. Auch Haustier, Erfolge und Einstellungen haben jetzt Glas und Auswahlkarten (`Modifier.auswahl`). Auch Vorschule und Spiele federn jetzt (`Modifier.federKarte`). Die Spiele selbst (Zeichenflächen) behalten ihr eigenes Aussehen. Die Designüberarbeitung ist damit abgeschlossen.

Ursprünglicher Plan:

Moderneres Aussehen wie die iOS-App: Glas-Karten (Unschärfe und feine Kante, ab Android 12 per Blur,
darunter flache Karten), weichere Hintergründe mit Lichtflecken, federnde Knöpfe, Fortschrittsringe,
Sparmodus für schwache Geräte. Erst nach den Funktionen, damit nicht doppelt gebaut wird.
