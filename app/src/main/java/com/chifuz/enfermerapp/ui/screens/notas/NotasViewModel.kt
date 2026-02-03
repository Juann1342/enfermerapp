package com.chifuz.enfermerapp.ui.screens.notas


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chifuz.enfermerapp.data.model.Nota
import com.chifuz.enfermerapp.data.repository.NotaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotasViewModel(private val repository: NotaRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Lista de notas observable por la UI
    val notas: StateFlow<List<Nota>> = repository.allNotas
        .combine(_searchQuery) { notas, query ->
            if (query.isBlank()) notas
            else {
                notas.filter {
                    it.titulo.contains(query, ignoreCase = true) ||
                            it.contenido.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun guardarNota(titulo: String, contenido: String, id: Int = 0) {
        viewModelScope.launch {
            val nota = Nota(
                id = id,
                titulo = titulo.trim(),
                contenido = contenido.trim(),
                timestamp = System.currentTimeMillis()
            )
            if (id == 0) repository.insertar(nota) else repository.actualizar(nota)
        }
    }

    fun eliminarNota(nota: Nota) {
        viewModelScope.launch {
            repository.borrar(nota)
        }
    }
}

class NotasViewModelFactory(private val repository: NotaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotasViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}