package com.finguia.dados

import com.finguia.BuildConfig

data class CotacaoAtivo(
    val ticker: String,
    val nomeLongo: String?,
    val moeda: String,
    val preco: Double,
    val variacaoAbs: Double?,
    val variacaoPct: Double?,
    val maxDia: Double?,
    val minDia: Double?,
    val abertura: Double?,
    val fechamentoAnterior: Double?,
    val max52sem: Double?,
    val min52sem: Double?,
    val volume: Long?,
    val marketCap: Double?,
    val logoUrl: String?,
    val historico: List<Double>
)

data class TaxasBcb(
    val selic: Double?,
    val cdi: Double?,
    val ipca12m: Double?
)

object MercadoRepository {

    /** BR ticker termina com dígito (PETR4, MXRF11). US é só letras. */
    fun ehTickerBr(ticker: String): Boolean = ticker.any { it.isDigit() }

    /** Adiciona sufixo .SA pra Yahoo se ticker BR. */
    private fun yahooSymbol(ticker: String): String {
        val t = ticker.uppercase()
        return if (ehTickerBr(t) && !t.endsWith(".SA")) "$t.SA" else t
    }

    suspend fun cotacao(ticker: String): CotacaoAtivo? = runCatching {
        val symbol = yahooSymbol(ticker)
        val resp = YahooRetrofit.servico.cotacao(symbol)
        val r = resp.chart?.result?.firstOrNull() ?: return@runCatching null
        val meta = r.meta ?: return@runCatching null
        val preco = meta.regularMarketPrice ?: return@runCatching null
        val previo = meta.previousClose ?: meta.chartPreviousClose
        val variacaoAbs = previo?.let { preco - it }
        val variacaoPct = previo?.let { if (it != 0.0) (preco - it) / it * 100.0 else null }
        val historico = r.indicators?.quote?.firstOrNull()?.close
            ?.filterNotNull()
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(preco)
        val abertura = r.indicators?.quote?.firstOrNull()?.open?.firstOrNull()
        CotacaoAtivo(
            ticker = ticker.uppercase(),
            nomeLongo = meta.longName ?: meta.shortName,
            moeda = meta.currency ?: if (ehTickerBr(ticker)) "BRL" else "USD",
            preco = preco,
            variacaoAbs = variacaoAbs,
            variacaoPct = variacaoPct,
            maxDia = meta.regularMarketDayHigh,
            minDia = meta.regularMarketDayLow,
            abertura = abertura,
            fechamentoAnterior = previo,
            max52sem = meta.fiftyTwoWeekHigh,
            min52sem = meta.fiftyTwoWeekLow,
            volume = meta.regularMarketVolume,
            marketCap = null,
            logoUrl = logoUrl(ticker),
            historico = historico
        )
    }.getOrNull()

    suspend fun grafico(ticker: String, range: String, interval: String): List<Double> = runCatching {
        YahooRetrofit.servico.cotacao(yahooSymbol(ticker), range, interval)
            .chart?.result?.firstOrNull()
            ?.indicators?.quote?.firstOrNull()?.close
            ?.filterNotNull() ?: emptyList()
    }.getOrElse { emptyList() }

    /** Busca livre (lupa). Retorna sugestões com nome+ticker. */
    suspend fun buscarTickers(query: String): List<YahooSearchQuote> = runCatching {
        YahooRetrofit.servico.buscar(query).quotes
            ?.filter { it.symbol != null && (it.typeDisp == "Equity" || it.typeDisp == "ETF" || it.typeDisp == null) }
            ?: emptyList()
    }.getOrElse { emptyList() }

    suspend fun noticias(query: String): List<GNewsArtigo> {
        val key = BuildConfig.GNEWS_API_KEY
        if (key.isBlank()) return emptyList()
        // Tenta múltiplas variantes da query pra maximizar resultados
        val tentativas = listOf(query, query.split(" ").firstOrNull() ?: query)
            .filter { it.isNotBlank() }
            .distinct()
        for (q in tentativas) {
            val r = runCatching {
                GNewsRetrofit.servico.buscar(query = q, apikey = key).articles ?: emptyList()
            }.getOrElse { emptyList() }
            if (r.isNotEmpty()) return r
        }
        return emptyList()
    }

    suspend fun taxasBcb(): TaxasBcb {
        suspend fun ultimo(codigo: Int): Double? = runCatching {
            BcbRetrofit.servico.ultimos(codigo, 1).firstOrNull()?.valor?.replace(",", ".")?.toDouble()
        }.getOrNull()
        // Selic over diária(11) → anualizar; CDI(12) idem. Para simplicidade usa Selic meta(432) anual.
        val selicMeta = ultimo(432)
        val cdiDiario = ultimo(12)
        val cdiAnual = cdiDiario?.let { ((1 + it / 100).pow(252) - 1) * 100 }
        // IPCA acumulado 12m: série 13522
        val ipca = ultimo(13522)
        return TaxasBcb(selic = selicMeta, cdi = cdiAnual, ipca12m = ipca)
    }

    private fun Double.pow(exp: Int): Double {
        var r = 1.0; repeat(exp) { r *= this }; return r
    }

    /** Logo via Google Favicon API + mapa expandido. Null = fallback ícone material. */
    fun logoUrl(ticker: String): String? {
        val dominio = LOGOS[ticker.uppercase()] ?: return null
        return "https://www.google.com/s2/favicons?domain=$dominio&sz=128"
    }

    private val LOGOS = mapOf(
        // BR — ações
        "PETR4" to "petrobras.com.br",
        "PETR3" to "petrobras.com.br",
        "VALE3" to "vale.com",
        "ITUB4" to "itau.com.br",
        "ITUB3" to "itau.com.br",
        "BBDC4" to "bradesco.com.br",
        "BBDC3" to "bradesco.com.br",
        "BBAS3" to "bb.com.br",
        "ABEV3" to "ambev.com.br",
        "WEGE3" to "weg.net",
        "MGLU3" to "magazineluiza.com.br",
        "LREN3" to "lojasrenner.com.br",
        "SUZB3" to "suzano.com.br",
        "RENT3" to "localiza.com",
        "JBSS3" to "jbs.com.br",
        "B3SA3" to "b3.com.br",
        "GGBR4" to "gerdau.com.br",
        "EMBR3" to "embraer.com",
        "RAIL3" to "rumolog.com",
        "ELET3" to "eletrobras.com",
        "ELET6" to "eletrobras.com",
        "PRIO3" to "prio3.com.br",
        "VBBR3" to "vibraenergia.com.br",
        "CSNA3" to "csn.com.br",
        "TIMS3" to "tim.com.br",
        "VIVT3" to "telefonica.com.br",
        "RDOR3" to "rededorsaoluiz.com.br",
        // FIIs (sites próprios)
        "MXRF11" to "maxiriofii.com.br",
        "HGLG11" to "csahg.com.br",
        "KNRI11" to "kineabr.com.br",
        "XPML11" to "xpasset.com.br",
        "BCFF11" to "btgpactual.com",
        "VISC11" to "vinci.com.br",
        // US
        "AAPL" to "apple.com",
        "MSFT" to "microsoft.com",
        "GOOGL" to "google.com",
        "GOOG" to "google.com",
        "AMZN" to "amazon.com",
        "NVDA" to "nvidia.com",
        "TSLA" to "tesla.com",
        "META" to "meta.com",
        "NFLX" to "netflix.com",
        "AMD" to "amd.com",
        "INTC" to "intel.com",
        "JPM" to "jpmorganchase.com",
        "BAC" to "bankofamerica.com",
        "V" to "visa.com",
        "MA" to "mastercard.com",
        "DIS" to "disney.com",
        "KO" to "coca-cola.com",
        "PEP" to "pepsico.com",
        "MCD" to "mcdonalds.com",
        "NKE" to "nike.com",
        "PYPL" to "paypal.com",
        "UBER" to "uber.com",
        "BABA" to "alibaba.com",
        // Criptomoedas
        "BTC-BRL" to "bitcoin.org",
        "BTC-USD" to "bitcoin.org",
        "ETH-BRL" to "ethereum.org",
        "ETH-USD" to "ethereum.org",
        "SOL-BRL" to "solana.com",
        "SOL-USD" to "solana.com",
        "BNB-BRL" to "binance.com",
        "BNB-USD" to "binance.com",
        "ADA-BRL" to "cardano.org",
        "ADA-USD" to "cardano.org",
        "DOGE-BRL" to "dogecoin.com",
        "DOGE-USD" to "dogecoin.com",
    )
}
