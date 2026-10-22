package de.cemaras.yem1n.logik.lehrplan

import de.cemaras.yem1n.logik.LehrFach
import de.cemaras.yem1n.logik.Lern
import de.cemaras.yem1n.logik.LernAufgabe
import de.cemaras.yem1n.logik.LernThema

// Lernwelt Deutsch: Lesen und verstehen. Port von LehrplanLesen.swift.
// Kurze, selbst geschriebene Texte mit Fragen dazu. Klasse 1 liest zwei Sätze,
// Klasse 4 längere Sachtexte, bei denen man auch zwischen den Zeilen lesen muss.
// Jede Antwort steht im Text oder folgt eindeutig daraus.

/** Frage, richtige Antwort, falsche Antworten */
class LeseFrage(val frage: String, val richtig: String, val falsch: List<String>)

class LeseText(val titel: String, val text: String, val fragen: List<LeseFrage>)

object LehrplanLesen {
    private val D = LehrFach.DEUTSCH

    val themen: List<LernThema> by lazy {
        listOf(
            LernThema(
                "l1-lesen", D, "Lesen und verstehen", "📗", "Zwei Sätze lesen und eine Frage beantworten", 1,
                tipp = "Lies den Text zweimal. Die Antwort steht im Text.", erzeuge = { aufgabe(klasse1) },
            ),
            LernThema(
                "l2-lesen", D, "Lesen und verstehen", "📘", "Kleine Geschichten", 2,
                tipp = "Lies erst die Frage, dann suche die Stelle im Text.", erzeuge = { aufgabe(klasse2) },
            ),
            LernThema(
                "l3-lesen", D, "Lesen und verstehen", "📙", "Geschichten und Sachtexte", 3,
                tipp = "Lies erst die Frage, dann suche die Stelle im Text.", erzeuge = { aufgabe(klasse3) },
            ),
            LernThema(
                "l4-lesen", D, "Lesen und verstehen", "📕", "Sachtexte und zwischen den Zeilen lesen", 4,
                tipp = "Manche Antwort steht nicht wörtlich da. Überlege, was der Text bedeutet.",
                erzeuge = { aufgabe(klasse4) },
            ),
        )
    }

    fun aufgabe(texte: List<LeseText>): LernAufgabe {
        val t = Lern.eins(texte)
        val f = Lern.eins(t.fragen)
        return Lern.wahl(
            frage = f.frage, rechnung = t.titel + "\n\n" + t.text, richtig = f.richtig, falsch = f.falsch,
            erklaerung = "Richtig ist: ${f.richtig}.",
        )
    }

    private fun f(frage: String, richtig: String, vararg falsch: String) = LeseFrage(frage, richtig, falsch.toList())

    val klasse1: List<LeseText> = listOf(
        LeseText(
            "Der Ball", "Tim hat einen roten Ball. Er spielt im Garten.",
            listOf(f("Welche Farbe hat der Ball?", "rot", "blau", "gelb"), f("Wo spielt Tim?", "im Garten", "in der Schule", "im Bad")),
        ),
        LeseText(
            "Mimi", "Die Katze heißt Mimi. Sie trinkt Milch.",
            listOf(f("Wie heißt die Katze?", "Mimi", "Lilo", "Bello"), f("Was trinkt Mimi?", "Milch", "Saft", "Tee")),
        ),
        LeseText(
            "Der Kuchen", "Oma backt einen Kuchen. Der Kuchen ist süß.",
            listOf(f("Wer backt?", "Oma", "Opa", "Mama"), f("Wie ist der Kuchen?", "süß", "sauer", "salzig")),
        ),
        LeseText(
            "Der Regenbogen", "Am Himmel ist ein Regenbogen. Er hat viele Farben.",
            listOf(
                f("Was ist am Himmel?", "ein Regenbogen", "ein Flugzeug", "ein Vogel"),
                f("Wie viele Farben hat der Regenbogen?", "viele", "nur eine", "zwei"),
            ),
        ),
        LeseText(
            "Die Hasen", "Ben hat zwei Hasen. Sie fressen Möhren.",
            listOf(f("Wie viele Hasen hat Ben?", "zwei", "drei", "einen"), f("Was fressen die Hasen?", "Möhren", "Äpfel", "Nüsse")),
        ),
        LeseText(
            "Der Apfel", "Ida isst einen Apfel. Der Apfel ist grün.",
            listOf(
                f("Was isst Ida?", "einen Apfel", "eine Birne", "eine Banane"),
                f("Welche Farbe hat der Apfel?", "grün", "rot", "gelb"),
            ),
        ),
    )

    val klasse2: List<LeseText> = listOf(
        LeseText(
            "Bruno im Park",
            "Lena geht mit ihrem Hund Bruno in den Park. Bruno holt gern Stöcke. Plötzlich sieht er ein Eichhörnchen und rennt los. Lena muss lachen.",
            listOf(
                f("Wie heißt der Hund?", "Bruno", "Max", "Rex"),
                f("Was holt Bruno gern?", "Stöcke", "Bälle", "Knochen"),
                f("Was sieht Bruno plötzlich?", "ein Eichhörnchen", "eine Katze", "einen Vogel"),
            ),
        ),
        LeseText(
            "Ali ist krank",
            "Heute ist Ali krank. Er hat Halsschmerzen und bleibt im Bett. Mama bringt ihm einen warmen Tee mit Honig. Am Nachmittag geht es ihm schon besser.",
            listOf(
                f("Warum bleibt Ali im Bett?", "Er ist krank.", "Er ist müde.", "Es ist Sonntag."),
                f("Was bringt Mama?", "Tee mit Honig", "Kakao", "Suppe"),
                f("Wann geht es Ali besser?", "am Nachmittag", "am Morgen", "am nächsten Tag"),
            ),
        ),
        LeseText(
            "Bunte Blätter",
            "Im Herbst fallen die Blätter von den Bäumen. Sie werden gelb, rot und braun. Mia sammelt die schönsten Blätter. Daraus bastelt sie ein Bild.",
            listOf(
                f("Wann fallen die Blätter?", "im Herbst", "im Sommer", "im Frühling"),
                f("Was bastelt Mia aus den Blättern?", "ein Bild", "eine Kette", "einen Hut"),
            ),
        ),
        LeseText(
            "Die Sandburg",
            "Tom und Sara bauen im Sand eine große Burg. Tom holt Wasser mit dem Eimer. Sara baut einen Turm. Am Ende stecken sie eine Muschel auf die Spitze.",
            listOf(
                f("Wer baut den Turm?", "Sara", "Tom", "Mama"),
                f("Womit holt Tom Wasser?", "mit dem Eimer", "mit der Schaufel", "mit den Händen"),
                f("Was stecken sie auf die Spitze?", "eine Muschel", "eine Fahne", "einen Stein"),
            ),
        ),
    )

    val klasse3: List<LeseText> = listOf(
        LeseText(
            "Der Igel",
            "Igel sind kleine Tiere mit vielen Stacheln. Ein Igel hat etwa 7000 Stacheln. Wenn er Angst hat, rollt er sich zu einer Kugel zusammen. Igel fressen am liebsten Käfer, Würmer und Schnecken. Im Winter halten sie Winterschlaf.",
            listOf(
                f("Wie viele Stacheln hat ein Igel etwa?", "etwa 7000", "etwa 700", "etwa 70"),
                f("Was macht ein Igel, wenn er Angst hat?", "Er rollt sich zusammen.", "Er rennt weg.", "Er bellt laut."),
                f("Was macht der Igel im Winter?", "Er hält Winterschlaf.", "Er fliegt in den Süden.", "Er sammelt Äpfel."),
                f("Was frisst der Igel am liebsten?", "Käfer, Würmer und Schnecken", "Äpfel und Birnen", "Gras und Blätter"),
            ),
        ),
        LeseText(
            "Allein zum Bäcker",
            "Jonas geht zum ersten Mal allein zum Bäcker. Mama gibt ihm fünf Euro. Er kauft vier Brötchen und eine Brezel. An der Kasse bekommt er Geld zurück. Stolz läuft er nach Hause.",
            listOf(
                f("Was kauft Jonas?", "vier Brötchen und eine Brezel", "ein Brot", "einen Kuchen"),
                f("Wie viel Geld gibt Mama ihm?", "fünf Euro", "zehn Euro", "zwei Euro"),
                f("Wie fühlt sich Jonas am Ende?", "stolz", "traurig", "wütend"),
            ),
        ),
        LeseText(
            "Bienen",
            "Bienen leben in einem Bienenstock. Dort wohnen viele tausend Bienen und eine Königin. Die Arbeiterinnen fliegen von Blüte zu Blüte und sammeln Nektar. Daraus machen sie Honig. Dabei bestäuben sie die Blüten, damit Früchte wachsen können.",
            listOf(
                f("Wer lebt mit den Bienen im Stock?", "eine Königin", "ein König", "ein Bär"),
                f("Was sammeln die Arbeiterinnen?", "Nektar", "Wasser", "Sand"),
                f("Was machen die Bienen aus dem Nektar?", "Honig", "Zucker", "Milch"),
            ),
        ),
        LeseText(
            "Das letzte Tor",
            "Beim Fußballspiel steht es 2 zu 2. Noch eine Minute ist zu spielen. Ida bekommt den Ball und läuft auf das Tor zu. Sie schießt, und der Ball landet im Netz. Ihre Mannschaft gewinnt 3 zu 2.",
            listOf(
                f("Wie steht es kurz vor Schluss?", "2 zu 2", "1 zu 0", "3 zu 1"),
                f("Wer schießt das letzte Tor?", "Ida", "der Torwart", "der Trainer"),
                f("Wie endet das Spiel?", "3 zu 2", "2 zu 2", "2 zu 3"),
            ),
        ),
    )

    val klasse4: List<LeseText> = listOf(
        LeseText(
            "Der Mond",
            "Der Mond ist der Begleiter der Erde. Er leuchtet nicht selbst, sondern wird von der Sonne angestrahlt. Deshalb sehen wir ihn mal als schmale Sichel und mal als volle Scheibe. Für eine Runde um die Erde braucht er ungefähr einen Monat. Im Jahr 1969 betraten zum ersten Mal Menschen den Mond.",
            listOf(
                f("Warum leuchtet der Mond?", "Die Sonne strahlt ihn an.", "Er hat eigene Lampen.", "Er brennt wie ein Feuer."),
                f("Wie lange braucht der Mond für eine Runde um die Erde?", "ungefähr einen Monat", "einen Tag", "ein Jahr"),
                f("Wann waren zum ersten Mal Menschen auf dem Mond?", "1969", "1869", "2009"),
            ),
        ),
        LeseText(
            "Alles weiß",
            "Als Luis morgens aus dem Fenster schaut, ist alles weiß. Schnell zieht er seine dicke Jacke, Mütze und Handschuhe an. Draußen holt er seinen Schlitten aus dem Keller. Mit seiner Schwester fährt er den Hügel hinunter, bis es dunkel wird. Abends sind beide müde, aber glücklich.",
            listOf(
                f("Warum ist draußen alles weiß?", "Es hat geschneit.", "Es ist neblig.", "Jemand hat alles angemalt."),
                f("Was holt Luis aus dem Keller?", "seinen Schlitten", "sein Fahrrad", "seinen Ball"),
                f("Wie fühlen sich die Kinder am Abend?", "müde und glücklich", "traurig", "wütend und hungrig"),
            ),
        ),
        LeseText(
            "Wasser",
            "Wasser gibt es in drei Formen. Als Eis ist es fest, im Glas ist es flüssig und als Dampf ist es gasförmig. Wasser gefriert bei 0 Grad Celsius zu Eis. Bei 100 Grad kocht es und wird zu Dampf. Ohne Wasser könnten Menschen, Tiere und Pflanzen nicht leben.",
            listOf(
                f("Bei wie viel Grad gefriert Wasser?", "bei 0 Grad", "bei 10 Grad", "bei 100 Grad"),
                f("Wie nennt man Wasser, wenn es gasförmig ist?", "Dampf", "Eis", "Regen"),
                f("In wie vielen Formen gibt es Wasser?", "in drei", "in zwei", "in vier"),
            ),
        ),
        LeseText(
            "Der Neue",
            "In der Pause sieht Nele, dass Emil allein auf der Bank sitzt. Er ist neu in der Klasse und kennt noch niemanden. Nele setzt sich zu ihm und fragt, ob er mitspielen möchte. Emil lächelt und nickt. Ab diesem Tag spielen die beiden jede Pause zusammen.",
            listOf(
                f("Warum sitzt Emil allein?", "Er ist neu und kennt noch niemanden.", "Er hat sich verletzt.", "Er möchte lieber lesen."),
                f("Was macht Nele?", "Sie fragt ihn, ob er mitspielt.", "Sie lacht ihn aus.", "Sie holt die Lehrerin."),
                f("Welches Wort passt zu Nele?", "freundlich", "frech", "ängstlich"),
            ),
        ),
    )
}
