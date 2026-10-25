# Konzept: kleiner Vermittler-Server für die Android-Cloud

> Stand 25.10.2026: Dieses Konzept beschreibt den bisherigen Weg über CloudKit. Er wird durch Protokoll 2 abgelöst:
> ein Fach je Familie bei Cloudflare (EU), Schlüssel je Gerät, Koppeln per QR-Code mit Freigabe durch die Eltern.
> Siehe `cloud/PROTOKOLL-V2.md`. Bis beide Apps umgestellt sind, laufen beide Wege.

Stand 21.10.2026. Grund: Die Apple-Anmeldung im Browser verlangt bei Cem einen Passkey, den Android-Browser
nicht liefern. Außerdem kann CloudKit Mitteilungen nur an Apple-Geräte schicken. Ein kleiner Server löst beides.

## Aufgabenteilung

| Was | Wer | Wie |
| --- | --- | --- |
| Lesen (Joker, Ergebnisse, Pakete, Duell) | Android direkt bei CloudKit | API-Token, alle Typen sind für `_world` lesbar. Kein Server nötig. |
| Schreiben (Joker senden, antworten, Daumen, Ergebnis, Duell) | Android an den Server, Server an CloudKit | Server-Schlüssel (Server-to-Server). Android-Nutzer brauchen keine Apple-ID. |
| Mitteilungen | Server | Fragt CloudKit regelmäßig nach Neuem und schickt Push an Android-Geräte der Familie. |

## Wo der Server läuft

Vorschlag: Cloudflare Workers (laut Dokumentation, Free-Tarif):

- 100.000 Anfragen pro Tag, 5 Cron-Aufrufe pro Konto, 50 Unteranfragen pro Aufruf, 10 ms Rechenzeit pro Aufruf.
- Geheime Schlüssel lassen sich als verschlüsselte Variablen hinterlegen (bis 5 KB je Wert).
- Der Free-Tarif kostet nichts, bei einer Familien-App sollte er reichen. Bei vielen Familien oder zu kurzer
  Rechenzeit (10 ms) gibt es einen bezahlten Tarif. Preise vor dem Umstieg erneut prüfen.
- Alternativen: Google Cloud Run, Fly.io, ein gemieteter Mini-Server (VPS). Mehr Pflege, kein echter Vorteil.

## Apple-Seite: Server-Schlüssel

- Erzeugen: `openssl ecparam -name prime256v1 -genkey -noout -out eckey.pem`, Public Key im CloudKit-Dashboard
  unter Tokens & Keys, Server-to-Server Keys eintragen, Key-ID notieren.
- Anfragen werden mit ECDSA (P-256) signiert. Signiert wird `Datum:Base64(SHA-256(Body)):/database/1/...`.
  Die Signatur ist 10 Minuten gültig. Hash und Ausgabeformat nennt Apple nicht ausdrücklich, SHA-256 und Base64
  sind die übliche Wahl und müssen beim ersten Test bestätigt werden.
- Server-Schlüssel dürfen nur die öffentliche Datenbank ansprechen, und zwar als Entwickler. Von ihnen erzeugte
  Records gehören dem Entwickler. Das passt zu den Rechten (`WRITE` für `_creator`), hat aber eine Folge:
  Ein iOS-Gerät kann einen von Android über den Server geschriebenen Record nicht ändern, nur lesen und
  neue Antworten dazuschreiben. Bei Joker-Status und Duell prüfen wir, ob das reicht (Entwurf vor dem Bau).
- Den privaten Schlüssel nie in die App oder ins Repo legen, nur als Geheimnis im Server.

## Android-Push

- Googles Firebase Cloud Messaging (FCM), nur als Postbote. Keine Firebase-Datenbank, keine Konten.
- Der Server braucht ein Google-Cloud-Dienstkonto, aus dem er Zugangsschlüssel für die FCM-Schnittstelle erzeugt.
  Ob FCM etwas kostet, steht in der geprüften Seite nicht. Nach meinem Kenntnisstand ist es kostenlos,
  das ist vor der Entscheidung zu bestätigen.
- Alternative ohne Google: ntfy oder UnifiedPush. Weniger verbreitet, schwerer für Eltern einzurichten.
- Jedes Android-Gerät meldet beim Server: Familiencode, Rolle (Kind oder Eltern), Push-Kennung.
  Speicherort: Cloudflare KV oder D1 (Kosten und Grenzen prüfen) oder ein neuer CloudKit-Record-Typ.

## Sicherheit

- Familiencode: 10 Zeichen aus 32 Symbolen, rund 50 Bit. Nicht zu erraten, solange der Server Ratversuche bremst.
- Der Server reicht keine freien CloudKit-Anfragen durch. Er kennt nur feste Operationen (Joker anlegen,
  Antwort anlegen, Daumen, Ergebnis, Gerät anmelden) und prüft Typ, Größe und Format.
- Begrenzung pro Gerät und Familiencode (Anfragen je Minute), damit niemand die Datenbank zumüllt.
- Alle Typen sind in CloudKit für alle lesbar (`READ` für `_world`). Das gilt schon für iOS. Wer den Familiencode
  nicht kennt, findet die Records aber nur, wenn er alle Joker-Records abfragt. Prüfen, ob die Abfrage ohne
  Familiencode ausgeschlossen werden kann (Index-Regeln), bevor es Android-Nutzer in großer Zahl gibt.
- Keine Kindernamen im Klartext in der Cloud, was iOS heute speichert, übernimmt Android 1:1.

## Aufwand (grob)

| Schritt | Wer | Zeit |
| --- | --- | --- |
| Cloudflare-Konto, Server-Schlüssel erzeugen, Dienstkonto für Push | Cem | etwa eine Stunde |
| Server-Code (Signieren, feste Operationen, Push, Tests) | Claude | mehrere Arbeitsrunden |
| Android: Datenmodelle, Senden, Lesen, Push empfangen | Claude | mehrere Arbeitsrunden |
| Erster Test mit echten Schlüsseln und Geräten | Cem | laufend |

## Offene Entscheidungen für Cem

1. Server auf Cloudflare Workers? (Vorschlag ja)
2. Push über Googles FCM? (Vorschlag ja, nur als Postbote)
3. Sollen Android-Nutzer wirklich ohne Apple-ID arbeiten? (Folge: Android-Records gehören dem Entwickler)
