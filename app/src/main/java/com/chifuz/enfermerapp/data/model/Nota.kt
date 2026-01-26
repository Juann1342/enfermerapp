package com.chifuz.enfermerapp.data.model


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notas_table")
data class Nota(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val titulo: String,
    val contenido: String,
    val timestamp: Long = System.currentTimeMillis()
)