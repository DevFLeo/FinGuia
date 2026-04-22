package com.finguia.ui.investimentos

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Savings
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
import com.finguia.dados.CategoriaInvestimento
import com.finguia.dados.Investimento
import com.finguia.ui.theme.CardBg
import com.finguia.ui.theme.DarkBg
import com.finguia.ui.theme.DebtRed
import com.finguia.ui.theme.GojoPurple
import com.finguia.ui.theme.GrayText
import com.finguia.ui.theme.MoneyGreen
import java.text.NumberFormat
import java.util.Locale

private val formatoReais = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

@Composable
fun TelaInvestimentos(
    modifier: Modifier = Modifier,
    viewModel: InvestimentoViewModel = viewModel()
) {
    val investimentos by viewModel.investimentos.collectAsState()
    val totalInvestido by viewModel.totalInvestido.collectAsState()

    val totalAtual = investimentos.sumOf { it.valorAtual }
    val totalLucro = totalAtual - totalInvestido

    var sugestaoSelecionada by remember { mutableStateOf<SugestaoAtivo?>(null) }
    var mostrarDialogNovo by remember { mutableStateOf(false) }

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

            if (investimentos.isNotEmpty()) {
                item { TituloSecao(texto = "Sua Carteira", icone = Icons.Default.Savings) }
                items(investimentos) { inv ->
                    CardCarteira(inv, aoRemover = { viewModel.remover(inv.id) })
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            CategoriaInvestimento.entries.forEach { categoria ->
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
                                CardSugestao(sug, aoClicar = { sugestaoSelecionada = sug })
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { mostrarDialogNovo = true },
            containerColor = GojoPurple,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White)
        }
    }

    sugestaoSelecionada?.let { sug ->
        DialogComprarSugestao(
            sugestao = sug,
            aoConfirmar = { valor ->
                viewModel.adicionar(
                    Investimento(
                        nome = sug.nome,
                        categoria = sug.categoria.name,
                        valorInvestido = valor,
                        rentabilidadePct = 0.0,
                        observacao = sug.ticker
                    )
                )
                sugestaoSelecionada = null
            },
            aoCancelar = { sugestaoSelecionada = null }
        )
    }

    if (mostrarDialogNovo) {
        DialogNovoInvestimento(
            aoConfirmar = { inv ->
                viewModel.adicionar(inv)
                mostrarDialogNovo = false
            },
            aoCancelar = { mostrarDialogNovo = false }
        )
    }
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
                text = "Sua carteira e sugestões de ativos",
                color = GrayText,
                fontSize = 13.sp,
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
private fun CardCarteira(inv: Investimento, aoRemover: () -> Unit) {
    val lucro = inv.lucro
    val corLucro = if (lucro >= 0) MoneyGreen else DebtRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(GojoPurple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    iconeCategoria(runCatching { CategoriaInvestimento.valueOf(inv.categoria) }.getOrDefault(CategoriaInvestimento.OUTROS)),
                    contentDescription = null,
                    tint = GojoPurple,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(inv.nome, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Investido: ${formatoReais.format(inv.valorInvestido)}",
                    color = GrayText, fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatoReais.format(inv.valorAtual), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${if (lucro >= 0) "+" else ""}${"%.2f".format(inv.rentabilidadePct)}%",
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
private fun CardSugestao(sug: SugestaoAtivo, aoClicar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(160.dp)
            .border(1.dp, Color(0xFF2A2A3E), RoundedCornerShape(16.dp))
            .clickable(onClick = aoClicar)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GojoPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(iconeCategoria(sug.categoria), contentDescription = null, tint = GojoPurple, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(sug.ticker, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(sug.nome, color = GrayText, fontSize = 10.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                sug.descricao,
                color = GrayText,
                fontSize = 10.sp,
                maxLines = 2,
                modifier = Modifier.height(28.dp)
            )
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

@Composable
private fun DialogComprarSugestao(
    sugestao: SugestaoAtivo,
    aoConfirmar: (Double) -> Unit,
    aoCancelar: () -> Unit
) {
    var valor by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf(false) }

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
                Text(sugestao.descricao, color = GrayText, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Rentabilidade estimada: ${"%.1f".format(sugestao.rentabilidadeEstimadaPct)}% a.a.",
                    color = MoneyGreen,
                    fontSize = 11.sp
                )
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = valor.replace(".", "").replace(",", ".").toDoubleOrNull()
                    if (v == null || v <= 0) erro = true else aoConfirmar(v)
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
private fun DialogNovoInvestimento(
    aoConfirmar: (Investimento) -> Unit,
    aoCancelar: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var valor by remember { mutableStateOf("") }
    var rentabilidade by remember { mutableStateOf("0") }
    var categoria by remember { mutableStateOf(CategoriaInvestimento.ACOES_BR) }
    var erro by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = aoCancelar,
        containerColor = CardBg,
        title = { Text("Novo Investimento", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nome, onValueChange = { nome = it; erro = false },
                    label = { Text("Nome / Ticker") }, singleLine = true,
                    isError = erro, colors = campoColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = valor, onValueChange = { valor = it; erro = false },
                    label = { Text("Valor investido (R$)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = erro, colors = campoColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rentabilidade, onValueChange = { rentabilidade = it },
                    label = { Text("Rentabilidade acumulada (%)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = campoColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Categoria", color = GrayText, fontSize = 12.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CategoriaInvestimento.entries.forEach { cat ->
                        val ativo = categoria == cat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (ativo) GojoPurple.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { categoria = cat }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(iconeCategoria(cat), contentDescription = null,
                                tint = if (ativo) GojoPurple else GrayText,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(nomeCategoria(cat),
                                color = if (ativo) Color.White else GrayText,
                                fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = valor.replace(".", "").replace(",", ".").toDoubleOrNull()
                    val r = rentabilidade.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (nome.isBlank() || v == null || v <= 0) erro = true
                    else aoConfirmar(
                        Investimento(
                            nome = nome.trim(),
                            categoria = categoria.name,
                            valorInvestido = v,
                            rentabilidadePct = r
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GojoPurple)
            ) { Text("Criar", color = Color.White) }
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
