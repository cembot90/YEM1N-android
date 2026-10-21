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
        "Einstellungen" to "Mit dem Zahnrad oben rechts: Name des Kindes (dann lobt die App mit Namen), Farbwelt Blau oder Rosa, Tagesziel und Arbeiten löschen. Gelöschte Arbeiten lassen sich wieder einfügen.",
        "Noch nicht dabei" to "Haustier, Spiele, Joker, Vorlesen und die Cloud für Eltern und Kind gibt es in der iOS-App und kommen später auf Android.",
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

    /** Gilt für den heutigen Stand der Android-App: Alles bleibt auf dem Gerät. */
    val datenschutz: List<Pair<String, String>> = listOf(
        "Verantwortlicher" to "$ANBIETER. Telefon: $TELEFON, E-Mail: $MAIL.",
        "Auf deinem Gerät" to "YEM1N speichert Arbeiten, Aufgaben, Ergebnisse und Einstellungen (zum Beispiel Name des Kindes, Farbwelt, Tagesziel, Münzen, Fehlerheft und Abzeichen) auf diesem Gerät. Das bleibt dort, bis du die App löschst oder ihre Daten in den Android-Einstellungen entfernst. Der Name ist freiwillig. Ein Vorname oder Spitzname reicht.",
        "Keine Cloud" to "Die Android-App schickt zurzeit keine Daten an mich oder andere. Es gibt kein Benutzerkonto, keine Werbung und keine Analysedienste. Es werden keine Fotos, Kontakte, Standortdaten oder Mikrofonaufnahmen erhoben.",
        "Internet" to "Nur wenn du die ausführliche Anleitung, die Datenschutzerklärung oder das Impressum öffnest, lädt die App eine Seite von GitHub Pages (GitHub, Inc., USA). GitHub sieht dabei wie bei jeder Webseite die IP-Adresse und technische Angaben zur Verbindung, aber keine Daten aus der App.",
        "Natürliche Stimmen" to "Nur wenn du in den Einstellungen eine natürliche Stimme lädst, holt die App ein Stimmenpaket von GitHub (GitHub, Inc., USA). GitHub sieht dabei die IP-Adresse und technische Angaben zur Verbindung, aber keine Daten aus der App. Danach liest die App ohne Internet vor.",
        "Dateien und Teilen" to "Wenn du eine Arbeit aus einer Datei öffnest oder aus einer anderen App teilst, liest die App nur diese eine Datei oder diesen Text. Die App hat keinen Zugriff auf andere Dateien.",
        "Zweck und Rechtsgrundlage" to "Die Daten werden nur verarbeitet, damit die App funktioniert: Üben, Fortschritt und Belohnungen. Rechtsgrundlage ist Art. 6 Abs. 1 lit. b DSGVO.",
        "Löschen" to "Alle Daten liegen nur auf diesem Gerät. Du löschst sie, indem du die App deinstallierst oder in den Android-Einstellungen unter Apps > YEM1N > Speicher die Daten löschst. Einzelne Arbeiten löschst du in der App unter Einstellungen.",
        "Deine Rechte" to "Du hast das Recht auf Auskunft, Berichtigung, Löschung, Einschränkung der Verarbeitung, Datenübertragbarkeit und Widerspruch sowie das Recht, eine Einwilligung jederzeit zu widerrufen. Schreibe dazu an die oben genannte E-Mail-Adresse. Du kannst dich außerdem bei einer Datenschutzaufsichtsbehörde beschweren, zum Beispiel beim Hessischen Beauftragten für Datenschutz und Informationsfreiheit in Wiesbaden.",
        "Kinder" to "Die App richtet sich an Eltern, die sie gemeinsam mit ihren Kindern nutzen. Eltern richten Name und Arbeiten ein.",
    )
}
