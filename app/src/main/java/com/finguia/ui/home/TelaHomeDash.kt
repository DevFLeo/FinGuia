package com.finguia.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finguia.dados.TipoTransacao
import com.finguia.dados.TransacaoBancaria
import com.finguia.ui.formato.emReais
import com.finguia.ui.theme.CardBg
import com.finguia.ui.theme.DarkBg
import com.finguia.ui.theme.DebtRed
import com.finguia.ui.theme.GojoPurple
import com.finguia.ui.theme.GrayText
import com.finguia.ui.theme.MoneyGreen
import com.finguia.ui.transacoes.TransacaoViewModel

@Composable
fun TelaHomeDash(
    modifier: Modifier = Modifier,
    viewModel: TransacaoViewModel
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "PAINEL FINANCEIRO",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "Visão geral das suas finanças",
            color = GrayText,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(20.dp))
        SecoesPainel(viewModel = viewModel)
        Spacer(Modifier.height(80.dp))
    }
}

/**
 * Blocos do painel reutilizáveis dentro da tela Início.
 * Mantém cards de saldo, entradas/saídas, gráfico por categoria,
 * fluxo de caixa e fluxo consolidado.
 */
@Composable
fun SecoesPainel(viewModel: TransacaoViewModel) {
    val totalReceitas by viewModel.totalReceitas.collectAsState()
    val totalDespesas by viewModel.totalDespesas.collectAsState()
    val todasTransacoes by viewModel.transacoes.collectAsState()
    val saldoTotal = totalReceitas - totalDespesas

    val proporcaoGastos = if (totalReceitas > 0.0) {
        (totalDespesas / totalReceitas).coerceIn(0.0, 1.0).toFloat()
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Saldo disponível", color = GrayText, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatarMoedaPainel(saldoTotal),
                color = if (saldoTotal >= 0) Color.White else DebtRed,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Comprometido: ${(proporcaoGastos * 100).toInt()}% das receitas",
                color = GrayText,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { proporcaoGastos },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    proporcaoGastos < 0.6f -> MoneyGreen
                    proporcaoGastos < 0.9f -> Color(0xFFFFB300)
                    else                   -> DebtRed
                },
                trackColor = Color(0xFF222222),
            )
        }
    }

    Spacer(Modifier.height(14.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CardResumo(
            modifier = Modifier.weight(1f),
            titulo = "Total Entradas",
            valor = totalReceitas,
            cor = MoneyGreen,
            icone = Icons.Default.TrendingUp
        )
        CardResumo(
            modifier = Modifier.weight(1f),
            titulo = "Total Saídas",
            valor = totalDespesas,
            cor = DebtRed,
            icone = Icons.Default.TrendingDown
        )
    }

    Spacer(Modifier.height(14.dp))

    if (todasTransacoes.isNotEmpty()) {
        val gastosPorCategoria = calcularGastosPorCategoria(todasTransacoes)

        if (gastosPorCategoria.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Distribuição de gastos",
                        color = GrayText,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    GraficoRosquinhaCategorias(gastos = gastosPorCategoria)
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Gastos por categoria",
                    color = GrayText,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(14.dp))
                val maiorGasto = gastosPorCategoria.values.maxOrNull() ?: 1.0
                gastosPorCategoria.forEach { (categoria, valor) ->
                    val proporcao = (valor / maiorGasto).toFloat()
                    BarraCategoria(
                        nome = categoria,
                        valor = valor,
                        proporcao = proporcao
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Fluxo de caixa", color = GrayText, fontSize = 12.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            GraficoFluxo(transacoes = todasTransacoes)
        }
    }

    Spacer(Modifier.height(14.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Fluxo consolidado",
                color = GrayText,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Receitas", color = GrayText, fontSize = 11.sp)
                    Text(
                        text = "+${formatarMoedaPainel(totalReceitas)}",
                        color = MoneyGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Despesas", color = GrayText, fontSize = 11.sp)
                    Text(
                        text = "-${formatarMoedaPainel(totalDespesas)}",
                        color = DebtRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENTES
// ─────────────────────────────────────────────

@Composable
private fun CardResumo(
    modifier: Modifier,
    titulo: String,
    valor: Double,
    cor: Color,
    icone: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(18.dp))
                Text(titulo, color = GrayText, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatarMoedaPainel(valor),
                color = cor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BarraCategoria(nome: String, valor: Double, proporcao: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(nome, color = Color.White, fontSize = 12.sp)
            Text(formatarMoedaPainel(valor), color = DebtRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { proporcao },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = DebtRed.copy(alpha = 0.7f + 0.3f * proporcao),
            trackColor = Color(0xFF222222),
        )
    }
}

private val CoresRosquinha = listOf(
    GojoPurple,
    Color(0xFFE91E63),
    Color(0xFFFFB300),
    Color(0xFF29B6F6),
    Color(0xFF66BB6A),
    Color(0xFFAB47BC),
)

@Composable
private fun GraficoRosquinhaCategorias(gastos: Map<String, Double>) {
    val total = gastos.values.sum().takeIf { it > 0 } ?: return
    val entradas = gastos.entries.toList()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 22.dp.toPx()
                var inicio = -90f
                entradas.forEachIndexed { index, (_, valor) ->
                    val varredura = ((valor / total) * 360.0).toFloat()
                    drawArc(
                        color = CoresRosquinha[index % CoresRosquinha.size],
                        startAngle = inicio,
                        sweepAngle = varredura,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                    )
                    inicio += varredura
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", color = GrayText, fontSize = 11.sp)
                Text(
                    text = formatarMoedaPainel(total),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entradas.forEachIndexed { index, (nome, valor) ->
                val percentual = (valor / total * 100).toInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                CoresRosquinha[index % CoresRosquinha.size],
                                RoundedCornerShape(3.dp)
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = nome,
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        text = "$percentual%",
                        color = GrayText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun GraficoFluxo(transacoes: List<TransacaoBancaria>) {
    val corLinha = GojoPurple

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (transacoes.size < 2) {
            drawLine(
                color = corLinha.copy(alpha = 0.3f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx()
            )
            return@Canvas
        }

        val ordenadas = transacoes.sortedBy { it.timestampMs }
        var acumulado = 0.0
        val pontos = ordenadas.map { t ->
            acumulado += if (t.ehEntrada()) t.valor else -t.valor
            acumulado
        }

        val minValor = pontos.minOrNull() ?: 0.0
        val maxValor = pontos.maxOrNull() ?: 1.0
        val intervalo = (maxValor - minValor).takeIf { it > 0 } ?: 1.0

        val larguraPasso = size.width / (pontos.size - 1)

        val path = Path()
        pontos.forEachIndexed { i, valor ->
            val x = i * larguraPasso
            val y = size.height - ((valor - minValor) / intervalo * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path = path, color = corLinha, style = Stroke(width = 2.5.dp.toPx()))

        val ultimoX = (pontos.size - 1) * larguraPasso
        val ultimoY = size.height - ((pontos.last() - minValor) / intervalo * size.height).toFloat()
        drawCircle(color = corLinha, radius = 5.dp.toPx(), center = Offset(ultimoX, ultimoY))
    }
}

// ─────────────────────────────────────────────
// UTILITÁRIOS
// ─────────────────────────────────────────────

private fun calcularGastosPorCategoria(transacoes: List<TransacaoBancaria>): Map<String, Double> {
    val nomesPorTipo = mapOf(
        TipoTransacao.COMPRA_DEBITO          to "Compras Débito",
        TipoTransacao.COMPRA_CREDITO         to "Compras Crédito",
        TipoTransacao.BOLETO_PAGO            to "Boletos",
        TipoTransacao.PIX_ENVIADO            to "Pix Enviado",
        TipoTransacao.TRANSFERENCIA_ENVIADA  to "Transferências",
        TipoTransacao.SAQUE                  to "Saques",
    )

    return transacoes
        .filter { it.tipo in nomesPorTipo.keys }
        .groupBy { nomesPorTipo[it.tipo] ?: it.tipo.name }
        .mapValues { (_, lista) -> lista.sumOf { it.valor } }
        .filter { it.value > 0 }
        .entries
        .sortedByDescending { it.value }
        .take(5)
        .associate { it.key to it.value }
}

private fun TransacaoBancaria.ehEntrada(): Boolean = tipo in listOf(
    TipoTransacao.PIX_RECEBIDO,
    TipoTransacao.TRANSFERENCIA_RECEBIDA,
    TipoTransacao.DEPOSITO,
    TipoTransacao.ESTORNO
)

private fun formatarMoedaPainel(valor: Double): String =
    valor.emReais()
