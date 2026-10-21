package de.cemaras.yem1n

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import de.cemaras.yem1n.ui.App
import de.cemaras.yem1n.ui.AppViewModel
import de.cemaras.yem1n.ui.theme.YEM1NTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by lazy { ViewModelProvider(this)[AppViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YEM1NTheme {
                val modell: AppViewModel = viewModel()
                App(modell)
            }
        }
        if (savedInstanceState == null) verarbeite(intent)
        addOnNewIntentListener { verarbeite(it) }
    }

    /** Eine Arbeit, die eine andere App geschickt hat: als Text geteilt, als Datei geteilt oder geöffnet. */
    private fun verarbeite(intent: Intent?) {
        intent ?: return
        val text: String? = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { lese(it) }
            Intent.ACTION_VIEW -> intent.data?.let { lese(it) }
            else -> null
        }
        if (!text.isNullOrBlank()) vm.importiereVonAussen(text)
    }

    private fun lese(uri: Uri): String? = try {
        contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    } catch (e: Exception) {
        null
    }
}
