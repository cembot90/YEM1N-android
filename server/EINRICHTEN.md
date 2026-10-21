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

## Offen (macht Claude danach)

- Android-App an den Server anbinden (Senden und Gerät melden).
- Push über Google (FCM): braucht später ein Google-Dienstkonto, das beschreibe ich dann Schritt für Schritt.
