package com.finguia.dados

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TipoTransacao {
    PIX_RECEBIDO,
    PIX_ENVIADO,
    COMPRA_DEBITO,
    COMPRA_CREDITO,
    BOLETO_PAGO,
    TRANSFERENCIA_RECEBIDA,
    TRANSFERENCIA_ENVIADA,
    COBRANCA,
    ESTORNO,
    SAQUE,
    DEPOSITO,
    DESCONHECIDO
}

@Entity(tableName = "transacoes_bancarias")
data class TransacaoBancaria(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val banco: String,
    val pacoteApp: String,
    val tipo: TipoTransacao,
    val valor: Double,
    val descricao: String,
    val tituloNotificacao: String,
    val textoNotificacao: String,
    val timestampMs: Long = System.currentTimeMillis()
)
