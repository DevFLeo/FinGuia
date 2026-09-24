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
import com.finguia.dados.CategoriaCustom
import com.finguia.dados.TipoTransacao
import com.finguia.dados.TransacaoBancaria
import com.finguia.motor.MascaraMoeda
import com.finguia.ui.formato.MascaraMoedaBR
import com.finguia.ui.formato.digitosMoeda
import com.finguia.ui.formato.emReais
import com.finguia.ui.theme.*
import com.finguia.ui.theme.TextoForte

// ─────────────────────────────────────────────
// MODELO DE CATEGORIA DE LANÇAMENTO
// ─────────────────────────────────────────────

private data class Categoria(
    val icone: ImageVector,
    val label: String,
    val sublabel: String,
    val tipo: TipoTransacao,
    val ehEntrada: Boolean,
    val idCustom: Long? = null
)

private val CATEGORIAS_GANHOS = listOf(
    Categoria(Icons.Default.WorkOutline, "Freelancer",  "Trabalho extra",   TipoTransacao.PIX_RECEBIDO, true),
    Categoria(Icons.Default.Shuffle,     "Esporádico",  "Ganhos variados",  TipoTransacao.DEPOSITO,     true),
    Categoria(Icons.Default.Payments,    "Salário",     "Renda fixa",       TipoTransacao.DEPOSITO,     true),
)

private val CATEGORIAS_DIVIDAS = listOf(
    Categoria(Icons.Default.AccountBalance, "Contas",        "Boletos / Aluguel",        TipoTransacao.BOLETO_PAGO,    false),
    Categoria(Icons.Default.CreditCard,     "Emergência",    "Imprevistos",              TipoTransacao.COMPRA_DEBITO,  false),
    Categoria(Icons.Default.Restaurant,     "Comida",        "Alimentação",              TipoTransacao.COMPRA_DEBITO,  false),
    Categoria(Icons.Default.LocalCafe,      "Saídas Rápidas","Saídas Não Especificadas", TipoTransacao.COMPRA_DEBITO,  false),
    Categoria(Icons.Default.Receipt,        "Boletos",       "Pagamentos",               TipoTransacao.BOLETO_PAGO,    false),
    Categoria(Icons.Default.MoneyOff,       "Dívidas Gerais","Outros gastos",            TipoTransacao.COMPRA_CREDITO, false),
)

private val TEMPLATES_RECORRENTES = listOf(
    Categoria(Icons.Default.TrendingUp,   "Salário",         "Renda mensal fixa",      TipoTransacao.DEPOSITO,      true),
    Categoria(Icons.Default.TrendingDown, "Contas Fixas",    "Despesas mensais fixas", TipoTransacao.BOLETO_PAGO,   false),
    Categoria(Icons.Default.WorkOutline,  "Freelancer Fixo", "Renda recorrente",       TipoTransacao.PIX_RECEBIDO,  true),
    Categoria(Icons.Default.CreditCard,   "Assinatura",      "Streaming / Serviços",   TipoTransacao.COMPRA_CREDITO,false),
)

private fun CategoriaCustom.paraCategoriaUi() = Categoria(
    icone     = Icons.Default.Category,
    label     = label,
    sublabel  = sublabel,
    tipo      = tipo,
    ehEntrada = ehEntrada,
    idCustom  = id
)

// ─────────────────────────────────────────────
// TELA PRINCIPAL
// ─────────────────────────────────────────────

@Composable
fun TelaLancar(
    modifier: Modifier = Modifier,
    viewModel: TransacaoViewModel = viewModel(),
    categoriaVm: CategoriaViewModel = viewModel()
) {
    var abaSelecionada by remember { mutableStateOf(0) }
    val abas = listOf("Lançar", "Recorrente", "Agendado")

    var categoriaSelecionada by remember { mutableStateOf<Categoria?>(null) }
    var mostrarDialogAvulso   by remember { mutableStateOf(false) }
    var mostrarDialogCriar    by remember { mutableStateOf(false) }
    var mostrarDialogAgendar  by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        BarraAbas(abas = abas, selecionada = abaSelecionada, aoSelecionar = { abaSelecionada = it })

        Spacer(Modifier.height(20.dp))

        when (abaSelecionada) {
            0 -> AbaLancar(
                categoriaVm       = categoriaVm,
                aoClicarCategoria = { categoriaSelecionada = it },
                aoLancarAvulso    = { mostrarDialogAvulso = true },
                aocriarCategoria  = { mostrarDialogCriar  = true }
            )
            1 -> AbaRecorrente(viewModel = viewModel, aoClicarCategoria = { categoriaSelecionada = it })
            2 -> AbaAgendado(viewModel = viewModel, aoCriarAgendamento = { mostrarDialogAgendar = true })
        }
    }

    // Dialog de lançamento agendado (valor futuro)
    if (mostrarDialogAgendar) {
        DialogLancamentoAgendado(
            aoConfirmar = { valor, descricao, tipo, dataMs ->
                viewModel.inserir(
                    TransacaoBancaria(
                        banco             = "Manual",
                        pacoteApp         = "manual",
                        tipo              = tipo,
                        valor             = valor,
                        descricao         = descricao,
                        tituloNotificacao = "",
                        textoNotificacao  = "",
                        dataAgendada      = dataMs,
                        efetivado         = false,
                        timestampMs       = dataMs
                    )
                )
                mostrarDialogAgendar = false
            },
            aoCancelar = { mostrarDialogAgendar = false }
        )
    }

    // Dialog de lançamento por categoria
    categoriaSelecionada?.let { categoria ->
        DialogLancamento(
            categoria   = categoria,
            ehRecorrente = abaSelecionada == 1,
            aoConfirmar = { valor, descricao ->
                viewModel.inserir(
                    TransacaoBancaria(
                        banco              = "Manual",
                        pacoteApp          = "manual",
                        tipo               = categoria.tipo,
                        valor              = valor,
                        descricao          = descricao,
                        tituloNotificacao  = "",
                        textoNotificacao   = "",
                        recorrente         = abaSelecionada == 1
                    )
                )
                categoriaSelecionada = null
            },
            aoCancelar = { categoriaSelecionada = null }
        )
    }

    // Dialog de lançamento avulso (sem categoria pré-definida)
    if (mostrarDialogAvulso) {
        DialogLancamentoAvulso(
            aoConfirmar = { valor, descricao, tipo ->
                viewModel.inserir(
                    TransacaoBancaria(
                        banco             = "Manual",
                        pacoteApp         = "manual",
                        tipo              = tipo,
                        valor             = valor,
                        descricao         = descricao,
                        tituloNotificacao = "",
                        textoNotificacao  = ""
                    )
                )
                mostrarDialogAvulso = false
            },
            aoCancelar = { mostrarDialogAvulso = false }
        )
    }

    // Dialog de criação de nova categoria
    if (mostrarDialogCriar) {
        DialogCriarCategoria(
            aoConfirmar = { nova ->
                categoriaVm.inserir(nova)
                mostrarDialogCriar = false
            },
            aoCancelar = { mostrarDialogCriar = false }
        )
    }
}

// ─────────────────────────────────────────────
// DIALOG DE LANÇAMENTO POR CATEGORIA
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
        containerColor   = CardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(categoria.icone, contentDescription = null, tint = corAcento, modifier = Modifier.size(24.dp))
                Column {
                    Text(categoria.label, color = TextoForte, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    if (ehRecorrente)
                        Text("Lançamento recorrente", color = GojoPurple, fontSize = 11.sp)
                    else
                        Text(categoria.sublabel, color = GrayText, fontSize = 11.sp)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CampoValor(valorTexto, corAcento, erroValor,
                    onChange = { valorTexto = it; erroValor = false })
                CampoDescricao(descricao, onChange = { descricao = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valor = valorDaMascara(valorTexto)
                    if (valor == null || valor <= 0.0) erroValor = true
                    else aoConfirmar(valor, descricao.ifBlank { categoria.label })
                },
                colors = ButtonDefaults.buttonColors(containerColor = corAcento)
            ) { Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) { Text("Cancelar", color = GrayText) }
        }
    )
}

// ─────────────────────────────────────────────
// DIALOG DE LANÇAMENTO AVULSO
// ─────────────────────────────────────────────

@Composable
private fun DialogLancamentoAvulso(
    aoConfirmar: (valor: Double, descricao: String, tipo: TipoTransacao) -> Unit,
    aoCancelar: () -> Unit
) {
    var valorTexto by remember { mutableStateOf("") }
    var descricao  by remember { mutableStateOf("") }
    var ehEntrada  by remember { mutableStateOf(false) }
    var erroValor  by remember { mutableStateOf(false) }

    val corAcento = if (ehEntrada) MoneyGreen else DebtRed
    val tipoSelecionado = if (ehEntrada) TipoTransacao.DEPOSITO else TipoTransacao.COMPRA_DEBITO

    AlertDialog(
        onDismissRequest = aoCancelar,
        containerColor   = CardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = GojoPurple, modifier = Modifier.size(24.dp))
                Text("Lançar Valor Avulso", color = TextoForte, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Toggle Entrada / Saída
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .border(1.dp, GojoPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    listOf("Saída" to false, "Entrada" to true).forEach { (label, valor) ->
                        val ativo = ehEntrada == valor
                        val cor   = if (valor) MoneyGreen else DebtRed
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (ativo) cor.copy(alpha = 0.18f) else Color.Transparent)
                                .border(if (ativo) 1.dp else 0.dp, if (ativo) cor else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { ehEntrada = valor }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = label,
                                color      = if (ativo) cor else GrayText,
                                fontSize   = 13.sp,
                                fontWeight = if (ativo) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                CampoValor(valorTexto, corAcento, erroValor,
                    onChange = { valorTexto = it; erroValor = false })
                CampoDescricao(descricao, onChange = { descricao = it },
                    placeholder = "Ex: Lanche, uber, transferência...")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valor = valorDaMascara(valorTexto)
                    if (valor == null || valor <= 0.0) erroValor = true
                    else aoConfirmar(valor, descricao.ifBlank { if (ehEntrada) "Entrada avulsa" else "Saída avulsa" }, tipoSelecionado)
                },
                colors = ButtonDefaults.buttonColors(containerColor = corAcento)
            ) { Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) { Text("Cancelar", color = GrayText) }
        }
    )
}

// ─────────────────────────────────────────────
// DIALOG DE CRIAR CATEGORIA
// ─────────────────────────────────────────────

@Composable
private fun DialogCriarCategoria(
    aoConfirmar: (CategoriaCustom) -> Unit,
    aoCancelar: () -> Unit
) {
    var label     by remember { mutableStateOf("") }
    var sublabel  by remember { mutableStateOf("") }
    var ehEntrada by remember { mutableStateOf(false) }
    var erroLabel by remember { mutableStateOf(false) }

    val tipoSelecionado = if (ehEntrada) TipoTransacao.DEPOSITO else TipoTransacao.COMPRA_DEBITO
    val corAcento       = if (ehEntrada) MoneyGreen else DebtRed

    AlertDialog(
        onDismissRequest = aoCancelar,
        containerColor   = CardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.AddBox, contentDescription = null, tint = GojoPurple, modifier = Modifier.size(24.dp))
                Text("Nova Categoria", color = TextoForte, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Toggle Ganho / Gasto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .border(1.dp, GojoPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    listOf("Gasto" to false, "Ganho" to true).forEach { (rotulo, valor) ->
                        val ativo = ehEntrada == valor
                        val cor   = if (valor) MoneyGreen else DebtRed
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (ativo) cor.copy(alpha = 0.18f) else Color.Transparent)
                                .border(if (ativo) 1.dp else 0.dp, if (ativo) cor else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { ehEntrada = valor }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = rotulo,
                                color      = if (ativo) cor else GrayText,
                                fontSize   = 13.sp,
                                fontWeight = if (ativo) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value       = label,
                    onValueChange = { label = it; erroLabel = false },
                    label       = { Text("Nome da categoria", color = GrayText) },
                    placeholder = { Text("Ex: Academias, Jogos...", color = GrayText.copy(alpha = 0.5f)) },
                    isError     = erroLabel,
                    supportingText = if (erroLabel) {
                        { Text("Informe um nome para a categoria", color = DebtRed, fontSize = 11.sp) }
                    } else null,
                    singleLine  = true,
                    colors      = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = corAcento,
                        unfocusedBorderColor = GrayText.copy(alpha = 0.3f),
                        focusedTextColor     = TextoForte,
                        unfocusedTextColor   = TextoForte,
                        cursorColor          = corAcento,
                        errorBorderColor     = DebtRed,
                        errorTextColor       = TextoForte,
                    ),
                    modifier    = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value       = sublabel,
                    onValueChange = { sublabel = it },
                    label       = { Text("Descrição (opcional)", color = GrayText) },
                    placeholder = { Text("Ex: Mensalidade, hobby...", color = GrayText.copy(alpha = 0.5f)) },
                    singleLine  = true,
                    colors      = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = GojoPurple,
                        unfocusedBorderColor = GrayText.copy(alpha = 0.3f),
                        focusedTextColor     = TextoForte,
                        unfocusedTextColor   = TextoForte,
                        cursorColor          = GojoPurple,
                    ),
                    modifier    = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isBlank()) { erroLabel = true; return@Button }
                    aoConfirmar(
                        CategoriaCustom(
                            label     = label.trim(),
                            sublabel  = sublabel.trim().ifBlank { if (ehEntrada) "Ganho" else "Gasto" },
                            tipo      = tipoSelecionado,
                            ehEntrada = ehEntrada
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GojoPurple)
            ) { Text("Criar", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) { Text("Cancelar", color = GrayText) }
        }
    )
}

// ─────────────────────────────────────────────
// ABA: LANÇAR
// ─────────────────────────────────────────────

@Composable
private fun AbaLancar(
    categoriaVm: CategoriaViewModel,
    aoClicarCategoria: (Categoria) -> Unit,
    aoLancarAvulso: () -> Unit,
    aocriarCategoria: () -> Unit
) {
    val categoriasCustom by categoriaVm.categorias.collectAsState()

    val customGanhos = categoriasCustom.filter { it.ehEntrada }
    val customGastos = categoriasCustom.filter { !it.ehEntrada }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Botões de ação rápida ──────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BotaoAcaoRapida(
                modifier   = Modifier.weight(1f),
                icone      = Icons.Default.FlashOn,
                texto      = "Lançar Avulso",
                cor        = GojoPurple,
                aoClicar   = aoLancarAvulso
            )
            BotaoAcaoRapida(
                modifier   = Modifier.weight(1f),
                icone      = Icons.Default.AddBox,
                texto      = "Nova Categoria",
                cor        = GojoPurple,
                aoClicar   = aocriarCategoria
            )
        }

        Spacer(Modifier.height(4.dp))

        // ── Ganhos ────────────────────────────────────────────
        TituloSecao(icone = Icons.Default.Add, texto = "GANHOS E FREELANCE", cor = MoneyGreen)

        val todosGanhos = CATEGORIAS_GANHOS + customGanhos.map { it.paraCategoriaUi() }
        todosGanhos.chunked(2).forEach { linha ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                linha.forEach { cat ->
                    CardGanho(modifier = Modifier.weight(1f), categoria = cat, aoClicar = { aoClicarCategoria(cat) })
                }
                if (linha.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Dívidas ───────────────────────────────────────────
        TituloSecao(icone = Icons.Default.Warning, texto = "DÍVIDAS E GASTOS", cor = DebtRed)

        val todasDividas = CATEGORIAS_DIVIDAS + customGastos.map { it.paraCategoriaUi() }
        todasDividas.chunked(2).forEach { linha ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                linha.forEach { cat ->
                    CardDivida(modifier = Modifier.weight(1f), categoria = cat, aoClicar = { aoClicarCategoria(cat) })
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
                        Text(transacao.descricao, color = TextoForte, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(transacao.banco, color = GrayText, fontSize = 11.sp)
                    }
                    Text(
                        text       = "${if (ehEntrada) "+" else "-"}${formatarValor(transacao.valor)}",
                        color      = if (ehEntrada) MoneyGreen else DebtRed,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.deletar(transacao.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remover", tint = GrayText, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ─────────────────────────────────────────────
// ABA: AGENDADO (lançamentos futuros)
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbaAgendado(
    viewModel: TransacaoViewModel,
    aoCriarAgendamento: () -> Unit
) {
    val agendadas by viewModel.agendadas.collectAsState()
    val fmtData = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BotaoAcaoRapida(
            modifier = Modifier.fillMaxWidth(),
            icone = Icons.Default.Schedule,
            texto = "Novo Lançamento Agendado",
            cor = GojoPurple,
            aoClicar = aoCriarAgendamento
        )

        TituloSecao(icone = Icons.Default.Event, texto = "AGENDADOS", cor = GojoPurple)

        if (agendadas.isEmpty()) {
            Text(
                "Nenhum valor agendado. Use o botão acima para criar um lançamento futuro.",
                color = GrayText,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            agendadas.forEach { transacao ->
                val ehEntrada = transacao.ehEntrada()
                val cor = if (ehEntrada) MoneyGreen else DebtRed
                val data = transacao.dataAgendada?.let { fmtData.format(java.util.Date(it)) } ?: "—"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBg)
                        .border(1.dp, cor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(transacao.descricao, color = TextoForte, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Para $data", color = GojoPurple, fontSize = 11.sp)
                    }
                    Text(
                        text = "${if (ehEntrada) "+" else "-"}${formatarValor(transacao.valor)}",
                        color = cor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.efetivar(transacao) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Efetivar", tint = MoneyGreen, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { viewModel.deletar(transacao.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remover", tint = GrayText, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogLancamentoAgendado(
    aoConfirmar: (valor: Double, descricao: String, tipo: TipoTransacao, dataMs: Long) -> Unit,
    aoCancelar: () -> Unit
) {
    var valorTexto by remember { mutableStateOf("") }
    var descricao  by remember { mutableStateOf("") }
    var ehEntrada  by remember { mutableStateOf(false) }
    var erroValor  by remember { mutableStateOf(false) }
    var mostrarPicker by remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    val dataSelecionada = pickerState.selectedDateMillis
    val fmtData = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR")) }
    val corAcento = if (ehEntrada) MoneyGreen else DebtRed
    val tipoSelecionado = if (ehEntrada) TipoTransacao.DEPOSITO else TipoTransacao.COMPRA_DEBITO

    AlertDialog(
        onDismissRequest = aoCancelar,
        containerColor = CardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = GojoPurple, modifier = Modifier.size(24.dp))
                Text("Agendar Lançamento", color = TextoForte, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Toggle Entrada / Saída
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .border(1.dp, GojoPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    listOf("Saída" to false, "Entrada" to true).forEach { (label, valor) ->
                        val ativo = ehEntrada == valor
                        val cor = if (valor) MoneyGreen else DebtRed
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (ativo) cor.copy(alpha = 0.18f) else Color.Transparent)
                                .clickable { ehEntrada = valor }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (ativo) cor else GrayText, fontSize = 13.sp,
                                fontWeight = if (ativo) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                CampoValor(valorTexto, corAcento, erroValor,
                    onChange = { valorTexto = it; erroValor = false })
                CampoDescricao(descricao, onChange = { descricao = it },
                    placeholder = "Ex: IPTU de janeiro")

                // Seletor de data
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBg)
                        .clickable { mostrarPicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GojoPurple)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = dataSelecionada?.let { "Data: ${fmtData.format(java.util.Date(it))}" } ?: "Escolher data",
                        color = TextoForte,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valor = valorDaMascara(valorTexto)
                    val data = dataSelecionada
                    when {
                        valor == null || valor <= 0.0 -> erroValor = true
                        data == null -> mostrarPicker = true
                        else -> aoConfirmar(valor, descricao.ifBlank { if (ehEntrada) "Entrada agendada" else "Saída agendada" }, tipoSelecionado, data)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = corAcento)
            ) { Text("Agendar", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) { Text("Cancelar", color = GrayText) }
        }
    )

    if (mostrarPicker) {
        DatePickerDialog(
            onDismissRequest = { mostrarPicker = false },
            confirmButton = {
                TextButton(onClick = { mostrarPicker = false }) {
                    Text("OK", color = GojoPurple)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}



// ─────────────────────────────────────────────
// COMPONENTES REUTILIZÁVEIS
// ─────────────────────────────────────────────

@Composable
private fun BotaoAcaoRapida(
    modifier: Modifier,
    icone: ImageVector,
    texto: String,
    cor: Color,
    aoClicar: () -> Unit
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cor.copy(alpha = 0.12f))
            .border(1.dp, cor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = aoClicar)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(18.dp))
        Text(
            texto,
            color = TextoForte,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

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
                    text        = titulo.uppercase(),
                    color       = if (ativa) Color.White else TextoForte,
                    fontSize    = 10.sp,
                    fontWeight  = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun TituloSecao(icone: ImageVector, texto: String, cor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(16.dp))
        Text(texto, color = cor, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun CardGanho(modifier: Modifier, categoria: Categoria, aoClicar: () -> Unit) {
    Column(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MoneyGreen.copy(alpha = 0.08f))
            .border(1.dp, MoneyGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(onClick = aoClicar)
            .padding(10.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Icon(categoria.icone, contentDescription = null, tint = MoneyGreen, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(categoria.label,    color = TextoForte, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(categoria.sublabel, color = GrayText,    fontSize = 9.sp, textAlign = TextAlign.Center)
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
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Icon(categoria.icone, contentDescription = null, tint = DebtRed, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(categoria.label,    color = TextoForte, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(categoria.sublabel, color = GrayText,    fontSize = 9.sp, textAlign = TextAlign.Center)
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
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(categoria.icone, contentDescription = null, tint = cor, modifier = Modifier.size(26.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(categoria.label,    color = TextoForte, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(categoria.sublabel, color = GrayText,    fontSize = 12.sp)
        }
        Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = cor, modifier = Modifier.size(20.dp))
    }
}


// ─────────────────────────────────────────────
// CAMPOS COMPARTILHADOS
// ─────────────────────────────────────────────

@Composable
private fun CampoValor(
    valor: String,
    corAcento: Color,
    isError: Boolean,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value          = valor,
        onValueChange  = { onChange(digitosMoeda(it)) },
        visualTransformation = MascaraMoedaBR,
        label          = { Text("Valor", color = GrayText) },
        prefix         = { Text("R$ ", color = GrayText) },
        isError        = isError,
        supportingText = if (isError) {
            { Text("Informe um valor válido maior que zero", color = DebtRed, fontSize = 11.sp) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine     = true,
        colors         = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = corAcento,
            unfocusedBorderColor = GrayText.copy(alpha = 0.3f),
            focusedTextColor     = TextoForte,
            unfocusedTextColor   = TextoForte,
            cursorColor          = corAcento,
            errorBorderColor     = DebtRed,
            errorTextColor       = TextoForte,
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CampoDescricao(
    valor: String,
    onChange: (String) -> Unit,
    placeholder: String = "Ex: Salário de abril"
) {
    OutlinedTextField(
        value         = valor,
        onValueChange = onChange,
        label         = { Text("Descrição", color = GrayText) },
        placeholder   = { Text(placeholder, color = GrayText.copy(alpha = 0.5f)) },
        singleLine    = true,
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = GojoPurple,
            unfocusedBorderColor = GrayText.copy(alpha = 0.3f),
            focusedTextColor     = TextoForte,
            unfocusedTextColor   = TextoForte,
            cursorColor          = GojoPurple,
        ),
        modifier      = Modifier.fillMaxWidth()
    )
}

// ─────────────────────────────────────────────
// UTILITÁRIOS
// ─────────────────────────────────────────────

/** Digitos da MascaraMoedaBR (ex.: "123456") para reais; zero vira null. */
private fun valorDaMascara(digitos: String): Double? =
    MascaraMoeda.reais(digitos).takeIf { it > 0 }

private fun formatarValor(valor: Double): String =
    valor.emReais()

private fun TransacaoBancaria.ehEntrada(): Boolean = tipo in listOf(
    TipoTransacao.PIX_RECEBIDO,
    TipoTransacao.TRANSFERENCIA_RECEBIDA,
    TipoTransacao.DEPOSITO,
    TipoTransacao.ESTORNO
)
