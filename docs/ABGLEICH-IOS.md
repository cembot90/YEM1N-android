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
| 27.10. | 122e4f9 | Kind-Gerät schickt beim Beitritt zu einer neuen Familie die eigenen Runden der letzten 365 Tage noch einmal an den Dienst (feste Kennung, kein Doppeln, kurze Pause wegen der Bremse). iOS: `ErgebnisUmzug.swift`, `Cloud.swift`, Tests in `FamilieTests.swift` | Android auf `entwicklung` 31.10. (`logik/ErgebnisUmzug.kt`), wartet auf Test am Gerät. Android merkt sich gesendete Runden erst ab diesem Update |
| 27.10. | 07c1589 | Handschrift entfernt | fertig (Android schon am 27.10.) |

### Von Android, für iOS
| Datum | Android-Commit | Was sich geändert hat | Stand |
|---|---|---|---|
| 28.10. | 73fbe3a | Server: Tageslimit je Familie und Notausschalter gegen unerwartete Kosten. iOS prüfen: zeigt die App eine verständliche Meldung, wenn der Dienst das Limit meldet? | fertig 31.10.: beide Apps haben neue Texte für 429 und 503 |

## Nicht geprüft
Texte der Lernwelt-Themen und Feinheiten der Spiele wurden nicht Zeile für Zeile verglichen. Fällt dir beim Testen ein Unterschied auf, schick ihn mir, ich trage ihn hier ein.
