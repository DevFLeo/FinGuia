package com.finguia.ui.busca

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finguia.R
import com.finguia.dados.CategoriaCustom
import com.finguia.dados.TipoTransacao
import com.finguia.dados.TransacaoBancaria
import com.finguia.ui.formato.emReais
import com.finguia.ui.theme.*
import com.finguia.ui.theme.TextoForte
import com.finguia.ui.transacoes.CategoriaViewModel
import com.finguia.ui.transacoes.TransacaoViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// ─── Modelo de resultado agrupado ────────────────────────────────────────────

data class ResultadoArea(val rotulo: String, val descricao: String, val icone: ImageVector, val destino: String)

private val AREAS_APP = listOf(
    ResultadoArea("Início", "Tela principal com saldo e ações", Icons.Default.Home, "INICIO"),
    ResultadoArea("Painel", "Dashboard com gráficos e resumos", Icons.Default.PieChart, "DASHBOARD"),
    ResultadoArea("Extratos", "Histórico de transações bancárias", Icons.Default.Receipt, "EXTRATO"),
    ResultadoArea("Lançar", "Registrar nova transação", Icons.Default.AddCircle, "LANCAR"),
    ResultadoArea("Calculadora", "Calculadora financeira", Icons.Default.Calculate, "CALCULADORA"),
    ResultadoArea("Configurações", "Preferências do app", Icons.Default.Settings, "CONFIGURACOES"),
)

private val ITENS_CONFIG = listOf(
    ResultadoArea("Ocultar saldo", "Esconder valores na tela inicial", Icons.Default.VisibilityOff, "CONFIGURACOES"),
    ResultadoArea("Privacidade", "Configurações de privacidade", Icons.Default.Lock, "CONFIGURACOES"),
)

// ─── Tela principal ───────────────────────────────────────────────────────────

@Composable
fun TelaBusca(
    modifier: Modifier = Modifier,
    transacaoViewModel: TransacaoViewModel = viewModel(),
    categoriaViewModel: CategoriaViewModel = viewModel(),
    aoNavegar: (String) -> Unit = {},
    aoAbrirDetalhe: (TransacaoBancaria) -> Unit = {},
    aoVoltar: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var transacoes by remember { mutableStateOf<List<TransacaoBancaria>>(emptyList()) }
    val categorias by categoriaViewModel.categorias.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // Debounce 300ms antes de buscar no banco
    LaunchedEffect(query) {
        if (query.isBlank()) {
            transacoes = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        transacoes = transacaoViewModel.buscar("%${query.trim()}%")
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val queryLimpa = query.trim().lowercase()

    val areasFiltradas = if (queryLimpa.isBlank()) emptyList()
    else AREAS_APP.filter {
        it.rotulo.lowercase().contains(queryLimpa) || it.descricao.lowercase().contains(queryLimpa)
    }

    val configFiltradas = if (queryLimpa.isBlank()) emptyList()
    else ITENS_CONFIG.filter {
        it.rotulo.lowercase().contains(queryLimpa) || it.descricao.lowercase().contains(queryLimpa)
    }

    val categoriasFiltradas = if (queryLimpa.isBlank()) emptyList()
    else categorias.filter {
        it.label.lowercase().contains(queryLimpa) || it.sublabel.lowercase().contains(queryLimpa)
    }

    val semResultados = queryLimpa.isNotBlank() &&
            areasFiltradas.isEmpty() && configFiltradas.isEmpty() &&
            categoriasFiltradas.isEmpty() && transacoes.isEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
    ) {
        // ── Barra de busca ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = aoVoltar) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextoForte)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(CardBg, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text("Buscar no FinGuia...", color = GrayText, fontSize = 14.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(color = TextoForte, fontSize = 14.sp),
                    cursorBrush = SolidColor(GojoPurple),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {})
                )
            }

            AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpar", tint = GrayText)
                }
            }
        }

        // ── Resultados ────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Estado inicial — sugestões
            if (queryLimpa.isBlank()) {
                item {
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        Text(
                            "Sugestões rápidas",
                            color = GrayText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                }
                items(AREAS_APP.take(5)) { area ->
                    ItemArea(area = area, aoClicar = { aoNavegar(area.destino) })
                }
                item { Spacer(Modifier.height(80.dp)) }
                return@LazyColumn
            }

            // Sem resultados
            if (semResultados) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = GrayText, modifier = Modifier.size(52.dp))
                        Text("Nenhum resultado para \"$query\"", color = GrayText, fontSize = 14.sp)
                    }
                }
                return@LazyColumn
            }

            // Seção: Áreas do app
            if (areasFiltradas.isNotEmpty()) {
                item { CabecalhoSecao("Áreas do app") }
                items(areasFiltradas) { area ->
                    ItemArea(area = area, aoClicar = { aoNavegar(area.destino) })
                }
            }

            // Seção: Configurações
            if (configFiltradas.isNotEmpty()) {
                item { CabecalhoSecao("Configurações") }
                items(configFiltradas) { item ->
                    ItemArea(area = item, aoClicar = { aoNavegar(item.destino) })
                }
            }

            // Seção: Categorias
            if (categoriasFiltradas.isNotEmpty()) {
                item { CabecalhoSecao("Categorias") }
                items(categoriasFiltradas) { cat ->
                    ItemCategoria(categoria = cat, aoClicar = { aoNavegar("CONFIGURACOES") })
                }
            }

            // Seção: Transações
            if (transacoes.isNotEmpty()) {
                item { CabecalhoSecao("Transações (${transacoes.size})") }
                items(transacoes, key = { it.id }) { transacao ->
                    ItemTransacao(transacao = transacao, aoClicar = { aoAbrirDetalhe(transacao) })
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ─── Componentes internos ─────────────────────────────────────────────────────

@Composable
private fun CabecalhoSecao(titulo: String) {
    Text(
        text = titulo.uppercase(),
        color = GrayText,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun ItemArea(area: ResultadoArea, aoClicar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .clickable(onClick = aoClicar)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(GojoPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(area.icone, contentDescription = null, tint = GojoPurple, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(area.rotulo, color = TextoForte, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(area.descricao, color = GrayText, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GrayText, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ItemCategoria(categoria: CategoriaCustom, aoClicar: () -> Unit) {
    val cor = if (categoria.ehEntrada) MoneyGreen else DebtRed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .clickable(onClick = aoClicar)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(cor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (categoria.ehEntrada) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null, tint = cor, modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(categoria.label, color = TextoForte, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(categoria.sublabel, color = GrayText, fontSize = 11.sp)
        }
        Text(
            if (categoria.ehEntrada) "Entrada" else "Saída",
            color = cor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ItemTransacao(transacao: TransacaoBancaria, aoClicar: () -> Unit) {
    val ehEntrada = transacao.tipo in listOf(
        TipoTransacao.PIX_RECEBIDO, TipoTransacao.TRANSFERENCIA_RECEBIDA,
        TipoTransacao.DEPOSITO, TipoTransacao.ESTORNO
    )
    val cor = if (ehEntrada) MoneyGreen else DebtRed
    val prefixo = if (ehEntrada) "+" else "-"
    val data = SimpleDateFormat("dd/MM/yy HH:mm", Locale("pt", "BR")).format(Date(transacao.timestampMs))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .clickable(onClick = aoClicar)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(cor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconeParaTipo(transacao.tipo), contentDescription = null, tint = cor, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(transacao.banco, color = TextoForte, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(transacao.descricao, color = GrayText, fontSize = 11.sp, maxLines = 1)
            Text(data, color = GrayText.copy(alpha = 0.6f), fontSize = 10.sp)
        }
        Text(
            "$prefixo${formatarValor(transacao.valor)}",
            color = cor, fontSize = 13.sp, fontWeight = FontWeight.Bold
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
    valor.emReais()
