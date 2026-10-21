package de.cemaras.yem1n.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import de.cemaras.yem1n.daten.InfoTexte
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Karte
import de.cemaras.yem1n.ui.theme.glas
import de.cemaras.yem1n.ui.theme.Navy
import de.cemaras.yem1n.ui.theme.TextSanft

/** Welche Seite im Info-Bereich offen ist. */
enum class InfoSeite(val titel: String) {
    ANLEITUNG("Anleitung"),
    DATENSCHUTZ("Datenschutzhinweise"),
    IMPRESSUM("Impressum"),
    BETA("Beta und Haftung"),
}

private fun versionsName(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
} catch (e: Exception) {
    "?"
}

private fun geraet(): String = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}"

@Composable
fun InfoScreen(vm: AppViewModel) {
    val kontext = LocalContext.current
    val version = remember { versionsName(kontext) }
    var kopiert by remember { mutableStateOf(false) }
    val infoText = "YEM1N $version, ${geraet()}"
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        Text("YEM1N", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Gelb)
        Text("Version $version", color = TextSanft, fontWeight = FontWeight.SemiBold)
        Text(geraet(), color = TextSanft, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        InfoZeile("📖", "Anleitung") { vm.oeffneInfoSeite(InfoSeite.ANLEITUNG) }
        InfoZeile("🤚", "Datenschutzhinweise") { vm.oeffneInfoSeite(InfoSeite.DATENSCHUTZ) }
        InfoZeile("🪪", "Impressum") { vm.oeffneInfoSeite(InfoSeite.IMPRESSUM) }
        InfoZeile("⚠️", "Beta und Haftung") { vm.oeffneInfoSeite(InfoSeite.BETA) }
        InfoZeile("✉️", "Feedback an den Admin") { feedback(kontext, infoText) }
        InfoZeile(if (kopiert) "✅" else "📋", if (kopiert) "Kopiert" else "Versionsinfo kopieren") {
            val zwischen = kontext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            zwischen.setPrimaryClip(ClipData.newPlainText("YEM1N", infoText))
            kopiert = true
        }
        Text(
            "Fehler gefunden oder eine Idee? Schreibe kurz, was passiert ist. Version und Gerät hängen automatisch an der Nachricht.",
            fontSize = 13.sp, color = TextSanft,
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun feedback(kontext: Context, infoText: String) {
    val text = "\n\n\nBitte oben schreiben, hier nicht löschen:\n$infoText"
    val absicht = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(InfoTexte.MAIL))
        putExtra(Intent.EXTRA_SUBJECT, "YEM1N Feedback")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        kontext.startActivity(absicht)
    } catch (e: Exception) {
        // Keine Mail-App auf dem Gerät: nichts tun, die Adresse steht im Impressum.
    }
}

@Composable
private fun InfoZeile(symbol: String, titel: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .glas(18.dp)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(symbol, fontSize = 22.sp)
        Spacer(Modifier.padding(start = 12.dp))
        Text(titel, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
        Text("›", fontSize = 22.sp, color = Gelb, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoSeiteScreen(vm: AppViewModel, seite: InfoSeite) {
    when (seite) {
        InfoSeite.ANLEITUNG -> AnleitungScreen(vm)
        InfoSeite.DATENSCHUTZ -> TextSeite(vm, seite.titel, InfoTexte.datenschutz, InfoTexte.SEITE)
        InfoSeite.IMPRESSUM -> TextSeite(vm, seite.titel, InfoTexte.impressum, InfoTexte.SEITE + "impressum.html")
        InfoSeite.BETA -> TextSeite(vm, seite.titel, InfoTexte.beta, InfoTexte.SEITE + "nutzung.html")
    }
}

@Composable
private fun TextSeite(vm: AppViewModel, titel: String, abschnitte: List<Pair<String, String>>, webAdresse: String) {
    val kontext = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        Text(titel, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
        abschnitte.forEach { (kopf, text) ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).glas(18.dp).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(kopf, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Gelb)
                Text(text, fontSize = 15.sp, color = Color.White)
            }
        }
        Button(
            onClick = { oeffneImBrowser(kontext, webAdresse) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) { Text("Im Browser öffnen", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(24.dp))
    }
}

private fun oeffneImBrowser(kontext: Context, adresse: String) {
    try {
        kontext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(adresse)))
    } catch (e: Exception) {
        // Kein Browser gefunden: nichts tun.
    }
}

/** Die ausführliche Anleitung von der Webseite, ohne Internet die Kurzfassung. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AnleitungScreen(vm: AppViewModel) {
    var laedt by remember { mutableStateOf(true) }
    var fehler by remember { mutableStateOf(false) }
    val adresse = InfoTexte.anleitungAdresse(vm.fortschritt.rosa)
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TextButton(onClick = vm::zurueck, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("‹ Zurück", color = Gelb, fontSize = 16.sp)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (fehler) {
                KurzAnleitung()
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    laedt = false
                                }

                                override fun onReceivedError(
                                    view: WebView?, request: WebResourceRequest?, error: WebResourceError?,
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        laedt = false
                                        fehler = true
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?, request: WebResourceRequest?,
                                ): Boolean {
                                    val ziel = request?.url ?: return false
                                    if (ziel.host == InfoTexte.HOST) return false
                                    oeffneImBrowser(ctx, ziel.toString())
                                    return true
                                }
                            }
                            loadUrl(adresse)
                        }
                    },
                )
                if (laedt) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = Gelb)
                        Spacer(Modifier.height(12.dp))
                        Text("Anleitung wird geladen", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun KurzAnleitung() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Kein Internet. Das ist die Kurzfassung.", color = Gelb, fontWeight = FontWeight.Bold)
        InfoTexte.kurzAnleitung.forEach { (kopf, text) ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).glas(16.dp).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(kopf, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Gelb)
                Text(text, fontSize = 14.sp, color = Color.White)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
