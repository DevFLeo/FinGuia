package com.finguia.ui.investimentos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finguia.dados.CategoriaInvestimento
import com.finguia.dados.Investimento
import com.finguia.dados.InvestimentoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sugestão estática de ativo. Não é cotação em tempo real — apenas inspiração
 * pro usuário preencher a carteira manual.
 */
data class SugestaoAtivo(
    val nome: String,
    val ticker: String,
    val categoria: CategoriaInvestimento,
    val descricao: String,
    val rentabilidadeEstimadaPct: Double
)

val SUGESTOES_ATIVOS = listOf(
    // Ações BR
    SugestaoAtivo("Petrobras", "PETR4", CategoriaInvestimento.ACOES_BR, "Petrolífera estatal brasileira", 12.5),
    SugestaoAtivo("Vale", "VALE3", CategoriaInvestimento.ACOES_BR, "Mineradora de ferro e níquel", 8.2),
    SugestaoAtivo("Itaú Unibanco", "ITUB4", CategoriaInvestimento.ACOES_BR, "Maior banco privado do Brasil", 10.0),
    SugestaoAtivo("Bradesco", "BBDC4", CategoriaInvestimento.ACOES_BR, "Banco privado com forte rede", 9.1),
    SugestaoAtivo("Ambev", "ABEV3", CategoriaInvestimento.ACOES_BR, "Cervejaria líder do mercado", 6.5),
    SugestaoAtivo("WEG", "WEGE3", CategoriaInvestimento.ACOES_BR, "Motores elétricos e automação", 14.3),

    // Ações Internacionais
    SugestaoAtivo("Apple", "AAPL", CategoriaInvestimento.ACOES_INTER, "Tecnologia — iPhone, Mac, serviços", 15.8),
    SugestaoAtivo("Microsoft", "MSFT", CategoriaInvestimento.ACOES_INTER, "Software, nuvem e IA", 17.2),
    SugestaoAtivo("Alphabet", "GOOGL", CategoriaInvestimento.ACOES_INTER, "Google e negócios relacionados", 13.4),
    SugestaoAtivo("Amazon", "AMZN", CategoriaInvestimento.ACOES_INTER, "E-commerce e AWS", 11.7),
    SugestaoAtivo("NVIDIA", "NVDA", CategoriaInvestimento.ACOES_INTER, "Chips de GPU e IA", 28.5),
    SugestaoAtivo("Tesla", "TSLA", CategoriaInvestimento.ACOES_INTER, "Veículos elétricos", 18.9),

    // Imóveis / FIIs
    SugestaoAtivo("MXRF11", "MXRF11", CategoriaInvestimento.IMOVEIS, "FII de recebíveis imobiliários", 10.2),
    SugestaoAtivo("HGLG11", "HGLG11", CategoriaInvestimento.IMOVEIS, "FII de galpões logísticos", 9.5),
    SugestaoAtivo("KNRI11", "KNRI11", CategoriaInvestimento.IMOVEIS, "FII misto com imóveis corporativos", 8.7),
    SugestaoAtivo("XPML11", "XPML11", CategoriaInvestimento.IMOVEIS, "FII de shopping centers", 8.1),

    // Renda Fixa (rentabilidades altas atuais)
    SugestaoAtivo("Tesouro Selic 2029", "TESOURO-SELIC", CategoriaInvestimento.RENDA_FIXA, "Título público pós-fixado, baixo risco", 14.6),
    SugestaoAtivo("CDB 115% CDI", "CDB-115CDI", CategoriaInvestimento.RENDA_FIXA, "CDB de banco médio, liquidez diária", 16.8),
    SugestaoAtivo("LCI 95% CDI", "LCI-95CDI", CategoriaInvestimento.RENDA_FIXA, "Letra imobiliária, isenta de IR", 13.9),
    SugestaoAtivo("LCA 97% CDI", "LCA-97CDI", CategoriaInvestimento.RENDA_FIXA, "Letra agronegócio, isenta de IR", 14.2),
    SugestaoAtivo("Tesouro IPCA+ 2035", "TESOURO-IPCA", CategoriaInvestimento.RENDA_FIXA, "Protege da inflação com juro real", 6.5),
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
