package de.cemaras.yem1n.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Die Farbwelt wählen die Eltern in den Einstellungen: Blau (Standard) oder Rosa. */
object Farbwelt {
    var rosa by mutableStateOf(false)
}

// Lacivert und Sarı, die Farben von Fenerbahçe, wie in der iOS-App. In der Rosa-Welt
// sind es dieselben Werte wie dort. Die Werte lesen den Zustand, die Oberfläche
// zeichnet sich deshalb beim Umschalten von selbst neu.
val Navy: Color get() = if (Farbwelt.rosa) Color(0xFF5C124F) else Color(0xFF00205B)
val TiefNavy: Color get() = if (Farbwelt.rosa) Color(0xFF240524) else Color(0xFF000B29)
val Gelb: Color get() = if (Farbwelt.rosa) Color(0xFFFF73BD) else Color(0xFFFFED00)
val Glanz: Color get() = if (Farbwelt.rosa) Color(0xFFEB4DA8) else Color(0xFF2966D9)
val Himmel: Color get() = if (Farbwelt.rosa) Color(0xFFCCADFF) else Color(0xFF73C7FF)
val Koralle = Color(0xFFFF6B6B)
val Mint = Color(0xFF73F29E)
val TextSanft = Color.White.copy(alpha = 0.78f)
val Karte = Color.White.copy(alpha = 0.09f)
