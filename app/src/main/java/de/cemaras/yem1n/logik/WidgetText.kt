package de.cemaras.yem1n.logik

/** Was das Haustier-Widget auf dem Startbildschirm zeigt. */
data class WidgetText(val emoji: String, val titel: String, val zeile: String) {
    companion object {
        fun von(f: Fortschritt, jetzt: Long, heute: Long): WidgetText {
            val h = f.haustier?.aktualisiert(jetzt)
                ?: return WidgetText("🥚", "Haustier", "Such dir eins aus")
            val serie = f.serie(heute).first
            val stimmung = when (h.stimmung) {
                HaustierStimmung.FROH -> "froh"
                HaustierStimmung.ZUFRIEDEN -> "zufrieden"
                HaustierStimmung.HUNGRIG -> "hungrig"
                HaustierStimmung.TRAURIG -> "traurig"
            }
            val hut = HaustierHut.finde(h.hut)?.emoji.orEmpty()
            val tag = if (serie == 1) "Tag" else "Tage"
            val zeile = if (h.stufe == HaustierStufe.EI) "wartet auf dich" else stimmung
            return WidgetText(hut + h.emoji, h.name, if (serie > 0) "$zeile\n🔥 $serie $tag" else zeile)
        }
    }
}
