package com.finguia.ui.noticias

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

data class Noticia(
    val titulo: String,
    val descricao: String,
    val link: String,
    val dataPublicacao: String,
    val imageUrl: String? = null
)

enum class CategoriaNoticia(val label: String, val emoji: String, val palavrasChave: List<String>) {
    TODAS("Todas", "🌐", emptyList()),
    POLITICA("Política", "🏛️", listOf(
        "política", "político", "governo", "lula", "bolsonaro", "stf", "congresso", "senado", 
        "câmara", "ministro", "eleições", "eleição", "projeto de lei", "pl", "pec", "partido", 
        "parlamentar", "tse", "haddad", "biden", "trump", "democracia", "voto"
    )),
    CRIPTO("Cripto", "🪙", listOf(
        "bitcoin", "cripto", "ethereum", "blockchain", "btc", "eth", "criptomoeda", "solana", 
        "token", "coinbase", "binance", "criptoativos", "halving", "web3", "satoshi"
    )),
    INVESTIMENTOS("Investimentos", "📈", listOf(
        "bolsa", "ações", "dividendos", "fiis", "ibovespa", "investimento", "investir", 
        "renda fixa", "tesouro", "fundos", "ações", "cdi", "selic", "fii", "dividendos", 
        "proventos", "b3", "nasdaq", "nyse", "investidor", "carteira", "fundo"
    )),
    ECONOMIA("Economia", "📊", listOf(
        "inflação", "pib", "juros", "copom", "banco central", "bc", "dólar", "cambio", "câmbio",
        "imposto", "receita", "tributária", "emprego", "desemprego", "mercado", "inflacionário", 
        "deflação", "recessão", "fomc", "fed"
    )),
    EMPRESAS("Empresas", "🏢", listOf(
        "lucro", "balanço", "receita", "faturamento", "fusão", "aquisição", "empresa", 
        "corporativo", "startup", "vendas", "petrobras", "vale", "itaú", "bradesco", 
        "magazine luiza", "magalu", "nubank", "apple", "microsoft", "google", "amazon", "tesla"
    ))
}

fun Noticia.combinaComCategoria(categoria: CategoriaNoticia): Boolean {
    if (categoria == CategoriaNoticia.TODAS) return true
    val texto = "$titulo $descricao".lowercase(Locale.getDefault())
    return categoria.palavrasChave.any { palavra -> texto.contains(palavra) }
}

class NoticiasViewModel : ViewModel() {
    private val _noticias = MutableStateFlow<List<Noticia>>(emptyList())
    val noticias: StateFlow<List<Noticia>> = _noticias

    private val _carregando = MutableStateFlow(false)
    val carregando: StateFlow<Boolean> = _carregando

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    init { carregar() }

    fun carregar() {
        viewModelScope.launch {
            _carregando.value = true
            _erro.value = null
            try {
                val xml = withContext(Dispatchers.IO) {
                    URL("https://www.infomoney.com.br/feed/").readText()
                }
                _noticias.value = parseRss(xml).take(30)
            } catch (e: Exception) {
                _erro.value = "Falha ao carregar notícias. Verifique a conexão."
            } finally {
                _carregando.value = false
            }
        }
    }
}

/**
 * Parse RSS 2.0 simples via regex. Aceita <item>...</item> com título, link,
 * descrição e pubDate. CDATA é desembrulhado.
 */
private fun parseRss(xml: String): List<Noticia> {
    val itemRegex = Regex("<item>([\\s\\S]*?)</item>", RegexOption.IGNORE_CASE)
    val tituloRegex = Regex("<title>([\\s\\S]*?)</title>", RegexOption.IGNORE_CASE)
    val linkRegex = Regex("<link>([\\s\\S]*?)</link>", RegexOption.IGNORE_CASE)
    val descRegex = Regex("<description>([\\s\\S]*?)</description>", RegexOption.IGNORE_CASE)
    val dataRegex = Regex("<pubDate>([\\s\\S]*?)</pubDate>", RegexOption.IGNORE_CASE)
    val imgRegex = Regex("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", RegexOption.IGNORE_CASE)

    fun limpar(s: String): String = s
        .replace(Regex("<!\\[CDATA\\[([\\s\\S]*?)]]>"), "$1")
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#8217;", "'")
        .replace("&#8211;", "–")
        .replace("&nbsp;", " ")
        .trim()

    fun formatarData(raw: String): String = try {
        val entrada = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).parse(raw.trim())
        val saida = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        if (entrada != null) saida.format(entrada) else raw.trim()
    } catch (e: Exception) { raw.trim() }

    return itemRegex.findAll(xml).map { m ->
        val bloco = m.groupValues[1]
        val descRaw = descRegex.find(bloco)?.groupValues?.get(1) ?: ""
        val imgMatch = imgRegex.find(descRaw)?.groupValues?.get(1)
        val finalImgUrl = if (imgMatch != null) {
            imgMatch.replace("&amp;", "&")
        } else {
            null
        }

        Noticia(
            titulo = limpar(tituloRegex.find(bloco)?.groupValues?.get(1) ?: ""),
            descricao = limpar(descRaw),
            link = limpar(linkRegex.find(bloco)?.groupValues?.get(1) ?: ""),
            dataPublicacao = formatarData(dataRegex.find(bloco)?.groupValues?.get(1) ?: ""),
            imageUrl = finalImgUrl
        )
    }.toList()
}

@Composable
fun TelaNoticias(
    modifier: Modifier = Modifier,
    aoVoltar: () -> Unit = {},
    vm: NoticiasViewModel = viewModel()
) {
    val noticias by vm.noticias.collectAsState()
    val carregando by vm.carregando.collectAsState()
    val erro by vm.erro.collectAsState()
    val contexto = LocalContext.current

    var categoriaSelecionada by remember { mutableStateOf(CategoriaNoticia.TODAS) }

    val noticiasFiltradas = remember(noticias, categoriaSelecionada) {
        if (categoriaSelecionada == CategoriaNoticia.TODAS) {
            noticias
        } else {
            noticias.filter { it.combinaComCategoria(categoriaSelecionada) }
        }
    }

    val contagemCategorias = remember(noticias) {
        CategoriaNoticia.values().associateWith { cat ->
            if (cat == CategoriaNoticia.TODAS) {
                noticias.size
            } else {
                noticias.count { it.combinaComCategoria(cat) }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = aoVoltar) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Notícias", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("InfoMoney", color = GrayText, fontSize = 12.sp)
            }
            IconButton(onClick = { vm.carregar() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = GojoPurple)
            }
        }

        if (erro != null) {
            Text(
                erro ?: "",
                color = DebtRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Barra de Categorias Horizontal
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CategoriaNoticia.values()) { categoria ->
                val selecionado = categoriaSelecionada == categoria
                val contagem = contagemCategorias[categoria] ?: 0
                val label = if (noticias.isEmpty()) {
                    "${categoria.emoji} ${categoria.label}"
                } else {
                    "${categoria.emoji} ${categoria.label} ($contagem)"
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selecionado) GojoPurple else CardBg)
                        .clickable {
                            categoriaSelecionada = categoria
                        }
                        .border(
                            width = 1.dp,
                            color = if (selecionado) GojoPurple.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selecionado) Color.White else GrayText,
                        fontSize = 12.sp,
                        fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        if (carregando && noticias.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GojoPurple)
            }
        } else if (noticiasFiltradas.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = categoriaSelecionada.emoji,
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Nenhuma notícia encontrada",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Não há matérias sobre ${categoriaSelecionada.label} no momento.",
                        color = GrayText,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(noticiasFiltradas) { index, n ->
                    val isDestaque = index == 0
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(n.link))
                                contexto.startActivity(intent)
                            },
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(if (isDestaque) 24.dp else 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            if (n.imageUrl != null) {
                                AsyncImage(
                                    model = n.imageUrl,
                                    contentDescription = "Imagem da notícia",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (isDestaque) 220.dp else 160.dp)
                                )
                            }
                            Column(Modifier.padding(if (isDestaque) 20.dp else 16.dp)) {
                                Text(
                                    text = n.titulo,
                                    color = Color.White,
                                    fontSize = if (isDestaque) 18.sp else 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = if (isDestaque) 24.sp else 20.sp
                                )
                                if (n.descricao.isNotBlank() && isDestaque) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = n.descricao,
                                        color = GrayText,
                                        fontSize = 13.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 18.sp
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "InfoMoney",
                                        color = GojoPurple,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "•",
                                        color = GrayText,
                                        fontSize = 12.sp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = n.dataPublicacao,
                                        color = GrayText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
