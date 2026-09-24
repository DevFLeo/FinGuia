package com.finguia.ui.investimentos

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.finguia.dados.GNewsArtigo
import com.finguia.dados.TaxasBcb
import com.finguia.ui.formato.emNumeroBR
import com.finguia.ui.theme.CardBg
import com.finguia.ui.theme.CardElevado
import com.finguia.ui.theme.DarkBg
import com.finguia.ui.theme.DestaqueTopo
import com.finguia.ui.theme.GojoPurple
import com.finguia.ui.theme.GrayText
import com.finguia.ui.theme.MoneyGreen
import com.finguia.ui.theme.TextoForte

@Composable
fun TelaDetalheRendaFixa(
    sugestao: SugestaoAtivo,
    aoVoltar: () -> Unit,
    aoComprar: () -> Unit,
    viewModel: InvestimentoViewModel
) {
    var taxas by remember { mutableStateOf<TaxasBcb?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var noticias by remember { mutableStateOf<List<GNewsArtigo>>(emptyList()) }

    LaunchedEffect(sugestao.ticker) {
        carregando = true
        taxas = viewModel.taxasBcb()
        carregando = false
        // Notícias: busca por nome do produto + termos genéricos do tipo
        val q = when {
            sugestao.ticker.startsWith("TESOURO") -> "Tesouro Direto"
            sugestao.ticker.startsWith("CDB") -> "CDB renda fixa"
            sugestao.ticker.startsWith("LCI") -> "LCI letra imobiliária"
            sugestao.ticker.startsWith("LCA") -> "LCA agronegócio"
            else -> sugestao.nome
        }
        noticias = viewModel.buscarNoticias(q)
    }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            // Header
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(DestaqueTopo, DarkBg)))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = aoVoltar) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextoForte)
                        }
                        Spacer(Modifier.width(4.dp))
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(GojoPurple.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = GojoPurple, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sugestao.nome, color = TextoForte, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text("Renda Fixa", color = GrayText, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "~${sugestao.rentabilidadeEstimadaPct.emNumeroBR(1)}% a.a. (estimado)",
                        color = MoneyGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            // Taxas BCB live
            CardSecaoRf("Indicadores oficiais (BCB)") {
                if (carregando && taxas == null) {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GojoPurple, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    }
                } else {
                    LinhaTaxa("Selic (meta a.a.)", taxas?.selic?.let { "${it.emNumeroBR(2)}%" } ?: "—")
                    LinhaTaxa("CDI (anualizado)", taxas?.cdi?.let { "${it.emNumeroBR(2)}%" } ?: "—")
                    LinhaTaxa("IPCA 12 meses", taxas?.ipca12m?.let { "${it.emNumeroBR(2)}%" } ?: "—")
                }
            }

            // Sobre o produto
            CardSecaoRf("Como funciona") {
                Text(infoProduto(sugestao.ticker), color = GrayText, fontSize = 12.sp, lineHeight = 18.sp)
            }

            // Simulação
            taxas?.let { t ->
                CardSecaoRf("Simulação para R$ 1.000 em 1 ano") {
                    val taxaEfetiva = taxaEstimada(sugestao.ticker, sugestao.rentabilidadeEstimadaPct, t)
                    val bruto = 1000.0 * (taxaEfetiva / 100.0)
                    val ir = if (sugestao.ticker.startsWith("LCI") || sugestao.ticker.startsWith("LCA")) 0.0
                            else bruto * 0.175 // alíquota média p/ 1 ano
                    val liquido = bruto - ir
                    LinhaTaxa("Taxa efetiva estimada", "${taxaEfetiva.emNumeroBR(2)}% a.a.")
                    LinhaTaxa("Rendimento bruto (1 ano)", "R$ ${bruto.emNumeroBR(2)}")
                    LinhaTaxa("IR estimado", if (ir == 0.0) "Isento" else "R$ ${ir.emNumeroBR(2)}")
                    LinhaTaxa("Rendimento líquido", "R$ ${liquido.emNumeroBR(2)}")
                }
            }

            Button(
                onClick = aoComprar,
                colors = ButtonDefaults.buttonColors(containerColor = GojoPurple),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("Adicionar à minha carteira", color = Color.White, fontWeight = FontWeight.Bold) }

            // Notícias
            CardSecaoRf("Notícias relacionadas") {
                if (noticias.isEmpty()) {
                    Text("Sem notícias agora. (Configure GNEWS_API_KEY em local.properties.)",
                        color = GrayText, fontSize = 11.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        noticias.forEach { ItemNoticiaRf(it) }
                    }
                }
            }
        }
    }
}

private fun taxaEstimada(ticker: String, fallback: Double, t: TaxasBcb): Double {
    val cdi = t.cdi ?: return fallback
    val selic = t.selic ?: cdi
    return when {
        ticker.contains("SELIC") -> selic
        ticker.contains("115CDI") -> cdi * 1.15
        ticker.contains("95CDI") -> cdi * 0.95
        ticker.contains("97CDI") -> cdi * 0.97
        ticker.contains("IPCA") -> (t.ipca12m ?: 4.0) + 6.0
        else -> fallback
    }
}

private fun infoProduto(ticker: String): String = when {
    ticker.contains("SELIC") -> "Título público pós-fixado atrelado à taxa Selic. Liquidez diária após 1 dia útil. Risco soberano (mais baixo do mercado). Tributação regressiva de IR (22,5% até 15%)."
    ticker.contains("CDB") -> "Certificado de Depósito Bancário. Banco capta seu dinheiro e devolve com juros. Garantido pelo FGC até R$ 250 mil por CPF/instituição. IR regressivo."
    ticker.contains("LCI") -> "Letra de Crédito Imobiliário. Lastreada em financiamentos imobiliários. ISENTA de IR para pessoa física. Garantia FGC. Liquidez geralmente após carência."
    ticker.contains("LCA") -> "Letra de Crédito do Agronegócio. Lastreada em crédito rural. ISENTA de IR. Garantia FGC. Costuma ter carência mínima."
    ticker.contains("IPCA") -> "Tesouro IPCA+ paga IPCA (inflação) + juro real fixo. Protege poder de compra no longo prazo. Marcação a mercado: preço oscila se vender antes do vencimento."
    else -> "Produto de renda fixa. Rentabilidade conforme indexador acordado. Verifique condições de carência, liquidez e garantias antes de investir."
}

@Composable
private fun CardSecaoRf(titulo: String, content: @Composable ColumnScope.() -> Unit) {
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
private fun LinhaTaxa(label: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = GrayText, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(valor, color = TextoForte, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ItemNoticiaRf(art: GNewsArtigo) {
    val ctx = LocalContext.current
    val url = art.url
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardElevado)
            .clickable(enabled = !url.isNullOrBlank()) {
                url?.let { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
            }
            .padding(8.dp)
    ) {
        art.image?.let {
            AsyncImage(model = it, contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(art.title ?: "(sem título)", color = TextoForte, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 3)
            Spacer(Modifier.height(2.dp))
            Text(art.source?.name ?: "", color = GrayText, fontSize = 10.sp)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = GrayText,
            modifier = Modifier.size(14.dp).align(Alignment.Top))
    }
}
