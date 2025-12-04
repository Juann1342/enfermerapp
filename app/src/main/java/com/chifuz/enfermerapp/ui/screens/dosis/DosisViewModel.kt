package com.chifuz.enfermerapp.ui.screens.dosis


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Modelo de estado para la interfaz de usuario de Cálculo de Dosis
data class DosisUiState(
    val dosisAdministrar: String = "", // mg
    val dosisAdministrarError: Boolean = false,
    val solvente: String = "", // ml
    val solventeError: Boolean = false,
    val soluto: String = "", // mg/ml
    val solutoError: Boolean = false,
    val resultadoMl: String? = null,
    val isCalculating: Boolean = false,
    val showResultDialog: Boolean = false // <--- NUEVO: Control del diálogo
)

class DosisViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DosisUiState())
    val uiState: StateFlow<DosisUiState> = _uiState.asStateFlow()

    // Manejo de la lógica de cambio de texto
    fun updateDosisAdministrar(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        _uiState.update {
            it.copy(
                dosisAdministrar = filtered,
                dosisAdministrarError = filtered.isEmpty() && input.isNotEmpty(), // Error si no es numérico
                resultadoMl = null, // Limpiar resultado al modificar input
                showResultDialog = false
            )
        }
    }

    fun updateSolvente(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        _uiState.update {
            it.copy(
                solvente = filtered,
                solventeError = filtered.isEmpty() && input.isNotEmpty(),
                resultadoMl = null,
                showResultDialog = false
            )
        }
    }

    fun updateSoluto(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        _uiState.update {
            it.copy(
                soluto = filtered,
                solutoError = filtered.isEmpty() && input.isNotEmpty(),
                resultadoMl = null,
                showResultDialog = false
            )
        }
    }

    // Lógica principal de cálculo
    fun calcularDosis() {
        _uiState.update { it.copy(isCalculating = true) }
        viewModelScope.launch {
            try {
                // 1. Validar y convertir a Double
                val dosisAdmin = _uiState.value.dosisAdministrar.toDoubleOrNull()
                val solvente = _uiState.value.solvente.toDoubleOrNull()
                val soluto = _uiState.value.soluto.toDoubleOrNull()

                // 2. Comprobar que todos los campos son válidos y mayores que cero
                if (dosisAdmin == null || solvente == null || soluto == null ||
                    dosisAdmin <= 0 || solvente <= 0 || soluto <= 0) {

                    // Mostrar error en la UI
                    _uiState.update {
                        it.copy(
                            dosisAdministrarError = dosisAdmin == null || dosisAdmin <= 0,
                            solventeError = solvente == null || solvente <= 0,
                            solutoError = soluto == null || soluto <= 0,
                            resultadoMl = null,
                            isCalculating = false
                        )
                    }
                    return@launch
                }

                val concentracionTotal = soluto / solvente
                val resultado = dosisAdmin / concentracionTotal // Volumen en ml

                // 3. Mostrar resultado en diálogo
                _uiState.update {
                    it.copy(
                        resultadoMl = String.format("%.2f", resultado),
                        isCalculating = false,
                        dosisAdministrarError = false,
                        solventeError = false,
                        solutoError = false,
                        showResultDialog = true // <--- NUEVO: Mostrar diálogo
                    )
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(resultadoMl = "Error de cálculo. $e", isCalculating = false) }
            }
        }
    }

    fun hideResultDialog() {
        _uiState.update { it.copy(showResultDialog = false) }
    }
}