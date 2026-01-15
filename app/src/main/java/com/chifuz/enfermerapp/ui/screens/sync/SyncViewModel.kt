package com.chifuz.enfermerapp.ui.screens.sync


import android.util.Log
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chifuz.enfermerapp.R
import com.chifuz.enfermerapp.utils.VibrationManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

enum class SyncMode { MANUAL, METRONOME }

data class SyncUiState(
    val currentGttsMin: Int = 0, // Gotas por minuto
    val isCalculating: Boolean = false,
    val mode: SyncMode = SyncMode.MANUAL,
    val lastTapTimestamp: Long = 0L, // Marca de tiempo del último toque
    val statusMessage: Int = R.string.sync_status_manual,
    val statusArg: Int = 0,
    val isMetronomeActive: Boolean = false, // Indica si el modo Metrónomo está corriendo
    val metronomeBlink: Boolean = false, // Para el efecto visual del metrónomo
    val tempoImported: Boolean = false // <--- NUEVO: Bandera de ritmo importado
)

class SyncViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private val timestamps = mutableListOf<Long>()
    private var metronomeJob: Job? = null
    private lateinit var vibrationManager: VibrationManager

    fun setVibrationManager(manager: VibrationManager) {
        this.vibrationManager = manager
    }

    // --- Inicialización con ritmo (Llamada desde navegación) ---
    fun initializeMetronome(gttsMin: Int) {
        // Ejecuta si se recibe un ritmo válido (> 0) Y NO ha sido importado previamente en este ciclo.
        Log.d("SYNC", "initializeMetronome($gttsMin) - tempoImported=${_uiState.value.tempoImported}, currentGttsMin=${_uiState.value.currentGttsMin}")

        if (gttsMin > 0 && !_uiState.value.tempoImported) { // <--- CLAVE: Comprobar la bandera
            // 1. Establecer el nuevo ritmo importado
            _uiState.update { it.copy(currentGttsMin = gttsMin) }

            // 2. Forzar el inicio en modo metrónomo
            startMetronome(gttsMin)

            _uiState.update {
                it.copy(
                    mode = SyncMode.METRONOME,
                    statusMessage = R.string.sync_status_metronome_active,
                    statusArg = gttsMin,
                    tempoImported = true // <--- Marcar como importado
                )
            }
            Log.d("SYNC", "initializeMetronome -> iniciando metrónomo a $gttsMin")

        } else {
            Log.d("SYNC", "initializeMetronome -> NO inicia (condición false)")
        }
    }

    // --- Lógica del Modo Manual (Pulsación) ---
    fun recordDrop() {
        // Asegurarse de que si el metrónomo estaba activo, no interfiera
        stopMetronome()
        _uiState.update { it.copy(mode = SyncMode.MANUAL) }

        val currentTime = System.currentTimeMillis()
        vibrationManager.doHapticFeedback()
        vibrationManager.playClickSound()

        timestamps.add(currentTime)

        // Limitar la lista a las últimas 10 pulsaciones
        while (timestamps.size > 10) {
            timestamps.removeAt(0)
        }

        if (timestamps.size >= 2) {
            calculateGttsMin()
        }

        _uiState.update { it.copy(lastTapTimestamp = currentTime) }
    }

    private fun calculateGttsMin() {
        if (timestamps.size < 2) return

        val firstTap = timestamps.first()
        val lastTap = timestamps.last()

        val numIntervals = (timestamps.size - 1).toDouble()
        val totalTimeMs = (lastTap - firstTap).toDouble()

        val averageTimePerDropMs = totalTimeMs / numIntervals
        val gttsPerMin = (60000.0 / averageTimePerDropMs).roundToInt()

        // Determinamos qué "molde" usar según la lógica
        val statusRes = if (timestamps.size < 5) {
            R.string.sync_status_provisional
        } else {
            R.string.sync_status_stable
        }

        _uiState.update {
            it.copy(
                currentGttsMin = gttsPerMin,
                statusMessage = statusRes, // <--- Usamos la variable aquí
                statusArg = timestamps.size
            )
        }
    }

    // --- Lógica del Modo Metrónomo (Automático) ---

    fun toggleMetronomeMode() {
        val currentState = _uiState.value

        if (currentState.mode == SyncMode.METRONOME) {
            stopMetronome()
            _uiState.update { it.copy(
                mode = SyncMode.MANUAL,
                statusMessage = R.string.sync_status_stopped,
                tempoImported = false
            ) }
        } else {
            if (currentState.currentGttsMin > 0) {
                startMetronome(currentState.currentGttsMin)
                _uiState.update { it.copy(
                    mode = SyncMode.METRONOME,
                    statusMessage = R.string.sync_status_active,
                    statusArg = currentState.currentGttsMin,
                    tempoImported = true
                ) }
            } else {
                _uiState.update { it.copy(
                    statusMessage = R.string.sync_status_need_calculation
                ) }
            }
        }
    }
    private fun startMetronome(gttsPerMin: Int) {

        // Detener cualquier trabajo anterior
        stopMetronome()

        // Calcular el intervalo en milisegundos entre gotas
        val intervalMs = (60000 / gttsPerMin).toLong()
        Log.d("SYNC", "startMetronome: NUEVO intervalo = $intervalMs")

        _uiState.update { it.copy(isMetronomeActive = true, metronomeBlink = false) }

        // Iniciar el metrónomo en un Coroutine
        metronomeJob = viewModelScope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                // Feedback
                vibrationManager.doHapticFeedback()
                vibrationManager.playClickSound()

                // Efecto visual de parpadeo del botón
                _uiState.update { it.copy(metronomeBlink = true) }
                delay(100) // Parpadeo rápido
                _uiState.update { it.copy(metronomeBlink = false) }

                // Esperar al siguiente intervalo
                delay(intervalMs - 100) // Se resta el tiempo del parpadeo/feedback
            }
        }
    }

    fun stopMetronome() {
        metronomeJob?.cancel()
        metronomeJob = null
        _uiState.update { it.copy(isMetronomeActive = false, metronomeBlink = false) }
    }

    // --- Lógica de Reinicio ---

    fun reset() {
        stopMetronome()
        timestamps.clear()
        _uiState.update {
            SyncUiState(
                currentGttsMin = 0, // Aseguramos que el ritmo también se reinicie
                mode = SyncMode.MANUAL, // Aseguramos que el modo sea manual
                statusMessage = R.string.sync_status_manual,
                tempoImported = false // <--- CLAVE: Reiniciar la bandera
            )
        }
    }

    // Asegurarse de detener el metrónomo cuando el ViewModel se destruye
    override fun onCleared() {
        super.onCleared()
        stopMetronome()
    }

    fun resetTempoImported() {
        Log.d("SYNC", "resetTempoImported() - antes tempoImported=${_uiState.value.tempoImported}")
        _uiState.update { it.copy(tempoImported = false) }
        Log.d("SYNC", "resetTempoImported() - despues tempoImported=${_uiState.value.tempoImported}")
    }

}