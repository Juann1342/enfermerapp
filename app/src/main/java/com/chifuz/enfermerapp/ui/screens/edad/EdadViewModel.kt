package com.chifuz.enfermerapp.ui.screens.edad

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.Period

data class EdadResultado(val años: Int = 0, val meses: Int = 0, val dias: Int = 0)

data class EdadUiState(
    val fechaNacimiento: LocalDate? = null,
    val esPrematuro: Boolean = false,
    val semanasGestacion: String = "",
    val resultadoCronologico: EdadResultado? = null,
    val resultadoCorregido: EdadResultado? = null,
    val mostrarCorregida: Boolean = false,
    val errorSemanas: Boolean = false,
    val esCalculable: Boolean = false,
    val showResultDialog: Boolean = false,
    val semanasCriterioTermino: Int = 40
)

class EdadViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EdadUiState())
    val uiState: StateFlow<EdadUiState> = _uiState.asStateFlow()

    fun onSemanasCriterioTerminoSelected(semanas: Int) {
        _uiState.update { it.copy(semanasCriterioTermino = semanas) }

        // CORRECCIÓN: Quitamos el "calcularYMostrar(true)" que disparaba el diálogo.
        // Simplemente validamos; si es calculable, actualizará el resultado en el fondo
        // sin interrumpir al usuario con el diálogo emergente.
        validarYPrepararCalculo()
    }

    fun onFechaSeleccionada(fecha: LocalDate) {
        val hoy = LocalDate.now()
        // Calculamos si tiene 24 meses o más
        val meses = java.time.temporal.ChronoUnit.MONTHS.between(fecha, hoy)
        val esMayorDeDosAnios = meses >= 24

        _uiState.update {
            it.copy(
                fechaNacimiento = fecha,
                // LIMPIEZA: Si tiene más de 2 años, reseteamos datos de prematurez
                esPrematuro = if (esMayorDeDosAnios) false else it.esPrematuro,
                semanasGestacion = if (esMayorDeDosAnios) "" else it.semanasGestacion,
                resultadoCorregido = if (esMayorDeDosAnios) null else it.resultadoCorregido,
                mostrarCorregida = if (esMayorDeDosAnios) false else it.mostrarCorregida
            )
        }
        validarYPrepararCalculo()
    }
    fun onEsPrematuroChanged(esPrematuro: Boolean) {
        _uiState.update { it.copy(esPrematuro = esPrematuro, mostrarCorregida = esPrematuro) }
        validarYPrepararCalculo()
    }

    fun onSemanasGestacionChanged(input: String) {
        val filtered = input.filter { it.isDigit() }
        val semanas = filtered.toIntOrNull()
        val error = semanas != null && (semanas < 20 || semanas > 42)
        _uiState.update { it.copy(semanasGestacion = filtered, errorSemanas = error) }
        validarYPrepararCalculo()
    }

    private fun validarYPrepararCalculo() {
        val estado = _uiState.value
        val fechaOk = estado.fechaNacimiento != null
        val prematuroOk = if (estado.esPrematuro) {
            estado.semanasGestacion.isNotEmpty() && !estado.errorSemanas
        } else true

        _uiState.update { it.copy(esCalculable = fechaOk && prematuroOk) }
        if (fechaOk && prematuroOk) {
            calcularYMostrar(debeMostrarDialog = false)
        }
    }

    fun calcularYMostrar(debeMostrarDialog: Boolean = true) {
        val estado = _uiState.value
        val nacimiento = estado.fechaNacimiento ?: return
        val hoy = LocalDate.now()

        // 1. Edad Cronológica
        val pCron = Period.between(nacimiento, hoy)
        val resCron = EdadResultado(pCron.years, pCron.months, pCron.days)

        // 2. Edad Corregida
        var resCorr: EdadResultado? = null
        if (estado.mostrarCorregida) {
            val semanasGestaInput = estado.semanasGestacion.toIntOrNull() ?: 40
            val criterio = estado.semanasCriterioTermino
            val diasFaltantes = ((criterio - semanasGestaInput) * 7).toLong()
            val fechaTermino = nacimiento.plusDays(diasFaltantes)

            if (!fechaTermino.isAfter(hoy)) {
                val pCorr = Period.between(fechaTermino, hoy)
                resCorr = EdadResultado(pCorr.years, pCorr.months, pCorr.days)
            } else {
                val diasTotales = java.time.temporal.ChronoUnit.DAYS.between(hoy, fechaTermino)
                resCorr = EdadResultado(años = -1, meses = (diasTotales / 7).toInt(), dias = (diasTotales % 7).toInt())
            }
        }

        _uiState.update {
            it.copy(
                resultadoCronologico = resCron,
                resultadoCorregido = resCorr,
                showResultDialog = debeMostrarDialog
            )
        }
    }

    fun hideResultDialog() = _uiState.update { it.copy(showResultDialog = false) }
}