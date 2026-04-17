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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.finguia.ui.cripto.TelaCripto
import com.finguia.ui.home.TelaHomeDash
import com.finguia.ui.home.telaHome
import com.finguia.ui.theme.CardBg
import com.finguia.ui.theme.DarkBg
import com.finguia.ui.theme.GojoPurple
import com.finguia.ui.theme.GrayText
import com.finguia.ui.transacoes.TelaLancar
import com.finguia.ui.transacoes.TelaTransacoes

@Composable
fun FinGuiaApp() {
    var destinoAtual by rememberSaveable { mutableStateOf(DestinosApp.INICIO) }

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
        when (destinoAtual) {
            DestinosApp.INICIO -> telaHome(
                modifier = modifier,
                aoClicarCriptos = { destinoAtual = DestinosApp.CRIPTOMOEDAS },
                aoClicarDashboard = { destinoAtual = DestinosApp.DASHBOARD },
                aoClicarLancar = { destinoAtual = DestinosApp.LANCAR },
                aoClicarExtrato = { destinoAtual = DestinosApp.EXTRATO },
                aoClicarTema = { destinoAtual = DestinosApp.TEMA }
            )
            DestinosApp.DASHBOARD -> TelaHomeDash(modifier = modifier)
            DestinosApp.CRIPTOMOEDAS -> TelaCripto(modifier = modifier)
            DestinosApp.LANCAR -> TelaLancar(modifier = modifier)
            DestinosApp.EXTRATO -> TelaTransacoes()
            DestinosApp.TEMA -> Text(
                text = "Configuracoes de tema",
                modifier = modifier.padding(24.dp),
                color = Color.White
            )
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
            DestinosApp.entries.forEach { destino ->
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
    val corConteudo = if (selecionado) Color.White else GrayText

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
            color = if (selecionado) Color.White else GrayText,
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
) {
    INICIO("Inicio", R.drawable.ic_home),
    DASHBOARD("Painel", R.drawable.ic_dashboard),
    LANCAR("Lancar", R.drawable.ic_favorite),
    EXTRATO("Extrato", R.drawable.ic_extrato),
    CRIPTOMOEDAS("Criptos", R.drawable.ic_cripto),
    TEMA("Tema", R.drawable.ic_palette),
}
