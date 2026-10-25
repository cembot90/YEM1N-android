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
        if (zugang == null) e.remove("zugang") else e.putString("zugang", json.encodeToString(zugang))
        e.apply()
    }

    /** Der Zeitunterschied zum Dienst, damit nicht bei jedem Start neu ausgeglichen werden muss. */
    var versatz: Long
        get() = prefs.getLong("versatz", 0L)
        set(v) { prefs.edit().putLong("versatz", v).apply() }
}
