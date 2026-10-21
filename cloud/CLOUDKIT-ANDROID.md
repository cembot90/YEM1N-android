# CloudKit auf Android: Plan und Stand

Entschieden am 21.10.2026: Android-Nutzer melden sich mit der Apple-ID an und nutzen dieselbe öffentliche
CloudKit-Datenbank wie iOS (Container `iCloud.de.cemaras.yem1n`).

## Was die Schema-Datei hergibt (`cloudkit/schema.ckdb` im iOS-Repo)

Alle Record-Typen haben `READ` für `_world`, `CREATE` für `_icloud` und `WRITE` für `_creator`.
Folge: Lesen geht ohne Anmeldung, Schreiben nur mit angemeldeter Apple-ID, ändern nur der Ersteller.
Typen: Duell, DuellAntwort, Haustier, HaustierPflege, JokerAnfrage, JokerAntwort, JokerDaumen,
KlassenSchluessel, Lernpaket, RundenErgebnis, Users.

## Wie die Web-Anmeldung laufen würde (nach Apple-Dokumentation, Web Services Reference)

1. Anfrage an `https://api.apple-cloudkit.com/database/1/<container>/<umgebung>/public/users/current?ckAPIToken=<Token>`
   liefert `AUTHENTICATION_REQUIRED` mit einer `redirectURL`.
2. Die App öffnet die `redirectURL` im Browser (Custom Tab). Der Nutzer meldet sich bei Apple an.
3. Apple lädt die im Token hinterlegte Rückkehr-Adresse mit `?ckWebAuthToken=...`.
4. Die App merkt sich den Web-Auth-Token und hängt ihn an jede Anfrage (`&ckWebAuthToken=...`, URL-kodiert).
5. Bei jeder Antwort liefert der Server einen neuen Token, der alte ist dann ungültig. Den neuen immer speichern.

Grenzen, die man kennen muss:

- Die Rückkehr-Adresse im Token ist eine normale `http(s)`-Adresse, kein App-Link. Lösung: eine kleine Seite auf
  GitHub Pages (`https://cembot90.github.io/YEM1N/android-anmeldung.html`), die den Token an die App weitergibt
  (`yem1n://cloudkit?ckWebAuthToken=...`). Die App fängt das Schema `yem1n` ab.
- Der Token gilt 30 Minuten, mit "Angemeldet bleiben" 2 Wochen. Danach muss sich ein Elternteil neu anmelden.
  Auf iOS gibt es diese Pflicht nicht. Das ist ein echter Unterschied im Alltag.
- Ein API-Token darf nicht mit einem Server-Schlüssel gemischt werden.
- Offen, weil nicht in der Seite genannt: In welchem Feld der Antwort der neue Token steht. Das wird beim ersten
  echten Test mit Cems Token festgestellt.

## Was Cem tun muss

1. CloudKit-Dashboard, Container `iCloud.de.cemaras.yem1n`, API Access, API Tokens, neuen Token anlegen.
2. Sign In Callback: `http://cembot90.github.io/YEM1N/android-anmeldung.html` (Seite baut Claude).
3. Den Token (Textkette) an Claude geben. Er ist kein Geheimnis im Sinn eines Passworts, steckt aber in der App.
4. Umgebung: erst `development` testen, später `production`. Schema ist laut iOS-Repo schon ausgerollt.

## Mitteilungen auf Android (offen)

CloudKit-Abos senden nur über Apples Push-Dienst. Android braucht einen eigenen Weg. Möglich:

- A: Kleiner Server fragt CloudKit ab und schickt über Firebase Cloud Messaging an Android (nur Push, ohne Firestore).
- B: Die App fragt regelmäßig selbst nach (WorkManager, mindestens alle 15 Minuten, spart keinen Akku).
- C: Eigener Push-Weg ohne Google (ntfy oder UnifiedPush).

Entscheidung steht aus.
