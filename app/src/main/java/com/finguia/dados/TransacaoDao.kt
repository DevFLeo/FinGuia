package com.finguia.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransacaoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(transacao: TransacaoBancaria): Long

    @Query("SELECT * FROM transacoes_bancarias WHERE efetivado = 1 ORDER BY timestampMs DESC")
    fun listarTodas(): Flow<List<TransacaoBancaria>>

    // Inclui agendadas (não efetivadas) para a tela de lançamentos futuros
    @Query("SELECT * FROM transacoes_bancarias WHERE efetivado = 0 ORDER BY dataAgendada ASC")
    fun listarAgendadas(): Flow<List<TransacaoBancaria>>

    @Query("SELECT * FROM transacoes_bancarias WHERE efetivado = 1 AND tipo IN ('PIX_RECEBIDO','TRANSFERENCIA_RECEBIDA','DEPOSITO','ESTORNO') ORDER BY timestampMs DESC")
    fun listarReceitas(): Flow<List<TransacaoBancaria>>

    @Query("SELECT * FROM transacoes_bancarias WHERE efetivado = 1 AND tipo IN ('PIX_ENVIADO','COMPRA_DEBITO','COMPRA_CREDITO','BOLETO_PAGO','TRANSFERENCIA_ENVIADA','SAQUE') ORDER BY timestampMs DESC")
    fun listarDespesas(): Flow<List<TransacaoBancaria>>

    @Query("SELECT SUM(valor) FROM transacoes_bancarias WHERE efetivado = 1 AND tipo IN ('PIX_RECEBIDO','TRANSFERENCIA_RECEBIDA','DEPOSITO','ESTORNO')")
    fun totalReceitas(): Flow<Double?>

    @Query("SELECT SUM(valor) FROM transacoes_bancarias WHERE efetivado = 1 AND tipo IN ('PIX_ENVIADO','COMPRA_DEBITO','COMPRA_CREDITO','BOLETO_PAGO','TRANSFERENCIA_ENVIADA','SAQUE')")
    fun totalDespesas(): Flow<Double?>

    // Busca apenas os lançamentos marcados como recorrentes pelo usuário
    @Query("SELECT * FROM transacoes_bancarias WHERE recorrente = 1 ORDER BY timestampMs DESC")
    fun listarRecorrentes(): Flow<List<TransacaoBancaria>>

    // Mesma notificacao ja gravada: mesmo app, mesmo texto e mesmo horario de postagem.
    // Pega re-entregas (ex.: ao reconectar o listener) sem juntar dois Pix iguais
    // recebidos em momentos diferentes.
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM transacoes_bancarias
            WHERE pacoteApp = :pacote
              AND tituloNotificacao = :titulo
              AND textoNotificacao = :texto
              AND timestampMs = :postadaEmMs
        )
    """)
    suspend fun existeCaptura(pacote: String, titulo: String, texto: String, postadaEmMs: Long): Boolean

    @Update
    suspend fun atualizar(transacao: TransacaoBancaria)

    @Query("DELETE FROM transacoes_bancarias WHERE id = :id")
    suspend fun deletar(id: Long)

    @Query("DELETE FROM transacoes_bancarias")
    suspend fun deletarTodas()

    @Query("""
        SELECT * FROM transacoes_bancarias
        WHERE LOWER(descricao) LIKE LOWER(:query)
           OR LOWER(banco) LIKE LOWER(:query)
           OR LOWER(tituloNotificacao) LIKE LOWER(:query)
           OR LOWER(textoNotificacao) LIKE LOWER(:query)
        ORDER BY timestampMs DESC
        LIMIT 50
    """)
    suspend fun buscar(query: String): List<TransacaoBancaria>
}
