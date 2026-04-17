package com.finguia.dados

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TransacaoBancaria::class], version = 1, exportSchema = false)
abstract class FinGuiaDatabase : RoomDatabase() {

    abstract fun transacaoDao(): TransacaoDao

    companion object {
        @Volatile
        private var INSTANCE: FinGuiaDatabase? = null

        fun obterInstancia(context: Context): FinGuiaDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FinGuiaDatabase::class.java,
                    "finguia_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
