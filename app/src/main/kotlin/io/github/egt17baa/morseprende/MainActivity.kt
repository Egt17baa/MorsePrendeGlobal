package io.github.egt17baa.morseprende

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.ui.navigation.AppNavHost
import io.github.egt17baa.morseprende.ui.theme.MorsePrendeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MorsePrendeApp).container
        setContent {
            // Accesibilidad (alto contraste y tamaño de texto) desde los ajustes guardados.
            val prefs by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            MorsePrendeTheme(highContrast = prefs.highContrast, textScale = prefs.textScale) { AppNavHost(container) }
        }
    }
}
