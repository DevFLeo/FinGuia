package com.finguia.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransacaoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(transacao: TransacaoBancaria): Long

    @Query("SELECT * FROM transacoes_bancarias ORDER BY timestampMs DESC")
    fun listarTodas(): Flow<List<TransacaoBancaria>>

    @Query("SELECT * FROM transacoes_bancarias WHERE tipo IN ('PIX_RECEBIDO','TRANSFERENCIA_RECEBIDA','DEPOSITO','ESTORNO') ORDER BY timestampMs DESC")
    fun listarReceitas(): Flow<List<TransacaoBancaria>>

    @Query("SELECT * FROM transacoes_bancarias WHERE tipo IN ('PIX_ENVIADO','COMPRA_DEBITO','COMPRA_CREDITO','BOLETO_PAGO','TRANSFERENCIA_ENVIADA','SAQUE') ORDER BY timestampMs DESC")
    fun listarDespesas(): Flow<List<TransacaoBancaria>>

    @Query("SELECT SUM(valor) FROM transacoes_bancarias WHERE tipo IN ('PIX_RECEBIDO','TRANSFERENCIA_RECEBIDA','DEPOSITO','ESTORNO')")
    fun totalReceitas(): Flow<Double?>

    @Query("SELECT SUM(valor) FROM transacoes_bancarias WHERE tipo IN ('PIX_ENVIADO','COMPRA_DEBITO','COMPRA_CREDITO','BOLETO_PAGO','TRANSFERENCIA_ENVIADA','SAQUE')")
    fun totalDespesas(): Flow<Double?>

    // Busca apenas os lançamentos marcados como recorrentes pelo usuário
    @Query("SELECT * FROM transacoes_bancarias WHERE recorrente = 1 ORDER BY timestampMs DESC")
    fun listarRecorrentes(): Flow<List<TransacaoBancaria>>

    @Query("DELETE FROM transacoes_bancarias WHERE id = :id")
    suspend fun deletar(id: Long)

    @Query("DELETE FROM transacoes_bancarias")
    suspend fun deletarTodas()
}
