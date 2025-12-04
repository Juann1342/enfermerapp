package com.chifuz.enfermerapp.ui.screens.sync

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop // <-- Icono de STOP
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // <--- NUEVA IMPORTACIÓN
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chifuz.enfermerapp.ui.screens.sync.SyncViewModel
import com.chifuz.enfermerapp.utils.VibrationManager

@Composable
fun SyncScreen(gttsMinInicial: Int = 0, viewModel: SyncViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Manejo del VibrationManager y SoundPool (Liberación al destruir la Activity)
    val vibrationManager = remember { VibrationManager(context) }
    DisposableEffect(key1 = lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(vibrationManager)
        viewModel.setVibrationManager(vibrationManager)
        onDispose {
            // Detiene y libera recursos del metrónomo cuando el composable se desecha
            viewModel.stopMetronome()
            lifecycleOwner.lifecycle.removeObserver(vibrationManager) // <--- Corregido: remover observador aquí
        }
    }

    // CAMBIO CLAVE: Utilizamos LaunchedEffect para inicializar.
    // Si gttsMinInicial cambia de 40 a 80, se ejecuta. Si cambia de 0 a 40, se ejecuta.
    // Si es 0 y se pulsa el botón de la barra inferior (navegando a 'sync'), no se ejecuta.
    LaunchedEffect(Unit) {
        viewModel.resetTempoImported()
    }
    LaunchedEffect(gttsMinInicial) {
        if (gttsMinInicial > 0) {
            viewModel.initializeMetronome(gttsMinInicial)
        }
    }

    // NOTA: El DisposableEffect(key1=viewModel) que deteníamos fue removido,
    // ya que la lógica de reinicio/detención está completamente en el ViewModel y
    // en el DisposableEffect de lifecycleOwner.

    // Ajustamos el padding general y los Spacers para pantallas pequeñas
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp) // Menos padding vertical
            .verticalScroll(rememberScrollState()), // Esencial para el scroll
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sincronización de Gota",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp) // Menos espacio
        )

        // --- Botón de Pulso Principal (Lógica de Tap/Stop) ---
        DropTapButton(
            mode = uiState.mode,
            onTap = viewModel::recordDrop,
            onStopMetronome = viewModel::toggleMetronomeMode,
            isMetronomeBlinking = uiState.metronomeBlink
        )

        Spacer(modifier = Modifier.height(24.dp)) // Menos espacio

        // --- Botones de Control (Reiniciar y Metrónomo Secundario) ---
        Row(
            modifier = Modifier.fillMaxWidth(0.9f), // Usamos más del ancho para que quepa bien
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ControlActionButton(
                icon = Icons.Default.Refresh,
                text = "Reiniciar",
                onClick = viewModel::reset,
                // Aplicar weight para ancho igualitario
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp)) // Espacio entre botones

            ControlActionButton(
                icon = Icons.Default.PlayArrow,
                text = if (uiState.mode == SyncMode.METRONOME) "Detener Metrónomo" else "Iniciar Metrónomo",
                onClick = viewModel::toggleMetronomeMode,
                enabled = uiState.currentGttsMin > 0 || uiState.mode == SyncMode.METRONOME,
                // Aplicar weight para ancho igualitario
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp)) // Menos espacio

        // --- Área de Resultado ---
        SyncResultCard(
            gttsMin = uiState.currentGttsMin,
            status = uiState.statusMessage,
            mode = uiState.mode
        )
    }
}

// Botón de pulso grande y unificado
@Composable
fun DropTapButton(
    mode: SyncMode,
    onTap: () -> Unit,
    onStopMetronome: () -> Unit,
    isMetronomeBlinking: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val baseColor = MaterialTheme.colorScheme.primary
    val tapColor = MaterialTheme.colorScheme.tertiary
    val metronomeColor = MaterialTheme.colorScheme.error

    val targetColor = when {
        mode == SyncMode.METRONOME && isMetronomeBlinking -> metronomeColor
        isPressed -> if (mode == SyncMode.MANUAL) tapColor else metronomeColor
        else -> baseColor
    }

    val animatedColor by animateColorAsState(targetColor, animationSpec = tween(durationMillis = 100), label = "TapColorAnimation")

    // Tamaño ajustado para pantallas pequeñas
    val buttonSize = 180.dp

    Box(
        // Tamaño ajustado y centrado
        modifier = Modifier
            .size(buttonSize)
            .shadow(
                elevation = if (isPressed || mode == SyncMode.METRONOME) 8.dp else 4.dp,
                shape = CircleShape
            )
            .background(animatedColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    when (mode) {
                        SyncMode.MANUAL -> onTap()
                        SyncMode.METRONOME -> onStopMetronome()
                    }
                },
                enabled = true
            ),
        contentAlignment = Alignment.Center // CLAVE: Asegura el centrado vertical y horizontal
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp) // Padding interno para evitar que el texto toque el borde
        ) {
            val icon: ImageVector
            val text: String

            when (mode) {
                SyncMode.MANUAL -> {
                    icon = Icons.Default.WaterDrop
                    text = "PULSAR AL CAER GOTA"
                }
                SyncMode.METRONOME -> {
                    icon = Icons.Default.Stop
                    text = "DETENER METRÓNOMO"
                }
            }

            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, // Centrar el texto en su propio ancho
                maxLines = 2 // Asegura que el texto pueda saltar de línea si es largo
            )
        }
    }
}

// Botones de acción secundaria (Reiniciar, Metrónomo)
@Composable
fun ControlActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier // Recibir el modificador
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier // Aplicar el modificador aquí
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

// Tarjeta de resultado
@Composable
fun SyncResultCard(gttsMin: Int, status: String, mode: SyncMode) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(), // Añadir fillMaxWidth para que los textos puedan alinearse
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ritmo Actual:",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center // Centrar
            )
            Text(
                text = if (gttsMin > 0) "$gttsMin GTT/MIN" else "-- GTT/MIN",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                textAlign = TextAlign.Center // Centrar
            )

            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(), // Aseguramos que ocupe todo el ancho para centrar el texto
                textAlign = TextAlign.Center // Centrar el texto, importante para multilínea
            )

            if (gttsMin > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (mode == SyncMode.METRONOME) "Guía rítmica activa." else "Ajuste el gotero a $gttsMin gotas por minuto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth(), // Aseguramos que ocupe todo el ancho
                    textAlign = TextAlign.Center // Centrar el texto, importante para multilínea
                )
            }
        }
    }
}