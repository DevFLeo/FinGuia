package com.finguia.ui.investimentos

import com.finguia.dados.CategoriaInvestimento

/**
 * Catálogo amplo de ativos com cotação live (BR + Internacional + FIIs).
 * Renda fixa fica no SUGESTOES_ATIVOS clássico (rentabilidade estimada, sem cotação).
 */
val CATALOGO_ATIVOS: List<SugestaoAtivo> = listOf(
    // ============== AÇÕES BR — Bovespa ==============
    sug("PETR4", "Petrobras PN", CategoriaInvestimento.ACOES_BR, "Petrolífera estatal — preferencial"),
    sug("PETR3", "Petrobras ON", CategoriaInvestimento.ACOES_BR, "Petrolífera estatal — ordinária"),
    sug("VALE3", "Vale", CategoriaInvestimento.ACOES_BR, "Mineradora — ferro, níquel, cobre"),
    sug("ITUB4", "Itaú Unibanco PN", CategoriaInvestimento.ACOES_BR, "Maior banco privado do país"),
    sug("ITUB3", "Itaú Unibanco ON", CategoriaInvestimento.ACOES_BR, "Itaú ordinária"),
    sug("BBDC4", "Bradesco PN", CategoriaInvestimento.ACOES_BR, "Banco privado nacional"),
    sug("BBDC3", "Bradesco ON", CategoriaInvestimento.ACOES_BR, "Bradesco ordinária"),
    sug("BBAS3", "Banco do Brasil", CategoriaInvestimento.ACOES_BR, "Banco estatal — varejo amplo"),
    sug("SANB11", "Santander Brasil", CategoriaInvestimento.ACOES_BR, "Unit Santander Brasil"),
    sug("ABEV3", "Ambev", CategoriaInvestimento.ACOES_BR, "Cervejaria líder LATAM"),
    sug("WEGE3", "WEG", CategoriaInvestimento.ACOES_BR, "Motores elétricos e automação industrial"),
    sug("MGLU3", "Magazine Luiza", CategoriaInvestimento.ACOES_BR, "Varejo + e-commerce"),
    sug("LREN3", "Lojas Renner", CategoriaInvestimento.ACOES_BR, "Maior varejista de moda do Brasil"),
    sug("SUZB3", "Suzano", CategoriaInvestimento.ACOES_BR, "Maior produtora mundial de celulose"),
    sug("RENT3", "Localiza", CategoriaInvestimento.ACOES_BR, "Aluguel de carros — líder LATAM"),
    sug("JBSS3", "JBS", CategoriaInvestimento.ACOES_BR, "Maior frigorífico do mundo"),
    sug("BEEF3", "Minerva Foods", CategoriaInvestimento.ACOES_BR, "Frigorífico — bovinos"),
    sug("MRFG3", "Marfrig", CategoriaInvestimento.ACOES_BR, "Frigorífico bovinos"),
    sug("B3SA3", "B3", CategoriaInvestimento.ACOES_BR, "Bolsa de valores brasileira"),
    sug("GGBR4", "Gerdau PN", CategoriaInvestimento.ACOES_BR, "Siderúrgica — aços longos"),
    sug("CSNA3", "CSN", CategoriaInvestimento.ACOES_BR, "Siderúrgica e mineração"),
    sug("USIM5", "Usiminas PNA", CategoriaInvestimento.ACOES_BR, "Siderúrgica de aços planos"),
    sug("EMBR3", "Embraer", CategoriaInvestimento.ACOES_BR, "Fabricante de aeronaves"),
    sug("RAIL3", "Rumo", CategoriaInvestimento.ACOES_BR, "Logística ferroviária"),
    sug("ELET3", "Eletrobras ON", CategoriaInvestimento.ACOES_BR, "Geração e transmissão elétrica"),
    sug("ELET6", "Eletrobras PNB", CategoriaInvestimento.ACOES_BR, "Eletrobras preferencial"),
    sug("PRIO3", "PRIO (PetroRio)", CategoriaInvestimento.ACOES_BR, "Petrolífera independente"),
    sug("VBBR3", "Vibra Energia", CategoriaInvestimento.ACOES_BR, "Distribuidora de combustíveis"),
    sug("UGPA3", "Ultrapar", CategoriaInvestimento.ACOES_BR, "Holding — Ipiranga, Ultragaz"),
    sug("RAIZ4", "Raízen", CategoriaInvestimento.ACOES_BR, "Energia — açúcar, etanol, combustíveis"),
    sug("CSAN3", "Cosan", CategoriaInvestimento.ACOES_BR, "Holding — Raízen, Compass, Rumo"),
    sug("TIMS3", "TIM", CategoriaInvestimento.ACOES_BR, "Telecomunicações móveis"),
    sug("VIVT3", "Telefônica Vivo", CategoriaInvestimento.ACOES_BR, "Telecomunicações"),
    sug("RDOR3", "Rede D'Or", CategoriaInvestimento.ACOES_BR, "Maior rede hospitalar privada"),
    sug("HAPV3", "Hapvida", CategoriaInvestimento.ACOES_BR, "Plano de saúde"),
    sug("FLRY3", "Fleury", CategoriaInvestimento.ACOES_BR, "Medicina diagnóstica"),
    sug("EQTL3", "Equatorial", CategoriaInvestimento.ACOES_BR, "Distribuição de energia"),
    sug("ENGI11", "Energisa", CategoriaInvestimento.ACOES_BR, "Distribuição de energia"),
    sug("CMIG4", "Cemig PN", CategoriaInvestimento.ACOES_BR, "Energia elétrica MG"),
    sug("CPFE3", "CPFL Energia", CategoriaInvestimento.ACOES_BR, "Distribuição de energia SP"),
    sug("TAEE11", "Taesa", CategoriaInvestimento.ACOES_BR, "Transmissão de energia"),
    sug("SBSP3", "Sabesp", CategoriaInvestimento.ACOES_BR, "Saneamento básico SP"),
    sug("KLBN11", "Klabin", CategoriaInvestimento.ACOES_BR, "Papel e celulose"),
    sug("CYRE3", "Cyrela", CategoriaInvestimento.ACOES_BR, "Construtora alto padrão"),
    sug("MRVE3", "MRV", CategoriaInvestimento.ACOES_BR, "Construtora MCMV"),
    sug("EZTC3", "EZTec", CategoriaInvestimento.ACOES_BR, "Construtora alto padrão SP"),
    sug("HYPE3", "Hypera Pharma", CategoriaInvestimento.ACOES_BR, "Farmacêutica"),
    sug("NTCO3", "Natura&Co", CategoriaInvestimento.ACOES_BR, "Cosméticos — Natura, Avon"),
    sug("AZUL4", "Azul", CategoriaInvestimento.ACOES_BR, "Companhia aérea"),
    sug("GOLL4", "Gol", CategoriaInvestimento.ACOES_BR, "Companhia aérea"),
    sug("CCRO3", "CCR", CategoriaInvestimento.ACOES_BR, "Concessões rodoviárias"),
    sug("ASAI3", "Assaí", CategoriaInvestimento.ACOES_BR, "Atacarejo"),
    sug("PCAR3", "Pão de Açúcar", CategoriaInvestimento.ACOES_BR, "Varejo alimentar"),
    sug("CRFB3", "Carrefour Brasil", CategoriaInvestimento.ACOES_BR, "Varejo alimentar"),
    sug("SLCE3", "SLC Agrícola", CategoriaInvestimento.ACOES_BR, "Agronegócio — soja, milho, algodão"),
    sug("TOTS3", "TOTVS", CategoriaInvestimento.ACOES_BR, "Software de gestão (ERP)"),
    sug("LWSA3", "Locaweb", CategoriaInvestimento.ACOES_BR, "Hospedagem e tecnologia"),

    // ============== AÇÕES INTERNACIONAIS ==============
    sug("AAPL", "Apple", CategoriaInvestimento.ACOES_INTER, "iPhone, Mac, serviços"),
    sug("MSFT", "Microsoft", CategoriaInvestimento.ACOES_INTER, "Software, Azure, IA"),
    sug("GOOGL", "Alphabet (Google) Class A", CategoriaInvestimento.ACOES_INTER, "Google e negócios"),
    sug("GOOG", "Alphabet (Google) Class C", CategoriaInvestimento.ACOES_INTER, "Google sem direito a voto"),
    sug("AMZN", "Amazon", CategoriaInvestimento.ACOES_INTER, "E-commerce e AWS"),
    sug("NVDA", "NVIDIA", CategoriaInvestimento.ACOES_INTER, "GPUs e chips de IA"),
    sug("TSLA", "Tesla", CategoriaInvestimento.ACOES_INTER, "Veículos elétricos e energia"),
    sug("META", "Meta Platforms", CategoriaInvestimento.ACOES_INTER, "Facebook, Instagram, WhatsApp"),
    sug("NFLX", "Netflix", CategoriaInvestimento.ACOES_INTER, "Streaming"),
    sug("AMD", "AMD", CategoriaInvestimento.ACOES_INTER, "Processadores e GPUs"),
    sug("INTC", "Intel", CategoriaInvestimento.ACOES_INTER, "Processadores"),
    sug("AVGO", "Broadcom", CategoriaInvestimento.ACOES_INTER, "Semicondutores e software"),
    sug("ORCL", "Oracle", CategoriaInvestimento.ACOES_INTER, "Banco de dados e cloud"),
    sug("CRM", "Salesforce", CategoriaInvestimento.ACOES_INTER, "CRM em nuvem"),
    sug("ADBE", "Adobe", CategoriaInvestimento.ACOES_INTER, "Photoshop, Creative Cloud"),
    sug("JPM", "JPMorgan Chase", CategoriaInvestimento.ACOES_INTER, "Maior banco dos EUA"),
    sug("BAC", "Bank of America", CategoriaInvestimento.ACOES_INTER, "Banco comercial"),
    sug("WFC", "Wells Fargo", CategoriaInvestimento.ACOES_INTER, "Banco de varejo"),
    sug("V", "Visa", CategoriaInvestimento.ACOES_INTER, "Pagamentos"),
    sug("MA", "Mastercard", CategoriaInvestimento.ACOES_INTER, "Pagamentos"),
    sug("PYPL", "PayPal", CategoriaInvestimento.ACOES_INTER, "Pagamentos online"),
    sug("DIS", "Disney", CategoriaInvestimento.ACOES_INTER, "Mídia e entretenimento"),
    sug("KO", "Coca-Cola", CategoriaInvestimento.ACOES_INTER, "Bebidas"),
    sug("PEP", "PepsiCo", CategoriaInvestimento.ACOES_INTER, "Bebidas e snacks"),
    sug("MCD", "McDonald's", CategoriaInvestimento.ACOES_INTER, "Fast food global"),
    sug("SBUX", "Starbucks", CategoriaInvestimento.ACOES_INTER, "Cafeterias"),
    sug("NKE", "Nike", CategoriaInvestimento.ACOES_INTER, "Material esportivo"),
    sug("UBER", "Uber", CategoriaInvestimento.ACOES_INTER, "Mobilidade e delivery"),
    sug("ABNB", "Airbnb", CategoriaInvestimento.ACOES_INTER, "Hospedagem peer-to-peer"),
    sug("BABA", "Alibaba", CategoriaInvestimento.ACOES_INTER, "E-commerce China"),
    sug("BRK-B", "Berkshire Hathaway B", CategoriaInvestimento.ACOES_INTER, "Holding de Warren Buffett"),
    sug("XOM", "ExxonMobil", CategoriaInvestimento.ACOES_INTER, "Petróleo e gás"),
    sug("CVX", "Chevron", CategoriaInvestimento.ACOES_INTER, "Petróleo e gás"),
    sug("PFE", "Pfizer", CategoriaInvestimento.ACOES_INTER, "Farmacêutica"),
    sug("JNJ", "Johnson & Johnson", CategoriaInvestimento.ACOES_INTER, "Saúde e farmacêuticos"),
    sug("LLY", "Eli Lilly", CategoriaInvestimento.ACOES_INTER, "Farmacêutica — Mounjaro/Zepbound"),
    sug("WMT", "Walmart", CategoriaInvestimento.ACOES_INTER, "Maior varejista do mundo"),
    sug("COST", "Costco", CategoriaInvestimento.ACOES_INTER, "Atacarejo de membros"),
    sug("HD", "Home Depot", CategoriaInvestimento.ACOES_INTER, "Construção e reforma"),
    sug("BA", "Boeing", CategoriaInvestimento.ACOES_INTER, "Aeronaves comerciais e defesa"),
    sug("F", "Ford", CategoriaInvestimento.ACOES_INTER, "Montadora"),
    sug("GM", "General Motors", CategoriaInvestimento.ACOES_INTER, "Montadora"),
    sug("PLTR", "Palantir", CategoriaInvestimento.ACOES_INTER, "Software de dados/IA"),
    sug("SHOP", "Shopify", CategoriaInvestimento.ACOES_INTER, "Plataforma de e-commerce"),
    sug("SQ", "Block (Square)", CategoriaInvestimento.ACOES_INTER, "Pagamentos e fintech"),
    sug("SPY", "S&P 500 ETF", CategoriaInvestimento.ACOES_INTER, "ETF do índice S&P 500"),
    sug("QQQ", "Nasdaq 100 ETF", CategoriaInvestimento.ACOES_INTER, "ETF Nasdaq 100"),

    // ============== FIIs ==============
    sug("MXRF11", "Maxi Renda FII", CategoriaInvestimento.IMOVEIS, "FII de recebíveis imobiliários"),
    sug("HGLG11", "CSHG Logística", CategoriaInvestimento.IMOVEIS, "FII de galpões logísticos"),
    sug("KNRI11", "Kinea Renda Imob.", CategoriaInvestimento.IMOVEIS, "FII híbrido"),
    sug("XPML11", "XP Malls", CategoriaInvestimento.IMOVEIS, "FII de shopping centers"),
    sug("BCFF11", "BTG Pactual Fundos de Fundos", CategoriaInvestimento.IMOVEIS, "FoF — investe em FIIs"),
    sug("VISC11", "Vinci Shopping Centers", CategoriaInvestimento.IMOVEIS, "FII de shoppings"),
    sug("VGHF11", "Valora Hedge Fund", CategoriaInvestimento.IMOVEIS, "FII de papel/CRIs"),
    sug("KNCR11", "Kinea Rendimentos", CategoriaInvestimento.IMOVEIS, "FII de papel CDI"),
    sug("HGRU11", "CSHG Renda Urbana", CategoriaInvestimento.IMOVEIS, "FII de varejo + educação"),
    sug("VILG11", "Vinci Logística", CategoriaInvestimento.IMOVEIS, "FII de galpões"),
    sug("BTLG11", "BTG Logística", CategoriaInvestimento.IMOVEIS, "FII logístico"),
    sug("RECT11", "REC Recebíveis", CategoriaInvestimento.IMOVEIS, "FII de recebíveis"),
    sug("HCTR11", "Hectare CE", CategoriaInvestimento.IMOVEIS, "FII de papel high yield"),
    sug("IRDM11", "Iridium Recebíveis", CategoriaInvestimento.IMOVEIS, "FII de recebíveis"),
    sug("RBRR11", "RBR Rendimento", CategoriaInvestimento.IMOVEIS, "FII de papel"),
    sug("VRTA11", "Fator Verità", CategoriaInvestimento.IMOVEIS, "FII de CRIs"),
    sug("BRCO11", "Bresco Logística", CategoriaInvestimento.IMOVEIS, "FII de galpões alto padrão"),
    sug("ALZR11", "Alianza Trust Renda", CategoriaInvestimento.IMOVEIS, "FII de tijolo diversificado"),
    sug("HGBS11", "CSHG Brasil Shopping", CategoriaInvestimento.IMOVEIS, "FII de shopping centers"),

    // ============== CRIPTOMOEDAS ==============
    sug("BTC-USD", "Bitcoin", CategoriaInvestimento.CRIPTO, "A primeira e maior criptomoeda descentralizada"),
    sug("ETH-USD", "Ethereum", CategoriaInvestimento.CRIPTO, "Plataforma de contratos inteligentes e DApps"),
    sug("SOL-USD", "Solana", CategoriaInvestimento.CRIPTO, "Blockchain de alto desempenho para DApps"),
    sug("BNB-USD", "BNB", CategoriaInvestimento.CRIPTO, "Moeda nativa do ecossistema Binance"),
    sug("ADA-USD", "Cardano", CategoriaInvestimento.CRIPTO, "Blockchain focada em segurança e pesquisa científica"),
    sug("DOGE-USD", "Dogecoin", CategoriaInvestimento.CRIPTO, "Criptomoeda baseada em meme com comunidade global ativa"),
)

private fun sug(ticker: String, nome: String, cat: CategoriaInvestimento, desc: String) =
    SugestaoAtivo(nome, ticker, cat, desc, 0.0, temCotacaoLive = true)

/** Busca fuzzy por ticker ou nome. Case-insensitive, sem acento simplificado. */
fun buscarCatalogo(query: String, limite: Int = 30): List<SugestaoAtivo> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return CATALOGO_ATIVOS.asSequence()
        .map { it to pontuacao(it, q) }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .take(limite)
        .map { it.first }
        .toList()
}

private fun pontuacao(s: SugestaoAtivo, q: String): Int {
    val tk = s.ticker.lowercase()
    val nome = s.nome.lowercase()
    return when {
        tk == q -> 100
        tk.startsWith(q) -> 80
        nome.startsWith(q) -> 60
        tk.contains(q) -> 40
        nome.contains(q) -> 20
        else -> 0
    }
}
