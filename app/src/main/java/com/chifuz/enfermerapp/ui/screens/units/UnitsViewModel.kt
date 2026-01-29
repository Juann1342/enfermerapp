package com.chifuz.enfermerapp.ui.screens.units

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UnitsUiState(
    val inputValue: String = "",
    val pesoPaciente: String = "", // Para mg/kg y mcg/kg
    val category: UnitCategory = UnitCategory.PESO,
    val fromUnit: String = "mg",
    val toUnit: String = "g",
    val result: String = "",
    val error: Boolean = false,
    val showResultDialog: Boolean = false
)

enum class UnitCategory {
    PESO, VOLUMEN, INFUSION, TEMPERATURA
}

class UnitsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UnitsUiState())
    val uiState: StateFlow<UnitsUiState> = _uiState.asStateFlow()

    fun onInputChanged(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(inputValue = filtered, error = false) }
    }

    fun onPesoChanged(peso: String) {
        val filtered = peso.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(pesoPaciente = filtered) }
    }

    fun onCategoryChanged(category: UnitCategory) {
        // Al cambiar categoría, reseteamos unidades por defecto
        val (defaultFrom, defaultTo) = when (category) {
            UnitCategory.PESO -> "mg" to "g"
            UnitCategory.VOLUMEN -> "L" to "mL"
            UnitCategory.INFUSION -> "mL/h" to "gtt/min"
            UnitCategory.TEMPERATURA -> "°C" to "°F"
        }
        _uiState.update { it.copy(category = category, fromUnit = defaultFrom, toUnit = defaultTo, result = "") }
    }

    fun onUnitsChanged(from: String, to: String) {
        _uiState.update { it.copy(fromUnit = from, toUnit = to) }
    }
    fun hideResultDialog() {
        _uiState.update { it.copy(showResultDialog = false) }
    }

    fun convert() {
        val valDouble = _uiState.value.inputValue.toDoubleOrNull()
        if (valDouble == null) {
            _uiState.update { it.copy(error = true) }
            return
        }

        val res = when (_uiState.value.category) {
            UnitCategory.PESO -> convertMasa(
                valDouble,
                _uiState.value.fromUnit,
                _uiState.value.toUnit
            )

            UnitCategory.VOLUMEN -> convertVolumen(
                valDouble,
                _uiState.value.fromUnit,
                _uiState.value.toUnit
            )

            UnitCategory.INFUSION -> convertInfusion(
                valDouble,
                _uiState.value.fromUnit,
                _uiState.value.toUnit
            )

            UnitCategory.TEMPERATURA -> convertTemp(
                valDouble,
                _uiState.value.fromUnit,
                _uiState.value.toUnit
            )
        }

        _uiState.update {
            val formattedResult = when {
                res == 0.0 -> "0"
                res >= 100 -> String.format("%.2f", res) // 125.50
                res >= 1 -> if (res % 1 == 0.0) res.toInt().toString() else String.format("%.2f", res)
                res >= 0.01 -> String.format("%.2f", res) // 0.05
                else -> String.format("%.4f", res).trimEnd('0').trimEnd('.') // 0.0005 sin ceros basura al final
            }

            it.copy(
                result = formattedResult,
                showResultDialog = true
            )
        }
    }
    // --- Lógica de Conversión Pragmática ---

    private fun convertMasa(v: Double, from: String, to: String): Double {
        val factors = mapOf("kg" to 1000000.0, "g" to 1000.0, "mg" to 1.0, "mcg" to 0.001)
        return v * (factors[from] ?: 1.0) / (factors[to] ?: 1.0)
    }

    private fun convertVolumen(v: Double, from: String, to: String): Double {
        val factors = mapOf("L" to 1000.0, "mL" to 1.0, "µL" to 0.001)
        return v * (factors[from] ?: 1.0) / (factors[to] ?: 1.0)
    }

    private fun convertInfusion(v: Double, from: String, to: String): Double {
        // Basado en factor estándar 1 mL/h = 1 microgtt/min = 3 gtt/min (macro)
        val inMlPerHour = when(from) {
            "gtt/min" -> v * 3.0
            "microgtt/min" -> v
            else -> v // mL/h
        }
        return when(to) {
            "gtt/min" -> inMlPerHour / 3.0
            "microgtt/min" -> inMlPerHour
            else -> inMlPerHour
        }
    }

    private fun convertTemp(v: Double, from: String, to: String): Double {
        return if (from == "°C" && to == "°F") (v * 9/5) + 32
        else if (from == "°F" && to == "°C") (v - 32) * 5/9
        else v
    }

    private fun convertDosisPeso(v: Double, from: String, to: String): Double {
        val peso = _uiState.value.pesoPaciente.toDoubleOrNull() ?: 1.0
        // mg/kg o mcg/kg a valor absoluto (mg o mcg)
        return v * peso
    }
}
