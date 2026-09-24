package com.finguia.ui.configuracoes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finguia.dados.ResumoImportacao
import com.finguia.ui.DestinosApp
import com.finguia.ui.calculadora.AbaCalculadora
import com.finguia.ui.calculadora.CalcCacheViewModel
import com.finguia.ui.theme.CardBg
import com.finguia.ui.theme.DarkBg
import com.finguia.ui.theme.GojoPurple
import com.finguia.ui.theme.GrayText
import com.finguia.ui.theme.TemaApp
import com.finguia.ui.theme.TextoForte

@Composable
fun TelaConfiguracoes(
    modifier: Modifier = Modifier,
    configViewModel: ConfiguracoesViewModel = viewModel(),
    cacheVm: CalcCacheViewModel = viewModel(),
    importacaoVm: ImportacaoViewModel = viewModel()
) {
    val estadoImportacao by importacaoVm.estado.collectAsState()
    // Seletor de arquivos do Android; "*/*" porque bancos entregam OFX com tipos
    // variados (application/x-ofx, text/plain, octet-stream)
    val escolherArquivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(importacaoVm::importar)
    }
    val ocultarSaldo by configViewModel.ocultarSaldo.collectAsState()
    val tema by configViewModel.tema.collectAsState()
    val padroes by configViewModel.padroes.collectAsState()
    val abasOcultas by cacheVm.ocultas.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Configurações",
            color = TextoForte,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        SecaoConfiguracoes(titulo = "Aparência") {
            TemaApp.entries.forEach { opcao ->
                ItemEscolha(
                    rotulo = opcao.rotulo,
                    subRotulo = when (opcao) {
                        TemaApp.SISTEMA -> "Acompanha o modo claro/escuro do Android"
                        TemaApp.ESCURO -> "Fundo escuro, padrão do FinGuia"
                        TemaApp.CLARO -> "Fundo branco"
                    },
                    selecionado = tema == opcao,
                    aoEscolher = { configViewModel.definirTema(opcao) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SecaoConfiguracoes(titulo = "Padrões") {
            ItemLista(
                rotulo = "Tela ao abrir o app",
                opcoes = PadroesApp.TELAS_INICIAIS.map { it to nomeTela(it) },
                atual = padroes.telaInicial,
                aoEscolher = configViewModel::definirTelaInicial
            )
            ItemLista(
                rotulo = "Aba inicial do Lançar",
                opcoes = PadroesApp.ABAS_LANCAR.mapIndexed { i, nome -> i to nome },
                atual = padroes.abaLancar,
                aoEscolher = configViewModel::definirAbaLancar
            )
            ItemLista(
                rotulo = "Lançamento avulso começa como",
                opcoes = listOf(false to "Saída", true to "Entrada"),
                atual = padroes.entradaPorPadrao,
                aoEscolher = configViewModel::definirEntradaPorPadrao
            )
            ItemLista(
                rotulo = "Calculadora que abre primeiro",
                opcoes = listOf<Pair<AbaCalculadora?, String>>(null to "Primeira visível") +
                    AbaCalculadora.entries.map { it to it.rotulo },
                atual = padroes.calculadoraInicial,
                aoEscolher = configViewModel::definirCalculadoraInicial
            )
            OutlinedTextField(
                value = padroes.contaManual,
                onValueChange = configViewModel::definirContaManual,
                label = { Text("Conta dos lançamentos manuais", color = GrayText) },
                supportingText = {
                    Text("Aparece como banco no extrato. Ex.: Nubank, Carteira", color = GrayText, fontSize = 11.sp)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GojoPurple,
                    unfocusedBorderColor = GrayText.copy(alpha = 0.3f),
                    focusedTextColor = TextoForte,
                    unfocusedTextColor = TextoForte,
                    cursorColor = GojoPurple
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            TextButton(
                onClick = configViewModel::restaurarPadroes,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Restaurar padrões", color = GojoPurple)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SecaoConfiguracoes(titulo = "Dados") {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text("Importar extrato", color = TextoForte, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Arquivo OFX exportado pelo app ou site do banco, ou resposta da API " +
                        "de Contas do Open Finance (JSON). Lançamentos já importados são ignorados.",
                    color = GrayText,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { escolherArquivo.launch(arrayOf("*/*")) },
                    enabled = estadoImportacao != EstadoImportacao.Importando,
                    colors = ButtonDefaults.buttonColors(containerColor = GojoPurple)
                ) {
                    if (estadoImportacao == EstadoImportacao.Importando) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importando…", color = Color.White)
                    } else {
                        Text("Escolher arquivo", color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SecaoConfiguracoes(titulo = "Privacidade") {
            ItemSwitch(
                rotulo = "Ocultar saldo",
                subRotulo = "Esconder valores na tela inicial",
                ativado = ocultarSaldo,
                aoMudar = configViewModel::toggleOcultarSaldo
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SecaoConfiguracoes(titulo = "Calculadoras visíveis") {
            AbaCalculadora.entries.forEach { aba ->
                val visivel = aba.name !in abasOcultas
                ItemSwitch(
                    rotulo = aba.rotulo,
                    subRotulo = if (visivel) "Visível na aba calculadora" else "Oculta",
                    ativado = visivel,
                    aoMudar = { ativo -> cacheVm.toggleAba(aba.name, oculta = !ativo) }
                )
            }
        }
    }

    when (val e = estadoImportacao) {
        is EstadoImportacao.Concluida -> DialogoImportacao(
            titulo = "Importação concluída",
            texto = textoResumo(e.resumo),
            aoFechar = importacaoVm::dispensarResultado
        )
        is EstadoImportacao.Falhou -> DialogoImportacao(
            titulo = "Não foi possível importar",
            texto = e.mensagem,
            aoFechar = importacaoVm::dispensarResultado
        )
        else -> Unit
    }
}

private fun textoResumo(r: ResumoImportacao): String = buildString {
    append(r.formato.rotulo)
    r.instituicao?.let { append(" · $it") }
    append("\n\n")
    append(
        when (r.importados) {
            0 -> "Nenhum lançamento novo."
            1 -> "1 lançamento importado."
            else -> "${r.importados} lançamentos importados."
        }
    )
    if (r.jaExistiam > 0) append("\n${r.jaExistiam} já estavam no app.")
    if (r.ignorados > 0) append("\n${r.ignorados} linha(s) sem valor ou data ignorada(s).")
}

@Composable
private fun DialogoImportacao(titulo: String, texto: String, aoFechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoFechar,
        confirmButton = { TextButton(onClick = aoFechar) { Text("OK", color = GojoPurple) } },
        title = { Text(titulo) },
        text = { Text(texto) }
    )
}

@Composable
private fun SecaoConfiguracoes(titulo: String, conteudo: @Composable ColumnScope.() -> Unit) {
    Text(
        text = titulo,
        color = GrayText,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            conteudo()
        }
    }
}

/** Nome por extenso da tela; os rotulos da barra inferior sao abreviados. */
private fun nomeTela(destino: DestinosApp): String = when (destino) {
    DestinosApp.INVESTIMENTOS -> "Investimentos"
    DestinosApp.CALCULADORA -> "Calculadora"
    else -> destino.rotulo
}

/** Linha que mostra a opcao atual e abre um menu com as outras ao tocar. */
@Composable
private fun <T> ItemLista(
    rotulo: String,
    opcoes: List<Pair<T, String>>,
    atual: T,
    aoEscolher: (T) -> Unit
) {
    var aberto by remember { mutableStateOf(false) }
    val nomeAtual = opcoes.firstOrNull { it.first == atual }?.second.orEmpty()

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { aberto = true }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = rotulo, color = TextoForte, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(text = nomeAtual, color = GojoPurple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Escolher", tint = GrayText)
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { (valor, nome) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            nome,
                            fontWeight = if (valor == atual) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        aoEscolher(valor)
                        aberto = false
                    }
                )
            }
        }
    }
}

/** Linha de escolha unica (radio). A linha inteira responde ao toque. */
@Composable
private fun ItemEscolha(
    rotulo: String,
    subRotulo: String,
    selecionado: Boolean,
    aoEscolher: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoEscolher)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = rotulo, color = TextoForte, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subRotulo, color = GrayText, fontSize = 12.sp)
        }
        RadioButton(
            selected = selecionado,
            onClick = aoEscolher,
            colors = RadioButtonDefaults.colors(
                selectedColor = GojoPurple,
                unselectedColor = GrayText
            )
        )
    }
}

@Composable
private fun ItemSwitch(
    rotulo: String,
    subRotulo: String,
    ativado: Boolean,
    aoMudar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = rotulo, color = TextoForte, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subRotulo, color = GrayText, fontSize = 12.sp)
        }
        Switch(
            checked = ativado,
            onCheckedChange = aoMudar,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GojoPurple,
                uncheckedThumbColor = GrayText,
                uncheckedTrackColor = CardBg
            )
        )
    }
}
