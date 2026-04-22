package com.finguia.dados

import android.content.Context
import kotlinx.coroutines.flow.Flow

class TransacaoRepository(context: Context) {

    private val dao = FinGuiaDatabase.obterInstancia(context).transacaoDao()

    suspend fun salvar(transacao: TransacaoBancaria): Long = dao.inserir(transacao)

    fun listarTodas(): Flow<List<TransacaoBancaria>> = dao.listarTodas()

    fun listarReceitas(): Flow<List<TransacaoBancaria>> = dao.listarReceitas()

    fun listarDespesas(): Flow<List<TransacaoBancaria>> = dao.listarDespesas()

    fun totalReceitas(): Flow<Double?> = dao.totalReceitas()

    fun totalDespesas(): Flow<Double?> = dao.totalDespesas()

    fun listarRecorrentes(): Flow<List<TransacaoBancaria>> = dao.listarRecorrentes()

    fun listarAgendadas(): Flow<List<TransacaoBancaria>> = dao.listarAgendadas()

    suspend fun atualizar(transacao: TransacaoBancaria) = dao.atualizar(transacao)

    suspend fun deletar(id: Long) = dao.deletar(id)

    suspend fun buscar(query: String): List<TransacaoBancaria> = dao.buscar(query)
}
