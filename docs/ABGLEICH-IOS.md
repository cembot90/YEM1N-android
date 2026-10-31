# Abgleich iOS und Android

Stand 31.10.2026. Grundlage: Dateien und Texte der iOS-App (`cembot90/YEM1N`) gegen den Android-Code. Gezählt wird, was ein Kind oder Elternteil in der App tut, nicht die Technik darunter.

## Gleichauf
Üben mit allen Aufgabenarten, Fortschritt, Münzen, Serie, Tagesziel, Wochenziel, Erfolge, Fehlerheft, Probearbeit, Training, Generator, Lernwelt (HE, BY, NRW), Sprachen mit Karteikasten, Vorschule mit Englisch und Türkisch, fünf Spiele, Haustier, natürliche Stimmen, Erklär-Freund mit Olli, Rechenweg als Bild, KI-Satz, Joker mit Nachschub, Ergebnisse mit Details, Wochenbericht, Radar, Duell, Familie per QR-Code koppeln (Protokoll 2) mit Eltern-PIN, Einwilligung, Klassencode (Empfangen und Admin), Mitteilungen, Lernzeit, Notizblock, Info und Datenschutz.

## Bewusst anders
- **Haustier**: auf Android nur auf dem Gerät. iOS legt es in die Cloud, die Eltern füttern es. Grund: Server-Einträge gehören dem Entwickler, iOS kann sie nicht ändern.
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
| 8 | Pakete der Familie über den Familien-Dienst (Typ `paket`) senden und holen | mittel | fertig 31.10. (Einstellungen > Familie und Cloud, Kind-Geräte holen von selbst) |
| 9 | Absturz beim Start mit Einwilligung (Familie wurde vor dem Anlegen benutzt) | klein | behoben 31.10. |

## Nicht geprüft
Texte der Lernwelt-Themen und Feinheiten der Spiele wurden nicht Zeile für Zeile verglichen. Fällt dir beim Testen ein Unterschied auf, schick ihn mir, ich trage ihn hier ein.
