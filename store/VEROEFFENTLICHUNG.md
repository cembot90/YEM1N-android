# YEM1N Android: Veröffentlichung im Play Store

Diese Datei sammelt alles, was Cem in der Play Console braucht. Die Texte sind Entwürfe zum Kopieren.
Stand: Entwicklung, noch nichts auf einem Gerät getestet.

## 1. Vorher (Cem)

1. Play-Konto anlegen (einmalig 25 $). Das Konto früh anlegen, weil vor der ersten Veröffentlichung ein
   geschlossener Test mit Testern laufen muss. Bei privaten Konten, die nach dem 13.11.2023 angelegt wurden, verlangt
   Google mindestens 12 Tester, die den Test 14 Tage lang ohne Unterbrechung mitmachen
   ([Google-Hilfe](https://support.google.com/googleplay/android-developer/answer/14151465)). Stand 26.10.2026, bitte vor dem Start
   in der Play Console gegenprüfen. Ein Organisationskonto ist davon nicht betroffen, braucht aber andere Nachweise.
2. Upload-Schlüssel erzeugen (Android Studio: Build, Generate Signed App Bundle, neuer Keystore). Den Keystore
   und das Passwort sichern. Ohne sie lässt sich die App nicht mehr aktualisieren.
3. App-Bundle bauen (`.aab`), Play App Signing aktivieren (Standard).
4. Auf echten Geräten prüfen: Tablet und Handy, Hochformat und Querformat (Spiele), Sprachausgabe, natürliche
   Stimmen laden, Erinnerung, Widget, Töne, Vibration.

## 2. Zielgruppe und Familienrichtlinien

- Die App richtet sich an Kinder. In der Play Console "Zielgruppe: Kinder" wählen und die Richtlinie für
  Familien beachten (keine Werbung, keine Tracker, keine Konten, keine Weitergabe von Daten).
- Altersgruppe: Vorschule bis Klasse 4, also etwa 5 bis 10 Jahre. Welche Gruppen in der Play Console angekreuzt werden, ist
  eine Entscheidung (siehe Abschnitt 7). Eltern sind die Verwalter (Eltern-PIN und Eltern-Schalter in den Einstellungen).
- Inhaltseinstufung (IARC-Fragebogen): keine Gewalt, keine Werbung, keine Käufe, keine Nutzerinhalte,
  kein Standort. Joker (Frage und Antwort) gehen nur innerhalb der Familie hin und her, es gibt keinen Chat mit Fremden.
- Googles Familienrichtlinie ([Text](https://support.google.com/googleplay/android-developer/answer/9893335)), Stand 26.10.2026,
  und wie die App sie erfüllt:
  - Keine Werbe-ID: `AD_ID` ist im Manifest entfernt (`tools:node="remove"`). Keine Standort-Berechtigung, keine Telefon-Kennungen.
  - "Erwachsenen-Aktion" für Elternfunktionen: die Eltern-PIN (Google nennt eine PIN ausdrücklich als Beispiel).
  - Datenschutzerklärung beschreibt die Datenverarbeitung (GitHub-Pages-Seite, Abschnitt 3a für Android).
  - Reine Kinder-Apps dürfen nur Bibliotheken nutzen, die für Kinderdienste zugelassen sind. Die App nutzt Firebase Cloud Messaging
    (nur als Postbote, Start erst nach Einwilligung), ML Kit (Gemini Nano auf dem Gerät) und den Code-Scanner der
    Play-Dienste. Die Richtlinienseite nennt diese Bibliotheken nicht einzeln. Bitte in der Play Console beim Zielgruppen-Fragebogen
    und im SDK-Index (Eintrag je Bibliothek) nachsehen, ob eine Warnung erscheint. Kommt eine, schreibe mir die Meldung.
  - Eine WebView darf nicht nur eine fremde Webseite zeigen: Die Anleitung ist die eigene Seite (GitHub Pages) mit Offline-Kurzfassung.

## 3. Datenschutz und Data Safety

Stand 25.10.2026. Üben, Fortschritt und Haustier bleiben auf dem Gerät. Mit Einwilligung der Eltern und einer
Familie nutzt die App einen Dienst (ein abgetrenntes Fach je Familie bei Cloudflare in der EU, Mitteilungen über Google FCM).
Klassenpakete kommen aus der öffentlichen CloudKit-Datenbank von Apple, dabei geht nur der Klassencode über das Netz.
Die Texte in der App stehen in `daten/InfoTexte.kt` (Datenschutz). Sie müssen zu den Angaben hier und auf
`https://cembot90.github.io/YEM1N/` passen. Bitte vor der Veröffentlichung selbst prüfen (kein Rechtsrat).

Data-Safety-Antworten für Google Play:

| Frage | Antwort |
| --- | --- |
| Erhebt oder teilt die App Nutzerdaten? | Ja, nur wenn Eltern eine Familie anlegen oder beitreten und einwilligen |
| Welche Daten? | Geräte- oder Nutzer-IDs (Geräte-Kennung aus dem öffentlichen Schlüssel, Push-Kennung), App-Aktivität (Rundenergebnisse), Nachrichten in der App (Joker-Fragen und Antworten), Name des Kindes und Name des Geräts (freiwillig) |
| Zweck | App-Funktionen (Joker, Ergebnisse für Eltern, Duell, Mitteilungen) |
| Teilen mit Dritten? | Verarbeiter: Cloudflare (Dienst der Familie, EU-Speicherung), Google (FCM, bekommt die Push-Kennung und eine Mitteilung mit festem Text ohne Namen und Inhalte). Apple CloudKit nur zum Lesen öffentlicher Klassenpakete, ohne Daten des Kindes. Keine Weitergabe zu Werbung oder Analyse |
| Daten verschlüsselt übertragen? | Ja (nur HTTPS) |
| Können Nutzer die Löschung verlangen? | Ja, in der App unter Familie (ganze Familie löschen, oder als Kind beim Verlassen die eigenen Einträge) und per E-Mail an cembot@icloud.com. Daten auf dem Gerät löscht die Deinstallation |
| Ist die Datenerhebung freiwillig? | Ja, ohne Familie wird nichts gesendet |
| Konto nötig? | Nein |
| Werbung? | Nein |
| In-App-Käufe? | Nein (Münzen sind nur Spielgeld) |

Löschung: Die Eltern löschen die ganze Familie mit allen Einträgen in der App (Familie > Ganze Familie löschen), ein Kind-Gerät kann
beim Verlassen seine Einträge mitnehmen. Der Dienst löscht Einträge nach ihrer Frist selbst (höchstens 365 Tage, Duelle 14 Tage, Nachschub 90 Tage),
eine unbenutzte Familie nach 365 Tagen ohne Nutzung, eine nie benutzte nach 30 Tagen. Alte Datensätze im CloudKit (aus der Zeit mit Familiencode)
werden beim Abschalten des alten Weges gelöscht (Ablauf in `cloud/ABSCHALTEN-V1.md`). Für Play ist die Löschung ohne Konto damit in der App möglich (Data-Safety-Angabe "Löschung anfordern": in der App und per E-Mail).

Offene Prüfungen vor der Veröffentlichung:

- Nach dem Deploy des Workers `node server/scripts/lokal-pruefen.mjs https://<adresse> --eu` ausführen, damit feststeht, dass die EU-Jurisdiktion wirkt. Erst dann darf "EU" in Store-Angaben und Datenschutztext stehen bleiben.
- Auf Cloudflare den Tarif Workers Paid wählen (Free reicht für den Test, nicht für den Betrieb).
- Auftragsverarbeitungs-Vertrag mit Cloudflare und Google (FCM) prüfen, Verzeichnis der Verarbeitungstätigkeiten ergänzen.
- Familienrichtlinie von Google: FCM und ML Kit im Kinder-Kontext prüfen, Eltern-Gate ist die Eltern-PIN.
- Datenschutztext und Texte in der App von einer fachkundigen Stelle prüfen lassen (kein Rechtsrat von Claude).

Wofür die App noch ins Internet geht:

- Bedienungsanleitung von GitHub Pages (`cembot90.github.io`), mit Offline-Kurzfassung.
- Download der natürlichen Stimmen aus dem GitHub-Release `stimmen-1`, nur wenn Eltern es auslösen. Größe und Prüfsumme werden geprüft.
- Klassencode: Pakete der Klasse aus CloudKit lesen. Nur der Klassencode geht über das Netz.
- Familien-Dienst bei Cloudflare (nur mit Familie und Einwilligung): Ergebnisse, Joker, Duelle, Geräteliste.
- Gemini Nano (KI auf dem Gerät) lädt sein Modell einmal von Google. Danach läuft die KI ohne Internet.
- Die Handschrift-Erkennung (ML Kit Digital Ink) ist seit 27.10.2026 entfernt. Damit entfallen die Modell-Ladung von Google und ein Baustein, der in der Familienrichtlinie zu prüfen wäre.

Berechtigungen:

- `INTERNET`: für die Punkte oben.
- `POST_NOTIFICATIONS`: für Erinnerungen (Haustier, Lernzeit, Wochenbericht) und Mitteilungen, wird erst nach Eltern-Schalter und Frage gefordert.
- Kamera: nur zum Scannen des QR-Codes beim Verbinden eines Geräts mit der Familie, wenn Eltern es auslösen (der Scanner kommt von Google Play-Diensten, die App braucht dafür keine eigene Kamera-Erlaubnis).

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

- `docs/index.html` im iOS-Repo (`cembot90/YEM1N`): Der Datenschutz-Text mit Android-Abschnitt 3a und dem Familien-Dienst steht auf dem
  Zweig `entwicklung`. Die öffentliche Seite (GitHub Pages) zeigt `main`, also noch den alten Stand. Sie wird erst aktuell, wenn die
  iPhone-Umstellung nach `main` kommt. Bis dahin passt die Datenschutz-URL nicht zu dieser App und darf nicht im Store-Eintrag stehen.
- Release-Bau: `app/proguard-rules.pro` hat Keep-Regeln für Sprachausgabe (sherpa-onnx), Unterschriften (eddsa) und
  kotlinx.serialization und ist im Release-Typ eingetragen. **Vor dem Hochladen einmal testen** (nur Cem kann das):
  Android Studio > Build > Generate Signed App Bundle, dann die App aus dem Bundle auf einem echten Gerät starten
  und prüfen: üben, Vorlesen (natürliche Stimme), Joker, QR-Code scannen, Klassencode. Stürzt etwas ab, die
  Fehlermeldung (Logcat) an Claude schicken, dann kommt die fehlende Keep-Regel dazu.
- Gemini Nano (ML Kit GenAI) braucht eigentlich Android 8. Die App läuft weiter ab Android 7, die KI ist dort aus.
- Versionsnummer: `versionCode` bei jedem Upload um 1 erhöhen.
- Lizenzen: Das Projekt steht unter GPL-3.0 (`LICENSE`, `THIRD-PARTY.md`). Der Quellcode ist im öffentlichen
  Repo, im Store-Eintrag den Link nennen.

## 7. Entscheidungen für Cem

Hier muss ein Mensch entscheiden. Claude kann es nicht für dich tun und gibt keinen Rechtsrat.

1. **Altersgruppen in der Play Console.** Google erlaubt mehrere Gruppen nur, wenn die App für alle gedacht und geeignet ist.
   Apps für Babys, Kleinkinder und Vorschulkinder sollen nur "Bis 5" wählen. YEM1N hat Inhalte für 5 bis 6 Jahre (Vorschule) und für
   6 bis 10 Jahre (Klasse 1 bis 4). Ich würde alle drei Gruppen ankreuzen (Bis 5, 6 bis 8, 9 bis 12), weil es für jede Inhalte
   gibt. Google kann die Angabe prüfen. Wer die Vorschule lieber nur als Teil für ältere Kinder sieht, wählt nur 6 bis 8 und 9 bis 12.
   Die technischen Regeln (keine Werbung, keine Kennungen, Eltern-PIN) erfüllt die App in beiden Fällen.
2. **Anbieterstatus (EU-Digitaldienstegesetz).** Apple und Google fragen, ob du als Händler (Trader) auftrittst. Bei "Ja" zeigt der Store
   Anschrift, Telefon und E-Mail öffentlich in den EU-Ländern, bei "Nein" steht dort ein Hinweis, dass Verbraucherrechte aus dem
   Vertrag mit dir nicht gelten. Apple sagt selbst, dass es das nicht für dich entscheiden kann
   ([Apple-Hilfe](https://developer.apple.com/help/app-store-connect/manage-compliance-information/manage-european-union-digital-services-act-trader-requirements)).
   Kostenlos, ohne Werbung und ohne Käufe spricht eher für "kein Händler", die Einschätzung ist aber deine (am besten kurz mit einer
   Rechtsberatung klären). Dieselbe Angabe muss in beiden Stores zusammenpassen.
3. **Kontaktadresse im Store.** Die E-Mail-Adresse im Store-Eintrag und in der Datenschutzerklärung ist öffentlich sichtbar. Bei Bedarf
   eine eigene Adresse nur für YEM1N einrichten und in `daten/InfoTexte.kt`, der Datenschutzseite und der Play Console gleich eintragen.
4. **Cloudflare und Google als Dienstleister.** Auftragsverarbeitung prüfen und im Verzeichnis der Verarbeitungstätigkeiten eintragen
   (Cloudflare für den Familien-Dienst, Google für FCM). Die EU-Speicherung gilt erst, wenn `lokal-pruefen.mjs --eu` grün war.
5. **Zeitpunkt.** Erst veröffentlichen, wenn beide Apps auf Protokoll 2 auf echten Geräten gelaufen sind (`cloud/ABSCHALTEN-V1.md`).
   Der geschlossene Test mit mindestens 12 Testern über 14 Tage kann vorher starten.
