package de.cemaras.yem1n.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.logik.Bundesland
import de.cemaras.yem1n.logik.LehrFach
import de.cemaras.yem1n.logik.LernThema
import de.cemaras.yem1n.logik.Lernwelt
import de.cemaras.yem1n.logik.Schuljahr
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Mint
import de.cemaras.yem1n.ui.theme.Navy
import de.cemaras.yem1n.ui.theme.TextSanft
import de.cemaras.yem1n.ui.theme.glas

@Composable
fun LernweltScreen(vm: AppViewModel) {
    val land = vm.bundesland
    val eigene = Lernwelt.klasse(vm.fortschritt.lernKlasse)
    var fachName by rememberSaveable { mutableStateOf(LehrFach.MATHE.name) }
    var andereOffen by rememberSaveable { mutableStateOf(false) }
    var landMenue by rememberSaveable { mutableStateOf(false) }
    val fach = LehrFach.valueOf(fachName)

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🌍", fontSize = 44.sp)
            Column(Modifier.weight(1f)) {
                Text("Lernwelt", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("Aufgaben nach dem Lehrplan, immer wieder neu", color = TextSanft, fontSize = 14.sp)
            }
            Box {
                Chip(land.kurz, true) { landMenue = true }
                androidx.compose.material3.DropdownMenu(expanded = landMenue, onDismissRequest = { landMenue = false }) {
                    for (b in Bundesland.entries) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(b.anzeige) },
                            onClick = { vm.waehleBundesland(b); landMenue = false },
                        )
                    }
                }
            }
        }

        // Klasse des Kindes
        Text("Meine Klasse", color = TextSanft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (k in 1..4) Chip("Klasse $k", eigene == k) { vm.waehleLernKlasse(if (eigene == k) 0 else k) }
        }

        // Fächer
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (f in LehrFach.entries) Chip("${f.emoji} ${f.anzeige}", f == fach) { fachName = f.name }
        }

        val reihen = Lernwelt.klassenReihenfolge(eigene)
        reihen.forEachIndexed { i, k ->
            if (eigene == null || i == 0 || andereOffen) KlassenAbschnitt(vm, fach, k, k == eigene, land)
            if (eigene != null && i == 0) {
                Text(
                    if (andereOffen) "▲ Andere Klassen ausblenden" else "▼ Andere Klassen zum Wiederholen oder Vorgreifen",
                    color = Gelb, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .background(Gelb.copy(alpha = 0.12f), CircleShape)
                        .clickable { andereOffen = !andereOffen }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
        Text(
            "Die Aufgaben folgen dem ${land.lehrplan} und den Bildungsstandards der Kultusministerkonferenz. " +
                "Mathe und Deutsch stehen in der Reihenfolge, in der die meisten Schulbücher sie im Schuljahr bringen. " +
                "Schulbücher und Lehrkräfte setzen die Reihenfolge manchmal etwas anders.",
            color = TextSanft, fontSize = 12.sp,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Chip(text: String, aktiv: Boolean, klick: () -> Unit) {
    Text(
        text,
        color = if (aktiv) Navy else Color.White,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        modifier = Modifier
            .heightIn(min = 38.dp)
            .background(if (aktiv) Gelb else Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable(onClick = klick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun KlassenAbschnitt(vm: AppViewModel, fach: LehrFach, klasse: Int, eigene: Boolean, land: Bundesland) {
    val liste = Lernwelt.themenFuer(fach, klasse, land)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Klasse $klasse", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            if (eigene) {
                Text(
                    "deine Klasse", color = Navy, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.background(Gelb, CircleShape).padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        if (liste.isEmpty()) {
            Text(
                if (fach == LehrFach.ENGLISCH) "Englisch steht ab Klasse 3 im Lehrplan." else "Für diese Klasse kommen bald Themen dazu.",
                color = TextSanft, fontSize = 13.sp,
            )
        }
        for (g in Schuljahr.gruppiert(liste, land)) {
            g.abschnitt?.let { ZeitKopf(it, eigene && it == Schuljahr.jetzt()) }
            for (t in g.themen) ThemaKarte(vm, t)
        }
    }
}

@Composable
private fun ZeitKopf(abschnitt: Int, jetzt: Boolean) {
    Row(
        Modifier.padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            Schuljahr.titel(abschnitt), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
            color = if (jetzt) Gelb else Color.White.copy(alpha = 0.85f),
        )
        val monate = Schuljahr.monate(abschnitt)
        if (monate.isNotEmpty()) Text(monate, color = TextSanft, fontSize = 12.sp)
        if (jetzt) {
            Text(
                "gerade dran", color = Navy, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.background(Gelb, CircleShape).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun ThemaKarte(vm: AppViewModel, t: LernThema) {
    val sterne = vm.lernSterne(t)
    Row(
        Modifier
            .fillMaxWidth()
            .glas(22.dp)
            .clickable { vm.startLernThema(t) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(52.dp).background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(t.emoji, fontSize = 28.sp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(t.titel, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color.White, modifier = Modifier.weight(1f, fill = false))
                if (!t.lehrplan) {
                    Text(
                        "Extra", color = Navy, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.background(Mint, CircleShape).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(t.beschreibung, color = TextSanft, fontSize = 12.sp)
        }
        Text(
            "★".repeat(sterne) + "☆".repeat(3 - sterne),
            color = if (sterne > 0) Gelb else Color.White.copy(alpha = 0.35f),
            fontSize = 14.sp,
        )
    }
}
