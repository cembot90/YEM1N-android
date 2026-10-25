package de.cemaras.yem1n.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.logik.ElternPin
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.TextSanft
import de.cemaras.yem1n.ui.theme.glas

/**
 * Das Tor zum Bereich der Eltern. Gehört das Gerät zu keiner Familie, ist der Bereich offen (es gibt nichts zu schützen).
 * Sonst muss eine PIN festgelegt und eingegeben werden. Gibt true zurück, wenn der Bereich offen ist.
 * Aufruf: `if (!PinTor(vm)) return@Column`.
 */
@Composable
fun ColumnScope.PinTor(vm: AppViewModel): Boolean {
    val fam = vm.familie
    if (!fam.aktiv) return true
    if (fam.pinGesetzt && fam.entsperrt) return true
    if (!fam.pinGesetzt) PinFestlegen(vm) else PinEingeben(vm)
    return false
}

@Composable
private fun PinKarte(inhalt: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = inhalt,
    )
}

@Composable
private fun PinFestlegen(vm: AppViewModel) {
    var pin by remember { mutableStateOf("") }
    var noch by remember { mutableStateOf("") }
    var fehler by remember { mutableStateOf("") }
    PinKarte {
        Text("🔒 Eltern-PIN festlegen", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Gelb)
        Text(
            "Dieser Bereich ist für Mama und Papa. Bitte legt jetzt eine PIN fest, die nur ihr kennt. " +
                "Ohne sie kommen Kinder hier nicht hinein.",
            color = Color.White, fontSize = 15.sp,
        )
        OutlinedTextField(
            value = pin, onValueChange = { pin = it.filter(Char::isDigit).take(ElternPin.MAX_LAENGE); fehler = "" },
            label = { Text("Neue PIN (4 bis 8 Ziffern)") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = noch, onValueChange = { noch = it.filter(Char::isDigit).take(ElternPin.MAX_LAENGE); fehler = "" },
            label = { Text("PIN noch einmal") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        if (fehler.isNotEmpty()) Text(fehler, color = Koralle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Button(
            onClick = { fehler = vm.familie.pinSetzen(pin, noch).orEmpty() },
            enabled = pin.isNotEmpty() && noch.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("PIN festlegen", fontWeight = FontWeight.Bold) }
        Text(
            "Vergessen? Dann nehmt dieses Gerät auf einem anderen Eltern-Gerät aus der Familie und verbindet es neu.",
            color = TextSanft, fontSize = 13.sp,
        )
    }
}

@Composable
private fun PinEingeben(vm: AppViewModel) {
    var pin by remember { mutableStateOf("") }
    var fehler by remember { mutableStateOf("") }
    PinKarte {
        Text("🔒 Nur für Eltern", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Gelb)
        Text("Bitte gib die Eltern-PIN ein.", color = Color.White, fontSize = 15.sp)
        OutlinedTextField(
            value = pin, onValueChange = { pin = it.filter(Char::isDigit).take(ElternPin.MAX_LAENGE); fehler = "" },
            label = { Text("PIN") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        if (fehler.isNotEmpty()) Text(fehler, color = Koralle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Button(
            onClick = {
                val f = vm.familie.pinPruefen(pin)
                pin = ""
                fehler = f.orEmpty()
            },
            enabled = pin.length >= ElternPin.MIN_LAENGE,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Öffnen", fontWeight = FontWeight.Bold) }
        Text(
            "PIN vergessen? Nehmt dieses Gerät auf einem anderen Eltern-Gerät aus der Familie und verbindet es neu.",
            color = TextSanft, fontSize = 13.sp,
        )
    }
}
