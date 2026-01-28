package com.chifuz.enfermerapp.ui.screens.edad

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chifuz.enfermerapp.R
import com.chifuz.enfermerapp.ads.AdsManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.chifuz.enfermerapp.ads.AdLocation

// Extensión necesaria para encontrar la Activity y mostrar ads
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdadScreen(viewModel: EdadViewModel = viewModel()) {

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(Unit) {
        AdsManager.loadInterstitial(context, AdLocation.EDAD)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.edad_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )


        OutlinedCard(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.fecha_nacimiento),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = uiState.fechaNacimiento?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            ?: stringResource(R.string.seleccionar_fecha),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- LÓGICA DE PREMATURO ---
        if (uiState.fechaNacimiento != null) {
            val meses = java.time.temporal.ChronoUnit.MONTHS.between(uiState.fechaNacimiento, java.time.LocalDate.now())

            if (meses < 24) {
                // Caja compacta para prematuros
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = uiState.esPrematuro, onCheckedChange = { viewModel.onEsPrematuroChanged(it) })
                            Text(stringResource(R.string.es_prematuro), style = MaterialTheme.typography.bodyMedium)
                        }

                        if (uiState.esPrematuro) {
                            SemanasTerminoSelector(
                                selected = uiState.semanasCriterioTermino,
                                onSelected = viewModel::onSemanasCriterioTerminoSelected
                            )

                            OutlinedTextField(
                                value = uiState.semanasGestacion,
                                onValueChange = { viewModel.onSemanasGestacionChanged(it) },
                                label = { Text(stringResource(R.string.gestational_weeks)) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = uiState.errorSemanas,
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                supportingText = { if(uiState.errorSemanas) Text(stringResource(R.string.error_semanas)) }
                            )
                        }
                    }
                }

            }

            Spacer(modifier = Modifier.height(16.dp))

// Botón Calcular con formato unificado
            Button(
                onClick = { viewModel.calcularYMostrar(debeMostrarDialog = true) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = uiState.esCalculable,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_calcular_edad).uppercase())
            }
        }

        // --- NATIVE AD ---
        Spacer(modifier = Modifier.height(24.dp))
        NativeAdEdad(
            modifier = Modifier
                //.padding(horizontal = 4.dp)
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }

    // --- DIÁLOGOS ---
    if (uiState.showResultDialog) {
        EdadResultDialog(
            anios = uiState.resultadoCronologico?.años ?: 0,
            cronologica = formatEdad(uiState.resultadoCronologico),
            resultadoCorregido = uiState.resultadoCorregido,
            mostrarCorregida = uiState.mostrarCorregida,
            criterioTermino = uiState.semanasCriterioTermino,
            onDismiss = {
                viewModel.hideResultDialog()
                activity?.let { act ->
                    AdsManager.showInterstitial(act, AdLocation.EDAD) {
                        Log.d("ADS", "Intersticial de Edad cerrado")
                    }
                }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onFechaSeleccionada(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.aceptar)) }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun SemanasTerminoSelector(
    selected: Int,
    onSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.criterio_termino_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (37..40).forEach { semana ->
                FilterChip(
                    selected = selected == semana,
                    onClick = { onSelected(semana) },
                    label = { Text("$semana") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
fun EdadResultDialog(
    anios: Int,
    cronologica: String,
    resultadoCorregido: EdadResultado?,
    mostrarCorregida: Boolean,
    criterioTermino: Int,
    onDismiss: () -> Unit
) {
    val icon = when {
        anios < 2 -> R.drawable.ic_age1
        anios < 13 -> R.drawable.ic_age2
        anios < 65 -> R.drawable.ic_age3
        else -> R.drawable.ic_age4
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.edad_result_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.edad_cronologica), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = cronologica,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (mostrarCorregida && resultadoCorregido != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(stringResource(R.string.edad_corregida), style = MaterialTheme.typography.titleSmall)

                    val textoFinal = if (resultadoCorregido.años == -1) {
                        formatTiempoFaltante(resultadoCorregido, criterioTermino)
                    } else {
                        formatEdad(resultadoCorregido)
                    }

                    Text(
                        text = textoFinal,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.aceptar))
            }
        }
    )
}

@Composable
fun formatTiempoFaltante(resultado: EdadResultado?, criterio: Int): String {
    if (resultado == null) return ""
    val semanasStr = pluralStringResource(R.plurals.semanas, resultado.meses, resultado.meses)
    val diasStr = pluralStringResource(R.plurals.dias, resultado.dias, resultado.dias)
    val conjuncion = stringResource(R.string.y_con_espacios)
    val meta = stringResource(R.string.parentesis_semanas, criterio)
    val tiempo = "$semanasStr$conjuncion$diasStr"
    return stringResource(R.string.faltan_para_termino, tiempo, meta)
}

@Composable
fun formatEdad(resultado: EdadResultado?): String {
    if (resultado == null) return ""
    val partes = mutableListOf<String>()
    if (resultado.años > 0) partes.add(pluralStringResource(R.plurals.años, resultado.años, resultado.años))
    if (resultado.meses > 0) partes.add(pluralStringResource(R.plurals.meses, resultado.meses, resultado.meses))
    partes.add(pluralStringResource(R.plurals.dias, resultado.dias, resultado.dias))
    return partes.joinToString(", ")
}