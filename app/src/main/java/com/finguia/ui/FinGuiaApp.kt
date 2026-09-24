package com.finguia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finguia.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finguia.dados.TransacaoBancaria
import com.finguia.ui.busca.TelaDetalheTransacao
import com.finguia.ui.busca.TelaBusca
import com.finguia.ui.calculadora.TelaCalculadora
import com.finguia.ui.configuracoes.ConfiguracoesViewModel
import com.finguia.ui.configuracoes.TelaConfiguracoes
import com.finguia.ui.home.TelaHomeDash
import com.finguia.ui.home.telaHome
import com.finguia.ui.investimentos.TelaInvestimentos
import com.finguia.ui.noticias.TelaNoticias
import com.finguia.ui.notificacoes.TelaNotificacoes
import com.finguia.ui.perfil.TelaPerfil
import com.finguia.ui.theme.CardBg
import com.finguia.ui.theme.DarkBg
import com.finguia.ui.theme.GojoPurple
import com.finguia.ui.theme.GrayText
import com.finguia.ui.theme.TextoForte
import com.finguia.ui.transacoes.CategoriaViewModel
import com.finguia.ui.transacoes.TelaLancar
import com.finguia.ui.transacoes.TelaTransacoes
import com.finguia.ui.transacoes.TransacaoViewModel

@Composable
fun FinGuiaApp() {
    var destinoAtual by rememberSaveable { mutableStateOf(DestinosApp.INICIO) }
    var transacaoDetalhe by remember { androidx.compose.runtime.mutableStateOf<TransacaoBancaria?>(null) }

    val transacaoViewModel: TransacaoViewModel = viewModel()
    val configViewModel: ConfiguracoesViewModel = viewModel()
    val categoriaViewModel: CategoriaViewModel = viewModel()
    val ocultarSaldo by configViewModel.ocultarSaldo.collectAsState()

    if (transacaoDetalhe != null) {
        BackHandler { transacaoDetalhe = null }
    } else if (destinoAtual != DestinosApp.INICIO) {
        BackHandler { destinoAtual = DestinosApp.INICIO }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        bottomBar = {
            BarraInferiorFinGuia(
                destinoAtual = destinoAtual,
                aoSelecionarDestino = { destinoAtual = it }
            )
        }
    ) { paddingInterno ->
        val modifier = Modifier.padding(paddingInterno)
        // Detalhe de transação sobrepõe qualquer tela (navegação imperativa)
        val detalhe = transacaoDetalhe
        if (detalhe != null) {
            TelaDetalheTransacao(
                transacao = detalhe,
                modifier = modifier,
                aoVoltar = { transacaoDetalhe = null }
            )
        } else {
            when (destinoAtual) {
                DestinosApp.INICIO -> telaHome(
                    modifier = modifier,
                    viewModel = transacaoViewModel,
                    ocultarSaldo = ocultarSaldo,
                    aoClicarDashboard = { destinoAtual = DestinosApp.INVESTIMENTOS },
                    aoClicarLancar = { destinoAtual = DestinosApp.LANCAR },
                    aoClicarExtrato = { destinoAtual = DestinosApp.EXTRATO },
                    aoClicarCalculadora = { destinoAtual = DestinosApp.CALCULADORA },
                    aoClicarTema = { destinoAtual = DestinosApp.CONFIGURACOES },
                    aoClicarBusca = { destinoAtual = DestinosApp.BUSCA },
                    aoClicarNoticias = { destinoAtual = DestinosApp.NOTICIAS },
                    aoClicarPerfil = { destinoAtual = DestinosApp.PERFIL },
                    aoClicarNotificacoes = { destinoAtual = DestinosApp.NOTIFICACOES }
                )
                DestinosApp.BUSCA -> TelaBusca(
                    modifier = modifier,
                    transacaoViewModel = transacaoViewModel,
                    categoriaViewModel = categoriaViewModel,
                    aoNavegar = { rota ->
                        val destino = DestinosApp.entries.find { it.name == rota }
                        if (destino != null) destinoAtual = destino
                    },
                    aoAbrirDetalhe = { transacao -> transacaoDetalhe = transacao },
                    aoVoltar = { destinoAtual = DestinosApp.INICIO }
                )
                DestinosApp.DASHBOARD      -> TelaHomeDash(modifier = modifier, viewModel = transacaoViewModel)
                DestinosApp.INVESTIMENTOS  -> TelaInvestimentos(modifier = modifier)
                DestinosApp.LANCAR         -> TelaLancar(modifier = modifier, viewModel = transacaoViewModel)
                DestinosApp.EXTRATO        -> TelaTransacoes(viewModel = transacaoViewModel)
                DestinosApp.CALCULADORA    -> TelaCalculadora(modifier = modifier)
                DestinosApp.CONFIGURACOES  -> TelaConfiguracoes(modifier = modifier, configViewModel = configViewModel)
                DestinosApp.NOTICIAS       -> TelaNoticias(modifier = modifier, aoVoltar = { destinoAtual = DestinosApp.INICIO })
                DestinosApp.PERFIL         -> TelaPerfil(modifier = modifier, aoVoltar = { destinoAtual = DestinosApp.INICIO })
                DestinosApp.NOTIFICACOES   -> TelaNotificacoes(modifier = modifier, viewModel = transacaoViewModel, aoVoltar = { destinoAtual = DestinosApp.INICIO })
            }
        }
    }
}

@Composable
private fun BarraInferiorFinGuia(
    destinoAtual: DestinosApp,
    aoSelecionarDestino: (DestinosApp) -> Unit
) {
    Surface(
        color = CardBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 10.dp,
        shadowElevation = 18.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            DestinosApp.entries.filter { it.exibirNaBarra }.forEach { destino ->
                ItemBarraInferior(
                    destino = destino,
                    selecionado = destino == destinoAtual,
                    aoSelecionar = { aoSelecionarDestino(destino) }
                )
            }
        }
    }
}

@Composable
private fun ItemBarraInferior(
    destino: DestinosApp,
    selecionado: Boolean,
    aoSelecionar: () -> Unit
) {
    val fundoIcone = if (selecionado) GojoPurple.copy(alpha = 0.16f) else Color.Transparent
    val corConteudo = if (selecionado) TextoForte else GrayText

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = aoSelecionar)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(fundoIcone, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(destino.icone),
                contentDescription = destino.rotulo,
                tint = corConteudo,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = destino.rotulo,
            color = if (selecionado) TextoForte else GrayText,
            fontSize = 10.sp,
            fontWeight = if (selecionado) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

enum class DestinosApp(
    val rotulo: String,
    val icone: Int,
    val exibirNaBarra: Boolean = true,
) {
    INICIO("Início", R.drawable.ic_home),
    DASHBOARD("Painel", R.drawable.ic_dashboard, exibirNaBarra = false),
    LANCAR("Lançar", R.drawable.ic_favorite),
    EXTRATO("Extratos", R.drawable.ic_extrato),
    INVESTIMENTOS("Invest", R.drawable.ic_dashboard, exibirNaBarra = false),
    CALCULADORA("Calc", R.drawable.ic_calculadora, exibirNaBarra = false),
    CONFIGURACOES("Config", R.drawable.ic_home, exibirNaBarra = false),
    BUSCA("Busca", R.drawable.ic_home, exibirNaBarra = false),
    NOTICIAS("Notícias", R.drawable.ic_home, exibirNaBarra = false),
    PERFIL("Perfil", R.drawable.ic_home, exibirNaBarra = false),
    NOTIFICACOES("Alertas", R.drawable.ic_home, exibirNaBarra = false),
}
