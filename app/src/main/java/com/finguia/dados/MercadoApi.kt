package com.finguia.dados

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ============================ HTTP client com User-Agent (Yahoo bloqueia default) ============================

private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 FinGuia/1.0")
                .header("Accept", "application/json,text/plain,*/*")
                .build()
            chain.proceed(req)
        }
        .build()
}

private fun buildRetrofit(baseUrl: String): Retrofit = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(httpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

// ============================ YAHOO Finance (BR via .SA + US) ============================

data class YahooMeta(
    val currency: String?,
    val symbol: String?,
    val exchangeName: String?,
    val instrumentType: String?,
    val regularMarketPrice: Double?,
    val previousClose: Double?,
    val regularMarketDayHigh: Double?,
    val regularMarketDayLow: Double?,
    val regularMarketVolume: Long?,
    val fiftyTwoWeekHigh: Double?,
    val fiftyTwoWeekLow: Double?,
    val chartPreviousClose: Double?,
    val longName: String?,
    val shortName: String?
)
data class YahooQuote(
    val close: List<Double?>?,
    val open: List<Double?>?
)
data class YahooIndicators(val quote: List<YahooQuote>?)
data class YahooResult(
    val meta: YahooMeta?,
    val timestamp: List<Long>?,
    val indicators: YahooIndicators?
)
data class YahooChart(val result: List<YahooResult>?, val error: Any?)
data class YahooResposta(val chart: YahooChart?)

// Search/quote summary p/ pegar nome longo
data class YahooSearchQuote(
    val symbol: String?,
    val shortname: String?,
    val longname: String?,
    val typeDisp: String?,
    val exchange: String?
)
data class YahooSearchResp(val quotes: List<YahooSearchQuote>?)

interface YahooServico {
    @GET("v8/finance/chart/{ticker}")
    suspend fun cotacao(
        @Path("ticker") ticker: String,
        @Query("range") range: String = "1d",
        @Query("interval") interval: String = "15m"
    ): YahooResposta

    @GET("v1/finance/search")
    suspend fun buscar(
        @Query("q") query: String,
        @Query("quotesCount") qty: Int = 10,
        @Query("newsCount") news: Int = 0
    ): YahooSearchResp
}

object YahooRetrofit {
    val servico: YahooServico by lazy {
        buildRetrofit("https://query1.finance.yahoo.com/").create(YahooServico::class.java)
    }
}

// ============================ BCB (Banco Central) — Selic / CDI / IPCA ============================

data class BcbSerie(val data: String, val valor: String)

interface BcbServico {
    // Códigos: Selic meta=432, CDI=12, IPCA=433, Selic over=11
    @GET("dados/serie/bcdata.sgs.{codigo}/dados/ultimos/{n}")
    suspend fun ultimos(
        @Path("codigo") codigo: Int,
        @Path("n") n: Int = 1,
        @Query("formato") formato: String = "json"
    ): List<BcbSerie>
}

object BcbRetrofit {
    val servico: BcbServico by lazy {
        buildRetrofit("https://api.bcb.gov.br/").create(BcbServico::class.java)
    }
}

// ============================ GNews ============================

data class GNewsArtigo(
    val title: String?,
    val description: String?,
    val url: String?,
    val image: String?,
    val publishedAt: String?,
    val source: GNewsFonte?
)
data class GNewsFonte(val name: String?, val url: String?)
data class GNewsResposta(val totalArticles: Int?, val articles: List<GNewsArtigo>?)

interface GNewsServico {
    @GET("api/v4/search")
    suspend fun buscar(
        @Query("q") query: String,
        @Query("lang") lang: String = "pt",
        @Query("max") max: Int = 10,
        @Query("apikey") apikey: String
    ): GNewsResposta
}

object GNewsRetrofit {
    val servico: GNewsServico by lazy {
        buildRetrofit("https://gnews.io/").create(GNewsServico::class.java)
    }
}
