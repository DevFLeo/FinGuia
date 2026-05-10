package com.finguia.ui.investimentos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finguia.dados.CategoriaInvestimento
import com.finguia.dados.CotacaoAtivo
import com.finguia.dados.GNewsArtigo
import com.finguia.dados.Investimento
import com.finguia.dados.InvestimentoRepository
import com.finguia.dados.MercadoRepository
import com.finguia.dados.TaxasBcb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sugestão de ativo com ticker real para cotação live (quando aplicável).
 */
data class SugestaoAtivo(
    val nome: String,
    val ticker: String,
    val categoria: CategoriaInvestimento,
    val descricao: String,
    val rentabilidadeEstimadaPct: Double,
    /** Se true, busca cotação real-time via Brapi/Yahoo. Renda fixa = false. */
    val temCotacaoLive: Boolean = true
)

val SUGESTOES_RENDA_FIXA = listOf(
    SugestaoAtivo("Tesouro Selic 2029", "TESOURO-SELIC", CategoriaInvestimento.RENDA_FIXA, "Título público pós-fixado, baixo risco", 14.6, temCotacaoLive = false),
    SugestaoAtivo("CDB 115% CDI", "CDB-115CDI", CategoriaInvestimento.RENDA_FIXA, "CDB de banco médio, liquidez diária", 16.8, temCotacaoLive = false),
    SugestaoAtivo("LCI 95% CDI", "LCI-95CDI", CategoriaInvestimento.RENDA_FIXA, "Letra imobiliária, isenta de IR", 13.9, temCotacaoLive = false),
    SugestaoAtivo("LCA 97% CDI", "LCA-97CDI", CategoriaInvestimento.RENDA_FIXA, "Letra agronegócio, isenta de IR", 14.2, temCotacaoLive = false),
    SugestaoAtivo("Tesouro IPCA+ 2035", "TESOURO-IPCA", CategoriaInvestimento.RENDA_FIXA, "Protege da inflação com juro real", 6.5, temCotacaoLive = false),
    SugestaoAtivo("Tesouro Prefixado 2027", "TESOURO-PRE", CategoriaInvestimento.RENDA_FIXA, "Taxa fixa conhecida no momento da compra", 12.0, temCotacaoLive = false),
    SugestaoAtivo("CDB 100% CDI", "CDB-100CDI", CategoriaInvestimento.RENDA_FIXA, "CDB de banco grande, liquidez diária", 14.5, temCotacaoLive = false),
    SugestaoAtivo("CRI High Yield", "CRI-HY", CategoriaInvestimento.RENDA_FIXA, "Certificado de Recebíveis Imobiliários, isento de IR", 16.0, temCotacaoLive = false),
    SugestaoAtivo("Debênture Incentivada", "DEB-INC", CategoriaInvestimento.RENDA_FIXA, "Dívida corporativa de infra, isenta de IR", 13.5, temCotacaoLive = false),
)

/** Lista combinada para a tela: catálogo amplo (cotação live) + renda fixa estática. */
val SUGESTOES_ATIVOS: List<SugestaoAtivo> = CATALOGO_ATIVOS + SUGESTOES_RENDA_FIXA

/** Estado da cotação para uso em cards. */
data class EstadoCotacao(
    val cotacao: CotacaoAtivo? = null,
    val carregando: Boolean = false
)

class InvestimentoViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = InvestimentoRepository(app)

    val investimentos: StateFlow<List<Investimento>> = repository
        .listarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalInvestido: StateFlow<Double> = repository
        .totalInvestido()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    private val _cotacoes = MutableStateFlow<Map<String, EstadoCotacao>>(emptyMap())
    val cotacoes: StateFlow<Map<String, EstadoCotacao>> = _cotacoes.asStateFlow()

    init {
        // Refresh periódico (30s) das cotações de tudo que importa: sugestões com ticker live + carteira
        viewModelScope.launch {
            while (true) {
                atualizarCotacoes()
                delay(30_000)
            }
        }
    }

    private suspend fun atualizarCotacoes() {
        // Refresh só carteira + tickers já em cache (vistos pelo usuário). Sugestões fazem lazy fetch.
        val tickersCarteira = runCatching { investimentos.first() }.getOrDefault(emptyList())
            .mapNotNull { it.ticker.takeIf { t -> t.isNotBlank() } }
        val cacheados = _cotacoes.value.keys
        val todos = (tickersCarteira + cacheados).distinct()
        if (todos.isEmpty()) return

        withContext(Dispatchers.IO) {
            val resultados = todos.map { t ->
                async { t to MercadoRepository.cotacao(t) }
            }.awaitAll()
            val novo = _cotacoes.value.toMutableMap()
            resultados.forEach { (t, c) ->
                if (c != null) novo[t] = EstadoCotacao(cotacao = c, carregando = false)
            }
            _cotacoes.value = novo
        }
    }

    /** Lazy fetch chamado pelos cards quando ficam visíveis. Não refaz se já em cache. */
    fun solicitarCotacao(ticker: String) {
        if (ticker.isBlank()) return
        if (_cotacoes.value[ticker]?.cotacao != null) return
        if (_cotacoes.value[ticker]?.carregando == true) return
        _cotacoes.value = _cotacoes.value + (ticker to EstadoCotacao(carregando = true))
        viewModelScope.launch(Dispatchers.IO) {
            val c = MercadoRepository.cotacao(ticker)
            _cotacoes.value = _cotacoes.value + (ticker to EstadoCotacao(cotacao = c, carregando = false))
        }
    }

    fun forcarRefresh() {
        viewModelScope.launch { atualizarCotacoes() }
    }

    suspend fun buscarCotacao(ticker: String): CotacaoAtivo? = withContext(Dispatchers.IO) {
        MercadoRepository.cotacao(ticker)
    }

    suspend fun buscarGrafico(ticker: String, range: String, interval: String): List<Double> =
        withContext(Dispatchers.IO) { MercadoRepository.grafico(ticker, range, interval) }

    suspend fun buscarNoticias(query: String): List<GNewsArtigo> =
        withContext(Dispatchers.IO) { MercadoRepository.noticias(query) }

    /** Busca local no catálogo (Yahoo search exige cookie/crumb e bloqueia). */
    fun buscarLocal(query: String): List<SugestaoAtivo> = buscarCatalogo(query)

    suspend fun taxasBcb(): TaxasBcb =
        withContext(Dispatchers.IO) { MercadoRepository.taxasBcb() }

    fun adicionar(inv: Investimento) {
        viewModelScope.launch { repository.salvar(inv) }
    }

    fun atualizar(inv: Investimento) {
        viewModelScope.launch { repository.atualizar(inv) }
    }

    fun remover(id: Long) {
        viewModelScope.launch { repository.deletar(id) }
    }
}
