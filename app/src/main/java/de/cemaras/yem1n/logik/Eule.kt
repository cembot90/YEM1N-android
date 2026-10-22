package de.cemaras.yem1n.logik

/** Eule Olli: erklärt das Haustier und gibt Tipps, die gerade passen. Port von Eule.swift (Kindseite). */
object Eule {
    const val NAME = "Olli"
    const val BILD = "🦉"

    /** Ab so wenigen fehlenden Punkten erinnert Olli an die nächste Stufe. */
    const val NAHE_GRENZE = 15

    fun einfuehrung(h: Haustier): List<String> {
        val n = h.name
        val anfang = if (h.stufe == HaustierStufe.EI) listOf(
            "Hallo, ich bin Olli, die Eule! Ich passe auf das Ei von $n auf, bis es schlüpft.",
            "Das Ei schlüpft, sobald du eine Runde übst. Egal ob Mathe, Deutsch, Sprachen oder Vorschule.",
        ) else listOf(
            "Hallo, ich bin Olli, die Eule! Ab jetzt passe ich zusammen mit dir auf $n auf.",
            "Jede Runde, die du übst, macht $n satt und froh. Egal ob Mathe, Deutsch, Sprachen oder Vorschule.",
        )
        return anfang + listOf(
            "Für jede richtige Aufgabe bekommt $n einen Punkt, für jeden Stern zwei. So wächst es: Baby, Kind, Großes Tier und am Ende Meister.",
            "Mein Geheimtipp: Probier jede Aufgabe erst selbst. Wer die Lösung anschaut, bekommt keine Sternpunkte.",
            "Übst du länger nicht, bekommt $n Hunger und wird traurig. Aber keine Sorge, es läuft nie weg. Eine Runde macht es wieder froh.",
            "Mit deinen Münzen kannst du $n verwöhnen: mit Leckerli, Spielen und Hüten. Und jeden Tag wartet hier ein kleines Geschenk.",
            "Hast du eine Frage? Tipp einfach auf mich. Los geht's!",
        )
    }

    /** Alle Tipps, die gerade passen: zuerst die zur Lage, dann allgemeine. */
    fun tipps(h: Haustier, muenzen: Int, familie: Boolean, heute: Long): List<String> =
        (lageTipps(h, muenzen, heute) + allgemeineTipps(h, familie)).distinct()

    fun lageTipps(h: Haustier, muenzen: Int, heute: Long): List<String> {
        val n = h.name
        val t = mutableListOf<String>()
        val geschenk = "Hol dir dein Tagesgeschenk ab: ${Haustier.GESCHENK_MUENZEN} Münzen warten auf dich."
        if (h.stufe == HaustierStufe.EI) {
            t += "Übe eine Runde, dann schlüpft $n. Ich bin schon ganz gespannt!"
            if (h.geschenkOffen(heute)) t += geschenk
            return t
        }
        when (h.stimmung) {
            HaustierStimmung.HUNGRIG -> t += "$n hat Hunger. Eine Runde üben macht es satt, ganz ohne Münzen."
            HaustierStimmung.TRAURIG -> t += "$n ist ein bisschen traurig. Eine Runde üben oder zusammen spielen hilft."
            else -> {}
        }
        if (h.geschenkOffen(heute)) t += geschenk
        h.stufe.naechste?.let { weiter ->
            val rest = weiter.schwelle - h.xp
            if (rest in 1..NAHE_GRENZE) t += "Nur noch $rest Punkte bis zur Stufe ${weiter.titel}! Das schaffst du bald."
        }
        guenstigsterHut(h, muenzen)?.let {
            t += "Du hast genug Münzen für einen neuen Look: ${it.emoji} ${it.name} für ${it.preis} Münzen."
        }
        return t
    }

    fun allgemeineTipps(h: Haustier, familie: Boolean): List<String> {
        val n = h.name
        val t = mutableListOf(
            "Drei Sterne bringen die meisten Punkte. Lies jede Aufgabe ganz genau.",
            "Probier es erst selbst. Wer die Lösung anschaut, bekommt keine Sternpunkte.",
            "Jede Runde zählt, auch in Deutsch, in den Sprachen und in der Vorschule.",
            "Jeden Tag ein bisschen üben ist besser als einmal ganz viel.",
        )
        if (h.stufe == HaustierStufe.EI) {
            t += "Für jede richtige Aufgabe gibt es einen Punkt. Schon mit dem ersten Punkt schlüpft $n."
        } else {
            t += "Satt wird $n auch gratis, wenn du übst. Deine Münzen sind für Extras."
            val weiter = h.stufe.naechste
            t += if (weiter != null) "$n hat schon ${h.xp} Punkte. Ab ${weiter.schwelle} Punkten kommt die Stufe ${weiter.titel}."
            else "$n ist ein Meister! Du hast so viel geübt. Ich bin stolz auf dich."
        }
        if (familie) t += "Deine Eltern sehen $n auch und können ihm jeden Tag ein Leckerli schenken."
        return t
    }

    /** Der günstigste Hut, den das Kind noch nicht hat und sich leisten kann. */
    fun guenstigsterHut(h: Haustier, muenzen: Int): HaustierHut? =
        HaustierHut.alle.filter { it.id !in h.huete && it.preis <= muenzen }.minByOrNull { it.preis }

    /** Was das Tier beim Antippen sagt. [nummer] zählt mit, damit nicht immer dasselbe kommt. */
    fun antippen(h: Haustier, nummer: Int): String {
        val liste = if (h.stufe == HaustierStufe.EI) listOf(
            "Klopf, klopf! Bald bin ich da.", "Hier drin ist es warm und kuschelig.",
            "Ich höre dich! Üb eine Runde, dann komme ich raus.",
        ) else when (h.stimmung) {
            HaustierStimmung.FROH -> listOf("Hihi, das kitzelt!", "Ich hab dich lieb!", "Heute ist ein super Tag!", "Nochmal hüpfen!")
            HaustierStimmung.ZUFRIEDEN -> listOf("Schön, dass du da bist.", "Üben wir gleich zusammen?", "Mir geht es gut. Und dir?")
            HaustierStimmung.HUNGRIG -> listOf("Mein Bauch knurrt ...", "Hast du was zu essen für mich?", "Eine Runde üben macht mich satt!")
            HaustierStimmung.TRAURIG -> listOf("Spielst du mit mir?", "Ich hab dich vermisst.", "Bleibst du noch ein bisschen?")
        }
        return liste[Math.floorMod(nummer, liste.size)]
    }

    /** Text zum Vorlesen: Emojis fallen weg, sonst liest die Stimme ihre Namen vor. */
    fun sprechbar(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            val bild = cp > 0x2000 && (Character.getType(cp) == Character.OTHER_SYMBOL.toInt() ||
                cp in 0x1F000..0x1FAFF || cp == 0xFE0F || cp == 0x200D)
            if (!bild) sb.appendCodePoint(cp)
        }
        return sb.toString().split(" ").filter { it.isNotEmpty() }.joinToString(" ").trim()
    }
}
