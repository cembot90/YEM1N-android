# Testplan: iOS und Android gegeneinander

Im Code sind Feldnamen, Duell-Format (JSON, Datum in Sekunden seit 2001) und die Unterschrift beim Klassencode
(`K-XXXXX-XXXXX|json`, Ed25519) auf beiden Seiten gleich. Das ersetzt keinen Test mit echten Geräten.
Vorher: Server neu veröffentlichen (`cd server`, `npx wrangler deploy`), damit die Mitteilung zur Anmeldung stimmt.

Geräte: iPhone der Eltern (Rolle Eltern), Android-Tablet bei Yemin (Rolle Kind), beide mit demselben Familiencode.

## 1. Anmeldung
1. Android: Einrichtung als Kind mit Name, dann Familiencode samt Einwilligung.
2. iPhone: Übersicht öffnen, nach unten ziehen. Unter "Kinder in der Familie" steht der Name mit "angemeldet ..., noch keine Runde".
3. Eine Runde auf Android spielen, iPhone neu laden: jetzt "1 Runde", Wochenbericht zählt sie.

## 2. Joker
1. Android (Kind): bei einer Aufgabe Joker, Frage an die Eltern schicken.
2. iPhone: Mitteilung kommt, Frage ist da. Antwort mit Emoji senden.
3. Android: Antwort erscheint, Daumen geben. iPhone: Daumen sichtbar.
4. Joker aufbrauchen, Nachschub erbitten. iPhone: Mitteilung, Freigabe. Android: Joker wieder da.
5. Dasselbe in umgekehrter Richtung mit einem iPhone als Kind (iOS schickt, Android liest als Eltern).

## 3. Duell
1. iPhone fordert heraus (Stufe Klasse 3). Android: Herausforderung erscheint, annehmen und spielen.
2. iPhone: Ergebnis der Antwort sichtbar, Sieger stimmt.
3. Umgekehrt: Android fordert heraus, iPhone nimmt an.

## 4. Klassencode
1. iPhone legt als Admin eine Klasse an und lädt ein Paket hoch (mit Unterschrift).
2. Android: Klassencode eingeben, Paket holen. Die Unterschrift muss "gültig" sein, das Paket erscheint unter den Arbeiten.
3. Umgekehrt: Android als Admin (Einstellungen > Familie und Cloud) sendet ein Paket, iPhone holt es.

## Wenn etwas fehlt
Auf dem Tablet unter Mehr > Joker steht der Status zum Senden. Unter Einstellungen > Technik gibt es den Verbindungstest.
Schick mir die Meldung, dann finde ich die Ursache.
