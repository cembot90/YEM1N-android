# YEM1N Android: Architektur für IT-Leute

Stand 22.10.2026. Zielgruppe: Entwickler und Administratoren, die das Projekt übernehmen, prüfen oder betreiben.

## 1. Überblick

Native Android-App (Kotlin, Jetpack Compose, Material3, Paket `de.cemaras.yem1n`) für Grundschul-Übungen.
Gegenstück zur iOS-App (Repo `cembot90/YEM1N`). Beide Apps teilen ein JSON-Format für Arbeiten und eine
CloudKit-Datenbank (öffentlich, Container `iCloud.de.cemaras.yem1n`). Lizenz GPL-3.0 (wegen sherpa-onnx, siehe `THIRD-PARTY.md`).

Grundsatz: alles Wesentliche läuft lokal auf dem Gerät. Die Cloud ist optional und nur für Familienfunktionen da
(Joker, Ergebnisse, Duell). Ohne Einwilligung der Eltern geht nichts hinaus.

## 2. Schichten

| Ordner | Inhalt | Android-Abhängigkeit |
| --- | --- | --- |
| `daten/` | JSON-Format (`ArbeitPaket`) und Modelle (`Arbeit`, `Uebung`, `Aufgabe`) | keine |
| `logik/` | Antwortprüfung, Runde, Generator, Lehrplan, Fortschritt, Haustier, Spiele, Cloud-Regeln | keine |
| `speicher/` | Dateien, Alarme, FCM, HTTP-Clients, Widget, Stimmen-Download | ja |
| `ui/` | Compose-Bildschirme, `AppViewModel`, Theme | ja |

Regel: `logik/` und `daten/` sind reines Kotlin und laufen als JVM-Tests ohne Emulator. Die Oberfläche hat keine Tests.
Navigation ist ein einfaches `sealed interface Ansicht` im `AppViewModel` (kein NavHost), `zurueck()` bildet die Rückwege ab.

## 3. Datenfluss lokal

1. Arbeiten kommen aus `assets/pakete`, eingefügtem JSON, Datei öffnen oder Teilen/Öffnen aus anderen Apps. `PaketLeser` bereinigt und validiert (Zahlenmauern, Auswahlaufgaben).
2. `Lernwelt` erzeugt Aufgaben aus `LernThema`-Generatoren (Lehrplan Hessen, Bayern, NRW, Klassen 1 bis 4). Sprachen nutzen `sprachkatalog.json` (Englisch, Türkisch, Italienisch).
3. `Runde` führt durch Aufgaben, `Pruefer` bewertet (Zahlen als Zahlen, Text mit Groß und Klein, "Lösung zeigen" zählt nicht).
4. `Fortschritt` (Sterne, Münzen, Serie, Fehlerheft, Haustier, Einstellungen, Einwilligung) wird als eine JSON-Datei in `speicher/Speicher.kt` abgelegt.
5. Fehler führen zu `FehlerAnalyse`/`ErklaerFreund` (feste Sätze, keine KI) und `ErklaerBild` (Rechenweg als Zeichnung).

## 4. Cloud

```
Android --Lesen (API-Token)------------------> CloudKit (öffentliche DB)
Android --Schreiben--> Cloudflare Worker --Server-Schlüssel (ECDSA P-256)--> CloudKit
CloudKit <--Abfrage jede Minute-- Worker --FCM (Dienstkonto)--> Android
iOS <------------------------------------------> CloudKit (direkt, mit Apple-ID)
```

- Warum ein Server: Android-Nutzer sollen keine Apple-ID brauchen (Entscheidung 21.10.2026), und CloudKit kann keine Push an Android senden.
- Lesen: `speicher/CloudClient.kt`, `logik/JokerCloud.kt`, `logik/CloudKit.kt`. Token ist ein reiner Lese-Token.
- Schreiben: `speicher/ServerClient.kt` und `logik/ServerAnfrage.kt` an den Worker (`server/src/index.js`). Der Worker signiert gegen CloudKit (`cloudkit.js`), begrenzt Anfragen (`limit.js`), prüft Regeln (`regeln.js`) und verschickt Push (`fcm.js`, `mitteilungen.js`).
- Records, die der Server schreibt, gehören dem Entwickler. iOS kann sie lesen und neue anlegen, aber nicht ändern. Darum sind alle geteilten Typen "nur neu anlegen": Joker, Antworten, Daumen, Ergebnisse, Duell, DuellAntwort, JokerNachschub, JokerFreigabe.
- Familiencode `XXXXX-XXXXX` (`Familiencode`), QR `yem1n://familie/CODE`. Er ist der einzige Schlüssel zur Familie. Wer ihn kennt, sieht deren Daten (Grenze des Modells, siehe Risiken).
- Schema: `cloud/schema.ckdb`. Details: `cloud/SERVER-KONZEPT.md`, `cloud/CLOUDKIT-ANDROID.md`.
- Haustier und Pakete bleiben je System getrennt (kein Schreiben ohne Eigentümer).

## 5. Datenschutz

- Einwilligung der Eltern vor dem Familiencode (`logik/Einwilligung.kt`), widerrufbar. Ohne sie: keine Netzzugriffe der Familienfunktionen.
- Gesendet wird nur: Familiencode, Rundenergebnisse (Fach, Übung, Sterne, richtig/falsch), Joker-Texte, Duelle. Kein Klarname, keine Geräte-ID außer dem FCM-Token pro Familie.
- Kein Tracking, keine Werbung, kein Firebase außer als Postbote.
- Natürliche Stimmen werden einmal geladen (GitHub-Release `stimmen-1`, SHA-256 fest im Code) und laufen danach offline.

## 6. Hintergrundarbeit

`speicher/Erinnerung.kt` plant Alarme mit `setAndAllowWhileIdle` (Request-Codes 17 Haustier, 18 Lernzeit, 19 Wochenbericht Sonntag). Ein `BroadcastReceiver` zeigt die Mitteilung. Mitteilungsrecht ab Android 13 wird erfragt. Das Widget (`HaustierWidget`) zeigt Haustier und Serie.

## 7. Bauen, Testen, Ausliefern

- Gradle (Version Catalog `gradle/libs.versions.toml`), Compose, kotlinx.serialization, sherpa-onnx 1.13.8, ZXing.
- CI: `.github/workflows/tests.yml` mit zwei Jobs. `test`: Unit-Tests und Debug-Build. `server`: Node-Tests des Workers.
- Zweige: `entwicklung` (Werkstatt, wird getestet), `main` (geprüft, Fast-Forward). Keine Umschreibung der Historie.
- Server: `cd server && npx wrangler deploy`, Geheimnisse (Server-Schlüssel, FCM-Dienstkonto) als Worker-Secrets, siehe `server/EINRICHTEN.md`.
- Veröffentlichung: `store/VEROEFFENTLICHUNG.md`.

## 8. Bekannte Grenzen und Risiken

| Thema | Stand |
| --- | --- |
| Production-Schlüssel | Test läuft noch mit Development-Umgebung, Wechsel auf Production offen |
| Mehr als 300 Ergebnisse | keine Seitenabfrage |
| Mitteilungen | Worker fragt jede Minute, ausgelegt für etwa 5 Familien (Free-Tarif Cloudflare) |
| Familiencode | Geheimnis ohne Konto. Rate-Limit im Worker, aber kein Schutz gegen Weitergabe |
| Klassencode mit Unterschrift | nicht umgesetzt, bräuchte Signatur im Worker |
| KI-Erklärung | nicht umgesetzt, Android hat kein Gegenstück zur Geräte-KI |
| Stift und Handschrift | nicht umgesetzt |
| Lizenz | GPL-3.0 verlangt Quelltext bei Weitergabe der App |

## 9. Einstieg für neue Entwickler

1. Repo klonen, in Android Studio öffnen, `app/src/test` mit Run Tests ausführen.
2. Neue Aufgabenart: `daten/Paket.kt`, `logik/Pruefer.kt`, Eingabe in `ui/UebenScreen.kt`.
3. Neues Lernthema: `logik/lehrplan/*` ergänzen, `Lernwelt.themen` erweitern, Test in `LernweltTest`.
4. Neuer geteilter Record: `cloud/schema.ckdb`, Regel in `server/src/regeln.js`, Client in `logik/JokerCloud.kt`.
