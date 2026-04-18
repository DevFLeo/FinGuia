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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finguia.ui.theme.CardBg
import com.finguia.ui.theme.DarkBg
import com.finguia.ui.theme.DebtRed
import com.finguia.ui.theme.GojoPurple
import com.finguia.ui.theme.GrayText
import com.finguia.ui.theme.MoneyGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
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
        Retrofit.Builder()
            .baseUrl("https://economia.awesomeapi.com.br/")
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
                    MoedasRetrofit.servico.cotacoes(PARES_CAMBIO.joinToString(","))
                }
                _cotacoes.value = resp.values.map { c ->
                    MoedaInfo(
                        codigo = c.code,
                        nome = c.name.substringBefore("/").trim(),
                        valorEmReais = c.bid.toDoubleOrNull() ?: 0.0,
                        variacaoPct = c.pctChange?.toDoubleOrNull() ?: 0.0
                    )
                }.sortedBy { it.codigo }
            } catch (e: Exception) {
                _erro.value = "Falha ao buscar cotacoes. Verifique sua conexao."
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
                    if (d == 0.0) erro("divisao por zero")
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
            if (!consume(')')) erro("parentese faltando")
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
                if (!consume('(')) erro("funcao '$nome' exige '('")
                val arg = expr()
                if (!consume(')')) erro("parentese faltando apos '$nome'")
                when (nome) {
                    "sin", "sen" -> sin(arg)
                    "cos" -> cos(arg)
                    "tan", "tg" -> tan(arg)
                    "sqrt", "raiz" -> sqrt(arg)
                    "ln" -> ln(arg)
                    "log" -> log10(arg)
                    "exp" -> exp(arg)
                    "abs" -> kotlin.math.abs(arg)
                    else -> erro("funcao desconhecida: $nome")
                }
            }
        }
    }

    private fun numero(): Double {
        saltarEspaco()
        val ini = pos
        while (pos < src.length && (src[pos].isDigit() || src[pos] == '.')) pos++
        if (ini == pos) erro("numero esperado")
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
        if (r == r.toLong().toDouble() && kotlin.math.abs(r) < 1e15) r.toLong().toString()
        else "%.8f".format(Locale.US, r).trimEnd('0').trimEnd('.')
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
        ResultadoAtivo("CDB (${"%.0f".format(p.rentCdbPctCdi)}% CDI)", cdbBruto, 0.0, cdbIr, cdbLiq, rentAA(cdbLiq, total, meses)),
        ResultadoAtivo("LCI/LCA (${"%.0f".format(p.rentLciLcaPctCdi)}% CDI)", lciBruto, 0.0, lciIr, lciLiq, rentAA(lciLiq, total, meses)),
        ResultadoAtivo("Fundo DI (${"%.0f".format(p.rentFundoDiPctCdi)}% CDI)", fdiBruto, fdiCustos, fdiIr, fdiLiq, rentAA(fdiLiq, total, meses)),
        ResultadoAtivo("Poupanca", poupBruto, 0.0, poupIr, poupLiq, rentAA(poupLiq, total, meses))
    ).sortedByDescending { it.liquido }
}

// ============================================================
// UI PRINCIPAL
// ============================================================

enum class AbaCalculadora(val rotulo: String) {
    CONVERSAO("Cambio"),
    FINANCEIRA("Renda Fixa"),
    CIENTIFICA("Cientifica"),
    INVESTIMENTOS("ROI")
}

@Composable
fun TelaCalculadora(modifier: Modifier = Modifier) {
    var aba by remember { mutableStateOf(AbaCalculadora.CONVERSAO) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Text(
            text = "Calculadora",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
        )

        TabRow(
            selectedTabIndex = aba.ordinal,
            containerColor = DarkBg,
            contentColor = Color.White
        ) {
            AbaCalculadora.entries.forEach { a ->
                Tab(
                    selected = a == aba,
                    onClick = { aba = a },
                    text = { Text(a.rotulo, fontSize = 12.sp) }
                )
            }
        }

        when (aba) {
            AbaCalculadora.CONVERSAO -> BlocoConversao()
            AbaCalculadora.FINANCEIRA -> BlocoFinanceira()
            AbaCalculadora.CIENTIFICA -> BlocoCientifica()
            AbaCalculadora.INVESTIMENTOS -> BlocoInvestimentos()
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
            Text("Cotacoes em tempo real", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
                val v = valor.replace(",", ".").toDoubleOrNull() ?: 0.0
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
        Text("Cotacoes (x1 = BRL)", color = GrayText, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        lista.forEach { m ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${m.codigo}  -  ${m.nome}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Variacao 24h: ${"%.2f".format(m.variacaoPct)}%",
                            color = if (m.variacaoPct >= 0) MoneyGreen else DebtRed, fontSize = 12.sp)
                    }
                    Text("R$ ${"%.4f".format(m.valorEmReais)}", color = Color.White, fontWeight = FontWeight.Bold)
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
            Text(selecionada, color = Color.White, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
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
private fun BlocoFinanceira() {
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
        Text("Parametros basicos", color = Color.White, fontWeight = FontWeight.Bold)
        CampoNumerico("Investimento inicial (R$)", inicial) { inicial = it }
        CampoNumerico("Aporte mensal (R$)", aporte) { aporte = it }
        CampoNumerico("Periodo (meses)", meses) { meses = it }

        Spacer(Modifier.height(8.dp))
        Text("Indices de mercado", color = Color.White, fontWeight = FontWeight.Bold)
        CampoNumerico("Selic efetiva a.a. (%)", selic) { selic = it }
        CampoNumerico("CDI a.a. (%)", cdi) { cdi = it }
        CampoNumerico("IPCA a.a. (%)", ipca) { ipca = it }
        CampoNumerico("TR a.m. (%)", tr) { tr = it }

        Spacer(Modifier.height(8.dp))
        Text("Tesouro Direto", color = Color.White, fontWeight = FontWeight.Bold)
        CampoNumerico("Juro nominal Tesouro Prefixado a.a. (%)", tesPre) { tesPre = it }
        CampoNumerico("Taxa de custodia B3 a.a. (%)", custodia) { custodia = it }
        CampoNumerico("Juro real Tesouro IPCA+ a.a. (%)", tesIpca) { tesIpca = it }

        Spacer(Modifier.height(8.dp))
        Text("Outros ativos", color = Color.White, fontWeight = FontWeight.Bold)
        CampoNumerico("Taxa de administracao Fundo DI a.a. (%)", admFdi) { admFdi = it }
        CampoNumerico("Rentabilidade CDB (% do CDI)", rentCdb) { rentCdb = it }
        CampoNumerico("Rentabilidade Fundo DI (% do CDI)", rentFdi) { rentFdi = it }
        CampoNumerico("Rentabilidade LCI/LCA (% do CDI)", rentLci) { rentLci = it }
        CampoNumerico("Rentabilidade Poupanca a.m. (%)", poup) { poup = it }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val m = meses.toIntOrNull() ?: 0
                val p = ParametrosInvestimento(
                    inicial = inicial.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    aporteMensal = aporte.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    meses = m,
                    selicAA = selic.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    cdiAA = cdi.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    ipcaAA = ipca.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    trAM = tr.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    tesouroPreAA = tesPre.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    custodiaB3AA = custodia.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    tesouroIpcaAA = tesIpca.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    admFundoDiAA = admFdi.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    rentCdbPctCdi = rentCdb.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    rentFundoDiPctCdi = rentFdi.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    rentLciLcaPctCdi = rentLci.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    taxaPoupancaAM = poup.replace(",", ".").toDoubleOrNull() ?: 0.0
                )
                resultados = calcularInvestimentos(p)
                totalInv = p.inicial + p.aporteMensal * m
            },
            colors = ButtonDefaults.buttonColors(containerColor = GojoPurple),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Calcular simulacao") }

        Spacer(Modifier.height(16.dp))
        if (resultados.isNotEmpty()) {
            Text("Total investido: ${formatarBrl(totalInv)}", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            resultados.forEachIndexed { idx, r ->
                CardResultadoAtivo(r, idx == 0)
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
            Text(r.nome, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Liquido: ${formatarBrl(r.liquido)}", color = MoneyGreen, fontWeight = FontWeight.Bold)
            Text("Bruto: ${formatarBrl(r.bruto)}", color = GrayText, fontSize = 12.sp)
            Text("Custos: ${formatarBrl(r.custos)}  |  IR: ${formatarBrl(r.ir)}", color = GrayText, fontSize = 12.sp)
            Text("Rent. liquida: ${"%.2f".format(r.rentLiquidaAA)}% a.a.", color = Color.White, fontSize = 12.sp)
        }
    }
}

// ============================================================
// ABA 3 - CIENTIFICA
// ============================================================

@Composable
private fun BlocoCientifica() {
    var expressao by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf("0") }

    val teclas = listOf(
        listOf("sin(", "cos(", "tan(", "ln(", "log("),
        listOf("sqrt(", "^", "pi", "e", "!"),
        listOf("7", "8", "9", "/", "("),
        listOf("4", "5", "6", "*", ")"),
        listOf("1", "2", "3", "-", "C"),
        listOf("0", ".", "=", "+", "<-")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(expressao.ifEmpty { " " }, color = GrayText, fontSize = 16.sp, maxLines = 2)
                Text(resultado, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        teclas.forEach { linha ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                linha.forEach { tecla ->
                    Button(
                        onClick = {
                            when (tecla) {
                                "=" -> resultado = avaliarCientifica(expressao)
                                "C" -> { expressao = ""; resultado = "0" }
                                "<-" -> if (expressao.isNotEmpty()) expressao = expressao.dropLast(1)
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
                                "C", "<-" -> DebtRed
                                in listOf("+", "-", "*", "/", "^") -> GojoPurple
                                else -> CardBg
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                        modifier = Modifier.weight(1f).height(54.dp)
                    ) {
                        if (tecla == "<-") Icon(Icons.Default.Backspace, contentDescription = null, tint = Color.White)
                        else Text(tecla, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ============================================================
// ABA 4 - ROI / INVESTIMENTOS
// ============================================================

@Composable
private fun BlocoInvestimentos() {
    var investido by remember { mutableStateOf("1000") }
    var aporte by remember { mutableStateOf("100") }
    var meses by remember { mutableStateOf("12") }
    var expectativaAA by remember { mutableStateOf("12.0") }

    val inv = investido.replace(",", ".").toDoubleOrNull() ?: 0.0
    val ap = aporte.replace(",", ".").toDoubleOrNull() ?: 0.0
    val m = meses.toIntOrNull() ?: 0
    val exp = expectativaAA.replace(",", ".").toDoubleOrNull() ?: 0.0
    val taxaMes = aaParaAm(exp)
    val totalInvestido = inv + ap * m
    val valorFinal = simularMensal(inv, ap, m, taxaMes)
    val ganho = valorFinal - totalInvestido
    val roi = if (totalInvestido > 0) ganho / totalInvestido * 100 else 0.0
    val taxaMesPct = taxaMes * 100

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Simulador rapido de ROI", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        CampoNumerico("Aporte inicial (R$)", investido) { investido = it }
        CampoNumerico("Aporte mensal (R$)", aporte) { aporte = it }
        CampoNumerico("Periodo (meses)", meses) { meses = it }
        CampoNumerico("Expectativa a.a. (%)", expectativaAA) { expectativaAA = it }

        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                LinhaResultado("Total investido", formatarBrl(totalInvestido))
                LinhaResultado("Valor final", formatarBrl(valorFinal))
                LinhaResultado("Ganho", formatarBrl(ganho), cor = if (ganho >= 0) MoneyGreen else DebtRed)
                LinhaResultado("ROI", "${"%.2f".format(roi)}%", cor = if (roi >= 0) MoneyGreen else DebtRed)
                LinhaResultado("Taxa equivalente a.m.", "${"%.4f".format(taxaMesPct)}%")
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Referencias (a.a., exceto quando indicado)", color = GrayText, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        val refs = listOf(
            "Selic efetiva" to "14,65% a.a.",
            "CDI" to "14,65% a.a.",
            "IPCA" to "4,05% a.a.",
            "TR" to "0,1667% a.m.",
            "Tesouro Prefixado" to "14,00% a.a.",
            "Custodia B3 (Tesouro)" to "0,20% a.a.",
            "Tesouro IPCA+ (juro real)" to "6,50% a.a.",
            "Adm. Fundo DI" to "0,25% a.a.",
            "CDB" to "100% do CDI",
            "Fundo DI" to "98,17% do CDI",
            "LCI/LCA" to "85% do CDI (isento IR)",
            "Poupanca" to "0,6675% a.m. (isento IR)"
        )
        refs.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(k, color = GrayText, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(v, color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LinhaResultado(rotulo: String, valor: String, cor: Color = Color.White) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(rotulo, color = GrayText, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(valor, color = cor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
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
