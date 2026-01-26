package com.chifuz.enfermerapp.ui.screens.edad

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth

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
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.edad_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // --- SELECTOR DE FECHA ---
        OutlinedCard(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Usamos stringResource para que cambie de idioma
                    Text(
                        text = stringResource(R.string.fecha_nacimiento),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = uiState.fechaNacimiento?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            ?: stringResource(R.string.seleccionar_fecha), // Fallback traducido
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary // Le damos un toque de color al icono
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- LÓGICA DE PREMATURO ---
        if (uiState.fechaNacimiento != null) {
            // Solo mostramos la opción si es menor de 2 años (24 meses)
            val meses = java.time.temporal.ChronoUnit.MONTHS.between(uiState.fechaNacimiento, java.time.LocalDate.now())

            if (meses < 24) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = uiState.esPrematuro, onCheckedChange = { viewModel.onEsPrematuroChanged(it) })
                    Text(stringResource(R.string.es_prematuro))
                }

                if (uiState.esPrematuro) {
                    OutlinedTextField(
                        value = uiState.semanasGestacion,
                        onValueChange = { viewModel.onSemanasChanged(it) },
                        label = { Text(stringResource(R.string.gestational_weeks)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.errorSemanas,
                        supportingText = { if(uiState.errorSemanas) Text(stringResource(R.string.error_semanas)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.calcularYMostrar() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.esCalculable
            ) {
                Text(stringResource(R.string.btn_calcular_edad))
            }
        }




// NUEVO: El Native Ad con un margen adecuado
        Spacer(modifier = Modifier.height(32.dp)) // Margen para que respire

        NativeAdEdad(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .padding(bottom = 16.dp) // Evita que pegue a la barra inferior
        )
    }

    // --- DIÁLOGO DE RESULTADOS ---
    if (uiState.showResultDialog) {
        EdadResultDialog(
            anios = uiState.resultadoCronologico?.años ?: 0,
            cronologica = formatEdad(uiState.resultadoCronologico),
            // Solo pasamos el objeto, el diálogo se encarga del resto
            resultadoCorregido = uiState.resultadoCorregido,
            mostrarCorregida = uiState.mostrarCorregida,
            onDismiss = {
                viewModel.dismissDialog()
                activity?.let { act ->
                    AdsManager.showInterstitial(activity, AdLocation.EDAD) {
                        android.util.Log.d("ADS", "Intersticial de Edad cerrado")
                    }
                }
            }
        )
    }

    // --- DATE PICKER ---
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
fun formatEdad(resultado: EdadResultado?): String {
    if (resultado == null) return ""
    val partes = mutableListOf<String>()
    if (resultado.años > 0) partes.add(pluralStringResource(R.plurals.años, resultado.años, resultado.años))
    if (resultado.meses > 0) partes.add(pluralStringResource(R.plurals.meses, resultado.meses, resultado.meses))
    partes.add(pluralStringResource(R.plurals.dias, resultado.dias, resultado.dias))
    return partes.joinToString(", ")
}

@Composable
fun EdadResultDialog(
    anios: Int,
    cronologica: String,
    resultadoCorregido: EdadResultado?, // Cambiamos String por el Objeto para tener los datos
    mostrarCorregida: Boolean,
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cronológica
                Text(stringResource(R.string.edad_cronologica), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = cronologica,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (mostrarCorregida && resultadoCorregido != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(stringResource(R.string.edad_corregida), style = MaterialTheme.typography.titleMedium)

                    val textoFinal = if (resultadoCorregido.años == -1) {
                        // Mostramos lo que falta para llegar a término
                        formatTiempoFaltante(resultadoCorregido)
                    } else {
                        // Mostramos la Edad Corregida real (ej: 2 meses y 5 días)
                        formatEdad(resultadoCorregido)
                    }

                    Text(
                        text = textoFinal,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
fun formatTiempoFaltante(resultado: EdadResultado?): String {
    if (resultado == null) return ""

    // Obtenemos los textos de plurales (ej: "8 semanas", "4 días")
    val semanasStr = pluralStringResource(R.plurals.semanas, resultado.meses, resultado.meses)
    val diasStr = pluralStringResource(R.plurals.dias, resultado.dias, resultado.dias)

    // Obtenemos el " y " y la meta
    val conjuncion = stringResource(R.string.y_con_espacios)
    val meta = stringResource(R.string.parentesis_semanas, 40)

    // Construimos la frase: "8 semanas" + " y " + "4 días"
    val tiempo = "$semanasStr$conjuncion$diasStr"

    // Retornamos: "Faltan 8 semanas y 4 días para el término (40 semanas)"
    return stringResource(R.string.faltan_para_termino, tiempo, meta)
}