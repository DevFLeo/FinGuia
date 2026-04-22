package com.finguia.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(categoria: CategoriaCustom): Long

    @Query("SELECT * FROM categorias_custom ORDER BY id ASC")
    fun listarTodas(): Flow<List<CategoriaCustom>>

    @Query("DELETE FROM categorias_custom WHERE id = :id")
    suspend fun deletar(id: Long)
}
