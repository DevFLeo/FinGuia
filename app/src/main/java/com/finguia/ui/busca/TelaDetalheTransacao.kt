package com.finguia.ui.busca

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finguia.dados.TipoTransacao
import com.finguia.dados.TransacaoBancaria
import com.finguia.ui.formato.corDoSentido
import com.finguia.ui.formato.emReais
import com.finguia.ui.formato.prefixoDoSentido
import com.finguia.ui.formato.rotuloDoSentido
import com.finguia.ui.formato.sentido
import com.finguia.ui.theme.*
import com.finguia.ui.theme.TextoForte
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TelaDetalheTransacao(
    transacao: TransacaoBancaria,
    modifier: Modifier = Modifier,
    aoVoltar: () -> Unit = {}
) {
    val sentido = transacao.tipo.sentido
    val cor = corDoSentido(sentido)
    val prefixo = prefixoDoSentido(sentido)
    val dataCompleta = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm:ss", Locale("pt", "BR"))
        .format(Date(transacao.timestampMs))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = aoVoltar) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextoForte)
            }
            Text(
                "Detalhes da transação",
                color = TextoForte,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Card principal com valor ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(cor.copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            iconeParaTipo(transacao.tipo),
                            contentDescription = null,
                            tint = cor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "$prefixo${formatarValor(transacao.valor)}",
                        color = cor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = rotuloPorTipo(transacao.tipo),
                        color = GrayText,
                        fontSize = 13.sp
                    )

                    Surface(
                        color = cor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = rotuloDoSentido(sentido),
                            color = cor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ── Informações ───────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    LinhaDetalhe("Banco / App", transacao.banco)
                    Divider(color = DarkBg, thickness = 1.dp)
                    LinhaDetalhe("Descrição", transacao.descricao)
                    if (transacao.tituloNotificacao.isNotBlank()) {
                        Divider(color = DarkBg, thickness = 1.dp)
                        LinhaDetalhe("Notificação", transacao.tituloNotificacao)
                    }
                    if (transacao.textoNotificacao.isNotBlank() &&
                        transacao.textoNotificacao != transacao.tituloNotificacao) {
                        Divider(color = DarkBg, thickness = 1.dp)
                        LinhaDetalhe("Detalhe", transacao.textoNotificacao)
                    }
                    Divider(color = DarkBg, thickness = 1.dp)
                    LinhaDetalhe("Data e hora", dataCompleta)
                    if (transacao.recorrente) {
                        Divider(color = DarkBg, thickness = 1.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recorrente", color = GrayText, fontSize = 12.sp)
                            Surface(
                                color = GojoPurple.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "Sim",
                                    color = GojoPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LinhaDetalhe(rotulo: String, valor: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(rotulo, color = GrayText, fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text(valor, color = TextoForte, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private fun rotuloPorTipo(tipo: TipoTransacao): String = when (tipo) {
    TipoTransacao.PIX_RECEBIDO           -> "Pix recebido"
    TipoTransacao.PIX_ENVIADO            -> "Pix enviado"
    TipoTransacao.COMPRA_CREDITO         -> "Compra no crédito"
    TipoTransacao.COMPRA_DEBITO          -> "Compra no débito"
    TipoTransacao.BOLETO_PAGO            -> "Boleto pago"
    TipoTransacao.TRANSFERENCIA_RECEBIDA -> "Transferência recebida"
    TipoTransacao.TRANSFERENCIA_ENVIADA  -> "Transferência enviada"
    TipoTransacao.ESTORNO                -> "Estorno"
    TipoTransacao.SAQUE                  -> "Saque"
    TipoTransacao.DEPOSITO               -> "Depósito"
    TipoTransacao.COBRANCA               -> "Cobrança"
    TipoTransacao.DESCONHECIDO           -> "Transação"
}

private fun iconeParaTipo(tipo: TipoTransacao): ImageVector = when (tipo) {
    TipoTransacao.PIX_RECEBIDO           -> Icons.Default.CallReceived
    TipoTransacao.PIX_ENVIADO            -> Icons.Default.CallMade
    TipoTransacao.COMPRA_CREDITO         -> Icons.Default.CreditCard
    TipoTransacao.COMPRA_DEBITO          -> Icons.Default.ShoppingCart
    TipoTransacao.BOLETO_PAGO            -> Icons.Default.Receipt
    TipoTransacao.TRANSFERENCIA_RECEBIDA -> Icons.Default.MoveToInbox
    TipoTransacao.TRANSFERENCIA_ENVIADA  -> Icons.Default.Outbox
    TipoTransacao.ESTORNO                -> Icons.Default.Undo
    TipoTransacao.SAQUE                  -> Icons.Default.LocalAtm
    TipoTransacao.DEPOSITO               -> Icons.Default.Savings
    TipoTransacao.COBRANCA               -> Icons.Default.Warning
    TipoTransacao.DESCONHECIDO           -> Icons.Default.AccountBalance
}

private fun formatarValor(valor: Double): String =
    valor.emReais()
