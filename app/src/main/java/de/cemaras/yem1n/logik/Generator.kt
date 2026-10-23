package de.cemaras.yem1n.logik

import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.daten.Aufgabe
import de.cemaras.yem1n.daten.Uebung
import kotlin.random.Random

/** Eine der 13 Übungsarten aus dem Rechenheft. */
class MatheVorlage(
    val id: String,
    val name: String,
    val symbol: String,
    val gruppe: String,
    val anzahl: Int,
    val tipp: String,
    val mach: (MatheGenerator) -> Aufgabe,
)

/**
 * Erzeugt neue Mathe-Aufgaben. Port von `MatheGenerator` aus der iOS-App.
 * Alle Ergebnisse sind ganze Zahlen von 0 bis 999, Zahlenraum wie in Klasse 3.
 */
class MatheGenerator(private val zufall: Random = Random.Default) {

    private fun z(a: Int, b: Int): Int = zufall.nextInt(minOf(a, b), maxOf(a, b) + 1)
    private fun wahr(p: Double): Boolean = zufall.nextDouble() < p
    private fun <T> aus(liste: List<T>): T = liste[zufall.nextInt(liste.size)]

    private fun zahlwort(k: Int): String {
        val liste = listOf("", "Einer", "Zweier", "Dreier", "Vierer", "Fünfer", "Sechser", "Siebener", "Achter", "Neuner", "Zehner")
        return liste.getOrElse(k) { "" }
    }

    private fun zahl(
        frage: String, rechnung: String, antwort: Int, hinweis: String = "", erklaerung: String = "",
    ) = Aufgabe(
        art = Aufgabe.ART_ZAHL, frage = frage, rechnung = rechnung, hinweis = hinweis,
        erklaerung = erklaerung, antwort = antwort.toString(), antwort2 = "", reihen = emptyList(),
        bild = "", tr = "", gross = "", folge = emptyList(), optionen = emptyList(),
    )

    fun minusWort(): Aufgabe {
        val art = z(1, 3)
        val a: Int
        var b: Int
        if (art == 1) {
            a = z(31, 99); b = z(3, 9)
        } else if (art == 2) {
            a = z(41, 99); b = z(11, 39)
        } else {
            a = z(50, 99); b = z(10, 50)
            b -= b % 10
            if (b < 10) b = 10
        }
        val text = if (zufall.nextBoolean()) "Subtrahiere von $a die Zahl $b."
        else "Wie heißt die Differenz der Zahlen $a und $b?"
        return zahl("Notiere die passende Rechnung.", text, a - b, erklaerung = "$a - $b = ${a - b}")
    }

    fun malWort(): Aufgabe {
        val a = z(2, 10)
        val b = z(2, 10)
        val text = if (zufall.nextBoolean()) "Multipliziere die Zahlen $a und $b."
        else "Wie heißt das Produkt der Zahlen $a und $b?"
        if (wahr(0.18)) {
            val c = aus(listOf(2, 5, 10))
            val d = aus(listOf(2, 5))
            val e = 10
            return zahl(
                "Notiere die passende Rechnung.",
                "Wie heißt das Produkt der Zahlen $d, $c und $e?",
                d * c * e, erklaerung = "$d · $c · $e = ${d * c * e}",
            )
        }
        return zahl("Notiere die passende Rechnung.", text, a * b, erklaerung = "$a · $b = ${a * b}")
    }

    fun kern(): Aufgabe {
        val k = aus(listOf(1, 2, 5, 10))
        val n = z(2, 10)
        return zahl("Das ist eine Kernaufgabe.", "$k · $n =", k * n)
    }

    fun nachbar(): Aufgabe {
        val k = aus(listOf(2, 5, 10))
        val n = z(3, 10)
        val nachbarZahl = if (k == 10) 9 else k + 1
        val zeichen = if (k == 10) "-" else "+"
        return zahl(
            "Erst die Kernaufgabe, dann der Nachbar.", "$nachbarZahl · $n =", nachbarZahl * n,
            hinweis = "Du weißt: $k · $n = ${k * n}. Also $zeichen $n.",
            erklaerung = "$k · $n = ${k * n}, dann $zeichen $n = ${nachbarZahl * n}",
        )
    }

    fun einmaleins(): Aufgabe {
        val a = z(2, 10)
        val b = z(2, 10)
        return zahl("Rechne.", "$a · $b =", a * b)
    }

    fun umkehr(): Aufgabe {
        val b = z(2, 10)
        val q = z(2, 10)
        val a = b * q
        return zahl(
            "Rechne und denk an die Umkehraufgabe.", "$a : $b =", q,
            hinweis = "Denn ___ · $b = $a", erklaerung = "$a : $b = $q, denn $q · $b = $a",
        )
    }

    fun rest(): Aufgabe {
        var a: Int
        val b: Int
        if (wahr(0.45)) {
            b = z(3, 9)
            val q0 = z(10, 16)
            a = b * q0 + z(0, b - 1)
            if (a > 100) a = 100
        } else {
            b = z(2, 9)
            a = z(b + 1, 60)
        }
        val q = a / b
        val r = a % b
        return Aufgabe(
            art = Aufgabe.ART_REST, frage = "Teile mit Rest. Rechne bis zum Ende.", rechnung = "$a : $b =",
            hinweis = "", erklaerung = "$a : $b = $q" + (if (r > 0) " R $r" else " (kein Rest)"),
            antwort = q.toString(), antwort2 = r.toString(), reihen = emptyList(),
            bild = "", tr = "", gross = "", folge = emptyList(), optionen = emptyList(),
        )
    }

    fun punkt(): Aufgabe {
        val f = z(1, 6)
        val text: String
        val erg: Int
        when (f) {
            1 -> {
                val b = z(2, 9); val c = z(2, 9); val a = z(2, 20)
                text = "$a + $b · $c"; erg = a + b * c
            }
            2 -> {
                val b = z(2, 9); val c = z(2, 9); val a = z(2, 20)
                text = "$b · $c + $a"; erg = b * c + a
            }
            3 -> {
                val b = z(2, 9); val c = z(2, 9)
                val a = z(b * c, b * c + 40)
                text = "$a - $b · $c"; erg = a - b * c
            }
            4 -> {
                val c = z(2, 9); val q = z(2, 9)
                val b = c * q
                val a = z(5, 60)
                text = "$a + $b : $c"; erg = a + q
            }
            5 -> {
                val c = z(2, 9); val q = z(2, 9)
                val b = c * q
                val a = z(5, 60)
                text = "$b : $c + $a"; erg = q + a
            }
            else -> {
                val c = z(2, 9); val q = z(2, 9)
                val b = c * q
                val a = z(q, 60)
                text = "$a - $b : $c"; erg = a - q
            }
        }
        return zahl("Punkt vor Strich.", "$text =", erg, erklaerung = "Zuerst die Punktrechnung, dann plus oder minus.")
    }

    fun vergleich(): Aufgabe {
        val links: String
        val rechts: String
        val lw: Int
        val rw: Int
        if (wahr(0.3)) {
            val a = z(2, 10); val b = z(2, 10)
            links = "$a · $b"
            lw = a * b
            var r = aus(listOf(lw, lw + z(1, 12), lw - z(1, 12)))
            if (r < 0) r = lw + 5
            rw = r
            rechts = r.toString()
        } else {
            val a1 = z(2, 10); val b1 = z(2, 10)
            var a2 = z(2, 10)
            var b2 = z(2, 10)
            if (wahr(0.25)) { a2 = b1; b2 = a1 }
            links = "$a1 · $b1"
            rechts = "$a2 · $b2"
            lw = a1 * b1
            rw = a2 * b2
        }
        val zeichen = if (lw > rw) ">" else if (lw < rw) "<" else "="
        return Aufgabe(
            art = Aufgabe.ART_VERGLEICH, frage = "Was gehört in die Mitte?", rechnung = "$links  ?  $rechts",
            hinweis = "", erklaerung = "$lw $zeichen $rw", antwort = zeichen, antwort2 = "",
            reihen = emptyList(), bild = "", tr = "", gross = "", folge = emptyList(), optionen = emptyList(),
        )
    }

    fun raetsel(): Aufgabe {
        repeat(400) {
            val gerade = zufall.nextBoolean()
            val k = z(3, 9)
            val start = z(20, 70)
            val ende = start + z(15, 28)
            val treffer = ((start + 1) until ende).filter { n ->
                n % k == 0 && (if (gerade) n % 2 == 0 else n % 2 == 1)
            }
            if (treffer.size == 1) {
                val paritaet = if (gerade) "gerade" else "ungerade"
                val text = "Meine Zahl ist $paritaet.\nSie ist eine ${zahlwort(k)}zahl.\nSie liegt zwischen $start und $ende."
                return zahl("Wie heißt die Zahl?", text, treffer[0])
            }
        }
        return zahl(
            "Wie heißt die Zahl?",
            "Meine Zahl ist ungerade.\nSie ist eine Neunerzahl.\nSie liegt zwischen 46 und 70.", 63,
        )
    }

    fun sach(): Aufgabe {
        val n = NAMEN.shuffled(zufall)
        val na = n[0]
        val nb = n[1]
        val frage = "Lies genau. Rechne und antworte."
        when (z(1, 11)) {
            6 -> {
                val a = z(2, 9); val b = z(1, 6)
                return zahl(
                    frage, "$na kauft ein Heft für $a Euro.\nDazu kauft $na einen Stift für $b Euro.\nWie viel Euro kostet das zusammen?",
                    a + b, erklaerung = "$a + $b = ${a + b}",
                )
            }
            7 -> {
                val c = z(3, 15)
                return zahl(
                    frage, "$na bezahlt mit 20 Euro.\nDas Eis kostet $c Euro.\nWie viel Euro bekommt $na zurück?",
                    20 - c, erklaerung = "20 - $c = ${20 - c}",
                )
            }
            8 -> {
                val a = z(20, 60); val b = z(5, 19)
                return zahl(
                    frage, "Ein Seil ist $a m lang.\n$na schneidet $b m ab.\nWie viele Meter bleiben übrig?",
                    a - b, erklaerung = "$a - $b = ${a - b}",
                )
            }
            9 -> {
                val a = z(20, 50); val b = z(5, 20); val c = z(3, 15)
                return zahl(
                    frage, "$na hat $a Murmeln.\n$nb schenkt $na noch $b Murmeln.\nDann verliert $na $c Murmeln.\nWie viele Murmeln hat $na jetzt?",
                    a + b - c, erklaerung = "$a + $b - $c = ${a + b - c}",
                )
            }
            10 -> {
                val r = z(5, 12); val p = z(2, 6)
                return zahl(
                    frage, "Ein Bus hat $r Reihen.\nIn jeder Reihe sind $p Plätze.\nWie viele Plätze hat der Bus?",
                    r * p, erklaerung = "$r · $p = ${r * p}",
                )
            }
            11 -> {
                val s = z(3, 6); val gr = z(3, 8)
                return zahl(
                    frage, "${s * gr} Kinder bilden Gruppen.\nIn jeder Gruppe sind $s Kinder.\nWie viele Gruppen gibt es?",
                    gr, erklaerung = "${s * gr} : $s = $gr",
                )
            }
            1 -> {
                val x = z(30, 80); val d = z(8, 20)
                return zahl(
                    frage, "$na schwimmt $x m weit.\n$nb schafft $d m mehr als $na.\nWie weit schwimmt $nb?",
                    x + d, erklaerung = "$x + $d = ${x + d}",
                )
            }
            2 -> {
                val y = z(40, 95); val e = z(8, 30)
                return zahl(
                    frage, "$na hat $y Sticker.\n$nb hat $e Sticker weniger.\nWie viele Sticker hat $nb?",
                    y - e, erklaerung = "$y - $e = ${y - e}",
                )
            }
            3 -> {
                val k = z(3, 9); val m = z(4, 10)
                return zahl(
                    frage, "In einer Kiste liegen $m Äpfel.\nEs gibt $k Kisten.\nWie viele Äpfel sind das?",
                    k * m, erklaerung = "$k · $m = ${k * m}",
                )
            }
            4 -> {
                val kinder = z(3, 8); val proKind = z(3, 9)
                val gesamt = kinder * proKind
                return zahl(
                    frage, "$kinder Kinder teilen $gesamt Bonbons gerecht auf.\nWie viele bekommt jedes Kind?",
                    proKind, erklaerung = "$gesamt : $kinder = $proKind",
                )
            }
        }
        val seiten = z(60, 100); val gelesen = z(15, 55)
        return zahl(
            frage, "Ein Buch hat $seiten Seiten.\n$na hat schon $gelesen Seiten gelesen.\nWie viele Seiten fehlen noch?",
            seiten - gelesen, erklaerung = "$seiten - $gelesen = ${seiten - gelesen}",
        )
    }

    fun mauer(): Aufgabe {
        var reihen: List<List<Int>>
        do {
            reihen = listOf(List(4) { z(3, 15) })
            while (reihen.last().size > 1) {
                val letzte = reihen.last()
                reihen = reihen + listOf(List(letzte.size - 1) { letzte[it] + letzte[it + 1] })
            }
        } while (reihen.last()[0] > 100)
        return Aufgabe(
            art = Aufgabe.ART_MAUER, frage = "Fülle die Mauer von unten nach oben.", rechnung = "",
            hinweis = "", erklaerung = "", antwort = "", antwort2 = "", reihen = reihen,
            bild = "", tr = "", gross = "", folge = emptyList(), optionen = emptyList(),
        )
    }

    fun gemischt(): Aufgabe {
        val liste: List<() -> Aufgabe> = listOf(
            ::minusWort, ::malWort, ::einmaleins, ::nachbar, ::umkehr, ::rest, ::punkt, ::vergleich, ::raetsel, ::sach,
        )
        return aus(liste)()
    }

    /**
     * Erzeugt [anzahl] neue Aufgaben, die in [ausschluss] noch nicht vorkommen,
     * und trägt sie dort ein.
     */
    fun fuelle(v: MatheVorlage, anzahl: Int, ausschluss: MutableSet<String>): List<Aufgabe> {
        val liste = mutableListOf<Aufgabe>()
        var versuche = 0
        while (liste.size < anzahl && versuche < 5000) {
            versuche++
            val t = v.mach(this)
            val k = schluessel(t)
            if (k in ausschluss) continue
            if (v.id == "rest") {
                val zwei = liste.count { (it.antwort.toIntOrNull() ?: 0) >= 10 }
                val ein = liste.size - zwei
                val istZwei = (t.antwort.toIntOrNull() ?: 0) >= 10
                if (istZwei) {
                    if (zwei >= (anzahl + 1) / 2) continue
                } else {
                    if (ein >= anzahl / 2) continue
                }
            }
            ausschluss += k
            liste += t
        }
        return liste
    }

    companion object {
        val NAMEN = listOf("Jule", "Ali", "Mia", "Tom", "Lena", "Ben", "Sara", "Emil", "Nele", "Jonas", "Ida", "Luis")

        const val REST_TIPP = "Bei geteilt nicht bei 10 stehen bleiben. 48 : 4 sind 12, nicht 10 Rest 8. " +
            "Teile weiter, bis der Rest kleiner ist als die Zahl, durch die du teilst."

        val vorlagen: List<MatheVorlage> = listOf(
            MatheVorlage("minuswort", "Minus mit Wörtern", "-", "Zum Aufwärmen", 10, "") { it.minusWort() },
            MatheVorlage("malwort", "Mal mit Wörtern", "·", "Zum Aufwärmen", 10, "") { it.malWort() },
            MatheVorlage("kern", "Kernaufgaben", "1· 2· 5·", "Mal und Geteilt", 10, "") { it.kern() },
            MatheVorlage("nachbar", "Nachbaraufgaben", "+1", "Mal und Geteilt", 10, "") { it.nachbar() },
            MatheVorlage("einmaleins", "Einmaleins", "7·8", "Mal und Geteilt", 12, "") { it.einmaleins() },
            MatheVorlage("umkehr", "Geteilt", ":", "Mal und Geteilt", 10, "") { it.umkehr() },
            MatheVorlage("rest", "Teilen mit Rest", "R", "Mal und Geteilt", 10, REST_TIPP) { it.rest() },
            MatheVorlage("punkt", "Punkt vor Strich", "·+", "Knobeln", 10, "") { it.punkt() },
            MatheVorlage("vergleich", "Größer oder kleiner", "< >", "Knobeln", 10, "") { it.vergleich() },
            MatheVorlage("mauer", "Zahlenmauern", "▲", "Knobeln", 4, "") { it.mauer() },
            MatheVorlage("raetsel", "Zahlenrätsel", "?", "Knobeln", 8, "") { it.raetsel() },
            MatheVorlage("sach", "Sachaufgaben", "✎", "Knobeln", 8, "") { it.sach() },
            MatheVorlage("gemischt", "Alles gemischt", "★", "Wie in der Klassenarbeit", 12, "") { it.gemischt() },
        )

        /** Schlüssel, an dem man doppelte Aufgaben erkennt. */
        fun schluessel(a: Aufgabe): String =
            if (a.art == Aufgabe.ART_MAUER) "M" + a.reihen.joinToString("|") { it.joinToString(",") }
            else a.rechnung

        /** Schlüssel aller Aufgaben der vorhandenen Arbeiten, damit nichts doppelt vorkommt. */
        fun vorhandeneSchluessel(arbeiten: List<Arbeit>): MutableSet<String> {
            val s = mutableSetOf<String>()
            for (arbeit in arbeiten) for (u in arbeit.uebungen) for (a in u.aufgaben) s += schluessel(a)
            return s
        }
    }
}

/** Probearbeit und Training: neue Aufgaben, die es noch nicht gab. */
object Sonderrunden {
    /** Jeder Bereich kommt mindestens einmal vor, die Reihenfolge bleibt wie in einer echten Klassenarbeit. */
    val probeBereiche = listOf(
        "minuswort", "malwort", "kern", "nachbar", "einmaleins", "umkehr", "rest", "punkt",
        "vergleich", "mauer", "raetsel", "sach",
    )
    const val PROBE_AUFGABEN = 20
    /** Mehr als zwei Zahlenmauern wären zu viel Schreibarbeit. */
    const val PROBE_MAX_MAUERN = 2
    const val PROBE_MINUTEN = 30

    fun probePlan(zufall: Random): List<Pair<String, Int>> {
        val anzahl = probeBereiche.associateWith { 1 }.toMutableMap()
        var uebrig = PROBE_AUFGABEN - probeBereiche.size
        while (uebrig > 0) {
            val id = probeBereiche[zufall.nextInt(probeBereiche.size)]
            if (id == "mauer" && anzahl.getValue(id) >= PROBE_MAX_MAUERN) continue
            anzahl[id] = anzahl.getValue(id) + 1
            uebrig--
        }
        return probeBereiche.map { it to anzahl.getValue(it) }
    }

    /** Eine Probearbeit mit 20 Aufgaben quer durch alle Bereiche. */
    fun probe(arbeiten: List<Arbeit>, zufall: Random = Random.Default): Arbeit {
        val gen = MatheGenerator(zufall)
        val sperre = MatheGenerator.vorhandeneSchluessel(arbeiten)
        val inDieserProbe = mutableSetOf<String>()
        val aufgaben = mutableListOf<Aufgabe>()
        for ((id, anzahl) in probePlan(zufall)) {
            val v = MatheGenerator.vorlagen.first { it.id == id }
            val neu = gen.fuelle(v, anzahl, sperre).toMutableList()
            neu.forEach { inDieserProbe += MatheGenerator.schluessel(it) }
            if (neu.size < anzahl) {
                // Kleine Bereiche sind nach vielen Klassenarbeiten leer gefischt. Dann dürfen sie wiederkommen.
                neu += gen.fuelle(v, anzahl - neu.size, inDieserProbe.toMutableSet())
            }
            aufgaben += neu
        }
        return Arbeit(
            klasse = arbeiten.firstOrNull()?.klasse ?: "Klasse 3", fach = "Mathe", titel = "Probearbeit",
            uebungen = listOf(Uebung("Probearbeit", "Wie in der Klassenarbeit", "★", "", aufgaben)),
        )
    }

    /** Wie in der Schule: Note von 1 bis 6. */
    fun note(gut: Int, gesamt: Int): Int {
        if (gesamt <= 0) return 6
        val anteil = gut.toDouble() / gesamt
        return when {
            anteil >= 0.95 -> 1
            anteil >= 0.82 -> 2
            anteil >= 0.67 -> 3
            anteil >= 0.50 -> 4
            anteil >= 0.25 -> 5
            else -> 6
        }
    }

    /**
     * Training: neue Aufgaben zu den zwei schwächsten Bereichen. [anteile] sagt je Übungsname,
     * wie viel Prozent der Aufgaben bisher richtig waren (0.0 bis 1.0). Fehlende Bereiche
     * werden zufällig aufgefüllt.
     */
    fun training(arbeiten: List<Arbeit>, anteile: Map<String, Double>, zufall: Random = Random.Default): Arbeit {
        val gen = MatheGenerator(zufall)
        val kandidaten = MatheGenerator.vorlagen.filter { it.id != "gemischt" }
        val picks = kandidaten
            .filter { anteile[it.name] != null && anteile.getValue(it.name) < 0.95 }
            .sortedBy { anteile.getValue(it.name) }
            .take(2)
            .toMutableList()
        for (v in kandidaten.shuffled(zufall)) {
            if (picks.size >= 2) break
            if (picks.none { it.id == v.id }) picks += v
        }
        val sperre = mutableSetOf<String>()
        val uebungen = picks.map { v ->
            Uebung(v.name, "Training", v.symbol, v.tipp, gen.fuelle(v, 8, sperre))
        }
        return Arbeit(
            klasse = arbeiten.firstOrNull()?.klasse ?: "Klasse 3", fach = "Mathe",
            titel = "Training heute", uebungen = uebungen,
        )
    }
}
