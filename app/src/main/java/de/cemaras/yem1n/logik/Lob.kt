package de.cemaras.yem1n.logik

import kotlin.random.Random

/** Sätze zum Loben, wie in der iOS-App (dort zusätzlich Türkisch und Englisch für die Vorschule). */
object Lob {

    private val richtig = listOf(
        "Super!", "Toll gemacht!", "Richtig!", "Prima!", "Klasse!", "Spitze!", "Genau!", "Stark!",
        "Wunderbar!", "Sehr gut!", "Bravo!", "Das stimmt!", "Perfekt!", "Ganz genau!", "Volltreffer!",
        "Du bist ein Profi!", "Wow, richtig!", "Das hast du super gemacht!", "Treffer!", "Du hast es!",
        "Klasse gemacht!", "Richtig gut!", "Juhu, richtig!", "Gut aufgepasst!",
    )

    private val mitName = listOf(
        "Super, {name}!", "Toll, {name}!", "Genau so, {name}!", "{name}, du bist spitze!",
        "Richtig, {name}!", "Klasse gemacht, {name}!", "Das kannst du, {name}!", "Wow, {name}!",
    )

    private val nachFehler = listOf(
        "Jetzt hat es geklappt!", "Siehst du, du schaffst das!", "Nicht aufgegeben. Stark!",
        "Genau, jetzt stimmt es!", "Gut nachgedacht!", "Dranbleiben lohnt sich!",
        "Beim zweiten Mal richtig. Prima!",
    )

    private val daneben = listOf(
        "Noch nicht richtig.", "Knapp daneben.", "Leider nicht.", "Fast.", "Das war es nicht.",
        "Hoppla, nicht ganz.", "Noch nicht ganz.", "Nicht schlimm, weiter geht es.",
    )

    private val endePerfekt = listOf(
        "Alles richtig! Du bist ein Star!", "Keinen einzigen Fehler. Wahnsinn!", "Perfekte Runde, {name}!",
        "Alles geschafft und alles richtig. Super!", "Volle Punktzahl! Darauf kannst du stolz sein.",
    )

    private val endeGut = listOf(
        "Das hast du toll gemacht!", "Starke Runde, {name}!", "Fast alles richtig. Klasse!",
        "Super geübt! Weiter so.", "Geschafft! Das war richtig gut.",
    )

    private val endeGeuebt = listOf(
        "Geschafft! Jedes Mal üben macht dich besser.", "Gut, dass du drangeblieben bist, {name}!",
        "Runde geschafft! Beim nächsten Mal klappt noch mehr.", "Du hast durchgehalten. Toll!",
        "Üben ist wie Sport. Du wirst immer stärker!",
    )

    /** Lob für eine richtige Antwort. [serie] zählt die richtigen Antworten hintereinander, diese mit. */
    fun fuerRichtig(
        serie: Int, nachFehler: Boolean, name: String, zufall: Random = Random.Default,
    ): String {
        serienLob(serie)?.let { return it }
        if (nachFehler) return waehle(this.nachFehler, "nachFehler", "", zufall)
        val n = vorname(name)
        if (n.isNotEmpty() && zufall.nextDouble() < 0.25) return waehle(mitName, "mitName", n, zufall)
        return waehle(richtig, "richtig", n, zufall)
    }

    /** Ein besonderes Lob bei einer schönen Serie, sonst null. */
    fun serienLob(n: Int): String? = when {
        n == 3 -> "Drei richtig hintereinander!"
        n == 5 -> "Fünf am Stück! Du bist in Fahrt!"
        n == 8 -> "Acht hintereinander! Unglaublich!"
        n == 10 -> "Zehn richtig am Stück! Du bist ein Star!"
        n >= 15 && n % 5 == 0 -> "Schon $n richtig hintereinander! Rekordverdächtig!"
        else -> null
    }

    fun titelDaneben(zufall: Random = Random.Default): String = waehle(daneben, "daneben", "", zufall)

    /** Satz am Ende einer Runde, passend zum Ergebnis. */
    fun fuerEnde(richtig: Int, gesamt: Int, name: String, zufall: Random = Random.Default): String {
        val anteil = if (gesamt > 0) richtig.toDouble() / gesamt else 0.0
        val n = vorname(name)
        return when {
            gesamt > 0 && richtig >= gesamt -> waehle(endePerfekt, "endePerfekt", n, zufall)
            anteil >= 0.7 -> waehle(endeGut, "endeGut", n, zufall)
            else -> waehle(endeGeuebt, "endeGeuebt", n, zufall)
        }
    }

    /** Nur der erste Vorname. "jonas" oder "JONAS" wird "Jonas", gemischte Schreibweisen bleiben. */
    fun vorname(name: String): String {
        val erster = name.trim().split(" ").firstOrNull { it.isNotEmpty() } ?: return ""
        val einheitlich = erster == erster.lowercase() || (erster.length > 1 && erster == erster.uppercase())
        return if (einheitlich) erster.take(1).uppercase() + erster.drop(1).lowercase() else erster
    }

    /** Setzt den Namen ein. Ohne Namen fällt der Name samt Komma weg. */
    fun einsetzen(text: String, name: String): String {
        if (!text.contains("{name}")) return text
        if (name.isNotEmpty()) return text.replace("{name}", name)
        var t = text
        for (muster in listOf(", {name}", " {name}", "{name}, ", "{name} ")) t = t.replace(muster, "")
        t = t.replace("{name}", "")
        return t.take(1).uppercase() + t.drop(1)
    }

    private val zuletzt = mutableMapOf<String, List<Int>>()

    private fun waehle(liste: List<String>, schluessel: String, name: String, zufall: Random): String {
        val i = abwechselnd(schluessel, liste.size, zufall)
        return einsetzen(liste[i], name)
    }

    /** Zieht zufällig, aber nie einen der zuletzt gezogenen Einträge. */
    private fun abwechselnd(schluessel: String, anzahl: Int, zufall: Random): Int {
        if (anzahl <= 1) return 0
        val sperre = minOf(anzahl / 2, 6)
        val gesperrt = zuletzt[schluessel].orEmpty()
        val frei = (0 until anzahl).filter { it !in gesperrt }
        val i = if (frei.isEmpty()) 0 else frei[zufall.nextInt(frei.size)]
        val neu = (gesperrt + i).let { if (it.size > sperre) it.drop(it.size - sperre) else it }
        zuletzt[schluessel] = neu
        return i
    }
}
