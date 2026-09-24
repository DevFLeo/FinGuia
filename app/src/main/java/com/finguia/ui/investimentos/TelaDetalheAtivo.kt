package com.finguia.ui.investimentos

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.finguia.dados.CotacaoAtivo
import com.finguia.dados.GNewsArtigo
import com.finguia.motor.NumeroBR
import com.finguia.ui.formato.emNumeroBR
import com.finguia.ui.theme.CardBg
import com.finguia.ui.theme.CardElevado
import com.finguia.ui.theme.DarkBg
import com.finguia.ui.theme.DebtRed
import com.finguia.ui.theme.DestaqueTopo
import com.finguia.ui.theme.GojoPurple
import com.finguia.ui.theme.GrayText
import com.finguia.ui.theme.MoneyGreen
import com.finguia.ui.theme.TextoForte
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

private val formatoBR = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
private val parseDataIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
private val formatoDataBr = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))

private fun formatarMoeda(valor: Double, moeda: String): String =
    if (moeda.equals("BRL", true)) formatoBR.format(valor) else NumeroBR.moeda(valor, NumeroBR.siglaMoeda(moeda))

@Composable
fun TelaDetalheAtivo(
    ticker: String,
    nomeAtivo: String,
    descricao: String,
    aoVoltar: () -> Unit,
    aoComprar: ((CotacaoAtivo) -> Unit)? = null,
    viewModel: InvestimentoViewModel
) {
    var cotacao by remember(ticker) { mutableStateOf<CotacaoAtivo?>(null) }
    var carregando by remember(ticker) { mutableStateOf(true) }
    var noticias by remember(ticker) { mutableStateOf<List<GNewsArtigo>>(emptyList()) }
    var rangeSelecionado by remember(ticker) { mutableStateOf("1d") }
    var grafico by remember(ticker) { mutableStateOf<List<Double>>(emptyList()) }
    var carregandoGrafico by remember(ticker) { mutableStateOf(true) }

    // Cotação inicial + refresh a cada 20s
    LaunchedEffect(ticker) {
        while (true) {
            carregando = cotacao == null
            val c = viewModel.buscarCotacao(ticker)
            if (c != null) {
                cotacao = c
                if (grafico.isEmpty()) grafico = c.historico
            }
            carregando = false
            delay(20_000)
        }
    }

    // Notícias (uma vez)
    LaunchedEffect(ticker, nomeAtivo) {
        noticias = viewModel.buscarNoticias("$nomeAtivo $ticker")
    }

    // Gráfico no range selecionado
    LaunchedEffect(ticker, rangeSelecionado) {
        carregandoGrafico = true
        val (range, interval) = when (rangeSelecionado) {
            "1d" -> "1d" to "15m"
            "5d" -> "5d" to "1h"
            "1mo" -> "1mo" to "1d"
            "1y" -> "1y" to "1wk"
            else -> "1d" to "15m"
        }
        val g = viewModel.buscarGrafico(ticker, range, interval)
        if (g.isNotEmpty()) grafico = g
        carregandoGrafico = false
    }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            // Header
            CabecalhoDetalhe(
                ticker = ticker,
                nome = nomeAtivo,
                cotacao = cotacao,
                carregando = carregando,
                aoVoltar = aoVoltar,
                aoRefresh = { viewModel.forcarRefresh() }
            )

            // Gráfico
            CardSecao(titulo = "Gráfico") {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("1d" to "1D", "5d" to "5D", "1mo" to "1M", "1y" to "1A").forEach { (k, lbl) ->
                        val ativo = rangeSelecionado == k
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (ativo) GojoPurple else CardElevado)
                                .clickable { rangeSelecionado = k }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text(lbl, color = TextoForte, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
                if (carregandoGrafico && grafico.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GojoPurple, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                } else {
                    GraficoLinha(grafico, cotacao?.moeda ?: "BRL",
                        Modifier.fillMaxWidth().height(180.dp))
                }
            }

            // Métricas
            cotacao?.let { c ->
                CardSecao(titulo = "Estatísticas") {
                    GridMetrica("Abertura", c.abertura?.let { formatarMoeda(it, c.moeda) } ?: "—",
                                "Fech. anterior", c.fechamentoAnterior?.let { formatarMoeda(it, c.moeda) } ?: "—")
                    GridMetrica("Máx. dia", c.maxDia?.let { formatarMoeda(it, c.moeda) } ?: "—",
                                "Mín. dia", c.minDia?.let { formatarMoeda(it, c.moeda) } ?: "—")
                    GridMetrica("Máx. 52 sem", c.max52sem?.let { formatarMoeda(it, c.moeda) } ?: "—",
                                "Mín. 52 sem", c.min52sem?.let { formatarMoeda(it, c.moeda) } ?: "—")
                    GridMetrica("Volume", c.volume?.let { "%,d".format(it) } ?: "—",
                                "Market Cap", c.marketCap?.let { formatarBilhoes(it, c.moeda) } ?: "—")
                }

                if (descricao.isNotBlank()) {
                    CardSecao(titulo = "Sobre") {
                        Text(descricao, color = GrayText, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }

                aoComprar?.let { fn ->
                    Button(
                        onClick = { fn(c) },
                        colors = ButtonDefaults.buttonColors(containerColor = GojoPurple),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("Adicionar à minha carteira", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }

            // Notícias
            CardSecao(titulo = "Notícias recentes") {
                when {
                    noticias.isEmpty() -> Text(
                        "Sem notícias agora. (Configure GNEWS_API_KEY em local.properties para habilitar.)",
                        color = GrayText, fontSize = 11.sp
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        noticias.forEach { ItemNoticia(it) }
                    }
                }
            }
        }
    }
}

private fun formatarBilhoes(v: Double, moeda: String): String {
    val sufixo = when {
        v >= 1e12 -> "T" to v / 1e12
        v >= 1e9 -> "B" to v / 1e9
        v >= 1e6 -> "M" to v / 1e6
        else -> "" to v
    }
    val sigla = NumeroBR.siglaMoeda(moeda) + " "
    return "$sigla${sufixo.second.emNumeroBR(2)}${sufixo.first}"
}

@Composable
private fun CabecalhoDetalhe(
    ticker: String,
    nome: String,
    cotacao: CotacaoAtivo?,
    carregando: Boolean,
    aoVoltar: () -> Unit,
    aoRefresh: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(DestaqueTopo, DarkBg)))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = aoVoltar) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextoForte)
                }
                Spacer(Modifier.width(4.dp))
                cotacao?.logoUrl?.let {
                    AsyncImage(
                        model = it, contentDescription = null,
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White)
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(ticker, color = TextoForte, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(cotacao?.nomeLongo ?: nome, color = GrayText, fontSize = 12.sp, maxLines = 1)
                }
                IconButton(onClick = aoRefresh) {
                    if (carregando) CircularProgressIndicator(color = GojoPurple, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    else Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = GrayText)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (cotacao == null) {
                if (carregando) Text("Carregando cotação…", color = GrayText, fontSize = 13.sp)
                else Text("Cotação indisponível para este ativo.", color = DebtRed, fontSize = 13.sp)
            } else {
                Text(formatarMoeda(cotacao.preco, cotacao.moeda),
                    color = TextoForte, fontSize = 32.sp, fontWeight = FontWeight.Black)
                val pct = cotacao.variacaoPct ?: 0.0
                val cor = if (pct >= 0) MoneyGreen else DebtRed
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (pct >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null, tint = cor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    val abs = cotacao.variacaoAbs?.let { formatarMoeda(it, cotacao.moeda) } ?: ""
                    Text(
                        "${if (pct >= 0) "+" else ""}${pct.emNumeroBR(2)}%  $abs (hoje)",
                        color = cor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CardSecao(titulo: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(titulo, color = TextoForte, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp))
            content()
        }
    }
}

@Composable
private fun GridMetrica(l1: String, v1: String, l2: String, v2: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(l1, color = GrayText, fontSize = 10.sp)
            Text(v1, color = TextoForte, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f)) {
            Text(l2, color = GrayText, fontSize = 10.sp)
            Text(v2, color = TextoForte, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GraficoLinha(pontos: List<Double>, moeda: String, modifier: Modifier = Modifier) {
    if (pontos.size < 2) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Sem dados de gráfico.", color = GrayText, fontSize = 11.sp)
        }
        return
    }
    val min = pontos.min()
    val max = pontos.max()
    val range = (max - min).takeIf { it > 0 } ?: 1.0
    val cor = if (pontos.last() >= pontos.first()) MoneyGreen else DebtRed

    var indiceTouch by remember(pontos) { mutableStateOf<Int?>(null) }
    var canvasWidth by remember { mutableStateOf(0f) }

    Column(modifier) {
        // Tooltip topo
        Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
            indiceTouch?.let { i ->
                val v = pontos[i]
                val pctVsInicio = if (pontos.first() != 0.0) (v - pontos.first()) / pontos.first() * 100 else 0.0
                val corPct = if (pctVsInicio >= 0) MoneyGreen else DebtRed
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatarMoeda(v, moeda), color = TextoForte,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("${if (pctVsInicio >= 0) "+" else ""}${pctVsInicio.emNumeroBR(2)}%",
                        color = corPct, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        val corLinhaBase = GrayText.copy(alpha = 0.2f)
        val corMira = TextoForte
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(pontos) {
                    detectTapGestures(
                        onPress = { off ->
                            val w = canvasWidth.takeIf { it > 0 } ?: size.width.toFloat()
                            val passo = w / (pontos.size - 1)
                            val idx = (off.x / passo).toInt().coerceIn(0, pontos.size - 1)
                            indiceTouch = idx
                            tryAwaitRelease()
                            indiceTouch = null
                        }
                    )
                }
                .pointerInput(pontos) {
                    detectDragGestures(
                        onDragStart = { off ->
                            val w = canvasWidth.takeIf { it > 0 } ?: size.width.toFloat()
                            val passo = w / (pontos.size - 1)
                            indiceTouch = (off.x / passo).toInt().coerceIn(0, pontos.size - 1)
                        },
                        onDragEnd = { indiceTouch = null },
                        onDragCancel = { indiceTouch = null }
                    ) { change, _ ->
                        val w = canvasWidth.takeIf { it > 0 } ?: size.width.toFloat()
                        val passo = w / (pontos.size - 1)
                        indiceTouch = (change.position.x / passo).toInt().coerceIn(0, pontos.size - 1)
                    }
                }
        ) {
            canvasWidth = size.width
            val w = size.width
            val h = size.height
            val passo = w / (pontos.size - 1)
            val path = Path()
            pontos.forEachIndexed { i, v ->
                val x = i * passo
                val y = h - (((v - min) / range) * h).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = cor, style = Stroke(width = 3f))
            drawLine(
                color = corLinhaBase,
                start = Offset(0f, h - 1),
                end = Offset(w, h - 1),
                strokeWidth = 1f
            )
            // Crosshair + ponto destacado
            indiceTouch?.let { i ->
                val x = i * passo
                val y = h - (((pontos[i] - min) / range) * h).toFloat()
                drawLine(
                    color = corMira.copy(alpha = 0.4f),
                    start = Offset(x, 0f), end = Offset(x, h),
                    strokeWidth = 1.5f
                )
                drawCircle(color = corMira, radius = 6f, center = Offset(x, y))
                drawCircle(color = cor, radius = 4f, center = Offset(x, y))
            }
        }
    }
}

@Composable
private fun ItemNoticia(art: GNewsArtigo) {
    val ctx = LocalContext.current
    val url = art.url
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardElevado)
            .clickable(enabled = !url.isNullOrBlank()) {
                url?.let { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
            }
            .padding(8.dp)
    ) {
        art.image?.let {
            AsyncImage(
                model = it, contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(art.title ?: "(sem título)", color = TextoForte, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 3)
            Spacer(Modifier.height(2.dp))
            val data = runCatching { art.publishedAt?.let { formatoDataBr.format(parseDataIso.parse(it)!!) } }.getOrNull()
            Text(
                listOfNotNull(art.source?.name, data).joinToString(" • "),
                color = GrayText, fontSize = 10.sp
            )
        }
        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = GrayText,
            modifier = Modifier.size(14.dp).align(Alignment.Top))
    }
}
