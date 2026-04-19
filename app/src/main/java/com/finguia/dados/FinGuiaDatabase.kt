package com.finguia.dados

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TransacaoBancaria::class], version = 2, exportSchema = false)
abstract class FinGuiaDatabase : RoomDatabase() {

    abstract fun transacaoDao(): TransacaoDao

    companion object {

        @Volatile
        private var INSTANCE: FinGuiaDatabase? = null

        // Migração da versão 1 para 2: adiciona coluna de lançamento recorrente
        private val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE transacoes_bancarias ADD COLUMN recorrente INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun obterInstancia(context: Context): FinGuiaDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FinGuiaDatabase::class.java,
                    "finguia_database"
                )
                    .addMigrations(MIGRACAO_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
