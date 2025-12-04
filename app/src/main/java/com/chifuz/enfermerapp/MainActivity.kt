package com.chifuz.enfermerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.chifuz.enfermerapp.ui.navigation.AppNavHost
import com.chifuz.enfermerapp.ui.theme.EnfermerAppTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EnfermerAppTheme {
                // Aquí el Surface usa el color de fondo del tema (BackgroundLight o BackgroundDark)
                // Al estar definido en Theme.kt con valores consistentes, esto asegura
                // que el fondo de TODA la pantalla sea el color que definimos para 'background'.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background // Esto ya usa el color fijo del tema
                ) {
                    // El AppNavHost se dibuja sobre esta superficie.
                    AppNavHost()
                }
            }
        }
    }
}