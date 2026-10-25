package de.cemaras.yem1n.speicher

import android.content.Context
import de.cemaras.yem1n.logik.FamilienZugang
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Merkt sich, zu welcher Familie dieses Gerät gehört. Die Datei kommt in keine Sicherung, denn ohne den Schlüssel
 * aus dem Keystore nützt sie nichts.
 */
class FamilienSpeicher(context: Context) {
    private val prefs = context.getSharedPreferences("familie", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun laden(): FamilienZugang? = try {
        prefs.getString("zugang", null)?.let { json.decodeFromString<FamilienZugang>(it) }
    } catch (e: Exception) {
        null
    }

    fun speichern(zugang: FamilienZugang?) {
        val e = prefs.edit()
        if (zugang == null) {
            // Ohne Familie gibt es nichts zu schützen, die PIN fällt mit weg.
            e.remove("zugang").remove("pinSalz").remove("pinHash").remove("pinFehler").remove("pinSperreBis")
        } else {
            e.putString("zugang", json.encodeToString(zugang))
        }
        e.apply()
    }

    // Eltern-PIN: nur Salz und Wert zum Prüfen, nie die PIN selbst. Liegt in derselben Datei wie die Familie (nicht in der Sicherung).
    var pinSalz: String
        get() = prefs.getString("pinSalz", "").orEmpty()
        set(v) { prefs.edit().putString("pinSalz", v).apply() }

    var pinHash: String
        get() = prefs.getString("pinHash", "").orEmpty()
        set(v) { prefs.edit().putString("pinHash", v).apply() }

    /** Falsche Eingaben in Folge. */
    var pinFehler: Int
        get() = prefs.getInt("pinFehler", 0)
        set(v) { prefs.edit().putInt("pinFehler", v).apply() }

    /** Bis wann die Sperre nach zu vielen Fehlern gilt (Uhrzeit des Geräts in Millisekunden). */
    var pinSperreBis: Long
        get() = prefs.getLong("pinSperreBis", 0L)
        set(v) { prefs.edit().putLong("pinSperreBis", v).apply() }

    /** Der Zeitunterschied zum Dienst, damit nicht bei jedem Start neu ausgeglichen werden muss. */
    var versatz: Long
        get() = prefs.getLong("versatz", 0L)
        set(v) { prefs.edit().putLong("versatz", v).apply() }
}
