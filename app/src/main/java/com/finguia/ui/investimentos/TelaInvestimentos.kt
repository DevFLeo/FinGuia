package com.finguia.ui.investimentos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.finguia.dados.CategoriaInvestimento
import com.finguia.dados.CotacaoAtivo
import com.finguia.dados.Investimento
import kotlinx.coroutines.delay
import com.finguia.ui.theme.CardBg
import com.finguia.ui.theme.DarkBg
import com.finguia.ui.theme.DebtRed
import com.finguia.ui.theme.GojoPurple
import com.finguia.ui.theme.GrayText
import com.finguia.ui.theme.MoneyGreen
import java.text.NumberFormat
import java.util.Locale

private val formatoReais = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
private val formatoDolar = NumberFormat.getCurrencyInstance(Locale.US)

private fun formatarMoeda(v: Double, moeda: String?) =
    if (moeda?.equals("BRL", true) != false) formatoReais.format(v) else formatoDolar.format(v)

@Composable
fun TelaInvestimentos(
    modifier: Modifier = Modifier,
    viewModel: InvestimentoViewModel = viewModel()
) {
    val investimentos by viewModel.investimentos.collectAsState()
    val totalInvestido by viewModel.totalInvestido.collectAsState()
    val cotacoes by viewModel.cotacoes.collectAsState()

    // Total atual com cotação live quando disponível, senão usa rentabilidadePct
    val totalAtual = investimentos.sumOf { inv ->
        val cot = cotacoes[inv.ticker]?.cotacao
        if (cot != null && inv.quantidade != null) inv.valorAtualComCotacao(cot.preco) else inv.valorAtual
    }
    val totalLucro = totalAtual - totalInvestido

    var sugestaoSelecionada by remember { mutableStateOf<SugestaoAtivo?>(null) }
    var rendaFixaSelecionada by remember { mutableStateOf<SugestaoAtivo?>(null) }
    var detalheTicker by remember { mutableStateOf<DetalheRequest?>(null) }
    var queryBusca by remember { mutableStateOf("") }
    val resultadosBusca = remember(queryBusca) {
        if (queryBusca.length < 1) emptyList() else viewModel.buscarLocal(queryBusca)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = 90.dp)) {
            item {
                CabecalhoInvestimentos(
                    totalInvestido = totalInvestido,
                    totalAtual = totalAtual,
                    totalLucro = totalLucro
                )
            }

            item {
                BarraBusca(
                    query = queryBusca,
                    onQueryChange = { queryBusca = it },
                    onLimpar = { queryBusca = "" }
                )
            }

            if (queryBusca.isNotEmpty()) {
                if (resultadosBusca.isEmpty()) {
                    item {
                        Text("Nenhum ativo encontrado no catálogo.", color = GrayText, fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp))
                    }
                }
                items(resultadosBusca) { sug ->
                    ItemResultadoBusca(sug) {
                        if (sug.temCotacaoLive) {
                            detalheTicker = DetalheRequest(sug.ticker, sug.nome, sug.descricao)
                        } else {
                            rendaFixaSelecionada = sug
                        }
                        queryBusca = ""
                    }
                }
            }

            if (queryBusca.isBlank() && investimentos.isNotEmpty()) {
                item { TituloSecao(texto = "Sua Carteira", icone = Icons.Default.Savings) }
                items(investimentos) { inv ->
                    val estado = cotacoes[inv.ticker]
                    CardCarteira(
                        inv = inv,
                        cotacao = estado?.cotacao,
                        aoClicar = {
                            if (inv.ticker.isNotBlank()) {
                                detalheTicker = DetalheRequest(inv.ticker, inv.nome, "")
                            }
                        },
                        aoRemover = { viewModel.remover(inv.id) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            if (queryBusca.isBlank()) CategoriaInvestimento.entries.forEach { categoria ->
                val sugestoesDaCat = SUGESTOES_ATIVOS.filter { it.categoria == categoria }
                if (sugestoesDaCat.isNotEmpty()) {
                    item {
                        TituloSecao(
                            texto = nomeCategoria(categoria),
                            icone = iconeCategoria(categoria)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(sugestoesDaCat) { sug ->
                                CardSugestao(
                                    sug = sug,
                                    cotacao = cotacoes[sug.ticker]?.cotacao,
                                    aoSolicitarCotacao = { viewModel.solicitarCotacao(sug.ticker) },
                                    aoClicar = {
                                        if (sug.temCotacaoLive) {
                                            detalheTicker = DetalheRequest(sug.ticker, sug.nome, sug.descricao)
                                        } else {
                                            rendaFixaSelecionada = sug
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Detalhe overlay (slide in da direita) — ativos com cotação
        AnimatedVisibility(
            visible = detalheTicker != null,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            detalheTicker?.let { req ->
                TelaDetalheAtivo(
                    ticker = req.ticker,
                    nomeAtivo = req.nome,
                    descricao = req.descricao,
                    aoVoltar = { detalheTicker = null },
                    aoComprar = { cot ->
                        sugestaoSelecionada = SUGESTOES_ATIVOS.firstOrNull { it.ticker == req.ticker }
                            ?: SugestaoAtivo(req.nome, req.ticker, categoriaPorTicker(req.ticker), req.descricao, 0.0)
                        detalheTicker = null
                    },
                    viewModel = viewModel
                )
            }
        }

        // Detalhe overlay — renda fixa
        AnimatedVisibility(
            visible = rendaFixaSelecionada != null,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            rendaFixaSelecionada?.let { sug ->
                TelaDetalheRendaFixa(
                    sugestao = sug,
                    aoVoltar = { rendaFixaSelecionada = null },
                    aoComprar = {
                        sugestaoSelecionada = sug
                        rendaFixaSelecionada = null
                    },
                    viewModel = viewModel
                )
            }
        }
    }

    sugestaoSelecionada?.let { sug ->
        val cotAtual = cotacoes[sug.ticker]?.cotacao
        DialogComprarSugestao(
            sugestao = sug,
            cotacao = cotAtual,
            aoConfirmar = { valor, rentPct ->
                val qty = if (cotAtual != null && cotAtual.preco > 0) valor / cotAtual.preco else null
                viewModel.adicionar(
                    Investimento(
                        nome = sug.nome,
                        categoria = sug.categoria.name,
                        valorInvestido = valor,
                        rentabilidadePct = rentPct,
                        observacao = sug.ticker,
                        ticker = if (sug.temCotacaoLive) sug.ticker else "",
                        precoEntrada = cotAtual?.preco,
                        quantidade = qty
                    )
                )
                sugestaoSelecionada = null
            },
            aoCancelar = { sugestaoSelecionada = null }
        )
    }


}

private data class DetalheRequest(val ticker: String, val nome: String, val descricao: String)

private fun logoLocal(ticker: String): String? = com.finguia.dados.MercadoRepository.logoUrl(ticker)

private fun categoriaPorTicker(ticker: String): CategoriaInvestimento = when {
    ticker.endsWith("11") -> CategoriaInvestimento.IMOVEIS
    ticker.any { it.isDigit() } -> CategoriaInvestimento.ACOES_BR
    else -> CategoriaInvestimento.ACOES_INTER
}

@Composable
private fun CabecalhoInvestimentos(
    totalInvestido: Double,
    totalAtual: Double,
    totalLucro: Double
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1040), DarkBg)
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            Text(
                text = "Investimentos",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Cotações em tempo real • toque para ver detalhes",
                color = GrayText,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(Modifier.height(18.dp))

            Text("PATRIMÔNIO INVESTIDO", color = GrayText, fontSize = 10.sp, letterSpacing = 1.sp)
            Text(
                text = formatoReais.format(totalAtual),
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(4.dp))
            val corLucro = if (totalLucro >= 0) MoneyGreen else DebtRed
            val sinal = if (totalLucro >= 0) "+" else ""
            Text(
                text = "$sinal${formatoReais.format(totalLucro)} (investido ${formatoReais.format(totalInvestido)})",
                color = corLucro,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TituloSecao(texto: String, icone: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icone, contentDescription = null, tint = GojoPurple, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CardCarteira(
    inv: Investimento,
    cotacao: CotacaoAtivo?,
    aoClicar: () -> Unit,
    aoRemover: () -> Unit
) {
    val valorAtualReal = if (cotacao != null && inv.quantidade != null)
        inv.valorAtualComCotacao(cotacao.preco) else inv.valorAtual
    val lucro = valorAtualReal - inv.valorInvestido
    val pct = if (inv.valorInvestido > 0) lucro / inv.valorInvestido * 100.0 else 0.0
    val corLucro = if (lucro >= 0) MoneyGreen else DebtRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = aoClicar),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LogoOuIcone(
                logoUrl = cotacao?.logoUrl ?: logoLocal(inv.ticker),
                ticker = inv.ticker.ifBlank { inv.nome },
                fallback = iconeCategoria(runCatching { CategoriaInvestimento.valueOf(inv.categoria) }
                    .getOrDefault(CategoriaInvestimento.OUTROS))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(inv.nome, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (cotacao != null) {
                    Text(
                        "${inv.ticker} • ${formatarMoeda(cotacao.preco, cotacao.moeda)}",
                        color = GrayText, fontSize = 11.sp
                    )
                } else {
                    Text("Investido: ${formatoReais.format(inv.valorInvestido)}",
                        color = GrayText, fontSize = 11.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatoReais.format(valorAtualReal),
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${if (lucro >= 0) "+" else ""}${"%.2f".format(pct)}%",
                    color = corLucro, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = aoRemover, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = GrayText, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun LogoOuIcone(logoUrl: String?, ticker: String, fallback: ImageVector, size: Int = 42) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(GojoPurple.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        // Fallback (atrás) — sempre visível enquanto imagem carrega ou se falhar
        if (ticker.isNotBlank()) {
            val ex = ticker.split("-").first()
            Text(
                ex.take(if (ex.length >= 4) 4 else ex.length),
                color = Color.White,
                fontSize = (size / 4).sp,
                fontWeight = FontWeight.Black
            )
        } else {
            Icon(fallback, contentDescription = null, tint = GojoPurple, modifier = Modifier.size((size / 2).dp))
        }
        if (!logoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = logoUrl,
                contentDescription = null,
                loading = { /* fallback continua atrás */ },
                error = { /* fallback continua atrás */ },
                success = { state ->
                    Box(
                        Modifier.fillMaxSize().clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = state.painter,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.85f).clip(CircleShape)
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CardSugestao(
    sug: SugestaoAtivo,
    cotacao: CotacaoAtivo?,
    aoSolicitarCotacao: () -> Unit,
    aoClicar: () -> Unit
) {
    LaunchedEffect(sug.ticker) {
        if (sug.temCotacaoLive) aoSolicitarCotacao()
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(170.dp)
            .border(1.dp, Color(0xFF2A2A3E), RoundedCornerShape(16.dp))
            .clickable(onClick = aoClicar)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogoSugestao(logoUrl = cotacao?.logoUrl ?: logoLocal(sug.ticker),
                    ticker = sug.ticker, fallback = iconeCategoria(sug.categoria))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(sug.ticker, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(sug.nome, color = GrayText, fontSize = 10.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.height(8.dp))

            if (sug.temCotacaoLive) {
                if (cotacao == null) {
                    Text("Carregando…", color = GrayText, fontSize = 10.sp)
                    Text(sug.descricao, color = GrayText, fontSize = 10.sp, maxLines = 2,
                        modifier = Modifier.height(28.dp))
                } else {
                    Text(
                        formatarMoeda(cotacao.preco, cotacao.moeda),
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                    val pct = cotacao.variacaoPct ?: 0.0
                    val cor = if (pct >= 0) MoneyGreen else DebtRed
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (pct >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null, tint = cor, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("${if (pct >= 0) "+" else ""}${"%.2f".format(pct)}%",
                            color = cor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Text(sug.descricao, color = GrayText, fontSize = 10.sp, maxLines = 2,
                    modifier = Modifier.height(28.dp))
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .background(MoneyGreen.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "~${"%.1f".format(sug.rentabilidadeEstimadaPct)}% a.a.",
                        color = MoneyGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LogoSugestao(logoUrl: String?, ticker: String, fallback: ImageVector) =
    LogoOuIcone(logoUrl = logoUrl, ticker = ticker, fallback = fallback, size = 36)

@Composable
private fun DialogComprarSugestao(
    sugestao: SugestaoAtivo,
    cotacao: CotacaoAtivo?,
    aoConfirmar: (Double, Double) -> Unit,
    aoCancelar: () -> Unit
) {
    var valor by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("%.2f".format(Locale.US, sugestao.rentabilidadeEstimadaPct)) }
    var configAberta by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf(false) }
    val isRendaFixa = cotacao == null

    AlertDialog(
        onDismissRequest = aoCancelar,
        containerColor = CardBg,
        title = {
            Column {
                Text("Adicionar à carteira", color = GrayText, fontSize = 12.sp)
                Text("${sugestao.ticker} — ${sugestao.nome}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                if (cotacao != null) {
                    Text("Cotação atual: ${formatarMoeda(cotacao.preco, cotacao.moeda)}",
                        color = MoneyGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                } else {
                    Text("Rentabilidade estimada: ${"%.2f".format(rent.replace(",", ".").toDoubleOrNull() ?: sugestao.rentabilidadeEstimadaPct)}% a.a.",
                        color = MoneyGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                }
                Text(sugestao.descricao, color = GrayText, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it; erro = false },
                    label = { Text("Valor investido (R$)") },
                    isError = erro,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = campoColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (cotacao != null) {
                    val v = valor.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (v > 0 && cotacao.preco > 0) {
                        val qty = v / cotacao.preco
                        Text("Aprox. ${"%.4f".format(qty)} unidades de ${sugestao.ticker}",
                            color = GrayText, fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
                if (isRendaFixa) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { configAberta = !configAberta }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Configuração adicional", color = GojoPurple, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Icon(
                            if (configAberta) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null, tint = GojoPurple
                        )
                    }
                    if (configAberta) {
                        OutlinedTextField(
                            value = rent,
                            onValueChange = { rent = it },
                            label = { Text("Rentabilidade a.a. (%)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = campoColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Personalize o rendimento estimado conforme oferta real.",
                            color = GrayText, fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = valor.replace(".", "").replace(",", ".").toDoubleOrNull()
                    val r = rent.replace(",", ".").toDoubleOrNull() ?: sugestao.rentabilidadeEstimadaPct
                    if (v == null || v <= 0) erro = true else aoConfirmar(v, r)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GojoPurple)
            ) { Text("Adicionar", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) { Text("Cancelar", color = GrayText) }
        }
    )
}



@Composable
private fun campoColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GojoPurple,
    unfocusedBorderColor = GrayText.copy(alpha = 0.3f),
    focusedLabelColor = GojoPurple,
    unfocusedLabelColor = GrayText,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = GojoPurple
)

@Composable
private fun BarraBusca(query: String, onQueryChange: (String) -> Unit, onLimpar: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Buscar ativo (PETR4, AAPL, Vale...)", color = GrayText, fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayText) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onLimpar) {
                    Icon(Icons.Default.Close, contentDescription = "Limpar", tint = GrayText)
                }
            }
        },
        singleLine = true,
        colors = campoColors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ItemResultadoBusca(sug: SugestaoAtivo, aoClicar: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clickable(onClick = aoClicar),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(GojoPurple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(sug.ticker.take(2), color = GojoPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(sug.ticker, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(sug.nome, color = GrayText, fontSize = 11.sp, maxLines = 1)
            }
            Icon(iconeCategoria(sug.categoria), contentDescription = null,
                tint = GrayText, modifier = Modifier.size(14.dp))
        }
    }
}

private fun nomeCategoria(cat: CategoriaInvestimento): String = when (cat) {
    CategoriaInvestimento.ACOES_BR -> "Ações Brasileiras"
    CategoriaInvestimento.ACOES_INTER -> "Ações Internacionais"
    CategoriaInvestimento.IMOVEIS -> "Imóveis e FIIs"
    CategoriaInvestimento.RENDA_FIXA -> "Renda Fixa"
    CategoriaInvestimento.CRIPTO -> "Cripto"
    CategoriaInvestimento.OUTROS -> "Outros"
}

private fun iconeCategoria(cat: CategoriaInvestimento): ImageVector = when (cat) {
    CategoriaInvestimento.ACOES_BR -> Icons.Default.Business
    CategoriaInvestimento.ACOES_INTER -> Icons.Default.Public
    CategoriaInvestimento.IMOVEIS -> Icons.Default.Apartment
    CategoriaInvestimento.RENDA_FIXA -> Icons.Default.Savings
    CategoriaInvestimento.CRIPTO -> Icons.Default.CurrencyBitcoin
    CategoriaInvestimento.OUTROS -> Icons.Default.MoreHoriz
}
