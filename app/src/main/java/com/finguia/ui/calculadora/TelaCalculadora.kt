package com.finguia.ui.calculadora

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finguia.motor.NumeroBR
import com.finguia.ui.formato.emNumeroBR
import com.finguia.ui.formato.lerNumeroBR
import com.finguia.ui.theme.CardBg
import com.finguia.ui.theme.DarkBg
import com.finguia.ui.theme.DebtRed
import com.finguia.ui.theme.GojoPurple
import com.finguia.ui.theme.GrayText
import com.finguia.ui.theme.MoneyGreen
import com.finguia.ui.theme.TextoForte
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

// ============================================================
// API DE CAMBIO - AwesomeAPI (gratuita, sem chave)
// ============================================================

private data class CotacaoMoeda(
    val code: String,
    val codein: String,
    val name: String,
    val bid: String,
    val ask: String,
    val pctChange: String?
)

private interface MoedasApi {
    @GET("json/last/{pares}")
    suspend fun cotacoes(@Path("pares") pares: String): Map<String, CotacaoMoeda>
}

private object MoedasRetrofit {
    val servico: MoedasApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "FinGuia-Android/1.0")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .build()
        Retrofit.Builder()
            .baseUrl("https://economia.awesomeapi.com.br/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MoedasApi::class.java)
    }
}

data class MoedaInfo(
    val codigo: String,
    val nome: String,
    val valorEmReais: Double,
    val variacaoPct: Double
)

private val PARES_CAMBIO = listOf(
    "USD-BRL", "EUR-BRL", "GBP-BRL", "JPY-BRL",
    "CAD-BRL", "AUD-BRL", "ARS-BRL", "CHF-BRL",
    "BTC-BRL", "ETH-BRL"
)

class MoedasViewModel : ViewModel() {
    private val _cotacoes = MutableStateFlow<List<MoedaInfo>>(emptyList())
    val cotacoes: StateFlow<List<MoedaInfo>> = _cotacoes

    private val _carregando = MutableStateFlow(false)
    val carregando: StateFlow<Boolean> = _carregando

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    init { atualizar() }

    fun atualizar() {
        viewModelScope.launch {
            _carregando.value = true
            _erro.value = null
            try {
                val resp = withContext(Dispatchers.IO) {
                    try {
                        MoedasRetrofit.servico.cotacoes(PARES_CAMBIO.joinToString(","))
                    } catch (primeira: Exception) {
                        // Retry uma vez — AwesomeAPI costuma falhar pontualmente
                        MoedasRetrofit.servico.cotacoes(PARES_CAMBIO.joinToString(","))
                    }
                }
                val mapeado = resp.values.mapNotNull { c ->
                    val valor = c.bid.toDoubleOrNull() ?: return@mapNotNull null
                    MoedaInfo(
                        codigo = c.code,
                        nome = c.name.substringBefore("/").trim(),
                        valorEmReais = valor,
                        variacaoPct = c.pctChange?.toDoubleOrNull() ?: 0.0
                    )
                }.sortedBy { it.codigo }
                if (mapeado.isEmpty()) {
                    _erro.value = "Nenhuma cotação recebida. Tente novamente em instantes."
                } else {
                    _cotacoes.value = mapeado
                }
            } catch (e: Exception) {
                val detalhe = e.message?.take(120) ?: e.javaClass.simpleName
                _erro.value = "Falha ao buscar cotações: $detalhe"
            } finally {
                _carregando.value = false
            }
        }
    }
}

// ============================================================
// AVALIADOR DE EXPRESSAO - CALCULADORA CIENTIFICA
// ============================================================

private class Avaliador(private val src: String) {
    private var pos = 0
    fun avaliar(): Double {
        val r = expr()
        if (pos < src.length) erro("token inesperado '${src[pos]}'")
        return r
    }

    private fun erro(msg: String): Nothing = throw IllegalArgumentException(msg)

    private fun peek(): Char? = if (pos < src.length) src[pos] else null
    private fun consume(c: Char): Boolean {
        while (pos < src.length && src[pos] == ' ') pos++
        return if (pos < src.length && src[pos] == c) { pos++; true } else false
    }
    private fun saltarEspaco() { while (pos < src.length && src[pos] == ' ') pos++ }

    private fun expr(): Double {
        var v = termo()
        while (true) {
            saltarEspaco()
            v = when {
                consume('+') -> v + termo()
                consume('-') -> v - termo()
                else -> return v
            }
        }
    }

    private fun termo(): Double {
        var v = potencia()
        while (true) {
            saltarEspaco()
            v = when {
                consume('*') || consume('x') || consume('X') -> v * potencia()
                consume('/') || consume('÷') -> {
                    val d = potencia()
                    if (d == 0.0) erro("divisão por zero")
                    v / d
                }
                else -> return v
            }
        }
    }

    private fun potencia(): Double {
        val v = unario()
        saltarEspaco()
        return if (consume('^')) v.pow(unario()) else v
    }

    private fun unario(): Double {
        saltarEspaco()
        if (consume('+')) return unario()
        if (consume('-')) return -unario()
        return primario()
    }

    private fun primario(): Double {
        saltarEspaco()
        if (consume('(')) {
            val v = expr()
            if (!consume(')')) erro("parêntese faltando")
            return v
        }
        val c = peek() ?: erro("fim inesperado")
        if (c.isLetter()) return chamarFuncao()
        return numero()
    }

    private fun chamarFuncao(): Double {
        val ini = pos
        while (pos < src.length && (src[pos].isLetter() || src[pos].isDigit())) pos++
        val nome = src.substring(ini, pos).lowercase()
        saltarEspaco()
        return when (nome) {
            "pi" -> Math.PI
            "e" -> Math.E
            else -> {
                if (!consume('(')) erro("função '$nome' exige '('")
                val arg = expr()
                if (!consume(')')) erro("parêntese faltando após '$nome'")
                when (nome) {
                    "sin", "sen" -> sin(arg)
                    "cos" -> cos(arg)
                    "tan", "tg" -> tan(arg)
                    "sqrt", "raiz" -> sqrt(arg)
                    "ln" -> ln(arg)
                    "log" -> log10(arg)
                    "exp" -> exp(arg)
                    "abs" -> kotlin.math.abs(arg)
                    else -> erro("função desconhecida: $nome")
                }
            }
        }
    }

    private fun numero(): Double {
        saltarEspaco()
        val ini = pos
        while (pos < src.length && (src[pos].isDigit() || src[pos] == '.')) pos++
        if (ini == pos) erro("número esperado")
        return src.substring(ini, pos).toDouble()
    }
}

private fun avaliarCientifica(expressao: String): String {
    return try {
        val limpa = expressao
            .replace(",", ".")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "pi")
        val r = Avaliador(limpa).avaliar()
        // Padrao BR: ate 8 casas, sem zeros sobrando (1.234,5 / 0,00001234)
        NumeroBR.formatarFlexivel(r, 0, 8)
    } catch (e: Exception) {
        "Erro: ${e.message}"
    }
}

// ============================================================
// CALCULOS DE INVESTIMENTO - VALOR INVESTE
// ============================================================

data class ParametrosInvestimento(
    val inicial: Double,
    val aporteMensal: Double,
    val meses: Int,
    val selicAA: Double,
    val cdiAA: Double,
    val ipcaAA: Double,
    val trAM: Double,
    val tesouroPreAA: Double,
    val custodiaB3AA: Double,
    val tesouroIpcaAA: Double,
    val admFundoDiAA: Double,
    val rentCdbPctCdi: Double,
    val rentFundoDiPctCdi: Double,
    val rentLciLcaPctCdi: Double,
    val taxaPoupancaAM: Double
)

data class ResultadoAtivo(
    val nome: String,
    val bruto: Double,
    val custos: Double,
    val ir: Double,
    val liquido: Double,
    val rentLiquidaAA: Double
)

private fun aaParaAm(aa: Double): Double = (1.0 + aa / 100.0).pow(1.0 / 12.0) - 1.0

private fun aliquotaIrPorDias(dias: Int): Double = when {
    dias <= 180 -> 0.225
    dias <= 360 -> 0.20
    dias <= 720 -> 0.175
    else -> 0.15
}

private fun simularMensal(
    inicial: Double,
    aporte: Double,
    meses: Int,
    rendAoMes: Double,
    custoAnual: Double = 0.0
): Double {
    val custoMes = custoAnual / 12.0
    var saldo = inicial
    repeat(meses) {
        saldo *= (1.0 + rendAoMes - custoMes)
        saldo += aporte
    }
    return saldo
}

private fun aplicarIr(bruto: Double, totalInvestido: Double, meses: Int, isento: Boolean): Pair<Double, Double> {
    if (isento) return 0.0 to bruto
    val lucro = (bruto - totalInvestido).coerceAtLeast(0.0)
    val ir = lucro * aliquotaIrPorDias(meses * 30)
    return ir to (bruto - ir)
}

fun calcularInvestimentos(p: ParametrosInvestimento): List<ResultadoAtivo> {
    val meses = p.meses.coerceAtLeast(1)
    val total = p.inicial + p.aporteMensal * meses
    val cdiMes = aaParaAm(p.cdiAA)
    val selicMes = aaParaAm(p.selicAA)

    // Tesouro Selic: Selic - custodia
    val tsBruto = simularMensal(p.inicial, p.aporteMensal, meses, selicMes, p.custodiaB3AA / 100)
    val tsCustos = tsBruto - simularMensal(p.inicial, p.aporteMensal, meses, selicMes)
    val (tsIr, tsLiq) = aplicarIr(tsBruto, total, meses, isento = false)

    // Tesouro Prefixado
    val tpMes = aaParaAm(p.tesouroPreAA)
    val tpBruto = simularMensal(p.inicial, p.aporteMensal, meses, tpMes, p.custodiaB3AA / 100)
    val tpCustos = tpBruto - simularMensal(p.inicial, p.aporteMensal, meses, tpMes)
    val (tpIr, tpLiq) = aplicarIr(tpBruto, total, meses, isento = false)

    // Tesouro IPCA+ (juro real + inflacao)
    val tipcaMes = aaParaAm(((1 + p.tesouroIpcaAA / 100) * (1 + p.ipcaAA / 100) - 1) * 100)
    val tipcaBruto = simularMensal(p.inicial, p.aporteMensal, meses, tipcaMes, p.custodiaB3AA / 100)
    val tipcaCustos = tipcaBruto - simularMensal(p.inicial, p.aporteMensal, meses, tipcaMes)
    val (tipcaIr, tipcaLiq) = aplicarIr(tipcaBruto, total, meses, isento = false)

    // CDB % CDI
    val cdbMes = cdiMes * (p.rentCdbPctCdi / 100)
    val cdbBruto = simularMensal(p.inicial, p.aporteMensal, meses, cdbMes)
    val (cdbIr, cdbLiq) = aplicarIr(cdbBruto, total, meses, isento = false)

    // LCI / LCA - isento de IR
    val lciMes = cdiMes * (p.rentLciLcaPctCdi / 100)
    val lciBruto = simularMensal(p.inicial, p.aporteMensal, meses, lciMes)
    val (lciIr, lciLiq) = aplicarIr(lciBruto, total, meses, isento = true)

    // Fundo DI - desconto de taxa adm + IR
    val fdiMes = cdiMes * (p.rentFundoDiPctCdi / 100)
    val fdiBruto = simularMensal(p.inicial, p.aporteMensal, meses, fdiMes, p.admFundoDiAA / 100)
    val fdiCustos = fdiBruto - simularMensal(p.inicial, p.aporteMensal, meses, fdiMes)
    val (fdiIr, fdiLiq) = aplicarIr(fdiBruto, total, meses, isento = false)

    // Poupanca - isento
    val poupMes = p.taxaPoupancaAM / 100 + p.trAM / 100
    val poupBruto = simularMensal(p.inicial, p.aporteMensal, meses, poupMes)
    val (poupIr, poupLiq) = aplicarIr(poupBruto, total, meses, isento = true)

    fun rentAA(liquido: Double, investido: Double, m: Int): Double {
        if (investido <= 0 || m <= 0) return 0.0
        val razao = liquido / investido
        return (razao.pow(12.0 / m) - 1) * 100
    }

    return listOf(
        ResultadoAtivo("Tesouro Selic", tsBruto, tsCustos, tsIr, tsLiq, rentAA(tsLiq, total, meses)),
        ResultadoAtivo("Tesouro Prefixado", tpBruto, tpCustos, tpIr, tpLiq, rentAA(tpLiq, total, meses)),
        ResultadoAtivo("Tesouro IPCA+", tipcaBruto, tipcaCustos, tipcaIr, tipcaLiq, rentAA(tipcaLiq, total, meses)),
        ResultadoAtivo("CDB (${p.rentCdbPctCdi.emNumeroBR(0)}% CDI)", cdbBruto, 0.0, cdbIr, cdbLiq, rentAA(cdbLiq, total, meses)),
        ResultadoAtivo("LCI/LCA (${p.rentLciLcaPctCdi.emNumeroBR(0)}% CDI)", lciBruto, 0.0, lciIr, lciLiq, rentAA(lciLiq, total, meses)),
        ResultadoAtivo("Fundo DI (${p.rentFundoDiPctCdi.emNumeroBR(0)}% CDI)", fdiBruto, fdiCustos, fdiIr, fdiLiq, rentAA(fdiLiq, total, meses)),
        ResultadoAtivo("Poupança", poupBruto, 0.0, poupIr, poupLiq, rentAA(poupLiq, total, meses))
    ).sortedByDescending { it.liquido }
}

// ============================================================
// UI PRINCIPAL
// ============================================================

enum class AbaCalculadora(val rotulo: String) {
    CONVERSAO("Conversão"),
    FINANCEIRA("Financeira"),
    CIENTIFICA("Científica"),
    INVESTIMENTOS("Investimentos"),
    PRECO_VENDA("Markup"),
    ENDIVIDAMENTO("Empréstimos"),
    HISTORICO("Histórico")
}

@Composable
fun TelaCalculadora(
    modifier: Modifier = Modifier,
    cacheVm: CalcCacheViewModel = viewModel()
) {
    val ocultas by cacheVm.ocultas.collectAsState()
    val abasVisiveis = AbaCalculadora.entries.filter { it.name !in ocultas }
    val abaInicial = abasVisiveis.firstOrNull() ?: AbaCalculadora.CONVERSAO
    var aba by remember { mutableStateOf(abaInicial) }
    
    val historicoAbas = remember { mutableStateListOf<AbaCalculadora>() }

    BackHandler(enabled = historicoAbas.isNotEmpty()) {
        val abaAnterior = historicoAbas.removeLast()
        if (abaAnterior in abasVisiveis) {
            aba = abaAnterior
        } else {
            // Se a aba anterior estiver oculta, volta pra primeira visível e limpa o histórico
            aba = abasVisiveis.firstOrNull() ?: AbaCalculadora.CONVERSAO
            historicoAbas.clear()
        }
    }

    // Se aba ativa virou oculta, troca para primeira visível
    LaunchedEffect(ocultas) {
        if (aba.name in ocultas && abasVisiveis.isNotEmpty()) {
            aba = abasVisiveis.first()
            historicoAbas.clear()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Text(
            text = "Calculadora",
            color = TextoForte,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
        )

        if (abasVisiveis.isEmpty()) {
            Text("Todas as calculadoras estão ocultas. Ative em Configurações.",
                color = GrayText, fontSize = 13.sp,
                modifier = Modifier.padding(20.dp))
            return@Column
        }

        val idx = abasVisiveis.indexOf(aba).coerceAtLeast(0)
        ScrollableTabRow(
            selectedTabIndex = idx,
            containerColor = DarkBg,
            contentColor = TextoForte,
            edgePadding = 0.dp
        ) {
            abasVisiveis.forEach { a ->
                Tab(
                    selected = a == aba,
                    onClick = { 
                        if (aba != a) {
                            historicoAbas.add(aba)
                            aba = a 
                        }
                    },
                    text = { Text(a.rotulo, fontSize = 13.sp, maxLines = 1) }
                )
            }
        }

        when (aba) {
            AbaCalculadora.CONVERSAO    -> BlocoConversao()
            AbaCalculadora.FINANCEIRA   -> BlocoFinanceira(cacheVm)
            AbaCalculadora.CIENTIFICA   -> BlocoCientifica(cacheVm)
            AbaCalculadora.INVESTIMENTOS -> BlocoInvestimentos(cacheVm)
            AbaCalculadora.PRECO_VENDA  -> BlocoPrecoVenda(cacheVm)
            AbaCalculadora.ENDIVIDAMENTO -> BlocoEndividamento(cacheVm)
            AbaCalculadora.HISTORICO    -> BlocoHistorico(cacheVm)
        }
    }
}

// ============================================================
// ABA 1 - CONVERSAO DE MOEDAS
// ============================================================

@Composable
private fun BlocoConversao(vm: MoedasViewModel = viewModel()) {
    val lista by vm.cotacoes.collectAsState()
    val carregando by vm.carregando.collectAsState()
    val erro by vm.erro.collectAsState()

    var valor by remember { mutableStateOf("1") }
    var origemCodigo by remember { mutableStateOf("BRL") }
    var destinoCodigo by remember { mutableStateOf("USD") }
    var menuOrigem by remember { mutableStateOf(false) }
    var menuDestino by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Cotações em tempo real", color = TextoForte, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(
                onClick = { vm.atualizar() },
                colors = ButtonDefaults.buttonColors(containerColor = GojoPurple)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Atualizar")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (erro != null) TextoErro(erro!!)
        if (carregando && lista.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GojoPurple)
            }
        }

        // Conversor
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                CampoNumerico("Valor", valor) { valor = it }
                Spacer(Modifier.height(8.dp))

                val moedas = buildList {
                    add("BRL" to "Real Brasileiro")
                    lista.forEach { add(it.codigo to it.nome) }
                }
                Text("De:", color = GrayText, fontSize = 12.sp)
                SeletorMoeda(origemCodigo, moedas, menuOrigem, { menuOrigem = it }) { origemCodigo = it }

                Spacer(Modifier.height(8.dp))
                Text("Para:", color = GrayText, fontSize = 12.sp)
                SeletorMoeda(destinoCodigo, moedas, menuDestino, { menuDestino = it }) { destinoCodigo = it }

                Spacer(Modifier.height(12.dp))
                val v = valor.lerNumeroBR() ?: 0.0
                val resultado = converter(v, origemCodigo, destinoCodigo, lista)
                Text(
                    text = "Resultado: ${formatarNumero(resultado)} $destinoCodigo",
                    color = MoneyGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Cotações (x1 = BRL)", color = GrayText, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        lista.forEach { m ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${m.codigo}  -  ${m.nome}", color = TextoForte, fontWeight = FontWeight.Bold)
                        Text("Variação 24h: ${m.variacaoPct.emNumeroBR(2)}%",
                            color = if (m.variacaoPct >= 0) MoneyGreen else DebtRed, fontSize = 12.sp)
                    }
                    Text("R$ ${NumeroBR.formatarFlexivel(m.valorEmReais, 2, 4)}", color = TextoForte, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun converter(valor: Double, de: String, para: String, cotacoes: List<MoedaInfo>): Double {
    if (de == para) return valor
    val emBrl = if (de == "BRL") valor else valor * (cotacoes.find { it.codigo == de }?.valorEmReais ?: 0.0)
    if (para == "BRL") return emBrl
    val destino = cotacoes.find { it.codigo == para }?.valorEmReais ?: return 0.0
    if (destino == 0.0) return 0.0
    return emBrl / destino
}

@Composable
private fun SeletorMoeda(
    selecionada: String,
    moedas: List<Pair<String, String>>,
    aberto: Boolean,
    mudarAberto: (Boolean) -> Unit,
    aoSelecionar: (String) -> Unit
) {
    Box {
        Button(
            onClick = { mudarAberto(true) },
            colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selecionada, color = TextoForte, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextoForte)
        }
        DropdownMenu(
            expanded = aberto,
            onDismissRequest = { mudarAberto(false) }
        ) {
            moedas.forEach { (cod, nome) ->
                DropdownMenuItem(
                    text = { Text("$cod - $nome") },
                    onClick = { aoSelecionar(cod); mudarAberto(false) }
                )
            }
        }
    }
}

// ============================================================
// ABA 2 - RENDA FIXA (VALOR INVESTE)
// ============================================================

@Composable
private fun BlocoFinanceira(cacheVm: CalcCacheViewModel) {
    var inicial by remember { mutableStateOf("1000") }
    var aporte by remember { mutableStateOf("300") }
    var meses by remember { mutableStateOf("24") }
    var selic by remember { mutableStateOf("14.65") }
    var cdi by remember { mutableStateOf("14.65") }
    var ipca by remember { mutableStateOf("4.0536") }
    var tr by remember { mutableStateOf("0.1667") }
    var tesPre by remember { mutableStateOf("14.0") }
    var custodia by remember { mutableStateOf("0.2") }
    var tesIpca by remember { mutableStateOf("6.5") }
    var admFdi by remember { mutableStateOf("0.25") }
    var rentCdb by remember { mutableStateOf("100") }
    var rentFdi by remember { mutableStateOf("98.17") }
    var rentLci by remember { mutableStateOf("85") }
    var poup by remember { mutableStateOf("0.6675") }
    var resultados by remember { mutableStateOf<List<ResultadoAtivo>>(emptyList()) }
    var totalInv by remember { mutableStateOf(0.0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Parâmetros básicos", color = TextoForte, fontWeight = FontWeight.Bold)
        CampoNumerico("Investimento inicial (R$)", inicial) { inicial = it }
        CampoNumerico("Aporte mensal (R$)", aporte) { aporte = it }
        CampoNumerico("Período (meses)", meses) { meses = it }

        Spacer(Modifier.height(8.dp))
        Text("Índices de mercado", color = TextoForte, fontWeight = FontWeight.Bold)
        CampoNumerico("Selic efetiva a.a. (%)", selic) { selic = it }
        CampoNumerico("CDI a.a. (%)", cdi) { cdi = it }
        CampoNumerico("IPCA a.a. (%)", ipca) { ipca = it }
        CampoNumerico("TR a.m. (%)", tr) { tr = it }

        Spacer(Modifier.height(8.dp))
        Text("Tesouro Direto", color = TextoForte, fontWeight = FontWeight.Bold)
        CampoNumerico("Tesouro Prefixado a.a. (%)", tesPre) { tesPre = it }
        CampoNumerico("Taxa de custódia B3 a.a. (%)", custodia) { custodia = it }
        CampoNumerico("Tesouro IPCA+ a.a. (%)", tesIpca) { tesIpca = it }

        Spacer(Modifier.height(8.dp))
        Text("Outros ativos", color = TextoForte, fontWeight = FontWeight.Bold)
        CampoNumerico("Taxa de administração Fundo DI a.a. (%)", admFdi) { admFdi = it }
        CampoNumerico("Rentabilidade CDB (% do CDI)", rentCdb) { rentCdb = it }
        CampoNumerico("Rentabilidade Fundo DI (% do CDI)", rentFdi) { rentFdi = it }
        CampoNumerico("Rentabilidade LCI/LCA (% do CDI)", rentLci) { rentLci = it }
        CampoNumerico("Rentabilidade Poupança a.m. (%)", poup) { poup = it }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val m = meses.toIntOrNull() ?: 0
                val p = ParametrosInvestimento(
                    inicial = inicial.lerNumeroBR() ?: 0.0,
                    aporteMensal = aporte.lerNumeroBR() ?: 0.0,
                    meses = m,
                    selicAA = selic.lerNumeroBR() ?: 0.0,
                    cdiAA = cdi.lerNumeroBR() ?: 0.0,
                    ipcaAA = ipca.lerNumeroBR() ?: 0.0,
                    trAM = tr.lerNumeroBR() ?: 0.0,
                    tesouroPreAA = tesPre.lerNumeroBR() ?: 0.0,
                    custodiaB3AA = custodia.lerNumeroBR() ?: 0.0,
                    tesouroIpcaAA = tesIpca.lerNumeroBR() ?: 0.0,
                    admFundoDiAA = admFdi.lerNumeroBR() ?: 0.0,
                    rentCdbPctCdi = rentCdb.lerNumeroBR() ?: 0.0,
                    rentFundoDiPctCdi = rentFdi.lerNumeroBR() ?: 0.0,
                    rentLciLcaPctCdi = rentLci.lerNumeroBR() ?: 0.0,
                    taxaPoupancaAM = poup.lerNumeroBR() ?: 0.0
                )
                resultados = calcularInvestimentos(p)
                totalInv = p.inicial + p.aporteMensal * m
            },
            colors = ButtonDefaults.buttonColors(containerColor = GojoPurple),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Calcular simulação") }

        Spacer(Modifier.height(16.dp))
        if (resultados.isNotEmpty()) {
            Text("Total investido: ${formatarBrl(totalInv)}", color = TextoForte, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            resultados.forEachIndexed { idx, r ->
                CardResultadoAtivo(r, idx == 0)
            }
            Spacer(Modifier.height(8.dp))
            BotaoSalvarCalc {
                val melhor = resultados.first()
                val det = buildString {
                    append("Total investido: ${formatarBrl(totalInv)}\n")
                    resultados.forEach {
                        append("${it.nome}: líquido ${formatarBrl(it.liquido)} (${it.rentLiquidaAA.emNumeroBR(2)}% a.a.)\n")
                    }
                }
                cacheVm.salvar("Financeira", "Melhor: ${melhor.nome}", det.trim())
            }
        }
    }
}

@Composable
private fun CardResultadoAtivo(r: ResultadoAtivo, destaque: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = if (destaque) GojoPurple.copy(alpha = 0.25f) else CardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(r.nome, color = TextoForte, fontWeight = FontWeight.Bold)
            Text("Líquido: ${formatarBrl(r.liquido)}", color = MoneyGreen, fontWeight = FontWeight.Bold)
            Text("Bruto: ${formatarBrl(r.bruto)}", color = GrayText, fontSize = 12.sp)
            Text("Custos: ${formatarBrl(r.custos)}  |  IR: ${formatarBrl(r.ir)}", color = GrayText, fontSize = 12.sp)
            Text("Rent. líquida: ${r.rentLiquidaAA.emNumeroBR(2)}% a.a.", color = TextoForte, fontSize = 12.sp)
        }
    }
}

// ============================================================
// ABA 3 - CIENTIFICA
// ============================================================

@Composable
private fun BlocoCientifica(cacheVm: CalcCacheViewModel) {
    var expressao by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf("0") }
    var memoria by remember { mutableStateOf(0.0) }

    val teclas = listOf(
        listOf("MC", "MR", "M+", "M-", "C"),
        listOf("sin(", "cos(", "tan(", "ln(", "log("),
        listOf("sqrt(", "^", "pi", "e", "!"),
        listOf("7", "8", "9", "/", "("),
        listOf("4", "5", "6", "*", ")"),
        listOf("1", "2", "3", "-", "<-"),
        listOf("0", ",", "=", "+", "abs(")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (memoria != 0.0) MoneyGreen.copy(alpha = 0.5f) else Color.Transparent)
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (memoria != 0.0) "M = ${NumeroBR.formatarFlexivel(memoria, 0, 8)}" else " ", color = MoneyGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(expressao.ifEmpty { " " }, color = GrayText, fontSize = 14.sp, maxLines = 2, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
                Text(resultado, color = TextoForte, fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                
                if (expressao.isNotEmpty() && !resultado.startsWith("Erro") && resultado != "0") {
                    Spacer(Modifier.height(6.dp))
                    BotaoSalvarCalc {
                        cacheVm.salvar("Científica", "$expressao = $resultado", "$expressao = $resultado")
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        teclas.forEach { linha ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                linha.forEach { tecla ->
                    Button(
                        onClick = {
                            when (tecla) {
                                "=" -> resultado = avaliarCientifica(expressao)
                                "C" -> { expressao = ""; resultado = "0" }
                                "<-" -> if (expressao.isNotEmpty()) expressao = expressao.dropLast(1)
                                "MC" -> memoria = 0.0
                                "MR" -> expressao += NumeroBR.formatarFlexivel(memoria, 0, 8).replace(".", "")
                                "M+" -> {
                                    val r = resultado.lerNumeroBR()
                                    if (r != null) memoria += r
                                }
                                "M-" -> {
                                    val r = resultado.lerNumeroBR()
                                    if (r != null) memoria -= r
                                }
                                "!" -> {
                                    val n = expressao.takeLastWhile { it.isDigit() }.toIntOrNull()
                                    if (n != null && n in 0..20) {
                                        val f = (1..n).fold(1L) { acc, i -> acc * i }
                                        expressao = expressao.dropLast(n.toString().length) + f.toString()
                                    }
                                }
                                else -> expressao += tecla
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (tecla) {
                                "=" -> MoneyGreen
                                "C", "<-", "MC" -> DebtRed
                                "MR", "M+", "M-" -> GojoPurple.copy(alpha = 0.8f)
                                in listOf("+", "-", "*", "/", "^") -> GojoPurple
                                else -> CardBg
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        if (tecla == "<-") Icon(Icons.Default.Backspace, contentDescription = null, tint = TextoForte, modifier = Modifier.size(18.dp))
                        else Text(tecla, color = TextoForte, fontSize = 13.sp, fontWeight = if (tecla in listOf("=", "M+", "M-", "MR", "MC")) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

// ============================================================
// ABA 4 - ROI / INVESTIMENTOS
// ============================================================

@Composable
private fun BlocoInvestimentos(cacheVm: CalcCacheViewModel) {
    var investidoInicial by remember { mutableStateOf("10000.00") }
    var fluxoAnual1 by remember { mutableStateOf("3000.00") }
    var fluxoAnual2 by remember { mutableStateOf("4000.00") }
    var fluxoAnual3 by remember { mutableStateOf("5000.00") }
    var taxaDesconto by remember { mutableStateOf("10.0") }

    val p = investidoInicial.lerNumeroBR() ?: 0.0
    val f1 = fluxoAnual1.lerNumeroBR() ?: 0.0
    val f2 = fluxoAnual2.lerNumeroBR() ?: 0.0
    val f3 = fluxoAnual3.lerNumeroBR() ?: 0.0
    val taxaD = (taxaDesconto.lerNumeroBR() ?: 0.0) / 100.0

    val fluxos = listOf(-p, f1, f2, f3)
    
    // Cálculo VPL
    var vpl = 0.0
    fluxos.forEachIndexed { t, fluxo ->
        vpl += fluxo / (1.0 + taxaD).pow(t.toDouble())
    }

    // Cálculo TIR (aproximação simples de Newton-Raphson)
    var tir = 0.10
    var iter = 0
    var foundTir = false
    while (iter < 100) {
        var vplTir = 0.0
        var derivVplTir = 0.0
        for (t in fluxos.indices) {
            vplTir += fluxos[t] / (1.0 + tir).pow(t.toDouble())
            if (t > 0) derivVplTir -= t * fluxos[t] / (1.0 + tir).pow(t + 1.0)
        }
        if (kotlin.math.abs(vplTir) < 1e-5) { foundTir = true; break }
        if (derivVplTir == 0.0) break
        tir -= vplTir / derivVplTir
        iter++
    }

    // Payback Simples
    var saldoPB = -p
    var payback = 0.0
    for (t in 1..3) {
        saldoPB += fluxos[t]
        if (saldoPB >= 0) {
            payback = t - (saldoPB / fluxos[t])
            break
        }
    }
    
    val roiBruto = if (p > 0) ((f1+f2+f3) - p) / p * 100 else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Simulador de Investimentos", color = MoneyGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Simule o retorno financeiro de um projeto ou novo negócio ao longo de 3 anos.", color = GrayText, fontSize = 12.sp)

        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DADOS DO PROJETO", color = TextoForte, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                CampoNumerico("Investimento Inicial (R$)", investidoInicial) { investidoInicial = it }
                CampoNumerico("Rendimento Esperado (% a.a.)", taxaDesconto) { taxaDesconto = it }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("LUCRO ESTIMADO POR ANO (R$)", color = TextoForte, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                CampoNumerico("Ano 1", fluxoAnual1) { fluxoAnual1 = it }
                CampoNumerico("Ano 2", fluxoAnual2) { fluxoAnual2 = it }
                CampoNumerico("Ano 3", fluxoAnual3) { fluxoAnual3 = it }
            }
        }

        if (p > 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MoneyGreen.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MoneyGreen.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LUCRO REAL ESPERADO", color = GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(formatarBrl(vpl), color = if (vpl >= 0) MoneyGreen else DebtRed, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text(if (vpl >= 0) "Vale a pena investir!" else "O rendimento será menor que o esperado.", color = GrayText, fontSize = 12.sp)

                    Divider(color = GrayText.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                    LinhaResultado("Rentabilidade Anual Média", if (foundTir && tir > -1) "${(tir * 100).emNumeroBR(2)}% a.a." else "N/A", cor = if (tir > taxaD) MoneyGreen else DebtRed)
                    LinhaResultado("Retorno Total do Período", "${roiBruto.emNumeroBR(2)}%")
                    LinhaResultado("Tempo para recuperar dinheiro", if (payback > 0) "${payback.emNumeroBR(1)} anos" else "Não se paga no período")
                }
            }

            BotaoSalvarCalc {
                val det = """
                    Investimento: ${formatarBrl(p)}
                    Lucro Previsto: ${formatarBrl(f1)}, ${formatarBrl(f2)}, ${formatarBrl(f3)}
                    Rendimento Esperado: $taxaDesconto% a.a.
                    Lucro Real Esperado: ${formatarBrl(vpl)}
                    Rentabilidade Anual: ${if (foundTir) "${(tir * 100).emNumeroBR(2)}%" else "N/A"}
                    Tempo de Retorno: ${payback.emNumeroBR(1)} anos
                """.trimIndent()
                cacheVm.salvar("Investimentos", "Lucro Real: ${formatarBrl(vpl)}", det)
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}
@Composable
private fun LinhaResultado(rotulo: String, valor: String, cor: Color = TextoForte) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(rotulo, color = GrayText, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(valor, color = cor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ============================================================
// ABA 5 - MARKUP
// ============================================================

@Composable
private fun BlocoPrecoVenda(cacheVm: CalcCacheViewModel) {
    var custoAquisicao by remember { mutableStateOf("100.00") }
    var freteSeguro by remember { mutableStateOf("0.00") }
    var impostosPerc by remember { mutableStateOf("4.00") }
    var comissoesPerc by remember { mutableStateOf("0.00") }
    var taxasCartaoPerc by remember { mutableStateOf("2.00") }
    var despesasFixasPerc by remember { mutableStateOf("20.00") }
    var margemLucroPerc by remember { mutableStateOf("10.00") }
    var descontoPerc by remember { mutableStateOf("10.00") }

    val c = custoAquisicao.lerNumeroBR() ?: 0.0
    val f = freteSeguro.lerNumeroBR() ?: 0.0
    val imp = (impostosPerc.lerNumeroBR() ?: 0.0) / 100.0
    val com = (comissoesPerc.lerNumeroBR() ?: 0.0) / 100.0
    val cart = (taxasCartaoPerc.lerNumeroBR() ?: 0.0) / 100.0
    val df = (despesasFixasPerc.lerNumeroBR() ?: 0.0) / 100.0
    val ml = (margemLucroPerc.lerNumeroBR() ?: 0.0) / 100.0
    val desc = (descontoPerc.lerNumeroBR() ?: 0.0) / 100.0

    val custoBase = c + f
    val deducoesVenda = imp + com + cart
    val totalIndices = deducoesVenda + df + ml
    
    val markupDivisor = (1.0 - desc) * (1.0 - totalIndices)
    val markupMultiplicador = if (markupDivisor > 0) 1.0 / markupDivisor else 0.0

    val precoVendaSemDesconto = if (markupDivisor > 0) custoBase / markupDivisor else 0.0
    val precoVendaComDesconto = precoVendaSemDesconto * (1.0 - desc)
    val valorLucro = precoVendaSemDesconto * ml
    
    val receitaLiquida = precoVendaComDesconto
    val despesasVariaveis = precoVendaSemDesconto * deducoesVenda
    val margemContribuicao = receitaLiquida - custoBase - despesasVariaveis
    val qtdBreakEven = if (margemContribuicao > 0) (precoVendaSemDesconto * df) / margemContribuicao else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Formação de Preço Corporativa", color = GojoPurple, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Análise detalhada de composição de preço, margem de contribuição e ponto de equilíbrio.", color = GrayText, fontSize = 12.sp)

        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CUSTOS DIRETOS (R$)", color = TextoForte, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { CampoNumerico("Produto/Serviço", custoAquisicao) { custoAquisicao = it } }
                    Box(Modifier.weight(1f)) { CampoNumerico("Frete/Outros", freteSeguro) { freteSeguro = it } }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DEDUÇÕES E DESPESAS (%)", color = TextoForte, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { CampoNumerico("Impostos", impostosPerc) { impostosPerc = it } }
                    Box(Modifier.weight(1f)) { CampoNumerico("Comissões", comissoesPerc) { comissoesPerc = it } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { CampoNumerico("Taxa Cartão", taxasCartaoPerc) { taxasCartaoPerc = it } }
                    Box(Modifier.weight(1f)) { CampoNumerico("Despesas Fixas", despesasFixasPerc) { despesasFixasPerc = it } }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MARGEM E DESCONTO (%)", color = TextoForte, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { CampoNumerico("Margem Lucro", margemLucroPerc) { margemLucroPerc = it } }
                    Box(Modifier.weight(1f)) { CampoNumerico("Desconto na Venda", descontoPerc) { descontoPerc = it } }
                }
            }
        }

        if (desc >= 1.0 || totalIndices >= 1.0) {
            Text("ERRO: Os percentuais de custo ou desconto excedem ou igualam 100%. Impossível precificar.", color = DebtRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        } else if (precoVendaSemDesconto > 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GojoPurple.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GojoPurple.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("R$ VENDA S/ DESCONTO", color = GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(formatarBrl(precoVendaSemDesconto), color = TextoForte, fontSize = 32.sp, fontWeight = FontWeight.Black)

                    if (desc > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text("R$ VENDA C/ DESCONTO", color = GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(formatarBrl(precoVendaComDesconto), color = MoneyGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    Divider(color = GrayText.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                    LinhaResultado("Lucro Líquido Unitário", formatarBrl(valorLucro), cor = MoneyGreen)
                    LinhaResultado("Margem de Contribuição", formatarBrl(margemContribuicao))
                    LinhaResultado("Markup Multiplicador", "${markupMultiplicador.emNumeroBR(4)}x")
                    LinhaResultado("Markup Divisor", "${markupDivisor.emNumeroBR(4)}")
                    LinhaResultado("Ponto de Equilíbrio", "${kotlin.math.ceil(qtdBreakEven).toInt()} un / mês", cor = GojoPurple)
                }
            }

            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Composição do Preço", color = TextoForte, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Row(Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp))) {
                    Box(Modifier.weight((custoBase/precoVendaSemDesconto).toFloat()).fillMaxSize().background(Color(0xFF4A90E2)))
                    if (deducoesVenda > 0) Box(Modifier.weight(deducoesVenda.toFloat()).fillMaxSize().background(Color(0xFFE2A04A)))
                    if (df > 0) Box(Modifier.weight(df.toFloat()).fillMaxSize().background(Color(0xFFE24A4A)))
                    if (ml > 0) Box(Modifier.weight(ml.toFloat()).fillMaxSize().background(MoneyGreen))
                    if (desc > 0) Box(Modifier.weight(desc.toFloat()).fillMaxSize().background(DebtRed))
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Custo", color = Color(0xFF4A90E2), fontSize = 10.sp)
                    Text("Vendas", color = Color(0xFFE2A04A), fontSize = 10.sp)
                    Text("Fixo", color = Color(0xFFE24A4A), fontSize = 10.sp)
                    Text("Lucro", color = MoneyGreen, fontSize = 10.sp)
                    if (desc > 0) Text("Desc", color = DebtRed, fontSize = 10.sp)
                }
            }

            BotaoSalvarCalc {
                val det = """
                    Custo Total: ${formatarBrl(custoBase)}
                    Venda S/ Desconto: ${formatarBrl(precoVendaSemDesconto)}
                    Venda C/ Desconto: ${formatarBrl(precoVendaComDesconto)}
                    Lucro Unitário: ${formatarBrl(valorLucro)}
                    Markup Mult: ${markupMultiplicador.emNumeroBR(3)}x
                """.trimIndent()
                cacheVm.salvar("Markup", "Venda ${formatarBrl(if (desc > 0) precoVendaComDesconto else precoVendaSemDesconto)}", det)
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

// ============================================================
// ABA 5 - SIMULADOR ENDIVIDAMENTO E DIVIDA
// ============================================================

enum class SistemaAmortizacao { PRICE, SAC }

@Composable
private fun BlocoEndividamento(cacheVm: CalcCacheViewModel) {
    var valorPrincipal by remember { mutableStateOf("150000.00") }
    var taxaAnual by remember { mutableStateOf("11.5") }
    var prazoMeses by remember { mutableStateOf("120") }
    var sistema by remember { mutableStateOf(SistemaAmortizacao.SAC) }

    val p = valorPrincipal.lerNumeroBR() ?: 0.0
    val iAno = (taxaAnual.lerNumeroBR() ?: 0.0) / 100.0
    val n = prazoMeses.toIntOrNull() ?: 0

    val iMes = (1.0 + iAno).pow(1.0 / 12.0) - 1.0

    var totalPago = 0.0
    var totalJuros = 0.0
    var primeiraParcela = 0.0
    var ultimaParcela = 0.0

    if (p > 0 && n > 0 && iMes > 0) {
        var saldo = p
        when (sistema) {
            SistemaAmortizacao.SAC -> {
                val amortizacao = p / n
                for (mes in 1..n) {
                    val juros = saldo * iMes
                    val parcela = amortizacao + juros
                    saldo -= amortizacao
                    totalJuros += juros
                    totalPago += parcela
                    if (mes == 1) primeiraParcela = parcela
                    if (mes == n) ultimaParcela = parcela
                }
            }
            SistemaAmortizacao.PRICE -> {
                val parcelaPrice = p * (iMes * (1 + iMes).pow(n.toDouble())) / ((1 + iMes).pow(n.toDouble()) - 1)
                primeiraParcela = parcelaPrice
                ultimaParcela = parcelaPrice
                for (mes in 1..n) {
                    val juros = saldo * iMes
                    val amortizacao = parcelaPrice - juros
                    saldo -= amortizacao
                    totalJuros += juros
                    totalPago += parcelaPrice
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Simulador de Empréstimos", color = DebtRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Descubra quanto você vai pagar de juros e veja o valor das parcelas.", color = GrayText, fontSize = 12.sp)

        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoNumerico("Valor Financiado (R$)", valorPrincipal) { valorPrincipal = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { CampoNumerico("Taxa de Juros (a.a. %)", taxaAnual) { taxaAnual = it } }
                    Box(Modifier.weight(1f)) { CampoNumerico("Prazo (Meses)", prazoMeses) { prazoMeses = it } }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { sistema = SistemaAmortizacao.SAC },
                        colors = ButtonDefaults.buttonColors(containerColor = if (sistema == SistemaAmortizacao.SAC) DebtRed else DarkBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Decrescente", color = TextoForte, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { sistema = SistemaAmortizacao.PRICE },
                        colors = ButtonDefaults.buttonColors(containerColor = if (sistema == SistemaAmortizacao.PRICE) DebtRed else DarkBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fixa", color = TextoForte, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (sistema == SistemaAmortizacao.SAC) {
                        " Parcela Decrescente: As parcelas começam mais caras e diminuem a cada mês. Você paga menos juros no total."
                    } else {
                        " Parcela Fixa: Todas as parcelas têm exatamente o mesmo valor do início ao fim. Facilita o planejamento mensal."
                    },
                    color = GrayText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        if (p > 0 && n > 0 && iMes > 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DebtRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DebtRed.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("RESUMO", color = GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Total a Pagar: ${formatarBrl(totalPago)}", color = TextoForte, fontSize = 24.sp, fontWeight = FontWeight.Black)

                    Divider(color = GrayText.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                    LinhaResultado("Total de Juros", formatarBrl(totalJuros), cor = DebtRed)
                    LinhaResultado("Primeira Parcela", formatarBrl(primeiraParcela))
                    LinhaResultado("Última Parcela", formatarBrl(ultimaParcela))
                    LinhaResultado("Proporção Juros/Principal", "${((totalJuros/p)*100).emNumeroBR(1)}%")
                }
            }
            
            BotaoSalvarCalc {
                val nomeAmortizacao = if (sistema == SistemaAmortizacao.SAC) "Decrescente (SAC)" else "Fixa (Price)"
                val det = """
                    Sistema: $nomeAmortizacao | Valor: ${formatarBrl(p)}
                    Prazo: $n meses | Taxa: $taxaAnual% a.a.
                    1ª Parcela: ${formatarBrl(primeiraParcela)} | Última: ${formatarBrl(ultimaParcela)}
                    Total em Juros: ${formatarBrl(totalJuros)}
                    Custo Total: ${formatarBrl(totalPago)}
                """.trimIndent()
                cacheVm.salvar("Financiamento", "$nomeAmortizacao: ${formatarBrl(totalPago)}", det)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ============================================================
// BLOCO HISTORICO
// ============================================================

@Composable
private fun BlocoHistorico(cacheVm: CalcCacheViewModel) {
    val entries by cacheVm.entries.collectAsState()
    val fmt = remember { java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Cálculos salvos", color = TextoForte, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.weight(1f))
            if (entries.isNotEmpty()) {
                Button(
                    onClick = { cacheVm.limparTudo() },
                    colors = ButtonDefaults.buttonColors(containerColor = DebtRed.copy(alpha = 0.7f))
                ) { Text("Limpar tudo", fontSize = 11.sp) }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                Text("Nenhum cálculo salvo ainda. Use o botão Salvar nas calculadoras.",
                    color = GrayText, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
            ) {
                items(entries) { e ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(e.tipo, color = GojoPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(e.titulo, color = TextoForte, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(fmt.format(java.util.Date(e.timestamp)), color = GrayText, fontSize = 10.sp)
                                }
                                androidx.compose.material3.TextButton(onClick = { cacheVm.remover(e.id) }) {
                                    Text("Excluir", color = DebtRed, fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(e.detalhes, color = GrayText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotaoSalvarCalc(aoSalvar: () -> Unit) {
    var salvo by remember { mutableStateOf(false) }
    LaunchedEffect(salvo) {
        if (salvo) {
            kotlinx.coroutines.delay(1500)
            salvo = false
        }
    }
    Button(
        onClick = { aoSalvar(); salvo = true },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (salvo) MoneyGreen else GojoPurple
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(if (salvo) "Salvo!" else "Salvar cálculo", color = TextoForte, fontWeight = FontWeight.Bold)
    }
}

// ============================================================
// UTILITARIOS COMPOSE
// ============================================================

@Composable
private fun CampoNumerico(rotulo: String, valor: String, aoMudar: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GojoPurple,
            unfocusedBorderColor = GrayText,
            focusedLabelColor = GojoPurple,
            unfocusedLabelColor = GrayText,
            focusedTextColor = TextoForte,
            unfocusedTextColor = TextoForte,
            cursorColor = GojoPurple
        )
    )
}

@Composable
private fun TextoErro(msg: String) {
    Text(msg, color = DebtRed, modifier = Modifier.padding(vertical = 8.dp))
}

private fun formatarBrl(v: Double): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return fmt.format(v)
}

private fun formatarNumero(v: Double): String {
    return if (kotlin.math.abs(v) >= 1000) "%,.2f".format(Locale("pt", "BR"), v)
    else "%.4f".format(Locale("pt", "BR"), v)
}
