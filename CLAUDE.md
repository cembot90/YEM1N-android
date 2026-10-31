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

Keine Firebase-Datenbank, keine Apple-ID auf Android. Familien laufen seit dem 25.10.2026 über den eigenen Familien-Dienst (Protokoll 2, siehe "Sicherheitsumbau" unten):
ein abgetrenntes Fach je Familie bei Cloudflare in der EU, Schlüssel je Gerät im Keystore, neue Geräte nur per QR-Code mit Freigabe der Eltern (`ui/FamilieScreen.kt`, `ui/FamilienModell.kt`, `logik/Familien*.kt`).
CloudKit wird auf Android nur noch zum Lesen öffentlicher, unterschriebener Klassenpakete benutzt (`speicher/CloudClient.kt`, `logik/JokerCloud.kt`). Es gibt keine Anmeldung bei Apple mehr.
Mitteilungen auf Android kommen über Googles FCM (nur als Postbote), neutral ohne Namen und Inhalte.
Plan und Grenzen des alten Wegs: `cloud/SERVER-KONZEPT.md`. Der alte Familiencode-Weg (`/v1`, CloudKit) ist seit 27.10.2026 abgeschaltet (`V1_AUS`), `/v1/klasse` für Klassenpakete läuft weiter.

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
Meilenstein 6 (Cloud ohne Apple-ID): Server läuft auf Cloudflare (`server/`, Adresse in `speicher/ServerClient.kt`), Schreiben mit Server-Schlüssel und Lesen mit API-Token sind bestätigt. Joker-Seite (Frage, Antwort, Daumen) und stilles Senden der Rundenergebnisse sind da (`ui/JokerScreen.kt`). Der Weg über Familiencode und CloudKit ist seit 25.10.2026 durch Protokoll 2 abgelöst.
Mitteilungen laufen über FCM und den Server (Abfrage jede Minute, höchstens etwa 5 Familien). Joker haben ein Tageslimit mit Nachschub und Freigabe durch die Eltern (`logik/JokerNachschub.kt`). Ein Kind-Gerät steht nach der Freigabe durch die Eltern schon vor der ersten Runde in der Kinderliste (aus der Geräteliste des Dienstes, `logik/Anmeldung.kt`). Dazu kommen Elternansicht der Ergebnisse mit Details, Wochenbericht in Worten, Radar, Duell (über den Familien-Dienst, Typen `duell` und `duellantwort`), Lernzeit mit täglicher Erinnerung und Notizblock.
`cloud/schema.ckdb` gehört zum alten CloudKit-Weg und wird mit der iPhone-Umstellung (Phase 5) für Familiendaten gelöscht. Pakete der Klasse bleiben in CloudKit.
Einwilligung der Eltern vor der Familie (`logik/Einwilligung.kt`, Version 2), ohne sie geht nichts in die Cloud. QR-Code zum Koppeln (`yem1n://koppeln?f=...&t=...`, gilt 10 Minuten, einmal).
Meilenstein 7 (Lernwelt): Aufgaben nach dem Lehrplan für Hessen, Bayern und NRW, Klassen 1 bis 4, Fächer Mathe, Deutsch, Englisch, Musik, Kunst, dazu Türkisch und Italienisch als Extras aus dem Sprachkatalog (`app/src/main/resources/sprachkatalog.json`, Wörter anhören und erkennen). Themen stehen in `logik/lehrplan/`, Bilder in `ui/AufgabenBild.kt`, Sterne je Thema bleiben erhalten. Bundesland und Klasse stellt man in der Lernwelt ein.
Meilenstein 8 (Erklären): Erklär-Freund (nach einer falschen Antwort "Warum war das falsch?", feste Sätze aus `logik/FehlerAnalyse.kt`, keine KI), Olli die Eule beim Haustier (`logik/Eule.kt`, Einführung und Tipps), Rechenweg als Bild (`logik/ErklaerBild.kt`, `ui/ErklaerBildView.kt`). Unter Einstellungen > Bereiche lassen sich Vorschule und Sprachen ausblenden.
Belohnung zu Hause: Eltern tragen unter Einstellungen > Ich einen Text ein (`Fortschritt.familienBelohnung`, `Erfolge.familienBelohnungText`), das Kind sieht ihn auf der Wochenziel-Karte (Erfolge). Bleibt auf dem Gerät, wie bei iOS (`familienBelohnung`).
Sprachen sind ein eigener Bereich (`ui/SprachenScreen.kt`, `logik/Sprachen.kt`): Karten, Paare, Fragen aus der Lernwelt. Haustier schläft von 20 bis 7 Uhr, Sonntagserinnerung zum Wochenbericht ist da.
Klassencode ist da (Einstellungen > Familie und Cloud): Pakete der Klasse holen, Ed25519-Unterschrift prüfen (`logik/Klassensiegel.kt`, Schlüssel wird beim ersten Mal gemerkt).
KI-Satz beim Erklär-Freund mit Gemini Nano über ML Kit GenAI (`speicher/KiText.kt`, `logik/KiPrompt.kt`, Beta, nur auf unterstützten Geräten, sonst feste Sätze, Zahlen werden geprüft).
Hauptmenü wie iOS (Kopf mit Knöpfen, Lernwelt, Extra, Mehr, Arbeiten nach Klasse und Fach), Einstellungen in sechs Gruppen. Sprachen mit Karteikasten (`logik/Karteikasten.kt`), Wiederholen und Elternbericht.
Stift und Handschrift gab es zeitweise, sind seit 27.10.2026 entfernt (Entscheidung Cem: weniger Google-Bausteine für die Familienrichtlinie, keine Modell-Ladung, 16-KB-Seiten). Antworten werden nur noch getippt. Einrichtungsassistent beim ersten Start (`ui/EinrichtungScreen.kt`, `logik/Profil.kt`, letzter Schritt optional mit Klassencode samt Einwilligung; Einstellungen haben einen eigenen ⚙️-Knopf im Kopf), Einstellungen > Technik mit Verbindungstest. Mathe-Editor (Plus > Eigene Arbeit bauen: Entwurf auf dem Gerät, Vorlagen, Formular mit Rechner, Prüfen, Speichern, JSON teilen; `logik/Editor.kt`, `ui/EditorScreen.kt`) und Vokabel-Editor (Sprachen > Eigene Wörter, `logik/EigeneThemen.kt`, `ui/VokabelEditorScreen.kt`, Datei `eigene-themen.json`) sind da. Eigene Themen haben keine Fragen aus der Lernwelt und keine Alternativen, nur Karten, Paare und Tippen.
Pakete der Familie (Typ `paket`, wie iOS): Eltern senden Arbeiten unter Einstellungen > Familie und Cloud, Kind-Geräte holen sie beim Start und über ＋ > Neue Aufgaben holen (`logik/FamilienPakete.kt`).
Klassencode als Admin ist da (`logik/KlassenAdmin.kt`, Server `server/src/klasse.js`, Route `/v1/klasse`: Schlüssel anlegen, Pakete unterschreiben, senden, löschen; der Server prüft die Unterschrift). Nach Änderungen im `server/` muss Cem mit `npx wrangler deploy` neu veröffentlichen.
Sprachen: Tippen der Antworten (mit Sonderzeichen) und Auswahl der Lernsprachen sind da. Ergebnisse werden seitenweise geholt (`JokerCloud.alleSeiten`). Production-Schlüssel sind umgestellt (Development gelöscht).
Datenschutztext (`daten/InfoTexte.kt`) und `store/VEROEFFENTLICHUNG.md` beschreiben die Cloud mit Einwilligung. R8 ist im Release an (`app/proguard-rules.pro`), Cem testet vor dem Store-Upload ein signiertes Release.
Aufgaben beim Üben vorlesen (Lautsprecher-Knopf, Schalter "von selbst vorlesen", `logik/VorleseText.kt`). Der Server löscht Cloud-Einträge nach 365 Tagen (`server/src/loeschfrist.js`, täglich 03:00 UTC). "Animationen entfernen" in Android schaltet den Sparmodus ein. Abgleich mit iOS und offene Lücken: `docs/ABGLEICH-IOS.md`. Die iOS-Datenschutzseite hat einen Android-Abschnitt (3a).
Dokumentation pflegen: bei jeder Funktionsänderung `CLAUDE.md`, `docs/ARCHITEKTUR.md`, `docs/ELTERNANLEITUNG.md`, `InfoTexte.kt`, `store/VEROEFFENTLICHUNG.md` und die Testliste im Claude-Projekt mitziehen.

## Sicherheitsumbau (Protokoll 2)

Entscheidung 25.10.2026 (Cem): ein Cloudflare-Fach je Familie in der EU, Schlüssel je Gerät, neue Geräte nur per QR-Code mit Freigabe durch ein Eltern-Gerät (auch Kinder). Grund: Der Familiencode war Passwort und Suchschlüssel zugleich in einer öffentlich lesbaren Datenbank.
Stand 25.10.2026: Worker fertig und getestet (`server/src/v2/`, `server/test/v2*.test.js`, Beschreibung `cloud/PROTOKOLL-V2.md`, Prüfwerte `cloud/testvektoren-v2.json`). Android-Client fertig (Keystore, QR-Koppeln, Freigabe, Geräteliste, Eltern-PIN, Familie verlassen und löschen, neutrale Mitteilungen), bisher nur auf `entwicklung`, noch nicht auf einem Gerät getestet.
Cem muss `npx wrangler@latest deploy` ausführen und später die APNs-Geheimnisse setzen (`server/EINRICHTEN.md`, Abschnitt 3), danach die EU-Jurisdiktion mit `server/scripts/lokal-pruefen.mjs --eu` prüfen.
Stand 26.10.2026: Der iPhone-Client (Secure Enclave, gleiche Testvektoren) ist im Repo `cembot90/yem1n` auf `entwicklung` fertig, Tests auf GitHub grün, aber noch nicht auf einem Gerät ausprobiert. Als Nächstes: Cem testet beide Apps auf echten Geräten, dann `/v1` für Familien abschalten (`cloud/ABSCHALTEN-V1.md`) und CloudKit-Familiendaten löschen. Bis dahin darf keine der beiden Apps öffentlich erscheinen (Play-Test und TestFlight mit geschlossenem Kreis sind ok).
Beim Ändern von `server/src/v2/typen.js` die Tabelle in `cloud/PROTOKOLL-V2.md` mitziehen.
Phase 5 ist erledigt (27./28.10.2026): `V1_AUS = "1"` ist in `server/wrangler.toml` gesetzt und veröffentlicht, die alte KV-Ablage ist geleert, die alten Familien-Einträge im CloudKit-Dashboard sind gelöscht. Geräte mit alter App-Version senden nichts mehr in die Cloud, bis sie aktualisiert und neu per QR gekoppelt sind. Der Schalter lässt sich ohne Codeänderung wieder auskommentieren. Ablauf und Aufräumen (KV-Skript `server/scripts/alte-ablage-leeren.mjs`, CloudKit-Dashboard) stehen in `cloud/ABSCHALTEN-V1.md`.

## Designüberarbeitung (begonnen)

Schon da: Glas-Karten als Verlauf mit feiner Kante (`ui/theme/Glas.kt`, ohne echte Unschärfe, weil das auf Kinder-Tablets zu viel Leistung braucht), Lichtflecken im Hintergrund, federnde Karten auf dem Startbildschirm, Fortschrittsring für das Tagesziel, Sparmodus (Schalter in den Einstellungen, an bei schwachen Geräten).
Federn und Ringe sind in Start, Sprachen und Lernwelt da. Auch Haustier, Erfolge und Einstellungen haben jetzt Glas und Auswahlkarten (`Modifier.auswahl`). Auch Vorschule und Spiele federn jetzt (`Modifier.federKarte`). Die Spiele selbst (Zeichenflächen) behalten ihr eigenes Aussehen. Die Designüberarbeitung ist damit abgeschlossen.

Ursprünglicher Plan:

Moderneres Aussehen wie die iOS-App: Glas-Karten (Unschärfe und feine Kante, ab Android 12 per Blur,
darunter flache Karten), weichere Hintergründe mit Lichtflecken, federnde Knöpfe, Fortschrittsringe,
Sparmodus für schwache Geräte. Erst nach den Funktionen, damit nicht doppelt gebaut wird.
