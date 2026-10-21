package de.cemaras.yem1n.logik

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/**
 * Haustier (Tamagotchi): Es wird satt und froh, wenn das Kind übt. Es stirbt nie und läuft nie weg:
 * Wer ein paar Tage nicht kommt, findet ein hungriges, aber freundliches Tier. Alles bleibt auf dem Gerät.
 * Die Regeln entsprechen der iOS-App (Haustier.swift).
 */
class HaustierArt(val id: String, val emoji: String, val name: String) {
    companion object {
        val alle = listOf(
            HaustierArt("katze", "🐱", "Katze"),
            HaustierArt("hund", "🐶", "Hund"),
            HaustierArt("hase", "🐰", "Hase"),
            HaustierArt("fuchs", "🦊", "Fuchs"),
            HaustierArt("drache", "🐲", "Drache"),
            HaustierArt("einhorn", "🦄", "Einhorn"),
        )

        fun finde(id: String): HaustierArt = alle.firstOrNull { it.id == id } ?: alle[0]
    }
}

class HaustierHut(val id: String, val emoji: String, val name: String, val preis: Int) {
    companion object {
        val alle = listOf(
            HaustierHut("brille", "🕶️", "Sonnenbrille", 30),
            HaustierHut("schleife", "🎀", "Schleife", 30),
            HaustierHut("kappe", "🧢", "Kappe", 30),
            HaustierHut("zylinder", "🎩", "Zylinder", 45),
            HaustierHut("doktor", "🎓", "Doktorhut", 45),
            HaustierHut("krone", "👑", "Krone", 80),
        )

        fun finde(id: String): HaustierHut? = alle.firstOrNull { it.id == id }
    }
}

enum class HaustierStufe(val titel: String, val schwelle: Int, val groesse: Int) {
    EI("Ei", 0, 70),
    BABY("Baby", 1, 78),
    KIND("Kind", 60, 100),
    GROSS("Großes Tier", 250, 126),
    MEISTER("Meister", 700, 148);

    val naechste: HaustierStufe? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun fuer(xp: Int): HaustierStufe = entries.last { xp >= it.schwelle }
    }
}

enum class HaustierStimmung(val emoji: String) {
    FROH("❤️"), ZUFRIEDEN("🙂"), HUNGRIG("🍽️"), TRAURIG("💧")
}

/** Was nach einer Runde passiert ist. */
data class HaustierEreignis(val geschluepft: Boolean, val aufgestiegen: Boolean, val stufe: HaustierStufe)

@Serializable
data class Haustier(
    val name: String = "Pip",
    val art: String = "katze",
    val xp: Int = 0,
    val satt: Double = 80.0,
    val laune: Double = 80.0,
    /** Zeitpunkt der letzten Berechnung in Millisekunden. */
    val zuletzt: Long = 0,
    val hut: String = "",
    val huete: List<String> = emptyList(),
    /** Tag (siehe [Tag.heute]), an dem zuletzt das Geschenk abgeholt wurde, -1 wenn noch nie. */
    val geschenkTag: Long = -1,
) {
    val stufe: HaustierStufe get() = HaustierStufe.fuer(xp)

    val stimmung: HaustierStimmung
        get() = when {
            satt < HUNGRIG_GRENZE -> HaustierStimmung.HUNGRIG
            laune < 30 -> HaustierStimmung.TRAURIG
            satt >= 60 && laune >= 60 -> HaustierStimmung.FROH
            else -> HaustierStimmung.ZUFRIEDEN
        }

    val emoji: String get() = if (stufe == HaustierStufe.EI) "🥚" else HaustierArt.finde(art).emoji

    /** Anteil auf dem Weg zur nächsten Stufe, 1 beim Meister. */
    val fortschritt: Double
        get() {
            val weiter = stufe.naechste ?: return 1.0
            val von = stufe.schwelle.toDouble()
            val bis = weiter.schwelle.toDouble()
            return ((xp - von) / (bis - von)).coerceIn(0.0, 1.0)
        }

    /** Rechnet Hunger und Laune bis zum Zeitpunkt [jetzt] aus. Das Ei hat keinen Hunger. */
    fun aktualisiert(jetzt: Long): Haustier {
        val stunden = (jetzt - zuletzt) / 3_600_000.0
        if (stunden <= 0) return if (jetzt < zuletzt) copy(zuletzt = jetzt) else this
        if (stufe == HaustierStufe.EI) return copy(zuletzt = jetzt)
        val bisHungrig = if (satt > HUNGRIG_GRENZE) (satt - HUNGRIG_GRENZE) / SATT_PRO_STUNDE else 0.0
        val normal = min(stunden, bisHungrig)
        val hungrig = stunden - normal
        val neueLaune = laune - (normal * LAUNE_PRO_STUNDE + hungrig * LAUNE_PRO_STUNDE_HUNGRIG)
        val neuSatt = satt - stunden * SATT_PRO_STUNDE
        return copy(
            satt = neuSatt.coerceIn(0.0, 100.0),
            laune = neueLaune.coerceIn(0.0, 100.0),
            zuletzt = jetzt,
        )
    }

    /** Nach einer fertigen Runde: wächst, wird satt und froh. Wer die Lösung ansieht, bekommt keinen Sternbonus. */
    fun lernrunde(richtig: Int, gesamt: Int, angesehen: Int, jetzt: Long): Pair<Haustier, HaustierEreignis> {
        val frisch = aktualisiert(jetzt)
        val vorher = frisch.stufe
        val sterne = if (angesehen == 0) Pruefer.sterneFuer(richtig, gesamt) else 0
        val neu = frisch.copy(
            xp = frisch.xp + max(1, richtig + 2 * sterne),
            satt = min(frisch.satt + 30, 100.0),
            laune = min(frisch.laune + (if (sterne >= 3) 25 else 10), 100.0),
        )
        return neu to HaustierEreignis(
            geschluepft = vorher == HaustierStufe.EI && neu.stufe != HaustierStufe.EI,
            aufgestiegen = vorher != HaustierStufe.EI && neu.stufe != vorher,
            stufe = neu.stufe,
        )
    }

    fun kannLeckerli(muenzen: Int): Boolean = stufe != HaustierStufe.EI && satt < 95 && muenzen >= LECKERLI_PREIS

    /** Null, wenn es nicht geht (Ei, schon satt oder zu wenig Münzen). */
    fun leckerli(muenzen: Int): Haustier? =
        if (!kannLeckerli(muenzen)) null else copy(satt = min(satt + 25, 100.0), laune = min(laune + 3, 100.0))

    fun kannSpielen(muenzen: Int): Boolean = stufe != HaustierStufe.EI && laune < 95 && muenzen >= SPIELZEUG_PREIS

    fun spielen(muenzen: Int): Haustier? =
        if (!kannSpielen(muenzen)) null else copy(laune = min(laune + 30, 100.0))

    fun kannKaufen(id: String, muenzen: Int): Boolean {
        val h = HaustierHut.finde(id) ?: return false
        return stufe != HaustierStufe.EI && id !in huete && muenzen >= h.preis
    }

    fun kaufe(id: String, muenzen: Int): Haustier? =
        if (!kannKaufen(id, muenzen)) null else copy(huete = huete + id, hut = id)

    /** Setzt einen gekauften Hut auf. Eine leere Kennung nimmt den Hut ab. */
    fun trage(id: String): Haustier = if (id.isEmpty() || id in huete) copy(hut = id) else this

    fun geschenkOffen(heute: Long): Boolean = geschenkTag != heute

    /** Gibt das Tier mit Geschenk-Vermerk zurück, null wenn heute schon abgeholt wurde. */
    fun holeGeschenk(heute: Long): Haustier? = if (geschenkOffen(heute)) copy(geschenkTag = heute) else null

    val spruch: String
        get() {
            if (stufe == HaustierStufe.EI) return "Löse eine Runde, dann schlüpfe ich."
            val liste = when (stimmung) {
                HaustierStimmung.FROH -> listOf("Mir geht es super!", "Heute lernen wir zusammen!", "Du bist klasse.", "Ich bin satt und froh.")
                HaustierStimmung.ZUFRIEDEN -> listOf("Ich bin zufrieden.", "Eine Runde wäre schön.", "Schön, dass du da bist.")
                HaustierStimmung.HUNGRIG -> listOf("Mein Bauch knurrt. Eine Runde macht mich satt!", "Ich habe Hunger. Üben macht satt.")
                HaustierStimmung.TRAURIG -> listOf("Mir ist langweilig. Spielst du mit mir?", "Ich vermisse dich.")
            }
            return liste[(xp / 3) % liste.size]
        }

    /** Text für die Ergebnisseite nach einer Runde. */
    fun meldung(e: HaustierEreignis): String = when {
        e.geschluepft -> "🐣 $name ist geschlüpft!"
        e.aufgestiegen -> "🎉 $name ist gewachsen: ${e.stufe.titel}!"
        else -> "$emoji $name ist satt und freut sich."
    }

    companion object {
        /** Unter diesem Wert ist das Tier hungrig. */
        const val HUNGRIG_GRENZE = 30.0
        const val SATT_PRO_STUNDE = 2.5
        const val LAUNE_PRO_STUNDE = 1.2

        /** Wenn das Tier hungrig ist, sinkt die Laune schneller. */
        const val LAUNE_PRO_STUNDE_HUNGRIG = 3.0
        const val LECKERLI_PREIS = 5
        const val SPIELZEUG_PREIS = 8
        const val GESCHENK_MUENZEN = 2
        const val NAMENSLAENGE = 14

        /** Was sich zwischen zwei Ständen getan hat, null ohne Haustier. */
        fun ereignis(vorher: Haustier?, nachher: Haustier?): HaustierEreignis? {
            if (vorher == null || nachher == null) return null
            val a = vorher.stufe
            val b = nachher.stufe
            return HaustierEreignis(a == HaustierStufe.EI && b != a, a != HaustierStufe.EI && b != a, b)
        }

        /** Ein neues Ei. Leerer Name wird "Pip", zu lange Namen werden gekürzt. */
        fun neu(name: String, art: String, jetzt: Long): Haustier =
            Haustier(name = sauber(name), art = art, zuletzt = jetzt)

        fun sauber(roh: String): String {
            val t = roh.trim()
            return if (t.isEmpty()) "Pip" else t.take(NAMENSLAENGE)
        }
    }
}
