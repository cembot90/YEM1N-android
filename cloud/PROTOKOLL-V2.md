# YEM1N Protokoll 2 (Familien-Dienst)

Dieses Papier beschreibt, wie die Apps (Android und iPhone) mit dem Dienst der Familie sprechen.
Es ersetzt den bisherigen Weg über die öffentliche CloudKit-Datenbank und den Familiencode.
Der Code des Dienstes steht in `server/src/v2/`, die Tests in `server/test/v2*.test.js`.
Feste Beispiele zum Nachprüfen stehen in `cloud/testvektoren-v2.json`. Swift und Kotlin müssen sie bestehen.

## Grundidee

- Jede Familie hat ein eigenes Durable Object (SQLite) in der EU-Jurisdiktion von Cloudflare.
- Die Kennung der Familie ist zufällig (64 Hex-Zeichen) und von Cloudflare vergeben. Sie lässt sich nicht erraten und ist kein Passwort.
- Jedes Gerät hat ein eigenes Schlüsselpaar (P-256). Der private Schlüssel verlässt das Gerät nie
  (Secure Enclave auf dem iPhone, Android Keystore auf Android).
- Jede Anfrage ist mit dem Schlüssel des Geräts unterschrieben. Wer die Familienkennung kennt, kann damit allein nichts lesen.
- Neue Geräte kommen nur per QR-Code in die Familie und müssen von einem Eltern-Gerät bestätigt werden. Das gilt auch für Kinder-Geräte.
- Mitteilungen sind neutral (Titel "YEM1N", kein Name, kein Inhalt). Die App holt den Inhalt selbst ab.

## Kennungen

| Was | Aussehen |
|---|---|
| Familie | 64 Hex-Zeichen (klein), vom Dienst vergeben |
| Öffentlicher Schlüssel (`pub`) | base64url (ohne `=`) der 65 Bytes des unkomprimierten Punkts: `04 ‖ X ‖ Y` |
| Gerät | die ersten 24 Hex-Zeichen von SHA-256 über die 65 Bytes des öffentlichen Schlüssels |
| Unterschrift | base64url von rohem `r ‖ s`, genau 64 Bytes |

Die Gerätekennung berechnet der Dienst selbst. Ein Gerät kann sich keine fremde Kennung geben.

## Unterschrift

Alle Wege außer `/v2/ping`, `/v2/familie` und `/v2/koppeln` brauchen diese Kopfzeilen:

| Kopfzeile | Inhalt |
|---|---|
| `Y-Familie` | Kennung der Familie |
| `Y-Geraet` | Kennung des Geräts |
| `Y-Zeit` | Zeit des Geräts in Millisekunden seit 1970 (erlaubt: 5 Minuten Abweichung) |
| `Y-Signatur` | Unterschrift über den folgenden Text |

Der Text, der unterschrieben wird (sieben Zeilen, getrennt durch `\n`, ohne Zeilenumbruch am Ende):

```
YEM1N-V2
POST
/v2/senden
<Familie>
<Gerät>
<Zeit in Millisekunden>
<SHA-256 des Bodys als Hex, klein>
```

Der Body ist genau der Text, der gesendet wird (UTF-8). Wer ihn nach dem Unterschreiben verändert, macht die Unterschrift ungültig.
Die Hash-Zeile steht auch bei leerem Body da (SHA-256 des leeren Texts).

Der Dienst weist ab:

- falsche Familie, unbekanntes Gerät oder falsche Unterschrift (401)
- Zeit außerhalb von 5 Minuten (401, die Antwort enthält `serverZeit`, damit das Gerät seine Uhr ausgleichen und einmal neu versuchen kann)
- dieselbe Unterschrift ein zweites Mal (401, Schutz gegen Mitschneiden und Wiederholen)
- mehr als 120 Anfragen pro Gerät und Minute (429)
- mehr als 20000 Anfragen pro Familie und Tag (429, schützt vor Fehlern und Kosten)
- Notausschalter: ist die Worker-Variable `NOTAUS` auf `1` gesetzt, antwortet der Dienst auf alles außer `/v2/ping` mit 503

Bei einem Wiederholungsversuch immer neu unterschreiben.

### iPhone (Swift)

```swift
let schluessel = try SecureEnclave.P256.Signing.PrivateKey()          // bleibt im Gerät
let pub = schluessel.publicKey.x963Representation                      // 65 Bytes
let signatur = try schluessel.signature(for: Data(text.utf8)).rawRepresentation   // 64 Bytes r ‖ s
```

CryptoKit hasht selbst mit SHA-256. Die Datei `testvektoren-v2.json` enthält `anfrage.nachricht` und `anfrage.signatur` zum Prüfen.

### Android (Kotlin)

- Der Keystore liefert Unterschriften im DER-Format (`SHA256withECDSA`). Der Dienst will rohes `r ‖ s`.
  Umrechnung: aus der DER-Folge die beiden ganzen Zahlen lesen, führende Nullbytes streichen, jede links mit Nullen auf 32 Bytes auffüllen, hintereinander legen.
  Ein Beispiel steht unter `derUmrechnung` in den Testvektoren.
- Der öffentliche Schlüssel kommt als X.509 (SPKI). Die letzten 65 Bytes davon sind der unkomprimierte Punkt, den der Dienst braucht (`spkiBase64` in den Testvektoren).
- Nach dem Hashen mit SHA-256 darf nicht noch einmal gehasht werden. `SHA256withECDSA` hasht selbst.

## Wege

Alle Wege sind `POST` mit JSON-Body. Antworten sind JSON. Fehler haben die Form `{ "fehler": "Text" }`. Texte sind für Menschen, Apps richten sich nur nach dem Statuscode.

### Ohne Unterschrift

| Weg | Body | Antwort |
|---|---|---|
| `/v2/ping` (auch GET) | | `{ ok, protokoll: 2 }` |
| `/v2/familie` | `{ pub, name }` | `{ ok, familie, geraet, rolle: "eltern", jurisdiktion }` Legt die Familie an. Das erste Gerät ist sofort Eltern-Gerät. Höchstens 5 neue Familien pro Stunde und Adresse. |
| `/v2/koppeln` (mit `Y-Familie`) | `{ token, pub, name }` | `{ ok, geraet, status: "wartet", rolle }` Die Rolle bestimmt die Einladung, nicht das Gerät. |

### Mit Unterschrift, für jedes freigegebene Gerät

| Weg | Body | Antwort |
|---|---|---|
| `/v2/status` | `{}` | `{ ok, status, rolle, name, geraet, familie, serverZeit, geraete, jurisdiktion }` Geht auch für wartende und abgelehnte Geräte. |
| `/v2/push` | `{ art: "apns" \| "fcm", token, umgebung?: "sandbox" \| "production" }` oder `{ art: "" }` zum Abmelden | `{ ok }` Meldet die Push-Kennung des Geräts an. Ein Token gehört immer nur einem Gerät. |
| `/v2/senden` | `{ typ, daten }` | `{ ok, neu, nr }` Legt einen Eintrag ab (siehe unten). |
| `/v2/lesen` | `{ typ, seit?, limit? }` | `{ ok, eintraege, letzte, mehr }` |
| `/v2/geraet/austreten` | `{ eintraegeLoeschen?: true }` | `{ ok }` Das letzte Eltern-Gerät kann nicht austreten (409). Ein wartendes Gerät darf austreten. |

### Nur Eltern-Geräte

| Weg | Body | Antwort |
|---|---|---|
| `/v2/einladung` | `{ rolle: "kind" \| "eltern" }` | `{ ok, token, rolle, bis }` Gilt 10 Minuten und einmal. Höchstens 5 offene Einladungen. |
| `/v2/geraete` | `{}` | `{ ok, geraete: [{ id, name, rolle, status, erstellt, zuletzt, ich }] }` Ohne Schlüssel und ohne Push-Kennungen. |
| `/v2/geraet/erlauben` | `{ geraet }` | `{ ok, geraet, status: "aktiv" }` |
| `/v2/geraet/ablehnen` | `{ geraet }` | `{ ok, geraet, status: "abgelehnt" }` |
| `/v2/geraet/entfernen` | `{ geraet }` | `{ ok }` Das eigene Gerät entfernt man mit `austreten`. |
| `/v2/familie/loeschen` | `{ bestaetigung: "LOESCHEN" }` | `{ ok }` Löscht alles unwiderruflich. |

## Koppeln per QR-Code

1. Ein Eltern-Gerät ruft `/v2/einladung` auf und zeigt den QR-Code `yem1n://koppeln?f=<Familie>&t=<token>`.
2. Das neue Gerät erzeugt sein Schlüsselpaar, liest den QR-Code und ruft `/v2/koppeln` auf. Es ist jetzt `wartet`.
3. Das Eltern-Gerät bekommt eine neutrale Mitteilung (Art `geraet`) und sieht das Gerät unter `/v2/geraete`.
   Die Eltern vergleichen den Namen und entscheiden: `erlauben` oder `ablehnen`.
4. Das neue Gerät fragt mit `/v2/status` nach (zum Beispiel alle paar Sekunden, solange der Bildschirm offen ist) und wird `aktiv`.
5. Höchstens 3 Geräte warten gleichzeitig, höchstens 12 Geräte gehören zu einer Familie.
   Abgelehnte Geräte verschwinden nach einem Tag.

## Einträge

`/v2/senden` legt einen Eintrag der angegebenen Art ab. Der Dienst prüft Rolle und Inhalt streng.
Den Namen des Absenders setzt der Dienst aus dem Gerät, nicht aus den Daten.
Wird derselbe Eintrag zweimal gesendet (gleiche Kennung), bleibt der erste, die Antwort sagt `neu: false`.
So kann eine App ohne Sorge noch einmal senden, wenn die Antwort verloren ging.

`/v2/lesen` liefert Einträge in der Reihenfolge ihrer Nummer `nr`. Mit `seit` (die letzte bekannte `nr`) holt man nur Neues.
`limit` ist 1 bis 200, ohne Angabe 100. `mehr: true` heißt, dass es vielleicht noch weitere gibt.
Abgelaufene Einträge werden nicht mehr geliefert.

| Art | Anlegen darf | Lesen darf | Tage | Höchstens | Doppelt | Mitteilung |
|---|---|---|---|---|---|---|
| frage | kind | Kind: nur eigene, Eltern: alle | 365 | 500 | erster bleibt | Eltern: "Ein Kind braucht Hilfe." |
| antwort | eltern | Kind: alle, Eltern: alle | 365 | 1000 | erster bleibt | Kinder: "Es gibt eine neue Antwort." |
| daumen | kind | Kind: alle, Eltern: alle | 365 | 1000 | erster bleibt | Eltern: "Jemand hat sich bei dir bedankt." |
| nachschub | kind | Kind: nur eigene, Eltern: alle | 90 | 200 | erster bleibt | Eltern: "Ein Kind möchte neue Joker." |
| freigabe | eltern | Kind: alle, Eltern: alle | 90 | 200 | erster bleibt | Kinder: "Es gibt neue Joker!" |
| ergebnis | kind | Kind: nein, Eltern: alle | 365 | 3000 | erster bleibt | Eltern: "Es gibt ein neues Ergebnis." |
| duell | kind, eltern | alle | 14 | 200 | erster bleibt | alle: "Neue Duell-Herausforderung!" |
| duellantwort | kind, eltern | alle | 14 | 400 | erster bleibt | alle: "Ein Duell ist entschieden." |
| haustier | kind | alle | 365 | 1 | überschreiben | keine |
| pflege | eltern | alle | 4 | 20 | erster bleibt | Kinder: "Dein Haustier wurde versorgt." |
| paket | eltern | alle | 365 | 50 | überschreiben | Kinder: "Neue Arbeit für euch." |

Die genauen Felder jeder Art stehen in `server/src/v2/typen.js` (Funktion `pruefe`). Kennungen (`id`) sind 6 bis 64 Zeichen aus `A-Z a-z 0-9 _ -`.
Je Gerät sind höchstens 600 neue Einträge pro Stunde erlaubt.

Die Mitteilung geht nie an das Gerät, das den Eintrag angelegt hat.

## Mitteilungen

- Titel immer "YEM1N", der Text steht in der Tabelle oben. Kein Kindername, keine Frage, kein Ergebnis.
- Die Art steht in den Daten der Mitteilung (bei APNs als `typ`, bei FCM als `art`): `frage`, `antwort`, `daumen`, `nachschub`, `freigabe`, `ergebnis`, `duell`, `duellantwort`, `pflege`, `paket`, `geraet`.
  Die App holt danach selbst mit `/v2/lesen` und `/v2/status` ab.
- iPhone: direkt über Apples Dienst (APNs). Die App meldet ihren Token und ob er aus der Sandbox oder der Produktion stammt (`umgebung`).
  Der Schlüssel (.p8) liegt nur als Geheimnis im Worker.
- Android: über Firebase Cloud Messaging als reiner Postbote.
- Meldet Apple oder Google, dass eine Kennung nicht mehr gilt, vergisst der Dienst sie.

## Fristen und Löschen

- Eine Familie ohne weiteres Gerät und ohne Eintrag wird nach 30 Tagen gelöscht.
- Wird die Familie benutzt, verschiebt sich die Frist (einmal pro Tag) auf 365 Tage nach der letzten Nutzung.
- Einträge laufen nach ihrer Frist (Spalte "Tage") ab und werden aufgeräumt.
- Eltern können die Familie jederzeit mit `/v2/familie/loeschen` löschen. Danach gibt es sie nicht mehr (404).
- Ein Kinder-Gerät kann mit `austreten` und `eintraegeLoeschen: true` seine eigenen Einträge mitnehmen.

## Statuscodes

| Code | Bedeutung |
|---|---|
| 200 | geklappt |
| 400 | Anfrage unlesbar oder Inhalt ungültig |
| 401 | Unterschrift, Uhrzeit, Gerät oder Wiederholung stimmen nicht |
| 403 | keine Berechtigung (Rolle, Einladung, Gerät nicht freigegeben) |
| 404 | Familie oder Gerät gibt es nicht |
| 405 | nur POST |
| 409 | passt gerade nicht (zum Beispiel das letzte Eltern-Gerät, doppeltes Gerät, Geräte voll) |
| 413 | zu groß (höchstens 120000 Zeichen) |
| 429 | zu viele Anfragen |
| 502, 503 | Dienst gerade nicht erreichbar oder noch nicht eingerichtet |

## Was die App beachten muss

- Die Uhr: Weicht sie mehr als 5 Minuten ab, antwortet der Dienst mit 401 und `serverZeit`. Die App merkt sich den Unterschied und versucht einmal neu.
- Nicht erreichbar heißt nicht verloren: Einträge mit fester Kennung dürfen jederzeit noch einmal gesendet werden.
- Der private Schlüssel gehört nicht in Sicherungen. Auf dem iPhone `ThisDeviceOnly`, auf Android vom Backup ausnehmen.
  Wer sein Gerät wechselt, koppelt neu.
- Freitext der Kinder (Fragen) läuft zu den Eltern. Die App weist darauf hin, dass dort keine Nachnamen, Adressen oder Telefonnummern stehen sollen.
