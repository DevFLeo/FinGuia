package com.finguia.ui.transacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finguia.ui.theme.*

// ─────────────────────────────────────────────
// TELA PRINCIPAL
// ─────────────────────────────────────────────
@Composable
fun TelaLancar(modifier: Modifier = Modifier) {
    var abaSelecionada by remember { mutableStateOf(0) }
    val abas = listOf("Lançar", "Recorrente", "Notificação")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // BARRA DE ABAS
        BarraAbas(
            abas = abas,
            selecionada = abaSelecionada,
            aoSelecionar = { abaSelecionada = it }
        )

        Spacer(Modifier.height(20.dp))

        // CONTEÚDO DA ABA
        when (abaSelecionada) {
            0 -> AbaLancar()
            1 -> AbaRecorrente()
            2 -> AbaNotificacao()
        }
    }
}

// ─────────────────────────────────────────────
// BARRA DE ABAS
// ─────────────────────────────────────────────
@Composable
private fun BarraAbas(abas: List<String>, selecionada: Int, aoSelecionar: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, GojoPurple.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        abas.forEachIndexed { index, titulo ->
            val ativa = index == selecionada
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (ativa) GojoPurple else Color.Transparent)
                    .clickable { aoSelecionar(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = titulo.uppercase(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// ABA: LANÇAR
// ─────────────────────────────────────────────
@Composable
private fun AbaLancar() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // GANHOS
        TituloSecao(icone = Icons.Default.Add, texto = "GANHOS E FREELANCE", cor = GojoPurple)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CardGanho(Modifier.weight(1f), Icons.Default.WorkOutline, "Freelancer", "Trabalho extra")
            CardGanho(Modifier.weight(1f), Icons.Default.Shuffle, "Esporádico", "Ganhos variados")
            CardGanho(Modifier.weight(1f), Icons.Default.Payments, "Salário", "Renda fixa")
        }

        Spacer(Modifier.height(4.dp))

        // DÍVIDAS
        TituloSecao(icone = Icons.Default.Warning, texto = "DÍVIDAS E GASTOS", cor = DebtRed)

        val dividas = listOf(
            Triple(Icons.Default.AccountBalance, "Contas", "Boletos / Aluguel"),
            Triple(Icons.Default.CreditCard, "Emergência", "Imprevistos"),
            Triple(Icons.Default.Restaurant, "Comida", "Alimentação"),
            Triple(Icons.Default.LocalCafe, "Lanches", "Saídas rápidas"),
            Triple(Icons.Default.Receipt, "Boletos", "Pagamentos"),
            Triple(Icons.Default.MoneyOff, "Dívidas Gerais", "Outros gastos"),
        )

        val linhas = dividas.chunked(2)
        linhas.forEach { linha ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                linha.forEach { (icone, label, sub) ->
                    CardDivida(Modifier.weight(1f), icone, label, sub)
                }
                if (linha.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ─────────────────────────────────────────────
// ABA: RECORRENTE
// ─────────────────────────────────────────────
@Composable
private fun AbaRecorrente() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TituloSecao(icone = Icons.Default.Repeat, texto = "REPETIÇÃO MENSAL", cor = GojoPurple)

        CardRecorrente(Icons.Default.TrendingUp, "Salário", "Ganhos Repetidos", GojoPurple)
        CardRecorrente(Icons.Default.TrendingDown, "Contas Fixas", "Dívidas Repetidas", DebtRed)
    }
}

// ─────────────────────────────────────────────
// ABA: NOTIFICAÇÃO
// ─────────────────────────────────────────────
@Composable
private fun AbaNotificacao() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TituloSecao(icone = Icons.Default.Notifications, texto = "GESTÃO DE ALERTAS", cor = GojoPurple)

        CardRecorrente(Icons.Default.AddAlert, "Nova Notificação", "Criar alerta personalizado", GojoPurple)

        Spacer(Modifier.height(8.dp))

        TituloSecao(icone = Icons.Default.Edit, texto = "EDITAR PALAVRAS", cor = GojoPurple)

        val opcoes = listOf("Deletar", "Arquivar", "Desarquivar")
        opcoes.forEach { opcao ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBg)
                    .border(width = 3.dp, color = GojoPurple, shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp))
                    .clickable { }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(opcao, color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENTES
// ─────────────────────────────────────────────
@Composable
private fun TituloSecao(icone: ImageVector, texto: String, cor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(16.dp))
        Text(texto, color = cor, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun CardGanho(modifier: Modifier, icone: ImageVector, label: String, sublabel: String) {
    Column(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GojoPurple.copy(alpha = 0.12f))
            .border(1.dp, GojoPurple.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable { }
            .padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icone, contentDescription = null, tint = GojoPurple, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(sublabel, color = GrayText, fontSize = 10.sp)
    }
}

@Composable
private fun CardDivida(modifier: Modifier, icone: ImageVector, label: String, sublabel: String) {
    Column(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DebtRed.copy(alpha = 0.08f))
            .border(1.dp, DebtRed.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable { }
            .padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icone, contentDescription = null, tint = DebtRed, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(sublabel, color = GrayText, fontSize = 9.sp)
    }
}

@Composable
private fun CardRecorrente(icone: ImageVector, label: String, sublabel: String, cor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cor.copy(alpha = 0.1f))
            .border(1.dp, cor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(26.dp))
        Column {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(sublabel, color = GrayText, fontSize = 12.sp)
        }
    }
}
