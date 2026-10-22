package de.cemaras.yem1n.logik

import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.daten.Aufgabe
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Eine Arbeit als Text im Importformat, gleich wie PaketJSON.text bei iOS. */
object PaketJson {
    fun aufgabe(a: Aufgabe): JsonObject = buildJsonObject {
        put("art", a.art)
        put("frage", a.frage)
        put("rechnung", a.rechnung)
        put("antwort", a.antwort)
        if (a.hinweis.isNotEmpty()) put("hinweis", a.hinweis)
        if (a.erklaerung.isNotEmpty()) put("erklaerung", a.erklaerung)
        if (a.antwort2.isNotEmpty()) put("antwort2", a.antwort2)
        if (a.istMauer) put("reihen", JsonArray(a.reihen.map { r -> JsonArray(r.map { JsonPrimitive(it) }) }))
        if (a.istWahl) {
            put("bild", a.bild)
            if (a.tr.isNotEmpty()) put("tr", a.tr)
            if (a.gross.isNotEmpty()) put("gross", a.gross)
            if (a.folge.isNotEmpty()) put("folge", buildJsonArray { a.folge.forEach { add(JsonPrimitive(it)) } })
            put("optionen", buildJsonArray { a.optionen.forEach { add(JsonPrimitive(it)) } })
        }
    }

    fun text(arbeit: Arbeit): String = buildJsonObject {
        put("klasse", arbeit.klasse)
        put("fach", arbeit.fach)
        put("arbeit", arbeit.titel)
        put("uebungen", buildJsonArray {
            for (u in arbeit.uebungen) add(buildJsonObject {
                put("titel", u.titel)
                put("gruppe", u.gruppe)
                put("symbol", u.symbol)
                if (u.tipp.isNotEmpty()) put("tipp", u.tipp)
                put("aufgaben", buildJsonArray { u.aufgaben.forEach { add(aufgabe(it)) } })
            })
        })
    }.toString()
}
