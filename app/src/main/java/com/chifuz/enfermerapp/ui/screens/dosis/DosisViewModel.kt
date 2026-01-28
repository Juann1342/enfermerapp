package com.chifuz.enfermerapp.ui.screens.dosis

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DosisCalcType { ESTANDAR, POR_PESO }

data class DosisUiState(
    val calcType: DosisCalcType = DosisCalcType.ESTANDAR,
    val unidadSeleccionada: String = "mg",
    // Campos comunes y estándar
    val dosisAdministrar: String = "",
    val solvente: String = "",
    val soluto: String = "",
    // Campos para cálculo por peso
    val dosisPorKilo: String = "",
    val pesoPaciente: String = "",
    val dosisMaxima: String = "",
    // Errores y resultados
    val resultadoFinal: String? = null,
    val esDosisExcedida: Boolean = false,
    val showResultDialog: Boolean = false
)

class DosisViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DosisUiState())
    val uiState: StateFlow<DosisUiState> = _uiState.asStateFlow()

    val unidadesDosis = listOf("mg", "mcg", "g", "UI")

    fun updateCalcType(type: DosisCalcType) {
        // Al cambiar de modo, reseteamos el estado para evitar mezclar cálculos
        _uiState.update { DosisUiState(calcType = type) }
    }

    fun updateUnidad(nuevaUnidad: String) {
        _uiState.update { it.copy(unidadSeleccionada = nuevaUnidad) }
    }

    // Actualización de campos de texto con filtrado básico
    fun updateDosisPorKilo(input: String) = _uiState.update { it.copy(dosisPorKilo = input.filter { c -> c.isDigit() || c == '.' }) }
    fun updatePeso(input: String) = _uiState.update { it.copy(pesoPaciente = input.filter { c -> c.isDigit() || c == '.' }) }
    fun updateDosisMaxima(input: String) = _uiState.update { it.copy(dosisMaxima = input.filter { c -> c.isDigit() || c == '.' }) }
    fun updateDosisAdministrar(input: String) = _uiState.update { it.copy(dosisAdministrar = input.filter { c -> c.isDigit() || c == '.' }) }
    fun updateSolvente(input: String) = _uiState.update { it.copy(solvente = input.filter { c -> c.isDigit() || c == '.' }) }
    fun updateSoluto(input: String) = _uiState.update { it.copy(soluto = input.filter { c -> c.isDigit() || c == '.' }) }

    fun calcularDosis() {
        if (_uiState.value.calcType == DosisCalcType.ESTANDAR) {
            ejecutarCalculoEstandar()
        } else {
            ejecutarCalculoPorPeso()
        }
    }

    private fun ejecutarCalculoPorPeso() {
        val dosisKilo = _uiState.value.dosisPorKilo.toDoubleOrNull() ?: 0.0
        val peso = _uiState.value.pesoPaciente.toDoubleOrNull() ?: 0.0
        val maxima = _uiState.value.dosisMaxima.toDoubleOrNull() ?: Double.MAX_VALUE

        val resultado = dosisKilo * peso
        // La advertencia solo se activa si el usuario ingresó una dosis máxima
        val excedido = _uiState.value.dosisMaxima.isNotEmpty() && resultado > maxima

        _uiState.update {
            it.copy(
                resultadoFinal = String.format("%.2f", resultado),
                esDosisExcedida = excedido,
                showResultDialog = true
            )
        }
    }

    private fun ejecutarCalculoEstandar() {
        val dosis = _uiState.value.dosisAdministrar.toDoubleOrNull() ?: 0.0
        val solvente = _uiState.value.solvente.toDoubleOrNull() ?: 0.0
        val soluto = _uiState.value.soluto.toDoubleOrNull() ?: 0.0

        if (soluto > 0) {
            val res = (dosis * solvente) / soluto
            _uiState.update {
                it.copy(
                    resultadoFinal = String.format("%.2f", res),
                    esDosisExcedida = false, // No aplica en estándar
                    showResultDialog = true
                )
            }
        }
    }

    fun hideResultDialog() = _uiState.update { it.copy(showResultDialog = false) }

    fun limpiarDatos() {
        _uiState.update { DosisUiState(calcType = it.calcType) }
    }
}