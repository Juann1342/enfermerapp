package com.chifuz.enfermerapp.data.dao

import androidx.room.*
import com.chifuz.enfermerapp.data.model.Nota
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaDao {
    @Query("SELECT * FROM notas_table ORDER BY timestamp DESC")
    fun getAllNotas(): Flow<List<Nota>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNota(nota: Nota)

    @Update
    suspend fun updateNota(nota: Nota)

    @Delete
    suspend fun deleteNota(nota: Nota)
}
