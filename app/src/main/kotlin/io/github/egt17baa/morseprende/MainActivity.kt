package io.github.egt17baa.morseprende

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.egt17baa.morseprende.ui.navigation.AppNavHost
import io.github.egt17baa.morseprende.ui.theme.MorsePrendeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MorsePrendeApp).container
        setContent { MorsePrendeTheme { AppNavHost(container) } }
    }
}
