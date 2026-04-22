package com.finguia.ui.noticias

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

data class Noticia(
    val titulo: String,
    val descricao: String,
    val link: String,
    val dataPublicacao: String
)

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
        Noticia(
            titulo = limpar(tituloRegex.find(bloco)?.groupValues?.get(1) ?: ""),
            descricao = limpar(descRegex.find(bloco)?.groupValues?.get(1) ?: ""),
            link = limpar(linkRegex.find(bloco)?.groupValues?.get(1) ?: ""),
            dataPublicacao = formatarData(dataRegex.find(bloco)?.groupValues?.get(1) ?: "")
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

        if (carregando && noticias.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GojoPurple)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(noticias) { n ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(n.link))
                                contexto.startActivity(intent)
                            },
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                n.titulo,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 3
                            )
                            if (n.descricao.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    n.descricao,
                                    color = GrayText,
                                    fontSize = 12.sp,
                                    maxLines = 3
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                n.dataPublicacao,
                                color = GojoPurple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
