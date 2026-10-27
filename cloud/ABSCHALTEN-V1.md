# Alten Familienweg abschalten (Phase 5)

Erledigt: `V1_AUS` am 27.10.2026, KV-Ablage geleert am 27.10.2026, CloudKit-Dashboard aufgeräumt am 28.10.2026. In CloudKit lassen sich einige Typen nur über einen Filter auf `familienCode` abfragen (Filter `familienCode ≠ ZZZ-GIBTS-NICHT`), weil `recordName` dort nicht abfragbar ist.

Der alte Weg (Familiencode, CloudKit, `/v1/senden` und `/v1/geraet`) ist unsicher, weil der Familiencode Passwort und
Suchschlüssel zugleich war und die Einträge in einer öffentlich lesbaren Datenbank lagen. Protokoll 2 löst ihn ab
(`cloud/PROTOKOLL-V2.md`). Dieses Blatt sagt, wann und wie der alte Weg ausgeschaltet wird.

## Wann

Erst wenn alles davon stimmt:

1. Der Worker mit Protokoll 2 ist veröffentlicht (`npx wrangler@latest deploy` im Ordner `server`), `/v2/ping` antwortet.
2. Die Android-App (`entwicklung`) und die iPhone-App (`entwicklung`) sind auf mindestens einem Gerät der Familie ausprobiert:
   Familie anlegen, zweites Gerät per QR koppeln, erlauben, Joker, Ergebnis, Duell, Haustier.
3. Die iPhone-App mit Protokoll 2 ist über `main` bei TestFlight angekommen und auf den Geräten der Familie installiert,
   ebenso die Android-App (Play-Test). Alte App-Versionen kommen danach nicht mehr in die Cloud.

Vorher darf keine der beiden Apps öffentlich erscheinen.

## Schritte

1. In `server/wrangler.toml` unter `[vars]` die Zeile `V1_AUS = "1"` einfügen und `npx wrangler@latest deploy` ausführen.
   Danach antworten `/v1/senden` und `/v1/geraet` mit 410, der Minutenlauf für Mitteilungen aus CloudKit ruht
   (`/v1/ping` zeigt `"familienWeg":"aus"`). Klassencodes (`/v1/klasse`) laufen weiter.
2. Alte Ablage leeren: im Ordner `server` erst `node scripts/alte-ablage-leeren.mjs` (zeigt nur, was gelöscht würde),
   dann `node scripts/alte-ablage-leeren.mjs --ja`. Das entfernt Familiencodes, Rollen und Push-Kennungen aus dem KV `GERAETE`.
3. CloudKit aufräumen:
   - Jede neue iPhone-App löscht beim ersten Start die Einträge, die dieses Gerät unter dem alten Familiencode angelegt hat
     (`AlterFamilienweg`). Fremde Einträge kann ein Gerät nicht löschen.
   - Den Rest löscht der Entwickler im CloudKit-Dashboard (Container `iCloud.de.cemaras.yem1n`, Umgebung Production, Records):
     je Typ `RundenErgebnis`, `JokerAnfrage`, `JokerAntwort`, `JokerDaumen`, `JokerNachschub`, `JokerFreigabe`, `Haustier`,
     `HaustierPflege`, `Duell`, `DuellAntwort` alle Einträge. Bei `Lernpaket` nur die der Familien, die Pakete der Klassen
     (Codes mit `K-`) bleiben.
   - Record-Typen lassen sich in Production nicht löschen. Leer sind sie harmlos.
4. Prüfen: Beide Apps zeigen unter Einstellungen > Familie "Verbunden", Joker und Ergebnisse kommen an, die Mitteilung
   erscheint. Ein altes Gerät mit dem alten Familiencode bekommt nichts mehr.

## Später aufräumen (kein Eile)

Nach einigen ruhigen Wochen kann der Code des alten Weges aus dem Worker verschwinden: `/v1/senden`, `/v1/geraet`,
`pruefeNeues`, `raeumeAuf`, `regeln.js` (nur der Familienteil), `mitteilungen.js`, `loeschfrist.js`. `/v1/klasse` und
`cloudkit.js` bleiben für die Klassen. Der CloudKit-Server-Schlüssel und der API-Token bleiben ebenfalls, denn die Pakete
der Klassen laufen weiter über CloudKit.
Das Android-Repo hat dann auch `cloud/schema.ckdb` und `cloud/SERVER-KONZEPT.md` nicht mehr nötig. Im iPhone-Repo ist
`docs/android-anmeldung.html` (alte Anmeldung bei Apple für Android) überflüssig.
