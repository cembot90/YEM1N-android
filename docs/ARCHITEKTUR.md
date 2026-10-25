# YEM1N Android: Architektur für IT-Leute

Stand 22.10.2026 (abends). Zielgruppe: Entwickler und Administratoren, die das Projekt übernehmen, prüfen oder betreiben.

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
Android --Familie (Protokoll 2, jede Anfrage unterschrieben)--> Cloudflare Worker, ein Fach je Familie (EU)
Cloudflare Worker --Mitteilung ohne Namen und Inhalt (FCM)--> Android
Android --Lesen von Klassenpaketen (API-Token)--> CloudKit (öffentliche DB)
```

- Familie: `logik/FamilienProtokoll.kt` (Körper der Anfragen, QR, Auswertung), `logik/FamilienClient.kt` (Unterschrift, Uhrabgleich), `logik/FamilienKrypto.kt` (base64url, Kennungen, DER nach roh), `speicher/GeraetSchluessel.kt` (P-256 im Android Keystore, der private Schlüssel verlässt das Gerät nie), `speicher/FamilienNetz.kt`, `speicher/FamilienSpeicher.kt` (Zugehörigkeit und PIN-Daten, nicht in der Sicherung), `ui/FamilienModell.kt` (Zustand für die Oberfläche), `ui/FamilieScreen.kt`, `ui/PinTor.kt`.
- Neue Geräte kommen nur per QR-Code und müssen von einem Eltern-Gerät erlaubt werden, auch Kinder-Geräte. Der Prüfcode (6 Zeichen der Geräte-Kennung) steht auf beiden Geräten.
- Eltern-PIN (`logik/ElternPin.kt`): schützt Familie, Ergebnisse, Joker-Antworten und die Familien-Einstellungen auf Geräten, die zur Familie gehören. 4 bis 8 Ziffern, gesalzener Hash, Wartezeit nach drei falschen Eingaben, 5 Minuten offen. Eine Sperre für Kinder, kein Tresor: Die Daten schützt der Schlüssel des Geräts.
- Warum ein Server: Android-Nutzer brauchen keine Apple-ID (Entscheidung 21.10.2026), und CloudKit kann keine Push an Android senden.
- Lesen von Klassenpaketen: `speicher/CloudClient.kt` (nur `lesen`), `logik/JokerCloud.kt` (`abfrage`, `alleSeiten`, `datensaetze`), `logik/CloudKit.kt`. Der Token ist ein reiner Lese-Token. Es gibt keine Anmeldung bei Apple mehr.
- Klassencode (`logik/Klassensiegel.kt`, `logik/KlassenAdmin.kt`, `server/src/klasse.js`): Pakete einer Klasse liegen als `Lernpaket` mit Code `K-XXXXX-XXXXX` in CloudKit. Jedes Paket trägt eine Ed25519-Unterschrift über `K-CODE|<json>`. Der öffentliche Schlüssel liegt als `KlassenSchluessel` (`klassenkey-K-CODE`). Empfänger merken sich den Schlüssel beim ersten Mal und lehnen einen anderen ab. Der Admin erzeugt den Schlüssel auf dem Gerät (privater Teil bleibt im `Fortschritt`, nie im Netz). Der Server (`POST /v1/klasse`, Arten `schluessel`, `paket`, `loeschen`) schreibt ein Paket nur, wenn die Unterschrift zum gespeicherten Schlüssel passt (WebCrypto Ed25519), und legt einen vorhandenen Schlüssel nie neu an. Paketnamen und Unterschriftsformat sind gleich wie bei iOS. Der Klassencode ist kein Geheimnis der Familie.
- Seitenabfrage: `JokerCloud.alleSeiten` folgt dem `continuationMarker` von CloudKit (bis zu 10 Seiten à 200 Ergebnisse).
- Der alte Familiencode (`xxxxx-xxxxx` in der öffentlichen Datenbank) ist abgelöst. Beim Start vergisst die App ihn und wartende Ergebnisse im alten Format (`altesAufraeumen` im ViewModel). Das Format `Familiencode` bleibt nur für den Klassencode.
- Schema: `cloud/schema.ckdb`. Details: `cloud/SERVER-KONZEPT.md`, `cloud/CLOUDKIT-ANDROID.md`.
- Haustier und Pakete bleiben je System getrennt.

### 4a. Protokoll 2 (Familien-Dienst)

Löst den Weg über die öffentliche CloudKit-Datenbank ab, in der der Familiencode zugleich Passwort und Suchschlüssel war.

- Eine Familie ist ein Durable Object (SQLite) in der EU-Jurisdiktion, Kennung zufällig (64 Hex-Zeichen). Code: `server/src/v2/` (`router.js` Eingangstür, `familie-do.js` Hülle, `familie.js` Kern ohne Cloudflare-Abhängigkeit, `typen.js` Rechte und Prüfungen, `krypto.js`, `apns.js`, `push.js`).
- Jedes Gerät hat ein P-256-Schlüsselpaar (Android Keystore, auf dem iPhone Secure Enclave) und unterschreibt jede Anfrage. Unterschriftstext, Wege, Rechte und Fristen: `cloud/PROTOKOLL-V2.md`. Gemeinsame Prüfwerte für Swift, Kotlin und Worker: `cloud/testvektoren-v2.json` (erzeugt mit `server/scripts/testvektoren.mjs`).
- Neue Geräte kommen nur per QR-Code und müssen von einem Eltern-Gerät bestätigt werden, auch Kinder-Geräte.
- Mitteilungen sind neutral (kein Name, kein Inhalt). iPhone über APNs direkt aus dem Worker, Android über FCM.
- Tests: `server/test/v2*.test.js` (Kern mit echtem SQLite über `node:sqlite`, Router, Rechte, Fristen, Mitteilungen, Testvektoren), auf Android `FamilienKryptoTest`, `FamilienClientTest`, `FamilienProtokollTest`, `ElternPinTest`.
- Stand: Worker und Android-Client fertig. Das iPhone-Gegenstück fehlt noch. Solange läuft `/v1` für iOS weiter, danach wird es abgeschaltet.

## 5. Datenschutz

- Einwilligung der Eltern vor der Familie (`logik/Einwilligung.kt`, Version 2), widerrufbar. Ohne sie: keine Netzzugriffe der Familienfunktionen.
- Gesendet wird nur: Geräte-Name, öffentlicher Schlüssel und Push-Kennung des Geräts, Rundenergebnisse (Fach, Übung, Sterne, richtig/falsch), Joker-Texte, Duelle. Kein Klarname.
- Mitteilungen enthalten weder Namen noch Inhalte, nur feste Sätze.
- Löschen: Eltern können die ganze Familie löschen, ein Kinder-Gerät kann beim Verlassen seine Einträge mitnehmen. Fristen je Art stehen in `cloud/PROTOKOLL-V2.md`.
- Kein Tracking, keine Werbung, kein Firebase außer als Postbote.
- Natürliche Stimmen werden einmal geladen (GitHub-Release `stimmen-1`, SHA-256 fest im Code) und laufen danach offline.

## 5a. KI auf dem Gerät

`speicher/KiText.kt` nutzt Gemini Nano über ML Kit GenAI Prompt API (Beta, Android 8 oder neuer, nur auf unterstützten Geräten, `tools:overrideLibrary` im Manifest, weil die App ab Android 7 läuft). `logik/KiPrompt.kt` baut den Auftrag und prüft die Antwort: leer, zu lang oder mit neuen Zahlen wird verworfen, dann gilt der feste Satz aus `FehlerAnalyse`. Es geht nichts ins Internet. Fehlt das Modell, startet einmal ein Hintergrund-Download.

## 6. Hintergrundarbeit

`speicher/Erinnerung.kt` plant Alarme mit `setAndAllowWhileIdle` (Request-Codes 17 Haustier, 18 Lernzeit, 19 Wochenbericht Sonntag). Ein `BroadcastReceiver` zeigt die Mitteilung. Mitteilungsrecht ab Android 13 wird erfragt. Das Haustier schläft von 20 bis 7 Uhr (`HaustierZeit`). Das Widget (`HaustierWidget`) zeigt Haustier und Serie.

## 7. Bauen, Testen, Ausliefern

- Gradle (Version Catalog `gradle/libs.versions.toml`), Compose, kotlinx.serialization, sherpa-onnx 1.13.8, ZXing.
- CI: `.github/workflows/tests.yml` mit zwei Jobs. `test`: Unit-Tests und Debug-Build. `server`: Node-Tests des Workers.
- Zweige: `entwicklung` (Werkstatt, wird getestet), `main` (geprüft, Fast-Forward). Keine Umschreibung der Historie.
- Server: `cd server && npx wrangler deploy`, Geheimnisse (Server-Schlüssel, FCM-Dienstkonto) als Worker-Secrets, siehe `server/EINRICHTEN.md`.
- Veröffentlichung: `store/VEROEFFENTLICHUNG.md`.

## 8. Bekannte Grenzen und Risiken

| Thema | Stand |
| --- | --- |
| Production-Schlüssel | App und Server laufen auf Production (Schlüssel in Development gelöscht, 22.10.2026) |
| Mitteilungen | Worker fragt jede Minute, ausgelegt für etwa 5 Familien (Free-Tarif Cloudflare) |
| Familie | Zugriff nur mit Schlüssel des Geräts und Freigabe der Eltern. Ein verlorenes Eltern-Gerät lässt sich von einem anderen Eltern-Gerät entfernen. Ob die EU-Jurisdiktion in der Produktion wirkt, prüft `server/scripts/lokal-pruefen.mjs --eu` nach dem Deploy |
| Eltern-PIN | Sperre für Kinder, nicht kryptografisch stark. Vergessen heißt: Gerät auf einem anderen Eltern-Gerät entfernen und neu verbinden |
| Klassencode | Admin-Schlüssel liegt nur auf einem Gerät. Geht das Gerät verloren, braucht die Klasse einen neuen Klassencode (kein Schlüsselwechsel) |
| KI-Erklärung | Beta-Schnittstelle von Google, nur auf unterstützten Geräten, sonst feste Sätze |
| Löschen in der Cloud | Familien-Daten löschen die Eltern in der App. Im alten CloudKit liegende Familien-Datensätze werden beim Abschalten von `/v1` gelöscht |
| Release mit R8 | Keep-Regeln in `app/proguard-rules.pro`, vor dem Store-Upload auf einem echten Gerät prüfen |
| Stift und Handschrift | ML Kit Digital Ink, Modell (etwa 20 MB) lädt einmal, Schrift bleibt auf dem Gerät |
| Lizenz | GPL-3.0 verlangt Quelltext bei Weitergabe der App |

Editoren: `logik/Editor.kt` (Rechner, `AufgabenBauer` für das Formular, `EntwurfPruefer` rechnet jede Lösung nach und meldet Gedankenstriche, Doppelte und kaputte Mauern, `EntwurfListe` mit Vorlagen aus `MatheGenerator`). Der Entwurf liegt als `entwurf.json` im App-Speicher und wird beim Speichern über `fuegeArbeitEin` zur normalen Arbeit. Vokabel-Editor: `logik/EigeneThemen.kt` (`eigene-themen.json`), die Sprachen-Seite mischt die Themen über `Sprachen.themen` und `Sprachen.woerter` ein. Beides ohne Cloud.

## 9. Einstieg für neue Entwickler

1. Repo klonen, in Android Studio öffnen, `app/src/test` mit Run Tests ausführen.
2. Neue Aufgabenart: `daten/Paket.kt`, `logik/Pruefer.kt`, Eingabe in `ui/UebenScreen.kt`.
3. Neues Lernthema: `logik/lehrplan/*` ergänzen, `Lernwelt.themen` erweitern, Test in `LernweltTest`.
4. Neuer geteilter Record: `cloud/schema.ckdb`, Regel in `server/src/regeln.js`, Client in `logik/JokerCloud.kt`.

## 10. Dokumentation pflegen

Bei jeder Funktionsänderung mitziehen: `CLAUDE.md` (Stand), `docs/ARCHITEKTUR.md` (Technik), `docs/ELTERNANLEITUNG.md` (Bedienung für Eltern), `daten/InfoTexte.kt` (Kurzanleitung und Datenschutz in der App), `store/VEROEFFENTLICHUNG.md` (Data Safety), Testliste im Claude-Projekt. Neue Daten, die ins Netz gehen, brauchen immer eine Anpassung des Datenschutztexts und der Einwilligung.
