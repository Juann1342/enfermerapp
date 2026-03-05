package com.chifuz.enfermerapp.ui.screens.perfusion

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.chifuz.enfermerapp.R
import com.chifuz.enfermerapp.ads.AdsManager
import com.chifuz.enfermerapp.ui.screens.dosis.CalculoTextField
import com.chifuz.enfermerapp.ui.navigation.Screen
import com.chifuz.enfermerapp.ads.AdLocation
import com.chifuz.enfermerapp.ui.components.PerfusionHelpDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfusionScreen(navController: NavController, viewModel: PerfusionViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var showHelp by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val isValidInput = uiState.volumen.toDoubleOrNull() != null &&
            uiState.tiempo.toDoubleOrNull() != null &&
            !uiState.volumenError && !uiState.tiempoError

    LaunchedEffect(Unit) {
        AdsManager.loadInterstitial(context, AdLocation.PERFUSION)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.perfusion_velocidad_goteo),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp),
                textAlign = TextAlign.Center
            )

            // --- 1. Selección de Gotero (Segmented Buttons) ---


            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                SegmentedButton(
                    selected = uiState.selectedGotero == GoteroType.MICRO,
                    onClick = { viewModel.selectGotero(GoteroType.MICRO) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(R.string.perfusion_micro),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SegmentedButton(
                    selected = uiState.selectedGotero == GoteroType.MACRO,
                    onClick = { viewModel.selectGotero(GoteroType.MACRO) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,

                        ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.perfusion_macro),
                        style = MaterialTheme.typography.labelSmall, // Fuente más pequeña para seguridad
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // --- 2. Volumen (ml) ---
            CalculoTextField(
                value = uiState.volumen,
                onValueChange = viewModel::updateVolumen,
                label = stringResource(R.string.perfusion_label_volumen_perfundir),
                unit = "ml",
                isError = uiState.volumenError,
                modifier = Modifier.padding(bottom = 16.dp)
            )


            CalculoTextField(
                value = uiState.tiempo,
                onValueChange = viewModel::updateTiempo,
                label = stringResource(R.string.perfusion_tiempo),
                unit = if (uiState.selectedTimeUnit == TimeUnit.HOURS) "hrs" else "min",
                isError = uiState.tiempoError,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                SegmentedButton(
                    selected = uiState.selectedTimeUnit == TimeUnit.HOURS,
                    onClick = { viewModel.selectTimeUnit(TimeUnit.HOURS) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(R.string.perfusion_hours))
                }
                SegmentedButton(
                    selected = uiState.selectedTimeUnit == TimeUnit.MINUTES,
                    onClick = { viewModel.selectTimeUnit(TimeUnit.MINUTES) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(R.string.perfusion_minutes))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.calcularPerfusion()
                },
                enabled = isValidInput && !uiState.isCalculating,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                if (uiState.isCalculating) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.dosis_btn_calcular).uppercase())
                }
            }
        }

        IconButton(
            onClick = { showHelp = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Guía de goteo",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }

        if (showHelp) {
            PerfusionHelpDialog(onDismiss = { showHelp = false })
        }

        if (uiState.showResultDialog && uiState.resultadoMlHr != null) {
            PerfusionResultDialog(
                resultadoMlHr = uiState.resultadoMlHr!!,
                resultadoMlMin = uiState.resultadoMlMin!!,
                resultadoGttsMin = uiState.resultadoGttsMin!!,
                resultadoGttsMinInt = uiState.resultadoGttsMinInt,
                onDismiss = {
                    viewModel.hideResultDialog()
                    activity?.let {
                        AdsManager.showInterstitial(it, AdLocation.PERFUSION) {
                            Log.d("ADS", "Intersticial de Perfusión cerrado")
                        }
                    }
                },
                onSync = {
                    viewModel.hideResultDialog()
                    val route = "${Screen.Sync.route}/${uiState.resultadoGttsMinInt}"
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
fun PerfusionResultDialog(
    resultadoMlHr: String,
    resultadoMlMin: String,
    resultadoGttsMin: String,
    resultadoGttsMinInt: Int,
    onDismiss: () -> Unit,
    onSync: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = { Text(stringResource(R.string.perfusion_resultados_perfusion), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.perfusion_goteo_objetivo),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.perfusion_gtts_unit, resultadoGttsMin),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                ResultRow(
                    label = stringResource(R.string.perfusion_label_velocidad),
                    value = stringResource(R.string.perfusion_value_ml_hr, resultadoMlHr),
                    unitColor = MaterialTheme.colorScheme.primary
                )
                ResultRow(
                    label = stringResource(R.string.perfusion_label_gasto),
                    value = stringResource(R.string.perfusion_value_ml_min, resultadoMlMin),
                    unitColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.aceptar))
            }
        },
        dismissButton = {
            if (resultadoGttsMinInt > 0) {
                OutlinedButton(
                    onClick = { onSync(resultadoGttsMinInt) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.perfusion_btn_sync))
                }
            }
        }
    )
}

@Composable
fun ResultRow(label: String, value: String, unitColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = unitColor)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}