package de.cemaras.yem1n.daten

/** Texte für Anleitung, Datenschutz und Impressum. Der Anbieter und die Seiten sind wie bei iOS. */
object InfoTexte {
    const val ANBIETER = "Cem Aras, Schulweg 20, 65618 Selters (Taunus)"
    const val TELEFON = "+49 172 7579888"
    const val MAIL = "cembot@icloud.com"
    const val SEITE = "https://cembot90.github.io/YEM1N/"
    const val ANLEITUNG = "https://cembot90.github.io/YEM1N/anleitung.html"
    const val HOST = "cembot90.github.io"

    /** Anleitung in der gewählten Farbwelt: Die Seite liest "farbwelt" aus der Adresse. */
    fun anleitungAdresse(rosa: Boolean) = ANLEITUNG + if (rosa) "?farbwelt=rosa" else ""

    /** Kurzfassung für ohne Internet. Sie beschreibt nur, was es in der Android-App schon gibt. */
    val kurzAnleitung: List<Pair<String, String>> = listOf(
        "Üben" to "Auf der Startseite eine Arbeit wählen, dann eine Übung. Antwort eintippen und auf Prüfen tippen. Nach jeder Übung gibt es bis zu drei Sterne.",
        "Hilfen" to "Mit Tipp anzeigen kommt ein Hinweis. Mit Lösung zeigen sieht das Kind die Lösung, sie zählt aber weder als richtig noch als falsch und bringt keine Münzen.",
        "Zahlenmauer" to "Die Steine werden von unten nach oben und von links nach rechts gefüllt. Jeder Stein ist die Summe der zwei Steine darunter.",
        "Fehlerheft" to "Falsche Aufgaben kommen ins Fehlerheft. Ein Fehler ist erst gemeistert, wenn er zweimal richtig war: Nach der ersten richtigen Antwort kommt er nach 3 Tagen noch einmal, bei einer falschen schon morgen. Auf dem Knopf steht, wie viele Fehler dran sind.",
        "Probearbeit" to "Zwanzig gemischte Aufgaben mit Uhr (30 Minuten), am Ende gibt es eine Note. Jede Probearbeit sieht anders aus.",
        "Training" to "Neue Aufgaben zu den zwei Bereichen, die am schwersten fallen.",
        "Münzen" to "Für jede gleich richtig gelöste Aufgabe gibt es eine Münze, für zwei Sterne zwei und für drei Sterne fünf extra. Angeschaute Lösungen bringen nichts.",
        "Serie und Wochenziel" to "Ein freier Tag pro Woche ist erlaubt, ohne dass die Serie endet. Das Wochenziel sind fünf Tage mal Tagesziel an Runden. Wer es schafft, holt sich unter Erfolge 10 Münzen ab.",
        "Abzeichen" to "Für Serien, Sterne und fehlerfreie Runden gibt es Abzeichen. Auf der Startseite auf die Zeile mit Serie und Münzen tippen zeigt sie alle. Ein neues Abzeichen erscheint gleich nach der Übung.",
        "Neue Arbeiten" to "Arbeit einfügen: das JSON aus Claude kopieren und einfügen. Datei öffnen: eine JSON-Datei vom Gerät wählen. Man kann auch aus einer anderen App teilen oder eine JSON-Datei antippen und YEM1N wählen. Prüfe ein paar Antworten, bevor dein Kind übt.",
        "Eigene Arbeiten" to "Plus > Eigene Arbeit bauen: eine Arbeit selbst zusammenstellen. Die App rechnet die Lösungen aus und prüft sie vor dem Speichern. Eigene Vokabeln gibt es unter Sprachen > Eigene Wörter.",
        "Einstellungen" to "Mit dem Plus oben rechts und dann Einstellungen. Sie sind in Gruppen sortiert: Ich (Name, Farbwelt, Tagesziel, Lernzeit), Bereiche, Spiele und Haustier, Vorlesen und Stimmen, Familie und Cloud, Info und Arbeiten. Gelöschte Arbeiten lassen sich wieder einfügen.",
        "Lernwelt und Sprachen" to "Die Lernwelt hat Aufgaben nach dem Lehrplan für Hessen, Bayern und Nordrhein-Westfalen, Klassen 1 bis 4. Unter Sprachen gibt es Karten, Paare, Tippen und Fragen in Englisch, Türkisch und Italienisch. Gelernte Wörter kommen nach dem Karteikasten-Prinzip wieder.",
        "Haustier, Spiele und Vorschule" to "Das Haustier wächst mit jeder Runde, die Spiele kosten Münzen und haben ein Tageslimit. Die Vorschule für die Kleinen lässt sich in den Einstellungen unter Bereiche ausblenden.",
        "Erklären" to "Nach einer falschen Antwort erklärt die Eule auf Wunsch, was schiefgelaufen ist, und zeigt den Rechenweg als Bild. Auf Geräten mit Gemini Nano formuliert die KI auf dem Gerät den Satz neu, sonst gibt es feste Sätze.",
        "Familie, Joker und Klassencode" to "Mit einem Familiencode und der Einwilligung der Eltern gibt es Joker (Fragen ans Elterngerät), Ergebnisse für die Eltern und Duelle. Mit einem Klassencode kommen Aufgabenpakete der Klasse von selbst, wenn sie vom Admin der Klasse unterschrieben sind. Beides ist freiwillig und steht unter Einstellungen > Familie und Cloud.",
        "Hilfe" to "Die ausführliche Anleitung öffnet sich, sobald das Gerät online ist. Unter Info kannst du Feedback an den Admin schicken.",
    )

    val impressum: List<Pair<String, String>> = listOf(
        "Angaben nach § 5 DDG" to "$ANBIETER, Deutschland.",
        "Kontakt" to "Telefon: $TELEFON\nE-Mail: $MAIL",
        "Hinweis" to "YEM1N wird kostenlos und ohne Werbung angeboten.",
        "Grafiken" to "Die Spielgrafiken stammen von Kenney (kenney.nl) und stehen unter der Lizenz CC0.",
        "Haftung für Links" to "Die App verlinkt auf die Anleitung und die Datenschutzerklärung auf GitHub Pages. Für fremde Inhalte, auf die verlinkt wird, sind die jeweiligen Anbieter verantwortlich.",
    )

    val beta: List<Pair<String, String>> = listOf(
        "Beta-Version" to "YEM1N wird laufend weiterentwickelt. Funktionen können sich ändern, Fehler sind möglich. Wenn dir etwas auffällt, schreib kurz unter Info > Feedback an den Admin.",
        "Aufgaben und Lösungen" to "Aufgaben und Lösungen werden sorgfältig erstellt und geprüft, können aber Fehler enthalten. Die App ersetzt keinen Unterricht und kein Lehrwerk. Aufgabenpakete der Eltern stammen von den Personen, die sie erstellt haben.",
        "Haftung" to "Für Schäden haftet der Anbieter nur bei Vorsatz und grober Fahrlässigkeit. Bei der Verletzung von Leben, Körper oder Gesundheit und nach zwingenden gesetzlichen Vorschriften haftet er nach dem Gesetz.",
        "Datenschutz" to "Wie die App mit Daten umgeht, steht unter Info > Datenschutzhinweise.",
    )

    /** Gilt für den heutigen Stand der Android-App: Üben bleibt auf dem Gerät, die Familien-Cloud ist freiwillig. */
    val datenschutz: List<Pair<String, String>> = listOf(
        "Verantwortlicher" to "$ANBIETER. Telefon: $TELEFON, E-Mail: $MAIL.",
        "Auf deinem Gerät" to "YEM1N speichert Arbeiten, Aufgaben, Ergebnisse und Einstellungen (zum Beispiel Name des Kindes, Farbwelt, Tagesziel, Münzen, Fehlerheft, Haustier, Lernstand der Sprachen und Abzeichen) auf diesem Gerät. Das bleibt dort, bis du die App löschst oder ihre Daten in den Android-Einstellungen entfernst. Der Name ist freiwillig. Ein Vorname oder Spitzname reicht.",
        "Ohne Familiencode" to "Solange du keinen Familiencode und keinen Klassencode einträgst, schickt die App keine Daten an mich oder andere. Es gibt kein Benutzerkonto, keine Werbung und keine Analysedienste. Es werden keine Fotos, Kontakte, Standortdaten oder Mikrofonaufnahmen erhoben.",
        "Familiencode (freiwillig, mit Einwilligung)" to "Wenn die Eltern einwilligen und einen Familiencode eintragen, nutzt die App eine gemeinsame Cloud für die Geräte der Familie. Dann werden gesendet: der Familiencode, Fragen und Antworten der Joker (Freitext), Daumen, Duelle sowie die Ergebnisse jeder Runde (Klasse, Fach, Arbeit, Übung, richtige und falsche Antworten, Sterne, Name des Kindes, Zeitpunkt). Die Daten liegen in einer öffentlich lesbaren Datenbank und sind dort nicht zusätzlich verschlüsselt. Der Familiencode ist der einzige Schutz: Wer ihn kennt, kann die Daten der Familie lesen. Technisch versierte Personen können die Datenbank außerdem ohne Code durchsuchen. Gib den Code deshalb nur an Familienmitglieder weiter und schreibe in Fragen keine persönlichen Daten.",
        "Wer die Daten verarbeitet" to "Die Daten liegen in Apple CloudKit (Apple Inc., USA, öffentliche Datenbank des Containers dieser App). Geschrieben werden sie über einen kleinen Vermittler-Server bei Cloudflare (Cloudflare, Inc., USA). Dort wird nur der Familiencode mit Rolle und Push-Kennung des Geräts zwischengespeichert. Mitteilungen auf Android laufen über Google Firebase Cloud Messaging (Google, USA). Google bekommt dafür die Push-Kennung des Geräts und den Text der Mitteilung. Der Text kann den Namen des Kindes und den Anfang einer Frage enthalten.",
        "Klassencode (freiwillig)" to "Mit einem Klassencode holt die App Aufgabenpakete aus CloudKit. Dabei geht nur der Klassencode über das Netz. Pakete werden nur angenommen, wenn sie mit dem Schlüssel des Admins der Klasse unterschrieben sind. Wer eine Klasse anlegt, schickt den Klassencode, den öffentlichen Schlüssel und die Pakete an die Cloud. Der private Schlüssel bleibt auf dem Gerät.",
        "KI auf dem Gerät" to "Wenn dein Gerät Gemini Nano hat, formuliert die Eule Erklärungen mit der KI auf dem Gerät neu. Dafür geht nichts ins Internet. Das Modell lädt Google bei der ersten Nutzung auf das Gerät.",
        "Internet" to "Wenn du die ausführliche Anleitung, die Datenschutzerklärung oder das Impressum öffnest, lädt die App eine Seite von GitHub Pages (GitHub, Inc., USA). GitHub sieht dabei wie bei jeder Webseite die IP-Adresse und technische Angaben zur Verbindung, aber keine Daten aus der App.",
        "Handschrift (freiwillig)" to "Wenn das Kind mit dem Stift oder Finger schreibt, lädt das Gerät einmal die Handschrift-Erkennung von Google (ML Kit, etwa 20 MB). Danach liest das Gerät die Zahlen ohne Internet. Die Schrift wird weder gespeichert noch verschickt.",
        "Natürliche Stimmen" to "Nur wenn du in den Einstellungen eine natürliche Stimme lädst, holt die App ein Stimmenpaket von GitHub (GitHub, Inc., USA). GitHub sieht dabei die IP-Adresse und technische Angaben zur Verbindung, aber keine Daten aus der App. Danach liest die App ohne Internet vor.",
        "Dateien und Teilen" to "Wenn du eine Arbeit aus einer Datei öffnest oder aus einer anderen App teilst, liest die App nur diese eine Datei oder diesen Text. Die App hat keinen Zugriff auf andere Dateien.",
        "Zweck und Rechtsgrundlage" to "Üben, Fortschritt und Belohnungen auf dem Gerät: Art. 6 Abs. 1 lit. b DSGVO. Familiencode, Joker, Ergebnisse, Duelle und Klassencode: Einwilligung der Eltern nach Art. 6 Abs. 1 lit. a DSGVO. Die Einwilligung kannst du jederzeit in den Einstellungen unter Familie und Cloud widerrufen. Danach sendet die App nichts mehr.",
        "Löschen" to "Daten auf dem Gerät löschst du, indem du die App deinstallierst oder in den Android-Einstellungen unter Apps > YEM1N > Speicher die Daten löschst. Einzelne Arbeiten löschst du in der App unter Einstellungen. Einträge in der Cloud löscht der Server nach 12 Monaten selbst. Früher lösche ich sie auf Anfrage. Schreibe dazu an die oben genannte E-Mail-Adresse und nenne deinen Familiencode.",
        "Deine Rechte" to "Du hast das Recht auf Auskunft, Berichtigung, Löschung, Einschränkung der Verarbeitung, Datenübertragbarkeit und Widerspruch sowie das Recht, eine Einwilligung jederzeit zu widerrufen. Schreibe dazu an die oben genannte E-Mail-Adresse. Du kannst dich außerdem bei einer Datenschutzaufsichtsbehörde beschweren, zum Beispiel beim Hessischen Beauftragten für Datenschutz und Informationsfreiheit in Wiesbaden.",
        "Kinder" to "Die App richtet sich an Eltern, die sie gemeinsam mit ihren Kindern nutzen. Eltern richten Name, Arbeiten, Familiencode und Klassencode ein und willigen in die Cloud ein.",
    )
}
