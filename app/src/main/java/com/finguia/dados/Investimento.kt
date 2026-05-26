package com.finguia.dados

import android.content.Context
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Categorias de investimento sugeridas. Usuário pode criar com nome livre,
 * mas as telas agrupam por uma destas.
 */
enum class CategoriaInvestimento {
    ACOES_BR,         // ações brasileiras
    ACOES_INTER,      // ações internacionais
    IMOVEIS,          // FIIs, imóveis físicos
    RENDA_FIXA,       // CDB, Tesouro, LCI/LCA
    CRIPTO,           // cotação automática via Yahoo Finance integrado
    OUTROS
}

@Entity(tableName = "investimentos")
data class Investimento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val categoria: String, // CategoriaInvestimento.name
    val valorInvestido: Double,
    val rentabilidadePct: Double, // rentabilidade acumulada estimada
    val dataCompraMs: Long = System.currentTimeMillis(),
    val observacao: String = "",
    /** Símbolo de mercado (PETR4, AAPL, MXRF11). Vazio para renda fixa/sem cotação. */
    val ticker: String = "",
    /** Preço unitário no momento da compra (na moeda do ativo). Null = sem cotação. */
    val precoEntrada: Double? = null,
    /** Quantidade de unidades adquiridas. Null = sem cotação. */
    val quantidade: Double? = null
) {
    /** Valor calculado pela rentabilidadePct (modo renda fixa / fallback). */
    val valorAtual: Double get() = valorInvestido * (1 + rentabilidadePct / 100.0)
    val lucro: Double get() = valorAtual - valorInvestido

    /** Quando há cotação live, recalcula valor atual em BRL multiplicando preço x qty x câmbio. */
    fun valorAtualComCotacao(precoAtual: Double, cambio: Double = 1.0): Double {
        val qty = quantidade ?: return valorAtual
        return precoAtual * qty * cambio
    }
}

@Dao
interface InvestimentoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(investimento: Investimento): Long

    @Update
    suspend fun atualizar(investimento: Investimento)

    @Query("DELETE FROM investimentos WHERE id = :id")
    suspend fun deletar(id: Long)

    @Query("SELECT * FROM investimentos ORDER BY dataCompraMs DESC")
    fun listarTodos(): Flow<List<Investimento>>

    @Query("SELECT SUM(valorInvestido) FROM investimentos")
    fun totalInvestido(): Flow<Double?>
}

class InvestimentoRepository(context: Context) {
    private val dao = FinGuiaDatabase.obterInstancia(context).investimentoDao()

    fun listarTodos(): Flow<List<Investimento>> = dao.listarTodos()
    fun totalInvestido(): Flow<Double?> = dao.totalInvestido()
    suspend fun salvar(investimento: Investimento) = dao.inserir(investimento)
    suspend fun atualizar(investimento: Investimento) = dao.atualizar(investimento)
    suspend fun deletar(id: Long) = dao.deletar(id)
}
