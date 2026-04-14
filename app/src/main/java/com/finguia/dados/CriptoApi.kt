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
    val image: String
)

interface CriptoApiServico {
    @GET("coins/markets")
    suspend fun buscarCriptos(
        @Query("vs_currency") moeda: String = "brl",
        @Query("order") ordem: String = "market_cap_desc",
        @Query("per_page") porPagina: Int = 10,
        @Query("page") pagina: Int = 1,
        @Query("sparkline") sparkline: Boolean = false
    ): List<Cripto>
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
