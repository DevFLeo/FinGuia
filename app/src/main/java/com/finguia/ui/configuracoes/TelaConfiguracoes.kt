package com.finguia.ui.configuracoes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    cacheVm: CalcCacheViewModel = viewModel()
) {
    val ocultarSaldo by configViewModel.ocultarSaldo.collectAsState()
    val tema by configViewModel.tema.collectAsState()
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
}

@Composable
private fun SecaoConfiguracoes(titulo: String, conteudo: @Composable () -> Unit) {
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
