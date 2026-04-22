package com.finguia.dados

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TransacaoBancaria::class, CategoriaCustom::class, Investimento::class], version = 4, exportSchema = false)
abstract class FinGuiaDatabase : RoomDatabase() {

    abstract fun transacaoDao(): TransacaoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun investimentoDao(): InvestimentoDao

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

        private val MIGRACAO_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS categorias_custom (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        sublabel TEXT NOT NULL,
                        tipo TEXT NOT NULL,
                        ehEntrada INTEGER NOT NULL
                    )"""
                )
            }
        }

        // Migração 3 → 4: lançamentos agendados + tabela de investimentos
        private val MIGRACAO_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE transacoes_bancarias ADD COLUMN dataAgendada INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE transacoes_bancarias ADD COLUMN efetivado INTEGER NOT NULL DEFAULT 1"
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS investimentos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nome TEXT NOT NULL,
                        categoria TEXT NOT NULL,
                        valorInvestido REAL NOT NULL,
                        rentabilidadePct REAL NOT NULL,
                        dataCompraMs INTEGER NOT NULL,
                        observacao TEXT NOT NULL
                    )"""
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
                    .addMigrations(MIGRACAO_1_2, MIGRACAO_2_3, MIGRACAO_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
