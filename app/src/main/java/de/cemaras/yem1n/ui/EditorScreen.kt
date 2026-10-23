package de.cemaras.yem1n.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.daten.Aufgabe
import de.cemaras.yem1n.daten.Uebung
import de.cemaras.yem1n.logik.AufgabenBauer
import de.cemaras.yem1n.logik.EntwurfListe
import de.cemaras.yem1n.logik.EntwurfPruefer
import de.cemaras.yem1n.logik.FormArt
import de.cemaras.yem1n.logik.FormEingabe
import de.cemaras.yem1n.logik.MatheGenerator
import de.cemaras.yem1n.logik.PaketJson
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.Mint
import de.cemaras.yem1n.ui.theme.TextSanft
import de.cemaras.yem1n.ui.theme.glas

/** Mathe-Editor: eigene Arbeiten bauen, mit Rechner und Prüfung. Entwurf bleibt auf dem Gerät. */
@Composable
fun EditorScreen(vm: AppViewModel) {
    val entwurf = vm.entwurf
    val titel = when {
        entwurf == null -> "Arbeit bauen"
        vm.editorEbene == 0 -> "Entwurf"
        vm.editorEbene == 1 -> entwurf.uebungen.getOrNull(vm.editorUebung)?.titel ?: "Übung"
        else -> if (vm.editorAufgabe < 0) "Neue Aufgabe" else "Aufgabe ändern"
    }
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.weight(1f))
        }
        Text(titel, color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                entwurf == null -> Anfang(vm)
                vm.editorEbene == 0 -> ArbeitEbene(vm, entwurf)
                vm.editorEbene == 1 -> {
                    val u = entwurf.uebungen.getOrNull(vm.editorUebung)
                    if (u == null) androidx.compose.runtime.SideEffect { vm.editorEbene = 0 } else UebungEbene(vm, entwurf, u)
                }
                else -> AufgabeEbene(vm, entwurf)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---- kleine Bausteine -------------------------------------------------------------------------

@Composable
internal fun EdFeld(
    label: String, wert: String, einzeilig: Boolean = true, zahl: Boolean = false, aenderung: (String) -> Unit,
) {
    OutlinedTextField(
        value = wert, onValueChange = aenderung, singleLine = einzeilig, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = if (zahl) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
    )
}

@Composable
internal fun EdKnopf(text: String, betont: Boolean = false, aktiv: Boolean = true, klick: () -> Unit) {
    Text(
        text,
        color = if (!aktiv) Color.White.copy(alpha = 0.4f) else if (betont) Color(0xFF00205B) else Color.White,
        fontWeight = FontWeight.Bold, fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth()
            .then(if (betont && aktiv) Modifier.background(Gelb, RoundedCornerShape(16.dp)) else Modifier.glas(16.dp))
            .clickable(enabled = aktiv, onClick = klick).padding(14.dp),
    )
}

@Composable
internal fun EdKlein(text: String, klick: () -> Unit) {
    Text(
        text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp,
        modifier = Modifier.clickable(onClick = klick).padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
internal fun EdHilfe(text: String) = Text(text, color = TextSanft, fontSize = 14.sp)

@Composable
private fun EdFehler(text: String) = Text(text, color = Koralle, fontSize = 14.sp, fontWeight = FontWeight.Bold)

@Composable
private fun EdWahl(text: String, an: Boolean, klick: () -> Unit) {
    Text(
        text, color = if (an) Color(0xFF00205B) else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
        modifier = Modifier
            .then(if (an) Modifier.background(Gelb, RoundedCornerShape(50)) else Modifier.border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50)))
            .clickable(onClick = klick).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

// ---- Ebene 0: Arbeit --------------------------------------------------------------------------

@Composable
private fun Anfang(vm: AppViewModel) {
    var klasse by remember { mutableStateOf("Klasse 3") }
    var fach by remember { mutableStateOf("Mathe") }
    var name by remember { mutableStateOf("") }
    EdHilfe("Hier baust du eine eigene Arbeit, zum Beispiel für die nächste Klassenarbeit. Der Rechner löst die Aufgaben selbst, so schleichen sich keine falschen Lösungen ein.")
    EdFeld("Klasse", klasse) { klasse = it }
    EdFeld("Fach", fach) { fach = it }
    EdFeld("Titel, zum Beispiel Klassenarbeit Nr. 5", name) { name = it }
    val ok = name.isNotBlank() && klasse.isNotBlank() && fach.isNotBlank()
    EdKnopf("✨ Komplette Arbeit erzeugen (13 Übungen)", betont = true, aktiv = ok) {
        vm.setzeEntwurf(EntwurfListe.ausVorlagen(klasse.trim(), fach.trim(), name.trim(), vm.vorhandeneAufgaben()))
    }
    EdHilfe("Das sind dieselben 13 Übungen wie im Rechenheft, mit neuen Zahlen. Danach kannst du jede Aufgabe ändern.")
    EdKnopf("📝 Leer anfangen", aktiv = ok) {
        vm.setzeEntwurf(EntwurfListe.neu(klasse.trim(), fach.trim(), name.trim()))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArbeitEbene(vm: AppViewModel, e: Arbeit) {
    val kontext = LocalContext.current
    var vorlagen by remember { mutableStateOf(false) }
    var befunde by remember { mutableStateOf<List<String>?>(null) }
    var meldung by remember { mutableStateOf<String?>(null) }
    var verwerfen by remember { mutableStateOf(false) }

    EdFeld("Klasse", e.klasse) { vm.setzeEntwurf(e.copy(klasse = it)) }
    EdFeld("Fach", e.fach) { vm.setzeEntwurf(e.copy(fach = it)) }
    EdFeld("Titel", e.titel) { vm.setzeEntwurf(e.copy(titel = it)) }

    Text("Übungen (${e.uebungen.size})", color = Gelb, fontWeight = FontWeight.Bold)
    if (e.uebungen.isEmpty()) EdHilfe("Noch keine Übung. Nimm eine Vorlage oder lege eine leere an.")
    e.uebungen.forEachIndexed { i, u ->
        Row(Modifier.fillMaxWidth().glas(16.dp).padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f).clickable { vm.editorUebung = i; vm.editorEbene = 1 }.padding(vertical = 6.dp),
            ) {
                Text("${u.symbol}  ${u.titel}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${u.aufgaben.size} Aufgaben · ${u.gruppe}", color = TextSanft, fontSize = 13.sp)
            }
            EdKlein("↑") { vm.setzeEntwurf(e.copy(uebungen = EntwurfListe.verschiebe(e.uebungen, i, i - 1))) }
            EdKlein("↓") { vm.setzeEntwurf(e.copy(uebungen = EntwurfListe.verschiebe(e.uebungen, i, i + 1))) }
            EdKlein("🗑") { vm.setzeEntwurf(e.copy(uebungen = e.uebungen.filterIndexed { j, _ -> j != i })) }
        }
    }
    EdKnopf("＋ Übung aus Vorlage") { vorlagen = true }
    EdKnopf("＋ Leere Übung") {
        vm.setzeEntwurf(e.copy(uebungen = e.uebungen + EntwurfListe.uebungNeu("Neue Übung")))
        vm.editorUebung = e.uebungen.size
        vm.editorEbene = 1
    }

    Spacer(Modifier.height(6.dp))
    EdKnopf("🔍 Prüfen") {
        val b = EntwurfPruefer.pruefe(e)
        befunde = if (b.isEmpty()) listOf("Alles in Ordnung. Alle Lösungen stimmen.") else b.map { (if (it.schwer) "Fehler: " else "Hinweis: ") + it.text }
    }
    EdKnopf("✅ Speichern als Arbeit", betont = true) {
        val f = vm.entwurfSpeichern()
        meldung = f ?: "Die Arbeit ist gespeichert und steht jetzt auf dem Startbildschirm."
    }
    EdKnopf("📤 JSON teilen") {
        val absicht = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, e.titel)
            putExtra(Intent.EXTRA_TEXT, PaketJson.text(e))
        }
        kontext.startActivity(Intent.createChooser(absicht, "Arbeit teilen"))
    }
    EdKnopf("🗑 Entwurf verwerfen") { verwerfen = true }

    meldung?.let { m ->
        AlertDialog(onDismissRequest = { meldung = null }, confirmButton = { TextButton({ meldung = null }) { Text("OK") } }, text = { Text(m) })
    }
    befunde?.let { b ->
        AlertDialog(
            onDismissRequest = { befunde = null }, title = { Text("Prüfung") },
            confirmButton = { TextButton({ befunde = null }) { Text("OK") } },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { b.forEach { Text("• $it", Modifier.padding(vertical = 3.dp)) } } },
        )
    }
    if (verwerfen) {
        AlertDialog(
            onDismissRequest = { verwerfen = false }, title = { Text("Entwurf verwerfen?") },
            text = { Text("Alles, was du hier gebaut hast, geht verloren.") },
            confirmButton = { TextButton({ verwerfen = false; vm.setzeEntwurf(null) }) { Text("Verwerfen") } },
            dismissButton = { TextButton({ verwerfen = false }) { Text("Abbrechen") } },
        )
    }
    if (vorlagen) {
        AlertDialog(
            onDismissRequest = { vorlagen = false }, title = { Text("Vorlage wählen") },
            confirmButton = { TextButton({ vorlagen = false }) { Text("Schließen") } },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    MatheGenerator.vorlagen.forEach { v ->
                        Text(
                            "${v.symbol}  ${v.name} (${v.anzahl})",
                            Modifier.fillMaxWidth().clickable {
                                vorlagen = false
                                val u = EntwurfListe.uebungAusVorlage(v, vm.vorhandeneAufgaben())
                                vm.setzeEntwurf(e.copy(uebungen = e.uebungen + u))
                            }.padding(vertical = 10.dp),
                        )
                    }
                }
            },
        )
    }
}

// ---- Ebene 1: Übung ---------------------------------------------------------------------------

private fun Arbeit.mitUebung(i: Int, u: Uebung) = copy(uebungen = uebungen.mapIndexed { j, x -> if (j == i) u else x })

@Composable
private fun UebungEbene(vm: AppViewModel, e: Arbeit, u: Uebung) {
    val i = vm.editorUebung
    EdFeld("Titel", u.titel) { vm.setzeEntwurf(e.mitUebung(i, u.copy(titel = it))) }
    EdFeld("Gruppe", u.gruppe) { vm.setzeEntwurf(e.mitUebung(i, u.copy(gruppe = it))) }
    EdFeld("Symbol", u.symbol) { vm.setzeEntwurf(e.mitUebung(i, u.copy(symbol = it))) }
    EdFeld("Tipp (freiwillig)", u.tipp, einzeilig = false) { vm.setzeEntwurf(e.mitUebung(i, u.copy(tipp = it))) }

    Text("Aufgaben (${u.aufgaben.size})", color = Gelb, fontWeight = FontWeight.Bold)
    u.aufgaben.forEachIndexed { k, a ->
        Row(Modifier.fillMaxWidth().glas(16.dp).padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable { vm.editorAufgabe = k; vm.editorEbene = 2 }.padding(vertical = 6.dp)) {
                Text(vorschau(a), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2)
                Text(loesung(a), color = Mint, fontSize = 13.sp)
            }
            EdKlein("↑") { vm.setzeEntwurf(e.mitUebung(i, u.copy(aufgaben = EntwurfListe.verschiebe(u.aufgaben, k, k - 1)))) }
            EdKlein("↓") { vm.setzeEntwurf(e.mitUebung(i, u.copy(aufgaben = EntwurfListe.verschiebe(u.aufgaben, k, k + 1)))) }
            EdKlein("🗑") { vm.setzeEntwurf(e.mitUebung(i, u.copy(aufgaben = u.aufgaben.filterIndexed { j, _ -> j != k }))) }
        }
    }
    EdKnopf("＋ Aufgabe hinzufügen", betont = true) { vm.editorAufgabe = -1; vm.editorEbene = 2 }
}

internal fun vorschau(a: Aufgabe): String =
    if (a.istMauer) "Zahlenmauer, unten ${a.reihen.firstOrNull()?.joinToString(" ") ?: ""}"
    else a.rechnung.replace("\n", " ").ifBlank { a.frage }

internal fun loesung(a: Aufgabe): String = when {
    a.istMauer -> "Spitze ${a.reihen.lastOrNull()?.firstOrNull() ?: ""}"
    a.art == Aufgabe.ART_REST -> "${a.antwort} Rest ${a.antwort2}"
    else -> a.antwort
}

// ---- Ebene 2: Aufgabe -------------------------------------------------------------------------

@Composable
private fun AufgabeEbene(vm: AppViewModel, e: Arbeit) {
    val i = vm.editorUebung
    val u = e.uebungen.getOrNull(i) ?: return
    val k = vm.editorAufgabe
    val alt = u.aufgaben.getOrNull(k)
    if (k >= 0 && alt != null) {
        AufgabeAendern(vm, e, u, alt)
    } else {
        AufgabeNeu(vm, e, u)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AufgabeNeu(vm: AppViewModel, e: Arbeit, u: Uebung) {
    var f by remember { mutableStateOf(FormEingabe(FormArt.RECHNUNG)) }
    var hinzugefuegt by remember { mutableStateOf(0) }
    val ergebnis = AufgabenBauer.baue(f)

    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormArt.values().forEach { art -> EdWahl(art.titel.substringBefore(" ("), f.art == art) { f = FormEingabe(art) } }
    }
    EdFeld("Frage (leer lassen für Standard)", f.frage) { f = f.copy(frage = it) }
    when (f.art) {
        FormArt.RECHNUNG -> {
            EdFeld("Rechnung, zum Beispiel 7 · 8 oder 9*9+8", f.ausdruck) { f = f.copy(ausdruck = it) }
            EdFeld("Hinweis (freiwillig)", f.hinweis) { f = f.copy(hinweis = it) }
        }
        FormArt.REST -> {
            EdFeld("Zahl, die geteilt wird", f.zahlA, zahl = true) { f = f.copy(zahlA = it.take(3)) }
            EdFeld("Geteilt durch", f.zahlB, zahl = true) { f = f.copy(zahlB = it.take(2)) }
        }
        FormArt.VERGLEICH -> {
            EdFeld("Links, zum Beispiel 5 · 5", f.links) { f = f.copy(links = it) }
            EdFeld("Rechts, zum Beispiel 9 · 7", f.rechts) { f = f.copy(rechts = it) }
        }
        FormArt.MAUER -> {
            EdHilfe("Die vier Zahlen der untersten Reihe. Der Rest wird ausgerechnet.")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (n in 0 until 4) {
                    Box(Modifier.weight(1f)) {
                        EdFeld("${n + 1}", f.mauer[n], zahl = true) { w ->
                            f = f.copy(mauer = f.mauer.mapIndexed { j, x -> if (j == n) w.take(2) else x })
                        }
                    }
                }
            }
        }
        FormArt.RAETSEL -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EdWahl("gerade", f.gerade) { f = f.copy(gerade = true) }
                EdWahl("ungerade", !f.gerade) { f = f.copy(gerade = false) }
            }
            EdHilfe("Sie ist eine ${f.xer}er-Zahl")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (x in 2..10) EdWahl("$x", f.xer == x) { f = f.copy(xer = x) }
            }
            EdFeld("Liegt zwischen", f.von, zahl = true) { f = f.copy(von = it.take(3)) }
            EdFeld("und", f.bis, zahl = true) { f = f.copy(bis = it.take(4)) }
        }
        FormArt.SACH -> {
            EdFeld("Text der Aufgabe", f.text, einzeilig = false) { f = f.copy(text = it) }
            EdFeld("Rechnung für die Lösung, zum Beispiel 4 · 6", f.ausdruck) { f = f.copy(ausdruck = it) }
        }
        FormArt.WORT -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EdWahl("Minus", f.wortMinus) { f = f.copy(wortMinus = true) }
                EdWahl("Mal", !f.wortMinus) { f = f.copy(wortMinus = false) }
                EdWahl("Satz 1", f.wortVariante == 0) { f = f.copy(wortVariante = 0) }
                EdWahl("Satz 2", f.wortVariante == 1) { f = f.copy(wortVariante = 1) }
            }
            EdFeld("Erste Zahl", f.zahlA, zahl = true) { f = f.copy(zahlA = it.take(3)) }
            EdFeld("Zweite Zahl", f.zahlB, zahl = true) { f = f.copy(zahlB = it.take(3)) }
        }
        FormArt.FREI -> {
            EdFeld("Aufgabe", f.freiRechnung, einzeilig = false) { f = f.copy(freiRechnung = it) }
            EdFeld("Lösung (0 bis 999)", f.freiAntwort, zahl = true) { f = f.copy(freiAntwort = it.take(3)) }
            EdFeld("Hinweis (freiwillig)", f.hinweis) { f = f.copy(hinweis = it) }
        }
    }

    val a = ergebnis.aufgabe
    if (a != null) {
        Column(Modifier.fillMaxWidth().glas(16.dp).padding(14.dp)) {
            Text("So sieht es aus", color = Gelb, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(a.frage, color = TextSanft, fontSize = 14.sp)
            Text(vorschau(a), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Lösung: ${loesung(a)}", color = Mint, fontWeight = FontWeight.Bold)
        }
        ergebnis.hinweis?.let { EdHilfe(it) }
    } else if (ergebnis.fehler != null && formAngefangen(f)) {
        EdFehler(ergebnis.fehler)
    }
    if (hinzugefuegt > 0) EdHilfe("$hinzugefuegt Aufgabe(n) hinzugefügt.")
    EdKnopf("＋ Hinzufügen und weiter", betont = true, aktiv = a != null) {
        if (a != null) {
            val i = vm.editorUebung
            vm.setzeEntwurf(e.mitUebung(i, u.copy(aufgaben = u.aufgaben + a)))
            f = FormEingabe(f.art)
            hinzugefuegt++
        }
    }
    EdKnopf("Fertig", aktiv = true) { vm.editorEbene = 1 }
}

private fun formAngefangen(f: FormEingabe): Boolean =
    listOf(f.ausdruck, f.zahlA, f.zahlB, f.links, f.rechts, f.von, f.bis, f.text, f.freiRechnung, f.freiAntwort).any { it.isNotBlank() } ||
        f.mauer.any { it.isNotBlank() }

@Composable
private fun AufgabeAendern(vm: AppViewModel, e: Arbeit, u: Uebung, alt: Aufgabe) {
    val i = vm.editorUebung
    val k = vm.editorAufgabe
    var a by remember(k) { mutableStateOf(alt) }
    fun speichern(neu: Aufgabe) {
        a = neu
        vm.setzeEntwurf(e.mitUebung(i, u.copy(aufgaben = u.aufgaben.mapIndexed { j, x -> if (j == k) neu else x })))
    }
    EdHilfe("Art: ${a.art}. Hier änderst du die Felder direkt. Vor dem Speichern der Arbeit rechnet die Prüfung noch einmal nach.")
    EdFeld("Frage", a.frage) { speichern(a.copy(frage = it)) }
    if (a.istMauer) {
        a.reihen.forEachIndexed { r, reihe -> Text("Reihe ${r + 1}: ${reihe.joinToString("  ")}", color = Color.White) }
        EdHilfe("Eine Zahlenmauer änderst du, indem du sie löschst und neu anlegst.")
    } else {
        EdFeld("Aufgabe", a.rechnung, einzeilig = false) { speichern(a.copy(rechnung = it)) }
        EdFeld(if (a.art == Aufgabe.ART_VERGLEICH) "Antwort (<, > oder =)" else "Antwort", a.antwort) { speichern(a.copy(antwort = it)) }
        if (a.art == Aufgabe.ART_REST) EdFeld("Rest", a.antwort2, zahl = true) { speichern(a.copy(antwort2 = it)) }
    }
    EdFeld("Hinweis (freiwillig)", a.hinweis) { speichern(a.copy(hinweis = it)) }
    EdFeld("Erklärung (freiwillig)", a.erklaerung, einzeilig = false) { speichern(a.copy(erklaerung = it)) }
    EdKnopf("Fertig", betont = true) { vm.editorEbene = 1 }
}
