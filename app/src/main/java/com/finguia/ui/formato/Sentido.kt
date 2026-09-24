package com.finguia.ui.formato

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.finguia.dados.TipoTransacao
import com.finguia.motor.SentidoTransacao
import com.finguia.ui.theme.DebtRed
import com.finguia.ui.theme.GrayText
import com.finguia.ui.theme.MoneyGreen

// Como exibir cada sentido de transacao. A regra de qual tipo e entrada,
// saida ou neutro fica em SentidoTransacao (motor, Java).

val TipoTransacao.sentido: SentidoTransacao get() = SentidoTransacao.de(this)

/** Verde para entrada, vermelho para saida, cinza para avisos. */
@Composable
@ReadOnlyComposable
fun corDoSentido(sentido: SentidoTransacao): Color = when (sentido) {
    SentidoTransacao.ENTRADA -> MoneyGreen
    SentidoTransacao.SAIDA -> DebtRed
    SentidoTransacao.NEUTRO -> GrayText
}

/** "+" ou "-" antes do valor; avisos nao levam sinal. */
fun prefixoDoSentido(sentido: SentidoTransacao): String = when (sentido) {
    SentidoTransacao.ENTRADA -> "+"
    SentidoTransacao.SAIDA -> "-"
    SentidoTransacao.NEUTRO -> ""
}

/** Rotulo curto: ENTRADA, SAÍDA ou AVISO. */
fun rotuloDoSentido(sentido: SentidoTransacao): String = when (sentido) {
    SentidoTransacao.ENTRADA -> "ENTRADA"
    SentidoTransacao.SAIDA -> "SAÍDA"
    SentidoTransacao.NEUTRO -> "AVISO"
}
