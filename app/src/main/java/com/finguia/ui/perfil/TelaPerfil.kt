package com.finguia.ui.perfil

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.finguia.ui.theme.CardBg
import com.finguia.ui.theme.DarkBg
import com.finguia.ui.theme.DebtRed
import com.finguia.ui.theme.GojoPurple
import com.finguia.ui.theme.GrayText
import com.finguia.ui.theme.MoneyGreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

data class MetaFinanceira(
    val id: Long,
    val descricao: String,
    val valorAtual: Double,
    val valorObjetivo: Double
) {
    val progresso: Float get() =
        if (valorObjetivo > 0) (valorAtual / valorObjetivo).coerceIn(0.0, 1.0).toFloat() else 0f
}

/**
 * Persistência local simples via SharedPreferences — evita introduzir nova tabela Room
 * por enquanto. Metas ficam em JSON.
 */
class PerfilViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("finguia_perfil", Context.MODE_PRIVATE)

    private val _nome = MutableStateFlow(prefs.getString("nome", "") ?: "")
    val nome: StateFlow<String> = _nome

    private val _fotoUri = MutableStateFlow(prefs.getString("foto_uri", null))
    val fotoUri: StateFlow<String?> = _fotoUri

    private val _metas = MutableStateFlow(carregarMetas())
    val metas: StateFlow<List<MetaFinanceira>> = _metas

    fun definirNome(novo: String) {
        _nome.value = novo
        prefs.edit().putString("nome", novo).apply()
    }

    fun definirFoto(uri: String?) {
        _fotoUri.value = uri
        prefs.edit().putString("foto_uri", uri).apply()
    }

    fun adicionarMeta(descricao: String, objetivo: Double) {
        val nova = MetaFinanceira(System.currentTimeMillis(), descricao, 0.0, objetivo)
        _metas.value = _metas.value + nova
        salvarMetas()
    }

    fun atualizarValorMeta(id: Long, novoValor: Double) {
        _metas.value = _metas.value.map { if (it.id == id) it.copy(valorAtual = novoValor) else it }
        salvarMetas()
    }

    fun removerMeta(id: Long) {
        _metas.value = _metas.value.filterNot { it.id == id }
        salvarMetas()
    }

    private fun carregarMetas(): List<MetaFinanceira> {
        val raw = prefs.getString("metas", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                MetaFinanceira(
                    id = o.getLong("id"),
                    descricao = o.getString("descricao"),
                    valorAtual = o.getDouble("valorAtual"),
                    valorObjetivo = o.getDouble("valorObjetivo")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun salvarMetas() {
        val arr = JSONArray()
        _metas.value.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("descricao", m.descricao)
                put("valorAtual", m.valorAtual)
                put("valorObjetivo", m.valorObjetivo)
            })
        }
        prefs.edit().putString("metas", arr.toString()).apply()
    }
}

@Composable
fun TelaPerfil(
    modifier: Modifier = Modifier,
    aoVoltar: () -> Unit = {},
) {
    val contexto = LocalContext.current
    val vm: PerfilViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(contexto.applicationContext as Application)
    )
    val nome by vm.nome.collectAsState()
    val fotoUri by vm.fotoUri.collectAsState()
    val metas by vm.metas.collectAsState()

    var editandoNome by remember { mutableStateOf(false) }
    var nomeTemp by remember { mutableStateOf(nome) }
    var mostrarDialogoMeta by remember { mutableStateOf(false) }
    var metaEditando by remember { mutableStateOf<MetaFinanceira?>(null) }

    val seletorFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                contexto.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { /* nem todas as URIs suportam persistência */ }
            vm.definirFoto(it.toString())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = aoVoltar) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Text("Perfil", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // ── Avatar e nome ─────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(CardBg)
                    .border(2.dp, GojoPurple, CircleShape)
                    .clickable { seletorFoto.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (fotoUri != null) {
                    AsyncImage(
                        model = fotoUri,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = GrayText,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Toque para alterar foto", color = GrayText, fontSize = 11.sp)

            Spacer(Modifier.height(16.dp))

            if (editandoNome) {
                OutlinedTextField(
                    value = nomeTemp,
                    onValueChange = { nomeTemp = it },
                    label = { Text("Seu nome") },
                    singleLine = true,
                    colors = campoColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.definirNome(nomeTemp); editandoNome = false },
                        colors = ButtonDefaults.buttonColors(containerColor = GojoPurple)
                    ) { Text("Salvar") }
                    OutlinedButton(onClick = { editandoNome = false }) { Text("Cancelar") }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = nome.ifBlank { "Defina seu nome" },
                        color = if (nome.isBlank()) GrayText else Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { nomeTemp = nome; editandoNome = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar nome", tint = GojoPurple)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Metas financeiras ─────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Metas Financeiras",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { metaEditando = null; mostrarDialogoMeta = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar meta", tint = GojoPurple)
            }
        }

        if (metas.isEmpty()) {
            Text(
                "Nenhuma meta ainda. Toque em + para criar uma.",
                color = GrayText,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            metas.forEach { meta ->
                CardMeta(
                    meta = meta,
                    aoAtualizar = { novoValor -> vm.atualizarValorMeta(meta.id, novoValor) },
                    aoRemover = { vm.removerMeta(meta.id) }
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    if (mostrarDialogoMeta) {
        DialogoNovaMeta(
            aoConfirmar = { descricao, objetivo ->
                vm.adicionarMeta(descricao, objetivo)
                mostrarDialogoMeta = false
            },
            aoCancelar = { mostrarDialogoMeta = false }
        )
    }
}

@Composable
private fun CardMeta(
    meta: MetaFinanceira,
    aoAtualizar: (Double) -> Unit,
    aoRemover: () -> Unit
) {
    var editando by remember { mutableStateOf(false) }
    var valorTemp by remember { mutableStateOf(meta.valorAtual.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    meta.descricao,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = aoRemover, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remover", tint = DebtRed, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { meta.progresso },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (meta.progresso >= 1f) MoneyGreen else GojoPurple,
                trackColor = Color(0xFF222222)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "R$ %,.2f / R$ %,.2f (${(meta.progresso * 100).toInt()}%)".format(meta.valorAtual, meta.valorObjetivo),
                color = GrayText,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            if (editando) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = valorTemp,
                        onValueChange = { valorTemp = it },
                        label = { Text("Valor atual") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = campoColors(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val novo = valorTemp.replace(",", ".").toDoubleOrNull() ?: meta.valorAtual
                            aoAtualizar(novo)
                            editando = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GojoPurple)
                    ) { Text("OK") }
                }
            } else {
                TextButton(onClick = { valorTemp = meta.valorAtual.toString(); editando = true }) {
                    Text("Atualizar progresso", color = GojoPurple, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DialogoNovaMeta(
    aoConfirmar: (String, Double) -> Unit,
    aoCancelar: () -> Unit
) {
    var descricao by remember { mutableStateOf("") }
    var objetivo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = aoCancelar,
        containerColor = CardBg,
        title = { Text("Nova Meta", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição (ex: Reserva de emergência)") },
                    singleLine = true,
                    colors = campoColors()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = objetivo,
                    onValueChange = { objetivo = it },
                    label = { Text("Valor objetivo (R$)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = campoColors()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val valor = objetivo.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (descricao.isNotBlank() && valor > 0) aoConfirmar(descricao, valor)
                }
            ) { Text("Criar", color = GojoPurple) }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) { Text("Cancelar", color = GrayText) }
        }
    )
}

@Composable
private fun campoColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GojoPurple,
    unfocusedBorderColor = GrayText,
    focusedLabelColor = GojoPurple,
    unfocusedLabelColor = GrayText,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = GojoPurple
)
