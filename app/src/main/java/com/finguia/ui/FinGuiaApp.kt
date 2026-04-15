package com.finguia.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.finguia.R
import com.finguia.ui.cripto.TelaCripto
import com.finguia.ui.home.telaHome
import com.finguia.ui.home.TelaHomeDash
import com.finguia.ui.transacoes.TelaLancar
import com.finguia.ui.transacoes.TelaTransacoes

@Composable
fun FinGuiaApp() {
    var destinoAtual by rememberSaveable { mutableStateOf(DestinosApp.INICIO) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            DestinosApp.entries.forEach { destino ->
                item(
                    icon = {
                        Icon(
                            painter = painterResource(destino.icone),
                            contentDescription = destino.rotulo
                        )
                    },
                    label = { Text(destino.rotulo) },
                    selected = destino == destinoAtual,
                    onClick = { destinoAtual = destino }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { paddingInterno ->
            val modifier = Modifier.padding(paddingInterno)
            when (destinoAtual) {
                DestinosApp.INICIO -> telaHome(
                    modifier = modifier,
                    aoClicarCriptos = { destinoAtual = DestinosApp.CRIPTOMOEDAS }
                )
                DestinosApp.DASHBOARD -> TelaHomeDash(modifier = modifier)
                DestinosApp.CRIPTOMOEDAS -> TelaCripto(modifier = modifier)
                DestinosApp.LANCAR -> TelaLancar(modifier = modifier)
                DestinosApp.EXTRATO -> TelaTransacoes()
                DestinosApp.TEMA -> Text("Configurações de tema", modifier = modifier)
            }
        }
    }
}

enum class DestinosApp(
    val rotulo: String,
    val icone: Int,
) {
    INICIO("Início", R.drawable.ic_home),
    DASHBOARD("Painel", R.drawable.ic_dashboard),
    LANCAR("Lançar", R.drawable.ic_favorite),
    EXTRATO("Extrato", R.drawable.ic_extrato),
    CRIPTOMOEDAS("Criptos", R.drawable.ic_cripto),
    TEMA("Tema", R.drawable.ic_palette),
}
