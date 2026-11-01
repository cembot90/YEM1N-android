package de.cemaras.yem1n.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.logik.ElternFrage
import de.cemaras.yem1n.logik.ElternPin
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.TextSanft
import de.cemaras.yem1n.ui.theme.glas

/**
 * Das Erwachsenen-Tor steht vor allem, was aus der App hinausführt (Links, Mail, Teilen), und vor den Einstellungen.
 * Es gilt auf jedem Gerät, auch ohne Familie. Mit Eltern-PIN reicht die PIN, sonst eine Malaufgabe, die Kinder bis
 * Klasse 4 nicht im Kopf rechnen. Danach bleibt das Tor fünf Minuten offen. Wie iOS (`ErwachsenenTor.swift`).
 */
object Tor {
    private var offenBis: Long by mutableLongStateOf(0L)

    fun offen(vm: AppViewModel): Boolean =
        System.currentTimeMillis() < offenBis || (vm.familie.pinGesetzt && vm.familie.entsperrt)

    fun oeffnen() {
        offenBis = System.currentTimeMillis() + ElternPin.OFFEN_MS
    }

    /** Prüft die Eingabe: richtige Malaufgabe oder Eltern-PIN. */
    fun pruefen(vm: AppViewModel, frage: ElternFrage, eingabe: String): Boolean {
        var richtig = frage.stimmt(eingabe)
        // Nur Eingaben, die eine PIN sein können, zählen als PIN-Versuch (sonst gäbe es unnötig Wartezeit).
        if (!richtig && vm.familie.pinGesetzt && ElternPin.gueltig(eingabe)) richtig = vm.familie.pinPruefen(eingabe) == null
        if (richtig) oeffnen()
        return richtig
    }
}

@Composable
private fun TorFelder(
    vm: AppViewModel, frage: ElternFrage, eingabe: String, fehler: String,
    farbe: Color = Color.White, onEingabe: (String) -> Unit,
) {
    Text("Hier geht es zu den Einstellungen oder aus der App hinaus. Bitte frag Mama oder Papa.", color = farbe, fontSize = 15.sp)
    Text(frage.text, color = farbe, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    OutlinedTextField(
        value = eingabe, onValueChange = { onEingabe(it.filter(Char::isDigit).take(8)) },
        label = { Text("Ergebnis") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
    if (vm.familie.pinGesetzt) Text("Oder die Eltern-PIN eingeben.", color = TextSanft, fontSize = 13.sp)
    if (fehler.isNotEmpty()) Text(fehler, color = Koralle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
}

private const val FALSCH = "Das stimmt leider nicht. Hier ist eine neue Aufgabe."

/**
 * Ein ganzer Bereich hinter dem Tor, zum Beispiel die Einstellungen. Gibt true zurück, wenn der Bereich offen ist.
 * Aufruf: `if (!ErwachsenenTor(vm)) return@Column`.
 */
@Composable
fun ColumnScope.ErwachsenenTor(vm: AppViewModel): Boolean {
    if (Tor.offen(vm)) return true
    var frage by remember { mutableStateOf(ElternFrage.neu()) }
    var eingabe by remember { mutableStateOf("") }
    var fehler by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("🔒 Nur für Erwachsene", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Gelb)
        TorFelder(vm, frage, eingabe, fehler) { eingabe = it; fehler = "" }
        Button(
            onClick = {
                if (!Tor.pruefen(vm, frage, eingabe)) {
                    fehler = FALSCH
                    frage = ElternFrage.neu()
                    eingabe = ""
                }
            },
            enabled = eingabe.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Weiter", fontWeight = FontWeight.Bold) }
    }
    return false
}

/**
 * Für einzelne Knöpfe (Teilen, Mail, Browser): `val tor = mitTor(vm)` und dann `tor { ... }`.
 * Ist das Tor offen, läuft die Aktion sofort, sonst erst nach der richtigen Antwort.
 */
@Composable
fun mitTor(vm: AppViewModel): (() -> Unit) -> Unit {
    var wartend by remember { mutableStateOf<(() -> Unit)?>(null) }
    wartend?.let { aktion ->
        var frage by remember { mutableStateOf(ElternFrage.neu()) }
        var eingabe by remember { mutableStateOf("") }
        var fehler by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { wartend = null },
            title = { Text("🔒 Nur für Erwachsene") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TorFelder(vm, frage, eingabe, fehler, farbe = Color.Unspecified) { eingabe = it; fehler = "" }
                }
            },
            confirmButton = {
                TextButton(enabled = eingabe.isNotEmpty(), onClick = {
                    if (Tor.pruefen(vm, frage, eingabe)) {
                        wartend = null
                        aktion()
                    } else {
                        fehler = FALSCH
                        frage = ElternFrage.neu()
                        eingabe = ""
                    }
                }) { Text("Weiter") }
            },
            dismissButton = { TextButton(onClick = { wartend = null }) { Text("Abbrechen") } },
        )
    }
    return { aktion -> if (Tor.offen(vm)) aktion() else wartend = aktion }
}
