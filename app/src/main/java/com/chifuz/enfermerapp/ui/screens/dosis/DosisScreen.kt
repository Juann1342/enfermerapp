package com.chifuz.enfermerapp.ui.screens.dosis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController // <--- NUEVO: Controlador de teclado
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.chifuz.enfermerapp.ui.screens.dosis.DosisViewModel
import com.chifuz.enfermerapp.ui.navigation.Screen

// Componente para un TextField estandarizado de la app
@Composable
fun CalculoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String, // La unidad se usa como sufijo
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        // Mejora B: La unidad como Trailing Icon para claridad
        trailingIcon = { Text(unit, style = MaterialTheme.typography.bodyLarge) },
        supportingText = {
            if (isError) {
                Text("Valor numérico inválido")
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun DosisScreen(navController: NavController, viewModel: DosisViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current // <--- NUEVO

    // Comprobamos si todos los campos tienen un valor válido para habilitar el botón
    val isValidInput = uiState.dosisAdministrar.toDoubleOrNull() != null &&
            uiState.solvente.toDoubleOrNull() != null &&
            uiState.soluto.toDoubleOrNull() != null &&
            !uiState.dosisAdministrarError && !uiState.solventeError && !uiState.solutoError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Cálculo de Dosis",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 1. Dosis a administrar (mg)
        CalculoTextField(
            value = uiState.dosisAdministrar,
            onValueChange = viewModel::updateDosisAdministrar,
            label = "Dosis a administrar",
            unit = "mg",
            isError = uiState.dosisAdministrarError,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 2. Solvente (ml)
        CalculoTextField(
            value = uiState.solvente,
            onValueChange = viewModel::updateSolvente,
            label = "Volumen del medicamento",
            unit = "ml",
            isError = uiState.solventeError,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 3. Soluto (mg) - Concentración total
        CalculoTextField(
            value = uiState.soluto,
            onValueChange = viewModel::updateSoluto,
            label = "Concentración total del fármaco",
            unit = "mg",
            isError = uiState.solutoError,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Botón CALCULAR
        Button(
            onClick = {
                keyboardController?.hide() // <--- OCULTAR TECLADO
                viewModel.calcularDosis()
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

    // --- NUEVO: Diálogo de Resultado de Dosis ---
    if (uiState.showResultDialog && uiState.resultadoMl != null) {
        DosisResultDialog(
            resultadoMl = uiState.resultadoMl!!,
            dosisAdmin = uiState.dosisAdministrar,
            onDismiss = viewModel::hideResultDialog
        )
    }
}

@Composable
fun DosisResultDialog(
    resultadoMl: String,
    dosisAdmin: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("ACEPTAR")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Resultado OK",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text("Resultado del Cálculo de Dosis", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Volumen a extraer de la ampolla:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$resultadoMl ml",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Extraiga $resultadoMl ml del frasco para lograr la dosis de $dosisAdmin mg.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}