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

Die App erhebt und überträgt keine personenbezogenen Daten. Alles bleibt auf dem Gerät.

| Frage | Antwort |
| --- | --- |
| Erhebt oder teilt die App Nutzerdaten? | Nein |
| Daten werden verschlüsselt übertragen? | Ja (nur HTTPS, siehe unten) |
| Können Nutzer die Löschung verlangen? | Nicht nötig, die Daten liegen nur auf dem Gerät. Deinstallieren löscht alles |
| Konto nötig? | Nein |
| Werbung? | Nein |
| In-App-Käufe? | Nein (Münzen sind nur Spielgeld, es gibt kein echtes Geld) |

Wofür die App ins Internet geht (ohne Nutzerdaten zu senden):

- Bedienungsanleitung von GitHub Pages (`cembot90.github.io`), mit Offline-Kurzfassung.
- Download der natürlichen Stimmen aus dem GitHub-Release `stimmen-1`, nur wenn Eltern es in den
  Einstellungen auslösen. Größe und Prüfsumme werden geprüft.

Berechtigungen:

- `INTERNET`: für die beiden Punkte oben.
- `POST_NOTIFICATIONS`: nur für die Erinnerung des Haustiers, wird erst nach Eltern-Schalter und Frage gefordert.

Datenschutzerklärung (URL für den Store-Eintrag): `https://cembot90.github.io/YEM1N/` (gleiche Seite wie iOS,
muss den Satz zu Android enthalten, siehe Punkt 6).

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
- Release-Bau mit Verkleinerung (R8) ist noch aus. Vor dem Store-Bau einschalten und mit Keep-Regeln für
  `com.k2fsa.sherpa.onnx` prüfen, weil die Sprach-Bibliothek ihre Klassen aus nativem Code ruft.
- Versionsnummer: `versionCode` bei jedem Upload um 1 erhöhen.
- Lizenzen: Das Projekt steht unter GPL-3.0 (`LICENSE`, `THIRD-PARTY.md`). Der Quellcode ist im öffentlichen
  Repo, im Store-Eintrag den Link nennen.
