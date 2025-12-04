package com.chifuz.enfermerapp.ui.screens.perfusion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController // <--- NUEVO
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.chifuz.enfermerapp.ui.screens.dosis.CalculoTextField
import com.chifuz.enfermerapp.ui.navigation.Screen

@Composable
fun PerfusionScreen(navController: NavController, viewModel: PerfusionViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current // <--- NUEVO

    // Comprobamos si todos los campos tienen un valor válido para habilitar el botón
    val isValidInput = uiState.volumen.toDoubleOrNull() != null &&
            uiState.tiempo.toDoubleOrNull() != null &&
            !uiState.volumenError && !uiState.tiempoError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Velocidad de Goteo",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // --- 1. Selección de Gotero (Toggle Buttons) ---
        Text(
            text = "Tipo de Gotero:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GoteroToggleButton(
                text = "Microgotero (60 gtt/ml)",
                isSelected = uiState.selectedGotero == GoteroType.MICRO,
                onClick = { viewModel.selectGotero(GoteroType.MICRO) },
                modifier = Modifier.weight(1f)
            )
            GoteroToggleButton(
                text = "Macrogotero (20 gtt/ml)",
                isSelected = uiState.selectedGotero == GoteroType.MACRO,
                onClick = { viewModel.selectGotero(GoteroType.MACRO) },
                modifier = Modifier.weight(1f)
            )
        }

        // --- 2. Volumen (ml) ---
        CalculoTextField(
            value = uiState.volumen,
            onValueChange = viewModel::updateVolumen,
            label = "Volumen a perfundir",
            unit = "ml",
            isError = uiState.volumenError,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // --- 3. Tiempo (Input y Toggle Buttons para Unidad) ---
        Text(
            text = "Tiempo de perfusión:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Input de Tiempo
            CalculoTextField(
                value = uiState.tiempo,
                onValueChange = viewModel::updateTiempo,
                label = "Tiempo",
                unit = if (uiState.selectedTimeUnit == TimeUnit.HOURS) "hrs" else "min",
                isError = uiState.tiempoError,
                modifier = Modifier.weight(0.6f)
            )

            // Toggle Buttons de Unidad de Tiempo
            Row(
                modifier = Modifier
                    .weight(0.4f)
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TimeUnitToggleButton(
                    text = "Horas",
                    isSelected = uiState.selectedTimeUnit == TimeUnit.HOURS,
                    onClick = { viewModel.selectTimeUnit(TimeUnit.HOURS) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                TimeUnitToggleButton(
                    text = "Minutos",
                    isSelected = uiState.selectedTimeUnit == TimeUnit.MINUTES,
                    onClick = { viewModel.selectTimeUnit(TimeUnit.MINUTES) }
                )
            }
        }


        Spacer(modifier = Modifier.height(32.dp))

        // Botón CALCULAR
        Button(
            onClick = {
                keyboardController?.hide() // <--- OCULTAR TECLADO
                viewModel.calcularPerfusion()
            },
            enabled = isValidInput && !uiState.isCalculating,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (uiState.isCalculating) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("CALCULAR")
            }
        }
    }

    // --- NUEVO: Diálogo de Resultado de Perfusión ---
    if (uiState.showResultDialog && uiState.resultadoMlHr != null) {
        PerfusionResultDialog(
            resultadoMlHr = uiState.resultadoMlHr!!,
            resultadoMlMin = uiState.resultadoMlMin!!,
            resultadoGttsMin = uiState.resultadoGttsMin!!,
            resultadoGttsMinInt = uiState.resultadoGttsMinInt, // <--- PASAMOS EL INT
            onDismiss = viewModel::hideResultDialog,
            onSync = {
                viewModel.hideResultDialog()

                val gttsMin = uiState.resultadoGttsMinInt  // <-- el que sí está actualizado

                val route = "${Screen.Sync.route}/$gttsMin"

                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                }

            }

        )
    }
}

// Composable para los botones de selección de gotero
@Composable
fun GoteroToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = if (isSelected) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    OutlinedButton(
        onClick = onClick,
        colors = colors,
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        modifier = modifier.height(IntrinsicSize.Min)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

// Composable para los botones de selección de unidad de tiempo
@Composable
fun TimeUnitToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.width(IntrinsicSize.Max)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

// NUEVO: Diálogo de Resultado de Perfusión
@Composable
fun PerfusionResultDialog(
    resultadoMlHr: String,
    resultadoMlMin: String,
    resultadoGttsMin: String,
    resultadoGttsMinInt: Int, // <--- RECIBE EL INT SEGURO
    onDismiss: () -> Unit,
    onSync: (Int) -> Unit // Función para navegar a la sincronización
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Resultado de Perfusión",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text("Resultados de Perfusión", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Bloque 1: Velocidad principal
                Text(
                    text = "Ritmo de Goteo (Objetivo):",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "$resultadoGttsMin gotas/min",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Bloque 2: Detalles de volumen
                Divider(Modifier.padding(vertical = 8.dp).fillMaxWidth())
                ResultRow(label = "Velocidad:", value = "$resultadoMlHr ml/hr", unitColor = MaterialTheme.colorScheme.primary)
                ResultRow(label = "Gasto:", value = "$resultadoMlMin ml/min", unitColor = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        // Botones de acción
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("ACEPTAR")
            }
        },
        dismissButton = {
            // Usamos el Int seguro del ViewModel
            if (resultadoGttsMinInt > 0) {
                OutlinedButton(
                    onClick = { onSync(resultadoGttsMinInt) }, // <--- USAMOS EL INT SEGURO
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Sincronizar")
                    Spacer(Modifier.width(4.dp))
                    Text("SINCRONIZAR RITMO")
                }
            }
        }
    )
}

@Composable
fun ResultRow(label: String, value: String, unitColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = unitColor)
    }
}