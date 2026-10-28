# Server einrichten (für Cem)

Der Server ist ein kleines Programm bei Cloudflare. Es nimmt Joker, Antworten, Daumen und Ergebnisse von der
Android-App an und schreibt sie mit dem Server-Schlüssel in die CloudKit-Datenbank. Der Code liegt in `server/`.

## 1. Server-Schlüssel bei Apple (am Mac, Terminal)

```
openssl ecparam -name prime256v1 -genkey -noout -out eckey.pem
openssl ec -in eckey.pem -pubout -out eckey-public.pem
openssl pkcs8 -topk8 -nocrypt -in eckey.pem -out eckey-pkcs8.pem
```

1. CloudKit-Dashboard, Container `iCloud.de.cemaras.yem1n`, Tokens & Keys, Server-to-Server Keys, neuen Schlüssel anlegen.
2. Den Inhalt von `eckey-public.pem` einfügen (nur den Teil zwischen den Strichen). Apple zeigt danach eine Key-ID. Die notieren.
3. `eckey.pem` und `eckey-pkcs8.pem` bleiben geheim. Nicht ins Repo, nicht per Mail.

## 2. Cloudflare

1. Konto auf cloudflare.com anlegen (kostenlos).
2. Im Ordner `server`: `npm install -g wrangler` (oder `npx wrangler` benutzen), dann `wrangler login`.
3. `wrangler kv namespace create GERAETE` ausführen und die ausgegebene `id` in `wrangler.toml` bei `id = ...` eintragen.
4. Geheimnisse setzen (jeweils eingeben oder einfügen):
   - `wrangler secret put CK_KEY_ID` (die Key-ID von Apple)
   - `wrangler secret put CK_PRIVATE_KEY` (kompletter Inhalt von `eckey-pkcs8.pem`)
5. `wrangler deploy`. Am Ende steht die Adresse, zum Beispiel `https://yem1n-server.NAME.workers.dev`. Die schickst du mir.
6. Kontrolle: `https://DEINE-ADRESSE/v1/ping` im Browser muss `{"ok":true}` zeigen.

## 3. Neu: Familien-Dienst (Protokoll 2)

Das ist der neue, sichere Weg (Beschreibung in `cloud/PROTOKOLL-V2.md`). Jede Familie bekommt ein eigenes Fach
(Durable Object) in der EU, jedes Gerät einen eigenen Schlüssel, und nichts ist mehr öffentlich lesbar.
Der alte Weg (`/v1/...`) bleibt, bis beide Apps umgestellt sind.

1. Wrangler muss Version 4.36 oder neuer sein. `npx wrangler --version` zeigt es. Mit `npx wrangler@latest deploy` ist man sicher.
2. Im Ordner `server`: `npx wrangler@latest deploy`. Die Tabellen für die Familien und die Bremsen je Adresse legt Cloudflare dabei selbst an.
3. Kontrolle: `https://DEINE-ADRESSE/v2/ping` im Browser muss `{"ok":true,"protokoll":2}` zeigen.
4. Mitteilungen für das iPhone (APNs). Die Datei mit dem Schlüssel schickst du mir **nicht** und legst sie auch in kein Repo.
   1. Apple Developer, Certificates, Identifiers & Profiles, Keys, neuen Schlüssel anlegen, "Apple Push Notifications service (APNs)" ankreuzen.
   2. Die Datei `AuthKey_XXXXXXXXXX.p8` herunterladen (geht nur einmal) und sicher aufbewahren.
   3. Im Ordner `server`, jeweils eingeben oder einfügen:
      - `npx wrangler secret put APNS_KEY` (kompletter Inhalt der .p8-Datei)
      - `npx wrangler secret put APNS_KEY_ID` (die zehn Zeichen aus dem Dateinamen)
      - `npx wrangler secret put APNS_TEAM_ID` (Team-ID aus dem Apple-Developer-Konto, oben rechts unter Mitgliedschaft)
   4. Die Key-ID und die Team-ID darfst du mir im Chat schicken, den Inhalt der .p8-Datei nicht.
5. Ob die Familien wirklich in der EU liegen, zeigt `/v2/status` jedes Geräts (`jurisdiktion` muss `eu` sein). Die App zeigt es später in den Einstellungen.
   Ein Probelauf gegen die echte Adresse legt eine Testfamilie an, spielt alles durch und löscht sie wieder:
   `node scripts/lokal-pruefen.mjs https://DEINE-ADRESSE --eu` (ohne `--eu` wird die Jurisdiktion nur angezeigt).
   Die Variable `FAMILIE_JURISDIKTION` darf in der echten Umgebung nie gesetzt werden. Sie gibt es nur für Tests auf dem eigenen Rechner,
   weil die lokale Laufzeit keine Jurisdiktionen kennt.
6. Vor der Veröffentlichung auf den Plan "Workers Paid" (5 US-Dollar im Monat) wechseln. Der kostenlose Plan reicht zum Testen, aber nicht für viele Familien.
7. Kosten im Blick: Im Dashboard unter Manage Account, Billing, Billable Usage zwei Budget-Alarme anlegen (1 und 5 US-Dollar Mehrverbrauch). Sie warnen nur per E-Mail, ein hartes Limit gibt es bei Cloudflare nicht. Der Dienst bremst selbst (120 Anfragen pro Gerät und Minute, 20000 pro Familie und Tag). Notausschalter: Worker `yem1n-server`, Einstellungen, Variablen, `NOTAUS` = `1` setzen (stoppt alle Familienanfragen sofort, `0` oder löschen schaltet wieder ein).
   Schreibvorgänge je Familie sind dort das Nadelöhr, deshalb schreibt der Dienst sparsam.

## 4. Später: den alten Familienweg abschalten

Wenn beide Apps (Android und iPhone) mit Protokoll 2 auf den Geräten der Familie laufen, schaltest du den alten Weg ab:
`V1_AUS = "1"` in `wrangler.toml` einkommentieren und `npx wrangler@latest deploy`. Die Schritte im Einzelnen (alte Ablage leeren,
CloudKit aufräumen, prüfen) stehen in `cloud/ABSCHALTEN-V1.md`. Klassencodes laufen danach weiter.

## Offen (macht Claude danach)

- Beide Apps auf Protokoll 2 sind umgestellt. Offen ist nur noch dein Test auf echten Geräten.
- Danach den alten Weg abschalten (Abschnitt 4) und die Familiendaten in CloudKit löschen.
