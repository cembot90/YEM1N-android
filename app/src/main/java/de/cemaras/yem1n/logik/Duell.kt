package de.cemaras.yem1n.logik

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Duell: Kopfrechnen gegen die Familie (Port von `Duell.swift` und `DuellCloud.swift`).
 * Das Format in der Cloud ist dasselbe wie bei iOS: ein JSON-Text im Feld "json".
 * Datum ist dort die Zeit in Sekunden seit dem 1.1.2001 (Standard von Swift).
 */
@Serializable
enum class DuellStufe(val kurz: String, val emoji: String, val beschreibung: String) {
    @SerialName("klasse1") KLASSE1("Klasse 1", "🐣", "Plus und Minus bis 20"),
    @SerialName("klasse2") KLASSE2("Klasse 2", "🐥", "Plus und Minus bis 100, Mal mit 2, 5 und 10"),
    @SerialName("klasse3") KLASSE3("Klasse 3", "🦊", "Einmaleins, Geteilt, Rechnen mit Zehnern bis 1000"),
    @SerialName("klasse4") KLASSE4("Klasse 4", "🦁", "Großes Mal und Geteilt, Rechnen bis 1000"),
    @SerialName("profi") PROFI("Profi", "🧠", "Für Erwachsene: richtig knifflig");

    companion object {
        /** Die Stufe zur Klasse aus dem Profil. Ohne Klasse ist es Klasse 3. */
        fun passend(klasse: String): DuellStufe = when (klasse) {
            "Vorschule", "Klasse 1" -> KLASSE1
            "Klasse 2" -> KLASSE2
            "Klasse 4" -> KLASSE4
            else -> KLASSE3
        }
    }
}

@Serializable
data class DuellAufgabe(val rechnung: String, val antwort: Int)

/** Das Ergebnis eines Durchgangs. Die Sekunden enthalten schon die Strafzeit. */
@Serializable
data class DuellStand(val richtig: Int, val falsch: Int, val sekunden: Double)

@Serializable
data class DuellHerausforderung(
    val id: String,
    val von: String,
    val vonGeraet: String,
    val vonStufe: DuellStufe,
    val vonStand: DuellStand,
    val anStufe: DuellStufe,
    /** Die Aufgaben für alle, die annehmen. */
    val aufgaben: List<DuellAufgabe>,
    /** Sekunden seit dem 1.1.2001, so speichert Swift ein Datum. */
    val zeitpunkt: Double,
)

@Serializable
data class DuellAntwort(
    val duellID: String,
    val name: String,
    val geraet: String,
    val stand: DuellStand,
    val zeitpunkt: Double,
) {
    val id: String get() = "$duellID-$geraet"
}

/** Was gespielt wird: eigene Herausforderung (dann [fuerAndere]) oder eine angenommene ([herausforderung]). */
data class DuellSpiel(
    val stufe: DuellStufe,
    val aufgaben: List<DuellAufgabe>,
    val herausforderung: DuellHerausforderung? = null,
    val anStufe: DuellStufe = DuellStufe.KLASSE3,
    val fuerAndere: List<DuellAufgabe> = emptyList(),
)

/** Ein Durchgang einer Person: welche Aufgabe dran ist und wie es bisher lief. */
data class DuellLauf(
    val aufgaben: List<DuellAufgabe>,
    val antworten: List<Boolean> = emptyList(),
    val strafe: Double = 0.0,
) {
    val index: Int get() = antworten.size
    val fertig: Boolean get() = index >= aufgaben.size
    val aktuelle: DuellAufgabe? get() = aufgaben.getOrNull(index)
    val richtig: Int get() = antworten.count { it }
    val falsch: Int get() = antworten.size - richtig

    /** Gibt den neuen Stand und zurück, ob es richtig war. Null, wenn nichts zu prüfen war. */
    fun antworte(text: String): Pair<DuellLauf, Boolean>? {
        val a = aktuelle ?: return null
        val n = text.trim().toIntOrNull() ?: return null
        val ok = n == a.antwort
        return copy(antworten = antworten + ok, strafe = if (ok) strafe else strafe + DuellRegeln.STRAF_SEKUNDEN) to ok
    }

    fun stand(rohSekunden: Double): DuellStand = DuellStand(richtig, falsch, rohSekunden + strafe)
}

object DuellRegeln {
    const val ANZAHL = 10
    const val STRAF_SEKUNDEN = 3.0
    const val HALTBAR_TAGE = 14

    /** Sekunden zwischen dem 1.1.1970 und dem 1.1.2001. */
    private const val REFERENZ = 978_307_200L

    fun zeitpunktJetzt(jetzt: Long = System.currentTimeMillis()): Double = jetzt / 1000.0 - REFERENZ

    fun zeitpunktMs(z: Double): Long = ((z + REFERENZ) * 1000).toLong()

    fun aufgaben(stufe: DuellStufe, anzahl: Int = ANZAHL, zufall: Random = Random.Default): List<DuellAufgabe> {
        val liste = mutableListOf<DuellAufgabe>()
        val gesehen = HashSet<String>()
        var versuche = 0
        while (liste.size < anzahl && versuche < 1000) {
            versuche++
            val a = eine(stufe, zufall)
            if (gesehen.add(a.rechnung)) liste.add(a)
        }
        return liste
    }

    /** Eine Aufgabe der Stufe. Alle Ergebnisse liegen zwischen 1 und 999. */
    fun eine(stufe: DuellStufe, g: Random): DuellAufgabe {
        fun z(a: Int, b: Int): Int = g.nextInt(a, b + 1)
        fun plus(a: Int, b: Int) = DuellAufgabe("$a + $b", a + b)
        fun minus(a: Int, b: Int) = DuellAufgabe("$a - $b", a - b)
        fun mal(a: Int, b: Int) = DuellAufgabe("$a · $b", a * b)
        fun geteilt(teiler: Int, ergebnis: Int) = DuellAufgabe("${teiler * ergebnis} : $teiler", ergebnis)

        val wahl = z(0, 99)
        return when (stufe) {
            DuellStufe.KLASSE1 -> if (wahl < 50) {
                val summe = z(3, 20)
                val a = z(1, summe - 1)
                plus(a, summe - a)
            } else {
                val a = z(3, 20)
                minus(a, z(1, a - 1))
            }
            DuellStufe.KLASSE2 -> when {
                wahl < 35 -> {
                    val a = z(11, 79)
                    plus(a, z(3, 99 - a))
                }
                wahl < 70 -> {
                    val a = z(20, 99)
                    minus(a, z(3, a - 5))
                }
                else -> {
                    val kern = listOf(2, 5, 10)[z(0, 2)]
                    if (wahl < 85) mal(z(1, 10), kern) else mal(kern, z(1, 10))
                }
            }
            DuellStufe.KLASSE3 -> when {
                wahl < 40 -> mal(z(2, 10), z(2, 10))
                wahl < 70 -> geteilt(z(2, 10), z(2, 10))
                wahl < 85 -> {
                    val a = z(10, 80) * 10
                    plus(a, z(1, 99 - a / 10) * 10)
                }
                else -> {
                    val a = z(20, 99) * 10
                    minus(a, z(1, a / 10 - 1) * 10)
                }
            }
            DuellStufe.KLASSE4 -> when {
                wahl < 35 -> mal(z(11, 40), z(2, 9))
                wahl < 60 -> geteilt(z(2, 9), z(11, 40))
                wahl < 80 -> {
                    val a = z(120, 880)
                    plus(a, z(11, 999 - a))
                }
                else -> {
                    val a = z(200, 999)
                    minus(a, z(11, a - 100))
                }
            }
            DuellStufe.PROFI -> when {
                wahl < 35 -> mal(z(12, 25), z(12, 25))
                wahl < 60 -> {
                    val t = z(3, 9)
                    geteilt(t, z(21, 999 / t))
                }
                wahl < 80 -> {
                    val a = z(234, 765)
                    plus(a, z(111, 999 - a))
                }
                else -> {
                    val a = z(400, 999)
                    minus(a, z(123, a - 50))
                }
            }
        }
    }

    /** 0, wenn der erste gewinnt, 1 für den zweiten, null bei Gleichstand. */
    fun sieger(a: DuellStand, b: DuellStand): Int? {
        if (a.richtig != b.richtig) return if (a.richtig > b.richtig) 0 else 1
        if (Math.abs(a.sekunden - b.sekunden) < 0.1) return null
        return if (a.sekunden < b.sekunden) 0 else 1
    }

    /** "48,3 s" */
    fun zeitText(sekunden: Double): String {
        val zehntel = Math.max(0, Math.round(sekunden * 10).toInt())
        return "${zehntel / 10},${zehntel % 10} s"
    }

    fun standText(s: DuellStand): String = "${s.richtig} von ${s.richtig + s.falsch} richtig in ${zeitText(s.sekunden)}"
}

/** Wer wartet, wer hat gewonnen: die Familie auswerten. */
object DuellPost {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun text(d: DuellHerausforderung): String = json.encodeToString(DuellHerausforderung.serializer(), d)
    fun text(a: DuellAntwort): String = json.encodeToString(DuellAntwort.serializer(), a)

    fun herausforderung(text: String): DuellHerausforderung? =
        try { json.decodeFromString(DuellHerausforderung.serializer(), text) } catch (e: Exception) { null }

    fun antwort(text: String): DuellAntwort? =
        try { json.decodeFromString(DuellAntwort.serializer(), text) } catch (e: Exception) { null }

    fun frisch(zeitpunkt: Double, jetzt: Long): Boolean =
        jetzt - DuellRegeln.zeitpunktMs(zeitpunkt) < DuellRegeln.HALTBAR_TAGE * 86_400_000L

    /** Herausforderungen der anderen, die dieses Gerät noch nicht angenommen hat. Neueste zuerst. */
    fun offen(
        duelle: List<DuellHerausforderung>, antworten: List<DuellAntwort>, geraet: String,
        jetzt: Long = System.currentTimeMillis(),
    ): List<DuellHerausforderung> = duelle
        .filter { d -> d.vonGeraet != geraet && frisch(d.zeitpunkt, jetzt) && antworten.none { it.duellID == d.id && it.geraet == geraet } }
        .sortedByDescending { it.zeitpunkt }

    /** Eigene Herausforderungen, auf die noch niemand geantwortet hat. */
    fun wartend(
        duelle: List<DuellHerausforderung>, antworten: List<DuellAntwort>, geraet: String,
        jetzt: Long = System.currentTimeMillis(),
    ): List<DuellHerausforderung> = duelle
        .filter { d -> d.vonGeraet == geraet && frisch(d.zeitpunkt, jetzt) && antworten.none { it.duellID == d.id } }
        .sortedByDescending { it.zeitpunkt }

    data class Paarung(val duell: DuellHerausforderung, val antwort: DuellAntwort) {
        val sieger: Int? get() = DuellRegeln.sieger(duell.vonStand, antwort.stand)
        val siegerName: String? get() = when (sieger) {
            0 -> duell.von
            1 -> antwort.name
            else -> null
        }
    }

    /** Alle entschiedenen Duelle der Familie, das zuletzt beantwortete zuerst. */
    fun entschieden(duelle: List<DuellHerausforderung>, antworten: List<DuellAntwort>, anzahl: Int = 10): List<Paarung> {
        val nachId = duelle.associateBy { it.id }
        return antworten.mapNotNull { a -> nachId[a.duellID]?.let { Paarung(it, a) } }
            .sortedByDescending { it.antwort.zeitpunkt }
            .take(anzahl)
    }
}
