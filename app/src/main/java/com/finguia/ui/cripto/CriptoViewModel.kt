package com.finguia.ui.cripto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finguia.dados.Cripto
import com.finguia.dados.TrendingCoinItem
import com.finguia.dados.CriptoRetrofit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val IDS_BLOQUEADOS = setOf("figure-lending", "figuredao", "figure-heloc", "heloc")

data class DadosCripto(
    val ranking: List<Cripto> = emptyList(),
    val maioresAltas: List<Cripto> = emptyList(),
    val trending: List<TrendingCoinItem> = emptyList()
)

sealed class EstadoCripto {
    object Carregando : EstadoCripto()
    data class Sucesso(val dados: DadosCripto) : EstadoCripto()
    data class Erro(val mensagem: String) : EstadoCripto()
}

class CriptoViewModel : ViewModel() {
    private val _estado = MutableStateFlow<EstadoCripto>(EstadoCripto.Carregando)
    val estado: StateFlow<EstadoCripto> = _estado

    init {
        iniciarAtualizacaoAutomatica()
    }

    private fun iniciarAtualizacaoAutomatica() {
        viewModelScope.launch {
            while (true) {
                buscarCriptos()
                delay(60_000L)
            }
        }
    }

    fun buscarCriptos() {
        viewModelScope.launch {
            try {
                val mercado = CriptoRetrofit.servico.buscarCriptos()
                    .filter { it.id !in IDS_BLOQUEADOS }

                val ranking = mercado.take(10)
                val maioresAltas = mercado
                    .filter { it.price_change_percentage_24h != null }
                    .sortedByDescending { it.price_change_percentage_24h }
                    .take(8)

                val trendingResp = CriptoRetrofit.servico.buscarTrending()
                val trending = trendingResp.coins
                    .map { it.item }
                    .filter { it.id !in IDS_BLOQUEADOS }
                    .take(7)

                _estado.value = EstadoCripto.Sucesso(
                    DadosCripto(ranking = ranking, maioresAltas = maioresAltas, trending = trending)
                )
            } catch (e: Exception) {
                if (_estado.value !is EstadoCripto.Sucesso) {
                    _estado.value = EstadoCripto.Erro("Falha ao carregar criptomoedas. Verifique sua conexão.")
                }
            }
        }
    }
}
