package com.chifuz.enfermerapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chifuz.enfermerapp.data.dao.NotaDao
import com.chifuz.enfermerapp.data.model.Nota

@Database(entities = [Nota::class], version = 1, exportSchema = false)
abstract class EnfermerAppDatabase : RoomDatabase() {

    abstract fun notaDao(): NotaDao

    companion object {
        @Volatile
        private var INSTANCE: EnfermerAppDatabase? = null

        fun getDatabase(context: Context): EnfermerAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EnfermerAppDatabase::class.java,
                    "enfermerapp_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
