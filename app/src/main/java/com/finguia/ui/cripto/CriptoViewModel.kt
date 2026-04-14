package com.finguia.ui.cripto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finguia.dados.Cripto
import com.finguia.dados.CriptoRetrofit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val IDS_BLOQUEADOS = setOf("figure-lending", "figuredao", "figure-heloc", "heloc")

sealed class EstadoCripto {
    object Carregando : EstadoCripto()
    data class Sucesso(val criptos: List<Cripto>) : EstadoCripto()
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
                delay(30_000L)
            }
        }
    }

    fun buscarCriptos() {
        viewModelScope.launch {
            try {
                val resultado = CriptoRetrofit.servico.buscarCriptos()
                val filtrado = resultado.filter { it.id !in IDS_BLOQUEADOS }
                _estado.value = EstadoCripto.Sucesso(filtrado)
            } catch (e: Exception) {
                if (_estado.value !is EstadoCripto.Sucesso) {
                    _estado.value = EstadoCripto.Erro("Falha ao carregar criptomoedas. Verifique sua conexão.")
                }
            }
        }
    }
}
