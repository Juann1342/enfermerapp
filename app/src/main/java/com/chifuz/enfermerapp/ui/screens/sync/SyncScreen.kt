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
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chifuz.enfermerapp.R
import com.chifuz.enfermerapp.utils.VibrationManager

@Composable
fun SyncScreen(gttsMinInicial: Int = 0, viewModel: SyncViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val vibrationManager = remember { VibrationManager(context) }

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(vibrationManager)
        viewModel.setVibrationManager(vibrationManager)
        onDispose {
            viewModel.stopMetronome()
            lifecycleOwner.lifecycle.removeObserver(vibrationManager)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.resetTempoImported()
    }

    LaunchedEffect(gttsMinInicial) {
        if (gttsMinInicial > 0) {
            viewModel.initializeMetronome(gttsMinInicial)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.sync_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        DropTapButton(
            mode = uiState.mode,
            onTap = viewModel::recordDrop,
            onStopMetronome = viewModel::toggleMetronomeMode,
            isMetronomeBlinking = uiState.metronomeBlink
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.95f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ControlActionButton(
                icon = Icons.Default.Refresh,
                text = stringResource(R.string.sync_reiniciar),
                onClick = viewModel::reset,
                modifier = Modifier.weight(1f)
            )

            ControlActionButton(
                icon = if (uiState.mode == SyncMode.METRONOME) Icons.Default.Stop else Icons.Default.PlayArrow,
                text = if (uiState.mode == SyncMode.METRONOME) stringResource(R.string.sync_detener) else stringResource(R.string.sync_iniciar),
                onClick = viewModel::toggleMetronomeMode,
                enabled = uiState.currentGttsMin > 0 || uiState.mode == SyncMode.METRONOME,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SyncResultCard(
            gttsMin = uiState.currentGttsMin,
            status = stringResource(id = uiState.statusMessage, uiState.statusArg),
            mode = uiState.mode
        )
    }
}

@Composable
fun DropTapButton(
    mode: SyncMode,
    onTap: () -> Unit,
    onStopMetronome: () -> Unit,
    isMetronomeBlinking: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetColor = when {
        mode == SyncMode.METRONOME && isMetronomeBlinking -> MaterialTheme.colorScheme.error
        isPressed -> if (mode == SyncMode.MANUAL) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 100),
        label = "TapColor"
    )

    Box(
        modifier = Modifier
            .size(200.dp) // Tamaño optimizado para pulgar
            .shadow(elevation = 6.dp, shape = CircleShape)
            .background(animatedColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (mode == SyncMode.MANUAL) onTap() else onStopMetronome()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val icon = if (mode == SyncMode.MANUAL) Icons.Default.WaterDrop else Icons.Default.Stop
            val text = if (mode == SyncMode.MANUAL) stringResource(R.string.sync_pulsar) else stringResource(R.string.sync_detener_metronomo)

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = text.uppercase(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ControlActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
fun SyncResultCard(gttsMin: Int, status: String, mode: SyncMode) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.sync_ritmo_actual), style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (gttsMin > 0) "$gttsMin GTT/MIN" else "-- GTT/MIN",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (gttsMin > 0) {
                Text(
                    text = if (mode == SyncMode.METRONOME) stringResource(R.string.sync_status_metronome) else stringResource(R.string.sync_instruction, gttsMin),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}