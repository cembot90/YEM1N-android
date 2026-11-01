# Testplan: iOS und Android gegeneinander

Stand 01.11.2026, Familien-Dienst (Protokoll 2). Im Code sind Feldnamen, Duell-Format und die Unterschriften auf beiden
Seiten gleich und mit denselben Prüfwerten getestet (`cloud/testvektoren-v2.json`). Das ersetzt keinen Test mit echten Geräten.

**Geräte:** iPhone (Eltern), Android-Tablet (Kind Yemin). Für Teil 6 zusätzlich ein zweites Kind-Gerät oder das iPhone als Kind.
**Vorher:** Beide Apps auf dem neuesten Stand (TestFlight und `entwicklung` aus Android Studio). Auf beiden Geräten stimmt die Uhrzeit
(automatisch), sonst lehnt der Dienst Anfragen ab.

Jeden Punkt abhaken. Wenn etwas nicht klappt: Bildschirmfoto der Meldung machen und unter Einstellungen > Technik den
Verbindungstest laufen lassen. Beides an Claude schicken.

## 1. Familie anlegen und koppeln
- [ ] iPhone: Einstellungen > Familie, Einwilligung geben, Gerätename "Papa", **Neue Familie anlegen**, Eltern-PIN festlegen.
- [ ] iPhone: unter Familie **Kind-Gerät** antippen, QR-Code erscheint.
- [ ] Android: Einrichtung als Kind "Yemin", dann Einstellungen > Familie und Cloud, Einwilligung, **QR-Code scannen**.
- [ ] Android zeigt einen Prüfcode und wartet. iPhone zeigt das Gerät mit demselben Prüfcode. Codes vergleichen, **Erlauben**.
- [ ] Android: Familie zeigt "gehört zur Familie (Kind)". Einstellungen > Technik > Verbindungstest: alles grün.
- [ ] iPhone: Unter den Kindern steht "Yemin", noch ohne Runde.
- [ ] Gegenprobe: Den gleichen QR-Code ein zweites Mal scannen. Es muss eine Meldung kommen (QR gilt nur einmal).

## 2. Eltern-PIN
- [ ] iPhone: App schließen und öffnen, Familie antippen. Die PIN wird verlangt.
- [ ] Dreimal falsche PIN: Wartezeit erscheint, Text ohne Fachbegriffe.
- [ ] Android (Kind): Familien-Einstellungen sind hinter der PIN der Eltern (oder für das Kind gesperrt).

## 3. Ergebnisse
- [ ] Android: eine Runde Mathe spielen, eine mit "Lösung zeigen". Joker-Seite zeigt "Letzte Runde an die Eltern gesendet."
- [ ] iPhone: Ergebnisse neu laden. Beide Runden sind da, die Runde mit "Lösung zeigen" zählt nicht als richtig.
- [ ] iPhone: Details einer Runde öffnen, Wochenbericht und Radar zeigen die Runde.
- [ ] Flugmodus auf Android an, eine Runde spielen. Technik zeigt "Noch nicht gesendet: 1". Flugmodus aus, App öffnen:
      die Runde kommt nach, iPhone zeigt sie.

## 4. Joker
- [ ] Android: bei einer Aufgabe Joker, Frage an die Eltern schicken.
- [ ] iPhone: Mitteilung kommt (ohne Namen und Inhalt), Frage ist in der App. Antwort mit Emoji senden.
- [ ] Android: Antwort erscheint, Daumen geben. iPhone: Daumen ist sichtbar.
- [ ] Joker aufbrauchen, Nachschub erbitten. iPhone: Mitteilung, Freigabe. Android: Joker wieder da.

## 5. Duell
- [ ] iPhone fordert heraus (Klasse 3). Android: Herausforderung erscheint, annehmen und spielen.
- [ ] iPhone: Ergebnis sichtbar, Sieger stimmt.
- [ ] Umgekehrt: Android fordert heraus, iPhone nimmt an.

## 6. Neue Familie (Runden ziehen mit um)
- [ ] Android: ein paar Runden spielen, alle gesendet.
- [ ] iPhone: Familie löschen (Nachfrage erscheint), neue Familie anlegen, Android neu koppeln und erlauben.
- [ ] iPhone: Nach ein bis zwei Minuten sind die Runden von vorher wieder da (Android merkt sich Runden ab dem Update vom 31.10.).
- [ ] Android: Sterne, Münzen und Haustier sind unverändert.
- [ ] Dasselbe mit dem iPhone als Kind-Gerät: Dort kommen alle Runden der letzten 365 Tage mit.

## 7. Klassencode
- [ ] iPhone legt als Admin eine Klasse an und sendet ein Paket.
- [ ] Android: Klassencode eingeben, Paket holen, die Unterschrift ist gültig, das Paket erscheint unter den Arbeiten.
- [ ] Umgekehrt: Android als Admin sendet ein Paket, iPhone holt es.

## 8. Störungen (optional, mit Cem am Cloudflare-Dashboard)
- [ ] Im Dashboard des Workers die Variable `NOTAUS = 1` setzen. Beide Apps zeigen beim Senden "Der Familien-Dienst macht gerade
      Pause ...". Runden bleiben gemerkt. Variable wieder entfernen, die Runden kommen nach.

## 9. Gerät entfernen
- [ ] iPhone: Android-Gerät in der Geräteliste entfernen (Nachfrage erscheint).
- [ ] Android: Familie zeigt, dass das Gerät nicht mehr dazugehört. Übungen, Sterne und Haustier sind noch da.
