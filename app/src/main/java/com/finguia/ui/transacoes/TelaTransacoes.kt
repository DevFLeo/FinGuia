package com.finguia.ui.transacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finguia.dados.TransacaoBancaria
import com.finguia.dados.TipoTransacao
import com.finguia.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TelaTransacoes(viewModel: TransacaoViewModel = viewModel()) {
    val transacoes by viewModel.transacoes.collectAsState()
    val totalReceitas by viewModel.totalReceitas.collectAsState()
    val totalDespesas by viewModel.totalDespesas.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Extrato Bancário",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Capturado automaticamente via notificações",
            color = GrayText,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(16.dp))

        // Cards de resumo receitas/despesas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResumoCard(
                titulo = "Entradas",
                valor = totalReceitas,
                cor = MoneyGreen,
                icone = Icons.Default.TrendingUp,
                modifier = Modifier.weight(1f)
            )
            ResumoCard(
                titulo = "Saídas",
                valor = totalDespesas,
                cor = DebtRed,
                icone = Icons.Default.TrendingDown,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        if (transacoes.isEmpty()) {
            EstadoVazio()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transacoes, key = { it.id }) { transacao ->
                    CartaoTransacao(
                        transacao = transacao,
                        onDeletar = { viewModel.deletar(transacao.id) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun ResumoCard(
    titulo: String,
    valor: Double,
    cor: Color,
    icone: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(24.dp))
            Column {
                Text(titulo, color = GrayText, fontSize = 11.sp)
                Text(
                    text = formatarValor(valor),
                    color = cor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CartaoTransacao(
    transacao: TransacaoBancaria,
    onDeletar: () -> Unit
) {
    val ehEntrada = transacao.tipo in listOf(
        TipoTransacao.PIX_RECEBIDO,
        TipoTransacao.TRANSFERENCIA_RECEBIDA,
        TipoTransacao.DEPOSITO,
        TipoTransacao.ESTORNO
    )
    val corValor = if (ehEntrada) MoneyGreen else DebtRed
    val prefixoValor = if (ehEntrada) "+" else "-"
    val dataFormatada = SimpleDateFormat("dd/MM/yy HH:mm", Locale("pt", "BR"))
        .format(Date(transacao.timestampMs))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone do tipo de transação
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(corValor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconeParaTipo(transacao.tipo),
                    contentDescription = null,
                    tint = corValor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transacao.banco,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = transacao.descricao,
                    color = GrayText,
                    fontSize = 12.sp,
                    maxLines = 2
                )
                Text(
                    text = dataFormatada,
                    color = GrayText.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefixoValor${formatarValor(transacao.valor)}",
                    color = corValor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDeletar,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Remover",
                        tint = GrayText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EstadoVazio() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.NotificationsNone,
            contentDescription = null,
            tint = GrayText,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "Nenhuma transação capturada ainda",
            color = GrayText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "As notificações dos seus bancos serão\ncapturadas automaticamente aqui.",
            color = GrayText.copy(alpha = 0.6f),
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
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
    "R$ %,.2f".format(valor).replace(",", "X").replace(".", ",").replace("X", ".")
