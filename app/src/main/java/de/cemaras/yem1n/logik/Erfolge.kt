package de.cemaras.yem1n.logik

import kotlin.math.abs

/** Münzen verdienen, wie in der iOS-App. */
object Muenzen {
    const val PRO_RICHTIG = 1
    const val BONUS_ZWEI_STERNE = 2
    const val BONUS_DREI_STERNE = 5

    /** Angeschaute Lösungen zählen nicht als richtig, Spicken bringt also nichts. */
    fun verdient(richtig: Int, gesamt: Int, angesehen: Int): Int {
        if (richtig <= 0 || gesamt <= 0) return 0
        var summe = richtig * PRO_RICHTIG
        if (angesehen == 0) {
            val sterne = Pruefer.sterneFuer(richtig, gesamt)
            if (sterne >= 3) summe += BONUS_DREI_STERNE else if (sterne == 2) summe += BONUS_ZWEI_STERNE
        }
        return summe
    }
}

/** Serie: Tage hintereinander mit mindestens einer Runde. */
object Serie {
    /** Ein einzelner verpasster Tag unterbricht die Serie nicht, aber nur einmal pro Woche. */
    const val FREIER_TAG_ABSTAND = 7

    /**
     * [laenge] zählt nur Tage, an denen geübt wurde. [gerettet] ist die Zahl der freien Tage,
     * die die Serie überbrückt haben. Heute ist noch offen und zählt nicht als verpasst.
     */
    fun mitSchutz(tage: Set<Long>, heute: Long): Pair<Int, Int> {
        var tag = heute
        if (tag !in tage) tag -= 1
        var laenge = 0
        var gerettet = 0
        var letzteLuecke: Long? = null
        var schutz = 0
        while (schutz < 4000) {
            schutz++
            if (tag in tage) {
                laenge++
                tag -= 1
                continue
            }
            val davor = tag - 1
            val frei = letzteLuecke?.let { abs(it - tag) >= FREIER_TAG_ABSTAND } ?: true
            if (davor !in tage || !frei) break
            gerettet++
            letzteLuecke = tag
            tag = davor
        }
        return laenge to gerettet
    }
}

/** Ein Abzeichen (bei iOS "Pokal"). */
data class Pokal(val id: String, val emoji: String, val titel: String, val erreicht: Boolean)

/** Abzeichen, Tagesziel und Wochenziel, aus dem Rundenverlauf berechnet. */
object Erfolge {
    const val WOCHEN_BELOHNUNG = 10

    /** Längste Serie aufeinanderfolgender Tage mit einer Runde. */
    fun besteSerie(tage: Collection<Long>): Int {
        var beste = 0
        var aktuell = 0
        var vorher: Long? = null
        for (t in tage.toSortedSet()) {
            aktuell = if (vorher != null && t == vorher + 1) aktuell + 1 else 1
            beste = maxOf(beste, aktuell)
            vorher = t
        }
        return beste
    }

    /** An so vielen Tagen wurde das Tagesziel erreicht. */
    fun zieltage(runden: List<RundenEintrag>, ziel: Int): Int =
        runden.groupingBy { it.tag }.eachCount().values.count { it >= ziel }

    /** Eine Übung, deren Name das Stichwort enthält, wurde einmal komplett richtig gelöst. */
    fun meister(runden: List<RundenEintrag>, stichwort: String): Boolean =
        runden.any { it.uebung.contains(stichwort, ignoreCase = true) && it.gesamt > 0 && it.richtig == it.gesamt }

    fun pokale(f: Fortschritt): List<Pokal> {
        val runden = f.runden
        val n = runden.size
        val sterne = runden.sumOf { it.sterne }
        val beste = besteSerie(f.tage)
        val dreiSterne = runden.any { it.sterne == 3 }
        val fehlerfrei = runden.any { it.gesamt > 0 && it.richtig == it.gesamt && it.angesehen == 0 }
        val zielTage = zieltage(runden, f.tagesziel)
        return listOf(
            Pokal("erste", "🎯", "Erste Runde", n >= 1),
            Pokal("r10", "🥉", "10 Runden", n >= 10),
            Pokal("r50", "🥈", "50 Runden", n >= 50),
            Pokal("r100", "🥇", "100 Runden", n >= 100),
            Pokal("s3", "🔥", "3 Tage in Folge", beste >= 3),
            Pokal("s7", "⚡", "7 Tage in Folge", beste >= 7),
            Pokal("s14", "👑", "14 Tage in Folge", beste >= 14),
            Pokal("s30", "🏆", "30 Tage in Folge", beste >= 30),
            Pokal("stern", "⭐", "3 Sterne", dreiSterne),
            Pokal("st50", "🌟", "50 Sterne", sterne >= 50),
            Pokal("st150", "💫", "150 Sterne", sterne >= 150),
            Pokal("fehlerfrei", "💯", "Ohne Fehler", fehlerfrei),
            Pokal("ziel5", "🎉", "Tagesziel 5 Tage", zielTage >= 5),
            Pokal("m_einmaleins", "🧮", "Einmaleins-Meister", meister(runden, "Einmaleins")),
            Pokal("m_kern", "🧠", "Kernaufgaben-Profi", meister(runden, "Kernaufgaben")),
            Pokal("m_nachbar", "🏘️", "Nachbar-Meister", meister(runden, "Nachbar")),
            Pokal("m_geteilt", "➗", "Geteilt-Held", meister(runden, "Geteilt")),
            Pokal("m_rest", "🧩", "Rest-Experte", meister(runden, "Rest")),
            Pokal("m_mauer", "🧱", "Mauer-Baumeister", meister(runden, "Zahlenmauern")),
            Pokal("m_raetsel", "🔍", "Rätsel-Knacker", meister(runden, "Zahlenrätsel")),
            Pokal("m_sach", "✏️", "Sachaufgaben-Profi", meister(runden, "Sachaufgaben")),
            Pokal("m_gemischt", "🎓", "Alles-gemischt-Meister", meister(runden, "Alles gemischt")),
            Pokal(
                "m_deutsch", "📚", "Deutsch-Held",
                runden.any { it.fach == "Deutsch" && it.gesamt > 0 && it.richtig == it.gesamt },
            ),
            Pokal("frueh", "🌅", "Frühstarter", runden.any { it.stunde < 9 }),
        )
    }

    // Wochenziel: fünf Tage pro Woche mit dem Tagesziel, der Rest ist Zugabe.
    // Die Woche beginnt am Montag. Der Tag 0 (1.1.1970) war ein Donnerstag.

    fun wochenStart(tag: Long): Long = tag - Math.floorMod(tag + 3, 7L)

    /** Der Text der Belohnung zu Hause: Leerzeichen glätten, höchstens 60 Zeichen. */
    fun familienBelohnungText(roh: String): String = roh.replace(Regex("\\s+"), " ").trim().take(60)

    fun wochenziel(tagesziel: Int): Int = maxOf(tagesziel, 1) * 5

    fun wochenRunden(f: Fortschritt, heute: Long): Int {
        val start = wochenStart(heute)
        return f.runden.count { it.tag in start..heute }
    }

    fun belohnungAbholbar(f: Fortschritt, heute: Long): Boolean =
        wochenRunden(f, heute) >= wochenziel(f.tagesziel) && f.belohnungWoche != wochenStart(heute)

    /** Merkt sich die Woche und schreibt die Münzen gut. Ohne Anspruch bleibt alles, wie es ist. */
    fun belohnungHolen(f: Fortschritt, heute: Long): Fortschritt =
        if (belohnungAbholbar(f, heute)) {
            f.copy(belohnungWoche = wochenStart(heute), muenzen = f.muenzen + WOCHEN_BELOHNUNG)
        } else {
            f
        }
}
