package de.cemaras.yem1n.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidFarbe
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Zeichnet einen QR-Code als Bitmap, schwarz auf weiß. */
fun qrBild(text: String, kante: Int = 512): Bitmap {
    val matrix = QRCodeWriter().encode(
        text, BarcodeFormat.QR_CODE, kante, kante,
        mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 2),
    )
    val bild = Bitmap.createBitmap(kante, kante, Bitmap.Config.ARGB_8888)
    for (x in 0 until kante) for (y in 0 until kante) {
        bild.setPixel(x, y, if (matrix[x, y]) AndroidFarbe.BLACK else AndroidFarbe.WHITE)
    }
    return bild
}

@Composable
fun QrDialog(titel: String, inhalt: String, hinweis: String, schliessen: () -> Unit) {
    val bild = remember(inhalt) { qrBild(inhalt).asImageBitmap() }
    AlertDialog(
        onDismissRequest = schliessen,
        confirmButton = { TextButton(onClick = schliessen) { Text("Schließen") } },
        title = { Text(titel) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(bild, contentDescription = "QR-Code", modifier = Modifier.size(240.dp))
                Text(hinweis)
            }
        },
    )
}

/** Öffnet den Scanner von Google Play-Diensten. Er braucht keine Kamera-Erlaubnis in der App. */
fun qrScannen(kontext: android.content.Context, ergebnis: (String) -> Unit, fehler: () -> Unit) {
    try {
        com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(kontext).startScan()
            .addOnSuccessListener { ergebnis(it.rawValue.orEmpty()) }
            .addOnFailureListener { fehler() }
    } catch (e: Exception) {
        fehler()
    }
}
