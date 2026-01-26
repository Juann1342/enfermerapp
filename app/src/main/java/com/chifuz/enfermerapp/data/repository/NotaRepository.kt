package com.chifuz.enfermerapp.data.repository


import com.chifuz.enfermerapp.data.dao.NotaDao
import com.chifuz.enfermerapp.data.model.Nota
import kotlinx.coroutines.flow.Flow

class NotaRepository(private val notaDao: NotaDao) {
    val allNotas: Flow<List<Nota>> = notaDao.getAllNotas()

    suspend fun insertar(nota: Nota) = notaDao.insertNota(nota)
    suspend fun actualizar(nota: Nota) = notaDao.updateNota(nota)
    suspend fun borrar(nota: Nota) = notaDao.deleteNota(nota)
}