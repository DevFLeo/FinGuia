package com.finguia.ui.transacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finguia.dados.TipoTransacao
import com.finguia.dados.TransacaoBancaria
import com.finguia.ui.theme.*

// ─────────────────────────────────────────────
// MODELO DE CATEGORIA DE LANÇAMENTO
// ─────────────────────────────────────────────

// Representa um tipo de lançamento que o usuário pode realizar manualmente
private data class Categoria(
    val icone: ImageVector,
    val label: String,
    val sublabel: String,
    val tipo: TipoTransacao,
    val ehEntrada: Boolean
)

// Categorias de ganho — mapeadas apenas para tipos que contam como receita no DAO
private val CATEGORIAS_GANHOS = listOf(
    Categoria(Icons.Default.WorkOutline, "Freelancer",  "Trabalho extra",   TipoTransacao.PIX_RECEBIDO, true),
    Categoria(Icons.Default.Shuffle,     "Esporádico",  "Ganhos variados",  TipoTransacao.DEPOSITO,     true),
    Categoria(Icons.Default.Payments,    "Salário",     "Renda fixa",       TipoTransacao.DEPOSITO,     true),
)

// Categorias de despesa — mapeadas apenas para tipos que contam como despesa no DAO
private val CATEGORIAS_DIVIDAS = listOf(
    Categoria(Icons.Default.AccountBalance, "Contas",        "Boletos / Aluguel", TipoTransacao.BOLETO_PAGO,    false),
    Categoria(Icons.Default.CreditCard,     "Emergência",    "Imprevistos",       TipoTransacao.COMPRA_DEBITO,  false),
    Categoria(Icons.Default.Restaurant,     "Comida",        "Alimentação",       TipoTransacao.COMPRA_DEBITO,  false),
    Categoria(Icons.Default.LocalCafe,      "Saídas Rápidas","Saídas Não Especificadas", TipoTransacao.COMPRA_DEBITO,  false),
    Categoria(Icons.Default.Receipt,        "Boletos",       "Pagamentos",        TipoTransacao.BOLETO_PAGO,    false),
    Categoria(Icons.Default.MoneyOff,       "Dívidas Gerais","Outros gastos",     TipoTransacao.COMPRA_CREDITO, false),
)

// Templates rápidos para a aba de recorrentes
private val TEMPLATES_RECORRENTES = listOf(
    Categoria(Icons.Default.TrendingUp,   "Salário",         "Renda mensal fixa",       TipoTransacao.DEPOSITO,      true),
    Categoria(Icons.Default.TrendingDown, "Contas Fixas",    "Despesas mensais fixas",   TipoTransacao.BOLETO_PAGO,   false),
    Categoria(Icons.Default.WorkOutline,  "Freelancer Fixo", "Renda recorrente",         TipoTransacao.PIX_RECEBIDO,  true),
    Categoria(Icons.Default.CreditCard,   "Assinatura",      "Streaming / Serviços",     TipoTransacao.COMPRA_CREDITO,false),
)

// ─────────────────────────────────────────────
// TELA PRINCIPAL
// ─────────────────────────────────────────────

@Composable
fun TelaLancar(
    modifier: Modifier = Modifier,
    viewModel: TransacaoViewModel = viewModel()
) {
    var abaSelecionada by remember { mutableStateOf(0) }
    val abas = listOf("Lançar", "Recorrente", "Notificação")

    // Categoria clicada — quando não-nula, abre o dialog de confirmação
    var categoriaSelecionada by remember { mutableStateOf<Categoria?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        BarraAbas(
            abas = abas,
            selecionada = abaSelecionada,
            aoSelecionar = { abaSelecionada = it }
        )

        Spacer(Modifier.height(20.dp))

        when (abaSelecionada) {
            0 -> AbaLancar(aoClicarCategoria = { categoriaSelecionada = it })
            1 -> AbaRecorrente(viewModel = viewModel, aoClicarCategoria = { categoriaSelecionada = it })
            2 -> AbaNotificacao()
        }
    }

    // Dialog de lançamento — exibido ao clicar em qualquer categoria
    categoriaSelecionada?.let { categoria ->
        DialogLancamento(
            categoria = categoria,
            ehRecorrente = abaSelecionada == 1,
            aoConfirmar = { valor, descricao ->
                viewModel.inserir(
                    TransacaoBancaria(
                        banco = "Manual",
                        pacoteApp = "manual",
                        tipo = categoria.tipo,
                        valor = valor,
                        descricao = descricao,
                        tituloNotificacao = "",
                        textoNotificacao = "",
                        recorrente = abaSelecionada == 1
                    )
                )
                categoriaSelecionada = null
            },
            aoCancelar = { categoriaSelecionada = null }
        )
    }
}

// ─────────────────────────────────────────────
// DIALOG DE LANÇAMENTO
// ─────────────────────────────────────────────

@Composable
private fun DialogLancamento(
    categoria: Categoria,
    ehRecorrente: Boolean,
    aoConfirmar: (valor: Double, descricao: String) -> Unit,
    aoCancelar: () -> Unit
) {
    var valorTexto by remember { mutableStateOf("") }
    var descricao  by remember { mutableStateOf(categoria.label) }
    var erroValor  by remember { mutableStateOf(false) }

    val corAcento = if (categoria.ehEntrada) MoneyGreen else DebtRed

    AlertDialog(
        onDismissRequest = aoCancelar,
        containerColor = CardBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(categoria.icone, contentDescription = null, tint = corAcento, modifier = Modifier.size(24.dp))
                Column {
                    Text(categoria.label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    if (ehRecorrente) {
                        Text("Lançamento recorrente", color = GojoPurple, fontSize = 11.sp)
                    } else {
                        Text(categoria.sublabel, color = GrayText, fontSize = 11.sp)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // ── Campo: Valor ──────────────────────────────────
                OutlinedTextField(
                    value = valorTexto,
                    onValueChange = { entrada ->
                        // Aceita apenas dígitos, vírgula e ponto decimal
                        valorTexto = entrada.filter { it.isDigit() || it == ',' || it == '.' }
                        erroValor = false
                    },
                    label      = { Text("Valor (R$)", color = GrayText) },
                    placeholder = { Text("Ex: 1.500,00", color = GrayText.copy(alpha = 0.5f)) },
                    isError    = erroValor,
                    supportingText = if (erroValor) {
                        { Text("Informe um valor válido maior que zero", color = DebtRed, fontSize = 11.sp) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = corAcento,
                        unfocusedBorderColor = GrayText.copy(alpha = 0.3f),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = corAcento,
                        errorBorderColor     = DebtRed,
                        errorTextColor       = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Campo: Descrição ──────────────────────────────
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label       = { Text("Descrição", color = GrayText) },
                    placeholder = { Text("Ex: Salário de abril", color = GrayText.copy(alpha = 0.5f)) },
                    singleLine  = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = GojoPurple,
                        unfocusedBorderColor = GrayText.copy(alpha = 0.3f),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = GojoPurple,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valor = parsearValorBrasileiro(valorTexto)
                    if (valor == null || valor <= 0.0) {
                        erroValor = true
                    } else {
                        aoConfirmar(valor, descricao.ifBlank { categoria.label })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = corAcento)
            ) {
                Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) {
                Text("Cancelar", color = GrayText)
            }
        }
    )
}

// ─────────────────────────────────────────────
// ABA: LANÇAR
// ─────────────────────────────────────────────

@Composable
private fun AbaLancar(aoClicarCategoria: (Categoria) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TituloSecao(icone = Icons.Default.Add, texto = "GANHOS E FREELANCE", cor = GojoPurple)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CATEGORIAS_GANHOS.forEach { cat ->
                CardGanho(
                    modifier = Modifier.weight(1f),
                    categoria = cat,
                    aoClicar = { aoClicarCategoria(cat) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        TituloSecao(icone = Icons.Default.Warning, texto = "DÍVIDAS E GASTOS", cor = DebtRed)

        CATEGORIAS_DIVIDAS.chunked(2).forEach { linha ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                linha.forEach { cat ->
                    CardDivida(
                        modifier = Modifier.weight(1f),
                        categoria = cat,
                        aoClicar = { aoClicarCategoria(cat) }
                    )
                }
                if (linha.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ─────────────────────────────────────────────
// ABA: RECORRENTE
// ─────────────────────────────────────────────

@Composable
private fun AbaRecorrente(
    viewModel: TransacaoViewModel,
    aoClicarCategoria: (Categoria) -> Unit
) {
    val recorrentes by viewModel.recorrentes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TituloSecao(icone = Icons.Default.Repeat, texto = "ADICIONAR RECORRENTE", cor = GojoPurple)

        TEMPLATES_RECORRENTES.forEach { cat ->
            CardTemplate(categoria = cat, aoClicar = { aoClicarCategoria(cat) })
        }

        // Lista de lançamentos recorrentes já salvos
        if (recorrentes.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            TituloSecao(icone = Icons.Default.List, texto = "SALVOS COMO RECORRENTE", cor = GojoPurple)

            recorrentes.forEach { transacao ->
                val ehEntrada = transacao.ehEntrada()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBg)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transacao.descricao,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = transacao.banco,
                            color = GrayText,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "${if (ehEntrada) "+" else "-"}${formatarValor(transacao.valor)}",
                        color = if (ehEntrada) MoneyGreen else DebtRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.deletar(transacao.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Remover",
                            tint = GrayText,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ─────────────────────────────────────────────
// ABA: NOTIFICAÇÃO
// ─────────────────────────────────────────────

@Composable
private fun AbaNotificacao() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TituloSecao(icone = Icons.Default.Notifications, texto = "GESTÃO DE ALERTAS", cor = GojoPurple)

        CardSimples(Icons.Default.AddAlert, "Nova Notificação", "Criar alerta personalizado", GojoPurple)

        Spacer(Modifier.height(8.dp))

        TituloSecao(icone = Icons.Default.Edit, texto = "EDITAR PALAVRAS", cor = GojoPurple)

        listOf("Deletar", "Arquivar", "Desarquivar").forEach { opcao ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBg)
                    .border(width = 3.dp, color = GojoPurple, shape = RoundedCornerShape(8.dp))
                    .clickable { }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(opcao, color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENTES REUTILIZÁVEIS
// ─────────────────────────────────────────────

@Composable
private fun BarraAbas(abas: List<String>, selecionada: Int, aoSelecionar: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, GojoPurple.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        abas.forEachIndexed { index, titulo ->
            val ativa = index == selecionada
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (ativa) GojoPurple else Color.Transparent)
                    .clickable { aoSelecionar(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = titulo.uppercase(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun TituloSecao(icone: ImageVector, texto: String, cor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(16.dp))
        Text(texto, color = cor, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun CardGanho(modifier: Modifier, categoria: Categoria, aoClicar: () -> Unit) {
    Column(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GojoPurple.copy(alpha = 0.12f))
            .border(1.dp, GojoPurple.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = aoClicar)
            .padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(categoria.icone, contentDescription = null, tint = GojoPurple, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(categoria.label,    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(categoria.sublabel, color = GrayText,    fontSize = 10.sp)
    }
}

@Composable
private fun CardDivida(modifier: Modifier, categoria: Categoria, aoClicar: () -> Unit) {
    Column(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DebtRed.copy(alpha = 0.08f))
            .border(1.dp, DebtRed.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(onClick = aoClicar)
            .padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(categoria.icone, contentDescription = null, tint = DebtRed, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(categoria.label,    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(categoria.sublabel, color = GrayText,    fontSize = 9.sp)
    }
}

@Composable
private fun CardTemplate(categoria: Categoria, aoClicar: () -> Unit) {
    val cor = if (categoria.ehEntrada) MoneyGreen else DebtRed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cor.copy(alpha = 0.10f))
            .border(1.dp, cor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = aoClicar)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(categoria.icone, contentDescription = null, tint = cor, modifier = Modifier.size(26.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(categoria.label,    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(categoria.sublabel, color = GrayText,    fontSize = 12.sp)
        }
        Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = cor, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CardSimples(icone: ImageVector, label: String, sublabel: String, cor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cor.copy(alpha = 0.10f))
            .border(1.dp, cor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(26.dp))
        Column {
            Text(label,    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(sublabel, color = GrayText,    fontSize = 12.sp)
        }
    }
}

// ─────────────────────────────────────────────
// UTILITÁRIOS
// ─────────────────────────────────────────────

// Converte "1.500,00" (padrão BR) ou "1500.50" (padrão internacional) para Double
private fun parsearValorBrasileiro(texto: String): Double? {
    if (texto.isBlank()) return null
    return try {
        val normalizado = if (texto.contains(",")) {
            // Formato BR: remove separador de milhar (ponto) e troca vírgula por ponto decimal
            texto.replace(".", "").replace(",", ".")
        } else {
            texto
        }
        normalizado.toDouble().takeIf { it > 0 }
    } catch (_: NumberFormatException) {
        null
    }
}

// Formata Double para o padrão monetário brasileiro: R$ 1.500,00
private fun formatarValor(valor: Double): String =
    "R$ %,.2f".format(valor).replace(",", "X").replace(".", ",").replace("X", ".")

// Verifica se a transação é de entrada com base nos mesmos critérios do DAO
private fun TransacaoBancaria.ehEntrada(): Boolean = tipo in listOf(
    TipoTransacao.PIX_RECEBIDO,
    TipoTransacao.TRANSFERENCIA_RECEBIDA,
    TipoTransacao.DEPOSITO,
    TipoTransacao.ESTORNO
)
