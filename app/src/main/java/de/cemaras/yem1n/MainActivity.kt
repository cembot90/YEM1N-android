package de.cemaras.yem1n

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import de.cemaras.yem1n.ui.App
import de.cemaras.yem1n.ui.AppViewModel
import de.cemaras.yem1n.ui.theme.YEM1NTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YEM1NTheme {
                val vm: AppViewModel = viewModel()
                App(vm)
            }
        }
    }
}
