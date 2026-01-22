package com.chifuz.enfermerapp.ui.screens.edad

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

// Clase para transportar los números a la UI
data class EdadResultado(
    val años: Int = 0,
    val meses: Int = 0,
    val dias: Int = 0
)

data class EdadUiState(
    val fechaNacimiento: LocalDate? = null,
    val esPrematuro: Boolean = false,
    val semanasGestacion: String = "",
    val resultadoCronologico: EdadResultado? = null,
    val resultadoCorregido: EdadResultado? = null,
    val mostrarCorregida: Boolean = false,
    val errorSemanas: Boolean = false,
    val esCalculable: Boolean = false,
    val showResultDialog: Boolean = false
)

class EdadViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EdadUiState())
    val uiState: StateFlow<EdadUiState> = _uiState.asStateFlow()

    fun onFechaSeleccionada(fecha: LocalDate) {
        _uiState.update { it.copy(fechaNacimiento = fecha) }
        validarYPreparar()
    }

    fun onEsPrematuroChanged(esPrematuro: Boolean) {
        _uiState.update { it.copy(esPrematuro = esPrematuro) }
        validarYPreparar()
    }

    fun onSemanasChanged(semanas: String) {
        val filtrado = semanas.filter { it.isDigit() }
        _uiState.update { it.copy(semanasGestacion = filtrado) }
        validarYPreparar()
    }

    private fun validarYPreparar() {
        val estado = _uiState.value
        val nacimiento = estado.fechaNacimiento ?: return
        val hoy = LocalDate.now()

        val mesesTotales = ChronoUnit.MONTHS.between(nacimiento, hoy)
        val puedeSerPrematuro = mesesTotales < 24

        val semanasInt = estado.semanasGestacion.toIntOrNull()
        val errorSemanas = estado.esPrematuro && puedeSerPrematuro &&
                (semanasInt == null || semanasInt !in 20..36)

        _uiState.update {
            it.copy(
                mostrarCorregida = estado.esPrematuro && puedeSerPrematuro,
                errorSemanas = errorSemanas,
                esCalculable = !errorSemanas
            )
        }
    }

// Dentro de EdadViewModel.kt -> calcularYMostrar()

    fun calcularYMostrar() {
        val estado = _uiState.value
        val nacimiento = estado.fechaNacimiento ?: return
        val hoy = LocalDate.now()

        // 1. Cronológica
        val pCron = Period.between(nacimiento, hoy)
        val resCron = EdadResultado(pCron.years, pCron.months, pCron.days)

        // 2. Corregida
        var resCorr: EdadResultado? = null
// Dentro de calcularYMostrar()
        if (estado.mostrarCorregida) {
            val semanasGestaInput = estado.semanasGestacion.toIntOrNull() ?: 40
            val diasFaltantesPara40 = ((40 - semanasGestaInput) * 7).toLong()
            val fechaTermino = nacimiento.plusDays(diasFaltantesPara40)

            if (!fechaTermino.isAfter(hoy)) {
                // CASO A: El bebé ya pasó las 40 semanas.
                // Se calcula la Edad Corregida normal (años, meses, días).
                val pCorr = Period.between(fechaTermino, hoy)
                resCorr = EdadResultado(pCorr.years, pCorr.months, pCorr.days)
            } else {
            // ES PRETÉRMINO: Calculamos cuánto falta para las 40 semanas
            val diasTotales = java.time.temporal.ChronoUnit.DAYS.between(hoy, fechaTermino)
            val semanasFaltan = (diasTotales / 7).toInt()
            val diasFaltan = (diasTotales % 7).toInt()

            // Usamos -1 en años como "bandera" técnica
            resCorr = EdadResultado(años = -1, meses = semanasFaltan, dias = diasFaltan)
        }
        }

        _uiState.update {
            it.copy(
                resultadoCronologico = resCron,
                resultadoCorregido = resCorr,
                showResultDialog = true
            )
        }
    }
    fun dismissDialog() = _uiState.update { it.copy(showResultDialog = false) }
}