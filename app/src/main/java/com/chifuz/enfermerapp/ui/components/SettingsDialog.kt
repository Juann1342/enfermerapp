package com.chifuz.enfermerapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chifuz.enfermerapp.R

@Composable
fun SettingsDialog(
    currentDark: Boolean,
    currentLang: String,
    onDismiss: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    onLangChanged: (String) -> Unit
) {
    val languages = listOf(
        "en" to "English",
        "es" to "Español",
        "fr" to "Français",
        "it" to "Italiano",
        "pt" to "Português (BR)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_settings)) },
        text = {
            Column {
                // TEMA
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.settings_dark_mode))
                    Switch(checked = currentDark, onCheckedChange = onThemeChanged)
                }

                Spacer(Modifier.height(16.dp))

                // IDIOMA
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.labelLarge)
                Column(Modifier.padding(top = 8.dp)) {
                    languages.forEach { (code, name) ->
                        FilterChip(
                            // Cambiamos la lógica de selección para que coincida con lo que guarda PrefsManager
                            selected = currentLang == code || (code == "pt" && currentLang == "pt-BR"),
                            onClick = {
                                // IMPORTANTE: Para el Locale de Java usamos "pt-BR" (sin la 'r')
                                val finalCode = if (code == "pt") "pt-BR" else code
                                onLangChanged(finalCode)
                            },
                            label = { Text(name) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.aceptar_theme)) }
        }
    )
}