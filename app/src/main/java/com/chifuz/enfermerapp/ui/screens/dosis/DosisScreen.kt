package com.chifuz.enfermerapp.ui.screens.dosis

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import com.chifuz.enfermerapp.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.chifuz.enfermerapp.ads.AdsManager
import com.chifuz.enfermerapp.ads.AdLocation
import com.chifuz.enfermerapp.utils.PrefsManager
import androidx.core.net.toUri

@Composable
fun CalculoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
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
        trailingIcon = { Text(unit, style = MaterialTheme.typography.bodyLarge) },
        supportingText = {
            if (isError) {
                Text(stringResource(R.string.valor_invalido))
            }
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
    val keyboardController = LocalSoftwareKeyboardController.current

    // Carga de anuncios para ambos flujos
    LaunchedEffect(Unit) {
        AdsManager.loadInterstitial(context, AdLocation.DOSIS)
        AdsManager.loadInterstitial(context, AdLocation.DOSIS_PESO)
    }

    // Validación dinámica según la pestaña activa
    val isValidInput = if (uiState.calcType == DosisCalcType.ESTANDAR) {
        uiState.dosisAdministrar.toDoubleOrNull() != null &&
                uiState.solvente.toDoubleOrNull() != null &&
                uiState.soluto.toDoubleOrNull() != null
    } else {
        uiState.dosisPorKilo.toDoubleOrNull() != null &&
                uiState.pesoPaciente.toDoubleOrNull() != null
    }

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
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Selector de modo: Estándar vs Por Peso
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.updateCalcType(DosisCalcType.ESTANDAR) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.calcType == DosisCalcType.ESTANDAR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (uiState.calcType == DosisCalcType.ESTANDAR) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(R.string.dosis_tab_estandar))
            }
            Button(
                onClick = { viewModel.updateCalcType(DosisCalcType.POR_PESO) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.calcType == DosisCalcType.POR_PESO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (uiState.calcType == DosisCalcType.POR_PESO) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(R.string.dosis_tab_peso))
            }
        }

        // Renderizado condicional de campos
        if (uiState.calcType == DosisCalcType.ESTANDAR) {
            CalculoTextField(
                value = uiState.dosisAdministrar,
                onValueChange = viewModel::updateDosisAdministrar,
                label = stringResource(R.string.dosis_label_admin),
                unit = "mg",
                isError = false,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            CalculoTextField(
                value = uiState.solvente,
                onValueChange = viewModel::updateSolvente,
                label = stringResource(R.string.dosis_label_vol_medic),
                unit = "ml",
                isError = false,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            CalculoTextField(
                value = uiState.soluto,
                onValueChange = viewModel::updateSoluto,
                label = stringResource(R.string.dosis_label_concentracion),
                unit = "mg",
                isError = false,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        } else {
            CalculoTextField(
                value = uiState.dosisPorKilo,
                onValueChange = viewModel::updateDosisPorKilo,
                label = stringResource(R.string.dosis_label_por_kilo),
                unit = uiState.unidadSeleccionada,
                isError = false,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            CalculoTextField(
                value = uiState.pesoPaciente,
                onValueChange = viewModel::updatePeso,
                label = stringResource(R.string.dosis_label_peso_paciente),
                unit = "kg",
                isError = false,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            CalculoTextField(
                value = uiState.dosisMaxima,
                onValueChange = viewModel::updateDosisMaxima,
                label = stringResource(R.string.dosis_label_maxima),
                unit = uiState.unidadSeleccionada,
                isError = false,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        Button(
            onClick = {
                keyboardController?.hide()
                viewModel.calcularDosis()
            },
            enabled = isValidInput,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.dosis_btn_calcular))
        }
    }

    // Lógica de Diálogos y Anuncios
    if (uiState.showResultDialog && uiState.resultadoFinal != null) {
        if (uiState.calcType == DosisCalcType.ESTANDAR) {
            DosisEstandarDialog(
                resultadoMl = uiState.resultadoFinal!!,
                dosisAdmin = uiState.dosisAdministrar,
                onDismiss = {
                    viewModel.hideResultDialog()
                    ejecutarFlujoPostCalculo(context, activity, AdLocation.DOSIS) { showRateDialog = true }
                }
            )
        } else {
            DosisPorPesoDialog(
                resultado = uiState.resultadoFinal!!,
                unidad = uiState.unidadSeleccionada,
                esAdvertencia = uiState.esDosisExcedida,
                onDismiss = {
                    viewModel.hideResultDialog()
                    ejecutarFlujoPostCalculo(context, activity, AdLocation.DOSIS_PESO) { showRateDialog = true }
                }
            )
        }
    }

    if (showRateDialog) {
        RatingDialog(
            onDismiss = { showRateDialog = false },
            onRate = {
                showRateDialog = false
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = "https://play.google.com/store/apps/details?id=com.chifuz.enfermerapp".toUri()
                }
                context.startActivity(intent)
            }
        )
    }
}

// Función para centralizar la lógica de Ads y Rating después de un cálculo
private fun ejecutarFlujoPostCalculo(
    context: Context,
    activity: Activity?,
    location: AdLocation,
    onShowRating: () -> Unit
) {
    val count = PrefsManager.incrementCalculationCount(context)
    if (count == 5 || count == 50) {
        onShowRating()
    } else {
        activity?.let {
            AdsManager.showInterstitial(it, location) {
                Log.d("ADS", "Intersticial mostrado en $location")
            }
        }
    }
}

@Composable
fun DosisEstandarDialog(resultadoMl: String, dosisAdmin: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.aceptar)) } },
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.dosis_result_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.dosis_volume_extract), style = MaterialTheme.typography.titleMedium)
                Text(text = "$resultadoMl ml", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.dosis_instruction_line, resultadoMl, dosisAdmin),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

@Composable
fun DosisPorPesoDialog(resultado: String, unidad: String, esAdvertencia: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.aceptar)) } },
        icon = {
            Icon(
                imageVector = if (esAdvertencia) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (esAdvertencia) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(stringResource(R.string.dosis_result_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.dosis_resultado_peso), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$resultado $unidad",
                    style = MaterialTheme.typography.displaySmall,
                    color = if (esAdvertencia) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                if (esAdvertencia) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.dosis_advertencia_excedida),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}

@Composable
fun RatingDialog(onDismiss: () -> Unit, onRate: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFFE91E63), modifier = Modifier.size(40.dp)) },
        title = { Text(text = stringResource(R.string.rating_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = { Text(text = stringResource(R.string.rating_message), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center) },
        confirmButton = { Button(onClick = onRate) { Text(stringResource(R.string.rating_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ahora_no_gracias), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}