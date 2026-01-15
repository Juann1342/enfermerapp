package com.chifuz.enfermerapp.ui.screens.dosis

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.chifuz.enfermerapp.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController // <--- NUEVO: Controlador de teclado
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.chifuz.enfermerapp.ads.AdsManager
import com.chifuz.enfermerapp.ui.screens.dosis.DosisViewModel
import com.chifuz.enfermerapp.ui.navigation.Screen
import androidx.core.net.toUri
import com.chifuz.enfermerapp.utils.PrefsManager

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
                Text(stringResource(R.string.valor_invalido))            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun DosisScreen(navController: NavController, viewModel: DosisViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var showRateDialog by remember { mutableStateOf(false) }
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
            text = stringResource(R.string.dosis_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 1. Dosis a administrar (mg)
        CalculoTextField(
            value = uiState.dosisAdministrar,
            onValueChange = viewModel::updateDosisAdministrar,
            label = stringResource(R.string.dosis_label_admin),
            unit = "mg",
            isError = uiState.dosisAdministrarError,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 2. Solvente (ml)
        CalculoTextField(
            value = uiState.solvente,
            onValueChange = viewModel::updateSolvente,
            label = stringResource(R.string.dosis_label_vol_medic),
            unit = "ml",
            isError = uiState.solventeError,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 3. Soluto (mg) - Concentración total
        CalculoTextField(
            value = uiState.soluto,
            onValueChange = viewModel::updateSoluto,
            label = stringResource(R.string.dosis_label_concentracion),
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
                Text(stringResource(R.string.dosis_btn_calcular))
            }
        }
    }

    // Lógica del diálogo de Rating
    if (showRateDialog) {
        RatingDialog(
            onDismiss = { showRateDialog = false },
            onRate = {
                showRateDialog = false
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data =
                        "https://play.google.com/store/apps/details?id=com.chifuz.enfermerapp".toUri()
                }
                context.startActivity(intent)
            }
        )
    }

    // --- NUEVO: Diálogo de Resultado de Dosis ---
    if (uiState.showResultDialog && uiState.resultadoMl != null) {
        DosisResultDialog(
            resultadoMl = uiState.resultadoMl!!,
            dosisAdmin = uiState.dosisAdministrar,
            onDismiss = {
                // Lógica: 1. Cerrar diálogo, 2. Mostrar Ad
                viewModel.hideResultDialog()
                val count = PrefsManager.incrementCalculationCount(context)
// 2. Lógica de decisión: ¿Rating o Ad?
                if (count == 5 || count == 50) {
                    showRateDialog = true
                } else {
                    // Si no es la vez 5 o 50, mostramos anuncio como siempre
                    activity?.let { act ->
                        AdsManager.showInterstitial(act) {
                            Log.d("ADS", "Flujo continuado")
                        }
                    }
                }
            }
        )
    }
}


private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
                Text(stringResource(R.string.aceptar))            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.dosis_content_desc_ok),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(stringResource(R.string.dosis_result_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.dosis_volume_extract),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$resultadoMl ml",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.dosis_instruction_line, resultadoMl, dosisAdmin),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun RatingDialog(
    onDismiss: () -> Unit,
    onRate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            // Un ícono que transmita calidez
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Favorite,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color(0xFFE91E63), // Un color cereza/rosa empático
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.rating_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.rating_message),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRate,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.rating_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ahora_no_gracias), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}