package com.finguia.dados

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorias_custom")
data class CategoriaCustom(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val sublabel: String,
    val tipo: TipoTransacao,
    val ehEntrada: Boolean
)
