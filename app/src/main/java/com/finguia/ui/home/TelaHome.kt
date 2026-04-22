package com.finguia.ui.home

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finguia.ui.theme.CardBg
import com.finguia.ui.theme.DarkBg
import com.finguia.ui.theme.DebtRed
import com.finguia.ui.theme.GojoPurple
import com.finguia.ui.theme.GrayText
import com.finguia.ui.theme.MoneyGreen
import com.finguia.ui.transacoes.TransacaoViewModel

@Composable
fun telaHome(
    modifier: Modifier = Modifier,
    viewModel: TransacaoViewModel,
    ocultarSaldo: Boolean = false,
    aoClicarDashboard: () -> Unit = {},
    aoClicarLancar: () -> Unit = {},
    aoClicarExtrato: () -> Unit = {},
    aoClicarCalculadora: () -> Unit = {},
    aoClicarCriptos: () -> Unit = {},
    aoClicarTema: () -> Unit = {},
    aoClicarBusca: () -> Unit = {},
    aoClicarNoticias: () -> Unit = {},
    aoClicarPerfil: () -> Unit = {},
    aoClicarNotificacoes: () -> Unit = {}
) {
    // Dados reais vindos do banco SQLite via ViewModel
    val totalReceitas by viewModel.totalReceitas.collectAsState()
    val totalDespesas by viewModel.totalDespesas.collectAsState()
    val saldoTotal = totalReceitas - totalDespesas

    // Proporção de gastos em relação às receitas (usado no gráfico de rosca)
    val proporcaoGastos = if (totalReceitas > 0.0) {
        (totalDespesas / totalReceitas).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        // ── Ícones superiores ────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 26.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BotaoIconeTopo(Icons.Default.Settings, aoClicar = aoClicarTema)
            BotaoIconeTopo(Icons.Default.Notifications, aoClicar = aoClicarNotificacoes)
            BotaoIconeTopo(Icons.Default.Search, aoClicar = aoClicarBusca)
            BotaoIconeTopo(Icons.Default.Person, aoClicar = aoClicarPerfil)
        }

        // ── Gráfico de rosca com saldo real ──────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            GraficoRosca(progresso = proporcaoGastos, saldoPositivo = saldoTotal >= 0)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SALDO TOTAL",
                    color = GrayText,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (ocultarSaldo) "R$ ••••••" else formatarMoeda(saldoTotal),
                    color = if (saldoTotal >= 0) Color.White else DebtRed,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (ocultarSaldo) "+••••••" else "+${formatarMoeda(totalReceitas)}",
                        color = MoneyGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = "·", color = GrayText, fontSize = 11.sp)
                    Text(
                        text = if (ocultarSaldo) "-••••••" else "-${formatarMoeda(totalDespesas)}",
                        color = DebtRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Botões de ação rápida ────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                BotaoAcao(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Default.AddCircle,
                    rotulo = "Lançar",
                    aoClicar = aoClicarLancar
                )
                BotaoAcao(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Default.Edit,
                    rotulo = "Extrato",
                    aoClicar = aoClicarExtrato
                )
                BotaoAcao(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Default.Calculate,
                    rotulo = "Calculadora",
                    aoClicar = aoClicarCalculadora
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                BotaoAcaoRes(
                    modifier = Modifier.weight(1f),
                    iconeRes = com.finguia.R.drawable.ic_cripto,
                    rotulo = "Criptos",
                    aoClicar = aoClicarCriptos
                )
                BotaoAcao(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Default.PieChart,
                    rotulo = "Investimentos",
                    aoClicar = aoClicarDashboard
                )
                BotaoAcao(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Default.Newspaper,
                    rotulo = "Notícias",
                    aoClicar = aoClicarNoticias
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Painel financeiro embutido ───────────────────────
        Text(
            text = "PAINEL FINANCEIRO",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "Visão geral das suas finanças",
            color = GrayText,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))
        SecoesPainel(viewModel = viewModel)

        Spacer(Modifier.height(80.dp))
    }
}

// ─────────────────────────────────────────────
// COMPONENTES
// ─────────────────────────────────────────────

@Composable
fun BotaoIconeTopo(
    icone: ImageVector,
    aoClicar: () -> Unit = {}
) {
    Surface(
        color = CardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .size(45.dp)
            .clickable(onClick = aoClicar)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icone, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun BotaoAcao(
    modifier: Modifier,
    icone: ImageVector,
    rotulo: String,
    aoClicar: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = aoClicar),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icone, contentDescription = null, tint = GojoPurple)
            Spacer(Modifier.height(8.dp))
            Text(rotulo, color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
fun BotaoAcaoRes(
    modifier: Modifier,
    iconeRes: Int,
    rotulo: String,
    aoClicar: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = aoClicar),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(painterResource(iconeRes), contentDescription = null, tint = GojoPurple)
            Spacer(Modifier.height(8.dp))
            Text(rotulo, color = Color.White, fontSize = 11.sp)
        }
    }
}

// Easing suave tipo "overshoot" para entrada do arco
private val EaseOutBack = Easing { t ->
    val c1 = 1.70158f
    val c3 = c1 + 1f
    (1 + c3 * (t - 1).let { it * it * it } + c1 * (t - 1).let { it * it }).coerceIn(0f, 1f)
}

@Composable
fun GraficoRosca(progresso: Float, saldoPositivo: Boolean = true) {
    val progressoAnimado by animateFloatAsState(
        targetValue = progresso,
        animationSpec = tween(durationMillis = 1200, easing = EaseOutBack),
        label = "grafico_rosca"
    )

    // Cor do arco: verde se saldo positivo e gastos < 80%, vermelho se negativo ou acima de 80%
    val corArco = when {
        !saldoPositivo || progresso >= 0.8f -> DebtRed
        progresso >= 0.6f -> Color(0xFFFFB300)
        else -> MoneyGreen
    }

    // Glow: cor semitransparente mais larga atrás do arco principal
    val corGlow = corArco.copy(alpha = 0.25f)

    Canvas(modifier = Modifier.size(200.dp)) {
        val strokePx = 15.dp.toPx()
        val glowPx = 28.dp.toPx()

        // Trilha de fundo
        drawArc(
            color = Color(0xFF1E1E1E),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        if (progressoAnimado > 0f) {
            // Camada de glow (brilho difuso)
            drawArc(
                color = corGlow,
                startAngle = -90f,
                sweepAngle = 360f * progressoAnimado,
                useCenter = false,
                style = Stroke(width = glowPx, cap = StrokeCap.Round)
            )
            // Arco principal com cor dinâmica
            drawArc(
                color = corArco,
                startAngle = -90f,
                sweepAngle = 360f * progressoAnimado,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
    }
}

// Formata Double para o padrão monetário brasileiro: R$ 1.500,00
private fun formatarMoeda(valor: Double): String =
    "R$ %,.2f".format(valor).replace(",", "X").replace(".", ",").replace("X", ".")
