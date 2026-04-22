package com.finguia.dados

import android.content.Context
import kotlinx.coroutines.flow.Flow

class CategoriaRepository(context: Context) {

    private val dao = FinGuiaDatabase.obterInstancia(context).categoriaDao()

    fun listarTodas(): Flow<List<CategoriaCustom>> = dao.listarTodas()

    suspend fun salvar(categoria: CategoriaCustom) = dao.inserir(categoria)

    suspend fun deletar(id: Long) = dao.deletar(id)
}
