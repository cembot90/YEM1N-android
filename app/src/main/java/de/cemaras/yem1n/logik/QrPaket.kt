package de.cemaras.yem1n.logik

/** Inhalt der QR-Codes, gleich wie bei iOS: yem1n://familie/XXXXX-XXXXX */
object QrPaket {
    fun inhalt(art: String, code: String): String = "yem1n://$art/$code"

    /** Gibt Art ("familie" oder "klasse") und den gültigen Code zurück, sonst null. */
    fun lese(text: String): Pair<String, String>? {
        val t = text.trim()
        val vorsatz = "yem1n://"
        if (!t.lowercase().startsWith(vorsatz)) return null
        val teile = t.substring(vorsatz.length).split("/").filter { it.isNotEmpty() }
        if (teile.size != 2) return null
        val art = teile[0].lowercase()
        if (art != "familie" && art != "klasse") return null
        val code = Familiencode.pruefe(teile[1]) ?: return null
        return art to code
    }
}
