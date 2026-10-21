package de.cemaras.yem1n.logik

/** Die Spiele, die man mit Münzen freispielt. */
enum class SpielArt(
    val schluessel: String, val titel: String, val emoji: String, val beschreibung: String,
    /** Gibt es das Spiel schon auf Android? */
    val verfuegbar: Boolean,
) {
    ELFMETER(
        "elfmeter", "Elfmeterschießen", "⚽",
        "Wische vom Ball zum Tor. Der Torwart wird immer schneller. Der Winkel bringt 3 Punkte.", false,
    ),
    EINHORN(
        "einhorn", "Einhorn-Flug", "🦄",
        "Tippe, dann fliegt das Einhorn hoch. Fang Sterne und Regenbögen, weich den Gewitterwolken aus.", true,
    ),
    LAUF("lauf", "Zahlenlauf", "🏃", "Spring über die Kakteen und sammle Münzen. Es wird immer schneller.", false),
    JAGD("jagd", "Münzjagd", "🪙", "Sammle auf den Plattformen alle Münzen ein. Weich den Fliegern aus.", false),
    HOCKEY("hockey", "Münz-Hockey", "🏒", "Tischhockey gegen den Computer. Wer zuerst fünf Tore hat, gewinnt.", false),
}

object SpielRegeln {
    /** Was eine Spielrunde kostet. */
    const val PREIS = 15

    /** Grund, warum gerade nicht gespielt werden kann, sonst null. */
    fun sperre(f: Fortschritt, heute: Long): String? {
        if (!f.spieleErlaubt) return "Die Spiele sind in den Einstellungen ausgeschaltet. Frag deine Eltern."
        if (!f.heuteGeuebt(heute)) return "Erst eine Übung schaffen, dann darfst du spielen."
        val gespielt = f.spieleHeute(heute)
        if (gespielt >= f.spieleProTag) {
            return "Für heute ist Schluss. Du hast schon $gespielt von ${f.spieleProTag} Spielen gemacht."
        }
        if (f.muenzen < PREIS) return "Du brauchst $PREIS Münzen. Du hast ${f.muenzen}. Übe weiter, dann klappt es."
        return null
    }
}
