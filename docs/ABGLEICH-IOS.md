# Abgleich iOS und Android

Stand 23.10.2026. Grundlage: Dateien und Texte der iOS-App (`cembot90/YEM1N`) gegen den Android-Code. Gezählt wird, was ein Kind oder Elternteil in der App tut, nicht die Technik darunter.

## Gleichauf
Üben mit allen Aufgabenarten, Fortschritt, Münzen, Serie, Tagesziel, Wochenziel, Erfolge, Fehlerheft, Probearbeit, Training, Generator, Lernwelt (HE, BY, NRW), Sprachen mit Karteikasten, Vorschule mit Englisch und Türkisch, fünf Spiele, Haustier, natürliche Stimmen, Erklär-Freund mit Olli, Rechenweg als Bild, KI-Satz, Joker mit Nachschub, Ergebnisse mit Details, Wochenbericht, Radar, Duell, Familie per QR-Code koppeln (Protokoll 2) mit Eltern-PIN, Einwilligung, Klassencode (Empfangen und Admin), Mitteilungen, Lernzeit, Notizblock, Info und Datenschutz.

## Bewusst anders
- **Haustier**: auf Android nur auf dem Gerät. iOS legt es in die Cloud, die Eltern füttern es. Grund: Server-Einträge gehören dem Entwickler, iOS kann sie nicht ändern.
- **Cloud-Pakete in der Familie**: iOS verteilt Pakete über CloudKit. Android nutzt dafür den Klassencode (Familien-Pakete kommen mit der iPhone-Umstellung auf Protokoll 2).
- **Haustier in 3D**: Android zeichnet das Tier in 2D.
- **Stift und Handschrift**: seit 27.10. auf beiden Systemen entfernt. Antworten werden getippt.

## Echte Lücken
| # | Lücke | Aufwand | Stand |
|---|---|---|---|
| 1 | Aufgaben vorlesen beim Üben (Lautsprecher-Knopf, automatisch) | klein | fertig 23.10. |
| 2 | Löschfrist 365 Tage für Cloud-Daten | klein | im Server fertig 23.10., Deploy nötig |
| 3 | Bewegung reduzieren (Systemeinstellung beachten) | klein | fertig 23.10. (Animationen entfernen schaltet den Sparmodus ein) |
| 4 | Cloud-Diagnose in den Einstellungen und Anzeige "noch nicht gesendet" | klein | fertig 23.10. (Einstellungen > Technik) |
| 5 | Einrichtungsassistent beim ersten Start | mittel | fertig 23.10. (Familie koppeln liegt unter Einstellungen > Familie) |
| 6 | Mathe-Editor und Vokabel-Editor in der App | groß | fertig 23.10. (Plus > Eigene Arbeit bauen, Sprachen > Eigene Wörter) |
| 7 | Profil-Assistent | mittel | fertig 23.10. (im Einrichtungsassistenten) |

## Übergabe zwischen den Chats

Meistens werden beide Apps in derselben Sitzung zusammen geändert. Hier steht nur, was dabei für eine Seite
offen bleibt oder bewusst nur eine Seite betrifft, egal ob im iOS- oder im Android-Chat entstanden.
Wer die Änderung nachgebaut oder bewusst weggelassen hat, setzt den Stand auf "fertig" oder "bewusst anders"
mit Datum und Commit. Erledigte Zeilen bleiben stehen, damit man den Verlauf sieht.

### Von iOS, für Android
| Datum | iOS-Commit | Was sich geändert hat | Stand |
|---|---|---|---|
| 10.11. | ae8debf / ad2e059 | Design-Prüfung mit Bildschirmfotos, beide Apps, nur auf dem Zweig `design-fable`: Arbeitsanweisung beim Üben groß und weiß, runder Vorlese-Knopf rechts oben in der Aufgabenkarte, Tippflächen mindestens 44 Punkte (iOS `RundKnopf`, `tippziel()` in `Design.swift`) und 48 dp (Android `RundKnopf`, `VorleseKnopf`, `tippText`, `Fortschrittsbalken` in `ui/theme/Glas.kt`), Android-Üben auf einer Glas-Karte wie iOS, Antworten zum Antippen als Glas-Karten, Inhalt auf Tablets höchstens 760 dp breit, größere Schrift beim Üben auf iPad und Tablet, Rosa-Welt aus der Einrichtung gilt auf Android sofort. Bildschirmfotos: iOS Workflow `bildschirmfotos.yml` (Start-Option `-bildschirmfoto`, nur Debug), Android `BildschirmfotoTest` mit `-Pfotos`. Die vier Knöpfe im Kopf des Android-Startbildschirms bleiben aus Platzgründen bei 44 dp | fertig auf `design-fable`, nicht auf `entwicklung`, wartet auf Entscheidung von Cem und Test am Gerät |
| 04.11. | Android | Richtig erst im zweiten Versuch: Android merkt die Aufgabe im Fehlerheft, iOS nur bei Aufgaben, die schon aus dem Fehlerheft kommen (iOS braucht dafür eine Änderung am Datenmodell, vorsichtig wegen Yemins Fortschritt) | offen für iOS, mittel |
| 04.11. | beide | Zweiter Versuch vor der Lösung (iOS `Lob.swift` `ZweiterVersuch`, `MatheUeben.swift`; Android `logik/ZweiterVersuch.kt`, `logik/Runde.kt`). Beide Apps gleichzeitig | fertig auf `entwicklung`, wartet auf Test am Gerät |
| 01.11. | 0764281 / 8e29c54 | Erwachsenen-Tor (Malaufgabe oder PIN) vor Einstellungen, Links, Mail, Teilen. Beide Apps gleichzeitig | fertig auf `entwicklung`, wartet auf Test am Gerät |
| 01.11. | a6cb8d8 / 48b90f1 | Mehrdeutige Aufgaben korrigiert (Großschreiben, I/l, Satzzeichen, Quader, Formen, Porträt, Zählfragen). Beide Apps | fertig auf `entwicklung` |
| 01.11. | 712602d | Android an iOS angeglichen: Sprachen zuerst aus, Englisch immer in der Lernwelt, Tagesziel 2 bis 10 | fertig auf `entwicklung` |
| 01.11. | Prüfung | Angefangene Lernwelt-Runde geht nur auf iOS weiter, Android würfelt neu (`ui/AppViewModel.kt` ~1248) | offen, mittel |
| 01.11. | Prüfung | Android spricht beim Üben kein Lob (iOS: Serien-Lob und Schlusssatz, `MatheUeben.swift` 963, 1031) | offen, klein |
| 01.11. | Prüfung | Vorschul-Lob auf Türkisch und Englisch fehlt auf Android (iOS `Lob.swift`, `LobText`) | offen, mittel |
| 01.11. | Prüfung | Memory bringt auf Android Münzen und schaltet Spiele frei, auf iOS nicht | offen, klein: Entscheidung Cem |
| 01.11. | Prüfung | Sprach-Runden zählen auf Android nicht für Haustier, Tagesziel und Eltern (iOS `Sprachen.swift` 818) | offen, mittel |
| 01.11. | Prüfung | Spiele auf dem Eltern-Gerät zum Ausprobieren gratis nur auf iOS | offen, klein |
| 01.11. | Prüfung | Türkisch und Italienisch als Fächer in der Lernwelt nur auf Android (sind mit dem Sprachen-Schalter versteckt) | bewusst anders, bis Cem entscheidet |
| 27.10. | 122e4f9 | Kind-Gerät schickt beim Beitritt zu einer neuen Familie die eigenen Runden der letzten 365 Tage noch einmal an den Dienst (feste Kennung, kein Doppeln, kurze Pause wegen der Bremse). iOS: `ErgebnisUmzug.swift`, `Cloud.swift`, Tests in `FamilieTests.swift` | Android auf `entwicklung` 31.10. (`logik/ErgebnisUmzug.kt`), wartet auf Test am Gerät. Android merkt sich gesendete Runden erst ab diesem Update |
| 27.10. | 07c1589 | Handschrift entfernt | fertig (Android schon am 27.10.) |

### Von Android, für iOS
| Datum | Android-Commit | Was sich geändert hat | Stand |
|---|---|---|---|
| 28.10. | 73fbe3a | Server: Tageslimit je Familie und Notausschalter gegen unerwartete Kosten. iOS prüfen: zeigt die App eine verständliche Meldung, wenn der Dienst das Limit meldet? | fertig 31.10.: beide Apps haben neue Texte für 429 und 503 |

## Nicht geprüft
Texte der Lernwelt-Themen und Feinheiten der Spiele wurden nicht Zeile für Zeile verglichen. Fällt dir beim Testen ein Unterschied auf, schick ihn mir, ich trage ihn hier ein.
