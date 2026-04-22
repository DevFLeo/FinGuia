package com.finguia.ui.transacoes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finguia.dados.TransacaoBancaria
import com.finguia.dados.TransacaoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransacaoViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = TransacaoRepository(app)

    val transacoes: StateFlow<List<TransacaoBancaria>> = repository
        .listarTodas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todasTransacoes = transacoes

    val totalReceitas: StateFlow<Double> = repository
        .totalReceitas()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val totalDespesas: StateFlow<Double> = repository
        .totalDespesas()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // Apenas lançamentos marcados como recorrentes
    val recorrentes: StateFlow<List<TransacaoBancaria>> = repository
        .listarRecorrentes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Lançamentos agendados (futuros, ainda não efetivados)
    val agendadas: StateFlow<List<TransacaoBancaria>> = repository
        .listarAgendadas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun efetivar(transacao: TransacaoBancaria) {
        viewModelScope.launch {
            repository.atualizar(
                transacao.copy(
                    efetivado = true,
                    timestampMs = System.currentTimeMillis()
                )
            )
        }
    }

    fun inserir(transacao: TransacaoBancaria) {
        viewModelScope.launch { repository.salvar(transacao) }
    }

    fun atualizar(transacao: TransacaoBancaria) {
        viewModelScope.launch { repository.atualizar(transacao) }
    }

    fun deletar(id: Long) {
        viewModelScope.launch { repository.deletar(id) }
    }

    suspend fun buscar(query: String): List<TransacaoBancaria> = repository.buscar(query)
}
