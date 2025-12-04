package com.chifuz.enfermerapp.ui.screens.perfusion


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt // Importar para redondeo

// Constantes para el cálculo de gotas
// Macrogotero (Macro) = 20 gotas/ml (asumo la convención más común)
const val MACRO_FACTOR = 20.0
// Microgotero (Micro) = 60 gotas/ml
const val MICRO_FACTOR = 60.0

enum class GoteroType { MACRO, MICRO }
enum class TimeUnit { HOURS, MINUTES }

// Modelo de estado para la interfaz de usuario de Cálculo de Perfusión
data class PerfusionUiState(
    val selectedGotero: GoteroType = GoteroType.MICRO,
    val volumen: String = "", // Volumen en ml
    val volumenError: Boolean = false,
    val selectedTimeUnit: TimeUnit = TimeUnit.HOURS,
    val tiempo: String = "", // Tiempo en horas o minutos
    val tiempoError: Boolean = false,
    val resultadoMlHr: String? = null, // Resultado en ml/hr
    val resultadoMlMin: String? = null, // Resultado en ml/min
    val resultadoGttsMin: String? = null, // Resultado en gotas/min (String con decimales)
    val resultadoGttsMinInt: Int = 0, // <--- NUEVO: Resultado en gotas/min (Int para navegación)
    val isCalculating: Boolean = false,
    val showResultDialog: Boolean = false
)

class PerfusionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PerfusionUiState())
    val uiState: StateFlow<PerfusionUiState> = _uiState.asStateFlow()

    // --- Handlers de UI ---

    fun selectGotero(type: GoteroType) {
        _uiState.update { it.copy(selectedGotero = type, resultadoMlHr = null, resultadoMlMin = null, resultadoGttsMin = null, resultadoGttsMinInt = 0, showResultDialog = false) }
    }

    fun selectTimeUnit(unit: TimeUnit) {
        _uiState.update { it.copy(selectedTimeUnit = unit, resultadoMlHr = null, resultadoMlMin = null, resultadoGttsMin = null, resultadoGttsMinInt = 0, showResultDialog = false) }
    }

    fun updateVolumen(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        _uiState.update {
            it.copy(
                volumen = filtered,
                volumenError = filtered.isEmpty() && input.isNotEmpty(),
                resultadoMlHr = null, resultadoMlMin = null, resultadoGttsMin = null, resultadoGttsMinInt = 0, showResultDialog = false
            )
        }
    }

    fun updateTiempo(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        _uiState.update {
            it.copy(
                tiempo = filtered,
                tiempoError = filtered.isEmpty() && input.isNotEmpty(),
                resultadoMlHr = null, resultadoMlMin = null, resultadoGttsMin = null, resultadoGttsMinInt = 0, showResultDialog = false
            )
        }
    }

    // --- Lógica de Cálculo ---

    fun calcularPerfusion() {
        _uiState.update { it.copy(isCalculating = true) }
        viewModelScope.launch {
            try {
                val volumen = _uiState.value.volumen.toDoubleOrNull()
                val tiempo = _uiState.value.tiempo.toDoubleOrNull()
                val goteroFactor = if (_uiState.value.selectedGotero == GoteroType.MACRO) MACRO_FACTOR else MICRO_FACTOR

                // 1. Validar inputs
                if (volumen == null || tiempo == null || volumen <= 0 || tiempo <= 0) {
                    _uiState.update {
                        it.copy(
                            volumenError = volumen == null || volumen <= 0,
                            tiempoError = tiempo == null || tiempo <= 0,
                            resultadoMlHr = null, resultadoMlMin = null, resultadoGttsMin = null, resultadoGttsMinInt = 0,
                            isCalculating = false
                        )
                    }
                    return@launch
                }

                // 2. Normalizar el tiempo a horas para el cálculo de ml/hr
                val tiempoEnHoras = when (_uiState.value.selectedTimeUnit) {
                    TimeUnit.HOURS -> tiempo
                    TimeUnit.MINUTES -> tiempo / 60.0
                }

                // Cálculo 1: Velocidad en ml/hr
                val velocidadMlHr = volumen / tiempoEnHoras

                // Cálculo 2: Velocidad en ml/min
                val velocidadMlMin = velocidadMlHr / 60.0

                // Cálculo 3: Velocidad en gotas/min (con decimales)
                val velocidadGttsMinDouble = (volumen * goteroFactor) / (tiempoEnHoras * 60.0)

                // NUEVO CÁLCULO: Redondear a entero para el metrónomo/navegación
                val velocidadGttsMinInt = velocidadGttsMinDouble.roundToInt()

                // 3. Mostrar resultado en diálogo
                _uiState.update {
                    it.copy(
                        resultadoMlHr = String.format("%.2f", velocidadMlHr),
                        resultadoMlMin = String.format("%.2f", velocidadMlMin),
                        resultadoGttsMin = String.format("%.2f", velocidadGttsMinDouble), // Mantenemos decimales para mostrar
                        resultadoGttsMinInt = velocidadGttsMinInt, // <--- GUARDAMOS EL ENTERO
                        isCalculating = false,
                        volumenError = false,
                        tiempoError = false,
                        showResultDialog = true
                    )
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(resultadoMlHr = "Error de cálculo. $e", resultadoMlMin = null, resultadoGttsMin = null, resultadoGttsMinInt = 0, isCalculating = false) }
            }
        }
    }

    fun hideResultDialog() {
        _uiState.update { it.copy(showResultDialog = false) }
    }
}