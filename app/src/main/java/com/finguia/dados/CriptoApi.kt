package com.finguia.dados

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class Cripto(
    val id: String,
    val symbol: String,
    val name: String,
    val current_price: Double,
    val price_change_percentage_24h: Double?,
    val image: String,
    val market_cap_rank: Int?
)

data class TrendingItem(val item: TrendingCoinItem)
data class TrendingCoinItem(
    val id: String,
    val name: String,
    val symbol: String,
    val thumb: String,
    val market_cap_rank: Int?
)
data class TrendingResponse(val coins: List<TrendingItem>)

interface CriptoApiServico {
    @GET("coins/markets")
    suspend fun buscarCriptos(
        @Query("vs_currency") moeda: String = "brl",
        @Query("order") ordem: String = "market_cap_desc",
        @Query("per_page") porPagina: Int = 50,
        @Query("page") pagina: Int = 1,
        @Query("sparkline") sparkline: Boolean = false
    ): List<Cripto>

    @GET("coins/markets")
    suspend fun buscarPorIds(
        @Query("vs_currency") moeda: String = "brl",
        @Query("ids") ids: String,
        @Query("sparkline") sparkline: Boolean = false
    ): List<Cripto>

    @GET("search/trending")
    suspend fun buscarTrending(): TrendingResponse
}

object CriptoRetrofit {
    val servico: CriptoApiServico by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CriptoApiServico::class.java)
    }
}
