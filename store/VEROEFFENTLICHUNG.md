# YEM1N Android: Veröffentlichung im Play Store

Diese Datei sammelt alles, was Cem in der Play Console braucht. Die Texte sind Entwürfe zum Kopieren.
Stand: Entwicklung, noch nichts auf einem Gerät getestet.

## 1. Vorher (Cem)

1. Play-Konto anlegen (einmalig 25 $). Das Konto früh anlegen, weil vor der ersten Veröffentlichung ein
   geschlossener Test mit Testern laufen muss. Die genaue Mindestzahl und Dauer in der Play Console nachlesen,
   sie ändern sich.
2. Upload-Schlüssel erzeugen (Android Studio: Build, Generate Signed App Bundle, neuer Keystore). Den Keystore
   und das Passwort sichern. Ohne sie lässt sich die App nicht mehr aktualisieren.
3. App-Bundle bauen (`.aab`), Play App Signing aktivieren (Standard).
4. Auf echten Geräten prüfen: Tablet und Handy, Hochformat und Querformat (Spiele), Sprachausgabe, natürliche
   Stimmen laden, Erinnerung, Widget, Töne, Vibration.

## 2. Zielgruppe und Familienrichtlinien

- Die App richtet sich an Kinder. In der Play Console "Zielgruppe: Kinder" wählen und die Richtlinie für
  Familien beachten (keine Werbung, keine Tracker, keine Konten, keine Weitergabe von Daten).
- Altersgruppe: 5 bis 8 Jahre (Vorschule bis Klasse 3). Eltern sind die Käufer und Verwalter (Eltern-Schalter
  in den Einstellungen).
- Inhaltseinstufung (IARC-Fragebogen): keine Gewalt, keine Werbung, keine Käufe, keine Nutzerinhalte,
  kein Standort, kein Chat.

## 3. Datenschutz und Data Safety

Stand 22.10.2026. Üben, Fortschritt und Haustier bleiben auf dem Gerät. Mit Einwilligung der Eltern und einem
Familiencode nutzt die App eine Cloud (Apple CloudKit über einen Vermittler bei Cloudflare, Mitteilungen über Google FCM).
Die Texte in der App stehen in `daten/InfoTexte.kt` (Datenschutz). Sie müssen zu den Angaben hier und auf
`https://cembot90.github.io/YEM1N/` passen. Bitte vor der Veröffentlichung selbst prüfen (kein Rechtsrat).

Data-Safety-Antworten für Google Play:

| Frage | Antwort |
| --- | --- |
| Erhebt oder teilt die App Nutzerdaten? | Ja, nur wenn Eltern einen Familiencode eintragen und einwilligen |
| Welche Daten? | Nutzer-IDs (Familiencode und Push-Kennung), App-Aktivität (Rundenergebnisse), Nachrichten in der App (Joker-Fragen und Antworten), Name des Kindes (freiwillig) |
| Zweck | App-Funktionen (Joker, Ergebnisse für Eltern, Duell, Mitteilungen) |
| Teilen mit Dritten? | Verarbeiter: Apple (CloudKit), Cloudflare (Vermittler), Google (FCM, bekommt die Push-Kennung und den Text der Mitteilung mit Name des Kindes und Anfang einer Frage). Keine Weitergabe zu Werbung oder Analyse |
| Daten verschlüsselt übertragen? | Ja (nur HTTPS) |
| Können Nutzer die Löschung verlangen? | Ja, per E-Mail an cembot@icloud.com mit dem Familiencode. Daten auf dem Gerät löscht die Deinstallation |
| Ist die Datenerhebung freiwillig? | Ja, ohne Familiencode wird nichts gesendet |
| Konto nötig? | Nein |
| Werbung? | Nein |
| In-App-Käufe? | Nein (Münzen sind nur Spielgeld) |

Wichtig für die Löschung: Datensätze, die der Server schreibt, gehören dem Entwickler. Nutzer können sie nicht selbst
löschen. Der Server löscht sie nach 365 Tagen selbst (täglich um 03:00 UTC), vorher gilt "Löschung auf Anfrage" aus dem Datenschutztext. Cem löscht dann im
CloudKit-Dashboard (Production > Records) alle Einträge mit diesem Familiencode.

Wofür die App noch ins Internet geht:

- Bedienungsanleitung von GitHub Pages (`cembot90.github.io`), mit Offline-Kurzfassung.
- Download der natürlichen Stimmen aus dem GitHub-Release `stimmen-1`, nur wenn Eltern es auslösen. Größe und Prüfsumme werden geprüft.
- Klassencode: Pakete der Klasse aus CloudKit lesen. Nur der Klassencode geht über das Netz.
- Gemini Nano (KI auf dem Gerät) lädt sein Modell einmal von Google. Danach läuft die KI ohne Internet.
- Die Handschrift-Erkennung (ML Kit Digital Ink, freiwillig) lädt ihr Modell (etwa 20 MB) einmal von Google. Die Schrift wird auf dem Gerät gelesen, nicht gespeichert und nicht verschickt. In Data Safety nichts zusätzlich angeben, weil keine Nutzerdaten das Gerät verlassen; nur die Modell-Anfrage geht an Google.

Berechtigungen:

- `INTERNET`: für die Punkte oben.
- `POST_NOTIFICATIONS`: für Erinnerungen (Haustier, Lernzeit, Wochenbericht) und Mitteilungen, wird erst nach Eltern-Schalter und Frage gefordert.
- Kamera: nur zum Scannen des QR-Codes für den Familiencode, wenn Eltern es auslösen.

Datenschutzerklärung (URL für den Store-Eintrag): `https://cembot90.github.io/YEM1N/` (gleiche Seite wie iOS,
muss die Cloud-Nutzung auf Android beschreiben, siehe Punkt 6).

## 4. Store-Eintrag (Entwurf)

Name: YEM1N

Kurzbeschreibung (80 Zeichen):
Üben für Klasse 3 und Vorschule: Mathe, Deutsch, Spiele und ein Haustier.

Langbeschreibung:

YEM1N ist die Übungs-App für Kinder von der Vorschule bis zur dritten Klasse.

Lernen
- Aufgaben aus der Schule üben: Mathe mit Zahlenmauern, Einmaleins, Geteilt, Rest und Knobeln, Deutsch mit Lückenwörtern.
- Fehlerheft mit Wiederholplan, Training für die schwächsten Bereiche, Probearbeit mit Uhr und Note.
- Vorschule mit über 20 Spielen: Zahlen, Mengen, Laute, Reime, Formen, Gefühle, Verkehr und Memory.
- Englisch für Kleine: Wörter anhören und spielen (Eltern schalten es ein).

Motivieren
- Münzen, Serie mit freiem Tag, Tagesziel, Wochenbelohnung und Abzeichen.
- Fünf Spiele zum Freispielen: Einhorn-Flug, Zahlenlauf, Münzjagd, Elfmeterschießen und Münz-Hockey.
- Ein Haustier, das mit jeder Übung wächst. Mit Hüten, Leckerli und Widget für den Startbildschirm.

Für Eltern
- Keine Werbung, keine Konten, keine Datenweitergabe. Alles bleibt auf dem Gerät.
- Spiele lassen sich ausschalten oder begrenzen. Vorlesen mit natürlichen Stimmen, auch ohne Internet.
- Eigene Arbeiten aus Claude oder anderen Quellen einfügen (JSON), der Anleitung folgen.

Stichworte: Lernen, Grundschule, Mathe, Deutsch, Vorschule, Einmaleins, Hausaufgaben, Kinder.

## 5. Grafiken

- App-Icon 512 x 512: `store/play-store-icon-512.png` ist da.
- Feature-Grafik 1024 x 500: fehlt noch (Navy `#00205B` mit Gelb `#FFED00`, Maskottchen oder Haustier).
- Screenshots (Handy, 7-Zoll- und 10-Zoll-Tablet): von Cem auf dem Gerät oder im Emulator aufnehmen.
  Gute Motive: Startbildschirm, Aufgabe, Zahlenmauer, Vorschule, Spiele, Haustier, Erfolge.

## 6. Noch zu tun im Code und auf der Webseite

- `docs/index.html` im iOS-Repo (`cembot90/YEM1N`): Datenschutz um Android ergänzen
  (kein Konto, nur Gerätespeicher, Stimmen-Download von GitHub, Mitteilung nur lokal). Das macht Claude im iOS-Repo.
- Release-Bau: `app/proguard-rules.pro` hat Keep-Regeln für Sprachausgabe (sherpa-onnx), Unterschriften (eddsa) und
  kotlinx.serialization und ist im Release-Typ eingetragen. **Vor dem Hochladen einmal testen** (nur Cem kann das):
  Android Studio > Build > Generate Signed App Bundle, dann die App aus dem Bundle auf einem echten Gerät starten
  und prüfen: üben, Vorlesen (natürliche Stimme), Joker, QR-Code scannen, Klassencode. Stürzt etwas ab, die
  Fehlermeldung (Logcat) an Claude schicken, dann kommt die fehlende Keep-Regel dazu.
- Gemini Nano (ML Kit GenAI) braucht eigentlich Android 8. Die App läuft weiter ab Android 7, die KI ist dort aus.
- Versionsnummer: `versionCode` bei jedem Upload um 1 erhöhen.
- Lizenzen: Das Projekt steht unter GPL-3.0 (`LICENSE`, `THIRD-PARTY.md`). Der Quellcode ist im öffentlichen
  Repo, im Store-Eintrag den Link nennen.
