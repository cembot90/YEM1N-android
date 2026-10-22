package de.cemaras.yem1n.logik

/** Der Bericht für ein Kind über die letzten 7 Tage. Port von `ElternBericht.swift`, ohne KI: der Text ist fest. */
data class BerichtMuster(val muster: FehlerMuster, val anzahl: Int, val beispiel: String)

data class KindBericht(
    val name: String,
    val runden: Int = 0,
    val tage: Int = 0,
    val richtig: Int = 0,
    val gesamt: Int = 0,
    val angesehen: Int = 0,
    val stark: List<String> = emptyList(),
    val ueben: List<String> = emptyList(),
    val muster: List<BerichtMuster> = emptyList(),
) {
    val prozent: Int get() = if (gesamt > 0) Math.round(richtig * 100f / gesamt) else 0
}

object Wochenbericht {
    const val TAGE = 7

    fun kindBericht(name: String, runden: List<RundenErgebnisEintrag>, offset: Int = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis())): KindBericht {
        val tage = runden.map { Tag.heute(it.zeitpunkt, offset) }.toSet().size
        val bereiche = runden.groupBy { it.uebung }.mapNotNull { (titel, liste) ->
            val g = liste.sumOf { it.gesamt }
            if (g > 0) Triple(titel, liste.sumOf { it.richtig }.toDouble() / g, g) else null
        }
        val stark = bereiche.filter { it.second >= 0.9 && it.third >= 5 }
            .sortedWith(compareByDescending<Triple<String, Double, Int>> { it.third }.thenBy { it.first })
            .take(2).map { it.first }
        val ueben = bereiche.filter { it.second < 0.75 && it.third >= 3 }
            .sortedWith(compareBy<Triple<String, Double, Int>> { it.second }.thenBy { it.first })
            .take(2).map { it.first }

        class Zaehler(val muster: FehlerMuster, var anzahl: Int, var beispiel: String, var zuletzt: Long)
        val zaehler = LinkedHashMap<String, Zaehler>()
        for (runde in runden) {
            for (d in AntwortProtokoll.liste(runde.details)) {
                val befund = FehlerAnalyse.befund(d) ?: continue
                val beispiel = "${AntwortProtokoll.einzeilig(d.rechnung).replace(" =", "")}, geschrieben ${d.eingabe}, richtig ${d.loesung}"
                val e = zaehler.getOrPut(befund.muster.elternText) { Zaehler(befund.muster, 0, beispiel, 0) }
                e.anzahl++
                if (runde.zeitpunkt >= e.zuletzt) {
                    e.beispiel = beispiel
                    e.zuletzt = runde.zeitpunkt
                }
            }
        }
        val muster = zaehler.values.filter { it.anzahl >= 2 }
            .sortedWith(compareByDescending<Zaehler> { it.anzahl }.thenBy { it.muster.name })
            .take(2).map { BerichtMuster(it.muster, it.anzahl, it.beispiel) }
        return KindBericht(
            name = name, runden = runden.size, tage = tage,
            richtig = runden.sumOf { it.richtig }, gesamt = runden.sumOf { it.gesamt },
            angesehen = runden.sumOf { it.angesehen }, stark = stark, ueben = ueben, muster = muster,
        )
    }

    /** Alle Kinder mit ihren Runden der letzten 7 Tage (heute und die 6 Tage davor). */
    fun berichte(
        runden: List<RundenErgebnisEintrag>, jetzt: Long = System.currentTimeMillis(),
        offset: Int = java.util.TimeZone.getDefault().getOffset(jetzt),
    ): List<KindBericht> {
        val heute = Tag.heute(jetzt, offset)
        val diese = runden.filter { it.zeitpunkt <= jetzt && Tag.heute(it.zeitpunkt, offset) >= heute - (TAGE - 1) }
        val namen = diese.map { it.kind.trim().ifBlank { "" } }.distinct().ifEmpty { listOf("") }
        return namen.map { n -> kindBericht(n, diese.filter { it.kind.trim() == n }, offset) }
    }

    private fun liste(l: List<String>): String =
        if (l.size > 1) l.dropLast(1).joinToString(", ") + " und " + l.last() else l.firstOrNull().orEmpty()

    /** Der Bericht als fertiger Text. */
    fun text(b: KindBericht): String {
        val n = b.name.ifEmpty { "Euer Kind" }
        if (b.runden == 0) return "$n hat in den letzten $TAGE Tagen nicht in YEM1N geübt."
        val s = mutableListOf<String>()
        val tageText = if (b.tage == 1) "an 1 Tag" else "an ${b.tage} Tagen"
        val rundenText = if (b.runden == 1) "1 Runde" else "${b.runden} Runden"
        s.add("$n hat in den letzten $TAGE Tagen $tageText geübt, $rundenText mit ${b.gesamt} Aufgaben.")
        s.add("${b.richtig} davon waren richtig, das sind ${b.prozent} Prozent.")
        if (b.angesehen > 0) {
            s.add(if (b.angesehen == 1) "Einmal wurde die Lösung angeschaut." else "${b.angesehen}-mal wurde die Lösung angeschaut.")
        }
        if (b.stark.isNotEmpty()) s.add("Sicher sitzt: ${liste(b.stark)}.")
        if (b.ueben.isNotEmpty()) s.add("Mehr Übung braucht: ${liste(b.ueben)}.")
        for (m in b.muster) s.add("Auffällig: $n ${m.muster.elternText} (${m.anzahl}-mal, zum Beispiel ${m.beispiel}).")
        b.muster.firstOrNull()?.muster?.elternTipp?.let { s.add("Tipp für zu Hause: $it") }
        if (b.prozent >= 90 && b.runden >= 3) s.add("Eine starke Woche!")
        return s.joinToString(" ")
    }

    /** Alle Berichte zum Teilen als Nachricht. */
    fun gesamtText(berichte: List<KindBericht>): String {
        val z = mutableListOf("📊 YEM1N Wochenbericht")
        for (b in berichte) {
            z.add("")
            z.add("⭐ " + b.name.ifEmpty { "Euer Kind" })
            z.add(text(b))
        }
        return z.joinToString("\n")
    }
}
