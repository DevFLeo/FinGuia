package com.finguia.ui.notificacoes

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.finguia.dados.TipoTransacao
import com.finguia.dados.TransacaoBancaria
import com.finguia.service.NotificationListenerHelper
import com.finguia.ui.theme.CardBg
import com.finguia.ui.theme.DarkBg
import com.finguia.ui.theme.DebtRed
import com.finguia.ui.theme.GojoPurple
import com.finguia.ui.theme.GrayText
import com.finguia.ui.theme.MoneyGreen
import com.finguia.ui.transacoes.TransacaoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Alerta(
    val titulo: String,
    val descricao: String,
    val icone: ImageVector,
    val cor: Color
)

@Composable
fun TelaNotificacoes(
    modifier: Modifier = Modifier,
    viewModel: TransacaoViewModel,
    aoVoltar: () -> Unit = {}
) {
    val transacoes by viewModel.transacoes.collectAsState()
    val totalReceitas by viewModel.totalReceitas.collectAsState()
    val totalDespesas by viewModel.totalDespesas.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissaoAtiva by remember { mutableStateOf(NotificationListenerHelper.listenerAtivo(context)) }

    // Reavalia o status quando o usuário volta das configurações. Se o estado
    // passou de inativo para ativo, força um rebind do listener — o Android
    // às vezes não reconecta sozinho na primeira concessão.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val agora = NotificationListenerHelper.listenerAtivo(context)
                if (agora && !permissaoAtiva) {
                    NotificationListenerHelper.reconectarListener(context)
                }
                permissaoAtiva = agora
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val alertas = gerarAlertas(totalReceitas, totalDespesas, transacoes)
    val capturas = transacoes.sortedByDescending { it.timestampMs }.take(20)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = aoVoltar) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Text("Notificações", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                CardStatusPermissao(
                    ativa = permissaoAtiva,
                    aoAtivar = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                )
            }

            if (alertas.isNotEmpty()) {
                item {
                    Text("ALERTAS", color = GojoPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                items(alertas) { a ->
                    CardAlerta(a)
                }
                item { Spacer(Modifier.height(10.dp)) }
            }

            item {
                Text("CAPTURADAS DOS BANCOS", color = GojoPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            if (capturas.isEmpty()) {
                item {
                    Text(
                        "Nenhuma notificação bancária capturada ainda. Verifique a permissão de leitura de notificações nas configurações do Android.",
                        color = GrayText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(capturas) { t ->
                    CardCapturaTransacao(t)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun CardAlerta(alerta: Alerta) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = alerta.cor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, alerta.cor.copy(alpha = 0.4f))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(alerta.icone, contentDescription = null, tint = alerta.cor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(alerta.titulo, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(alerta.descricao, color = GrayText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CardCapturaTransacao(t: TransacaoBancaria) {
    val fmtData = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))
    val ehEntrada = t.tipo in listOf(
        TipoTransacao.PIX_RECEBIDO,
        TipoTransacao.TRANSFERENCIA_RECEBIDA,
        TipoTransacao.DEPOSITO,
        TipoTransacao.ESTORNO
    )
    val cor = if (ehEntrada) MoneyGreen else DebtRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MoneyGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    t.banco.ifBlank { "Banco" },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    t.descricao.ifBlank { t.tipo.name },
                    color = GrayText,
                    fontSize = 11.sp,
                    maxLines = 2
                )
                Text(
                    fmtData.format(Date(t.timestampMs)),
                    color = GrayText.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
            Text(
                text = (if (ehEntrada) "+" else "-") + "R$ %,.2f".format(t.valor),
                color = cor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CardStatusPermissao(ativa: Boolean, aoAtivar: () -> Unit) {
    val cor = if (ativa) MoneyGreen else DebtRed
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, cor.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (ativa) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = cor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (ativa) "Captura ativa" else "Captura desativada",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (ativa)
                    "O FinGuia está lendo as notificações dos seus bancos."
                else
                    "Ative o acesso a notificações do FinGuia para capturar transações automaticamente.",
                color = GrayText,
                fontSize = 12.sp
            )
            if (!ativa) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = aoAtivar,
                    colors = ButtonDefaults.buttonColors(containerColor = GojoPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ativar agora", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun gerarAlertas(
    totalReceitas: Double,
    totalDespesas: Double,
    transacoes: List<TransacaoBancaria>
): List<Alerta> {
    val alertas = mutableListOf<Alerta>()

    // Alerta 1: gastos acima de 90% das receitas
    if (totalReceitas > 0) {
        val pct = totalDespesas / totalReceitas
        when {
            pct >= 1.0 -> alertas += Alerta(
                titulo = "Gastos ultrapassaram receitas",
                descricao = "Você gastou ${"%.0f".format(pct * 100)}% das suas entradas. Revise despesas.",
                icone = Icons.Default.Warning,
                cor = DebtRed
            )
            pct >= 0.9 -> alertas += Alerta(
                titulo = "Atenção: ${"%.0f".format(pct * 100)}% comprometidos",
                descricao = "Gastos próximos do total de receitas.",
                icone = Icons.Default.Warning,
                cor = Color(0xFFFFB300)
            )
            pct < 0.5 -> alertas += Alerta(
                titulo = "Saúde financeira em dia",
                descricao = "Apenas ${"%.0f".format(pct * 100)}% das receitas gastas.",
                icone = Icons.Default.CheckCircle,
                cor = MoneyGreen
            )
        }
    }

    // Alerta 2: gasto único alto nas últimas 24h
    val agora = System.currentTimeMillis()
    val umDia = 24 * 60 * 60 * 1000L
    val recentes = transacoes.filter { agora - it.timestampMs < umDia }
    val maiorGasto = recentes
        .filter { it.tipo !in listOf(
            TipoTransacao.PIX_RECEBIDO,
            TipoTransacao.TRANSFERENCIA_RECEBIDA,
            TipoTransacao.DEPOSITO,
            TipoTransacao.ESTORNO
        )}
        .maxByOrNull { it.valor }
    if (maiorGasto != null && maiorGasto.valor > 500) {
        alertas += Alerta(
            titulo = "Gasto alto nas últimas 24h",
            descricao = "R$ %,.2f em %s".format(maiorGasto.valor, maiorGasto.descricao.take(40).ifBlank { "transação" }),
            icone = Icons.Default.Info,
            cor = GojoPurple
        )
    }

    // Alerta 3: sem movimentação recente
    if (transacoes.isEmpty()) {
        alertas += Alerta(
            titulo = "Nenhuma transação capturada",
            descricao = "Ative a permissão de leitura de notificações do Android para capturar automaticamente.",
            icone = Icons.Default.Info,
            cor = GojoPurple
        )
    }

    return alertas
}
