package com.finguia.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finguia.ui.theme.*

@Composable
fun telaHome(
    modifier: Modifier = Modifier,
    aoClicarCriptos: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(20.dp)
    ) {
        // 1. BARRA SUPERIOR
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 30.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BotaoIconeTopo(Icons.Default.Settings)
            BotaoIconeTopo(Icons.Default.Notifications)
            BotaoIconeTopo(Icons.Default.Person)
        }

        // 2. GRÁFICO ROSCA
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            GraficoRosca(progresso = 0.75f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SALDO TOTAL", color = GrayText, fontSize = 12.sp, letterSpacing = 1.sp)
                Text("R$ 5.240,00", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // 3. GRADE DE BOTÕES
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                BotaoAcao(Modifier.weight(1f), Icons.Default.AddCircle, "Lançar")
                BotaoAcao(Modifier.weight(1f), Icons.Default.Edit, "Editar")
                BotaoAcao(Modifier.weight(1f), Icons.Default.PieChart, "Painel")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                BotaoAcao(Modifier.weight(1f), Icons.Default.Calculate, "Calculadora")
                BotaoAcao(Modifier.weight(1f), Icons.Default.AdsClick, "Metas")
                BotaoAcao(Modifier.weight(1f), Icons.Default.Search, "Busca")
            }

            // Botão Criptomoedas
            Button(
                onClick = aoClicarCriptos,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
            ) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Criptomoedas", fontWeight = FontWeight.Bold, color = Color.Black)
            }

            // Botão Minhas Contas
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GojoPurple)
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Minhas Contas", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BotaoIconeTopo(icone: ImageVector) {
    Surface(
        color = CardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.size(45.dp)
    ) {
        Icon(icone, contentDescription = null, tint = Color.White, modifier = Modifier.padding(10.dp))
    }
}

@Composable
fun BotaoAcao(modifier: Modifier, icone: ImageVector, rotulo: String) {
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icone, contentDescription = null, tint = GojoPurple)
            Spacer(Modifier.height(8.dp))
            Text(rotulo, color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
fun GraficoRosca(progresso: Float) {
    Canvas(modifier = Modifier.size(200.dp)) {
        drawArc(
            color = Color(0xFF222222),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 15.dp.toPx())
        )
        drawArc(
            color = GojoPurple,
            startAngle = -90f,
            sweepAngle = 360f * progresso,
            useCenter = false,
            style = Stroke(width = 15.dp.toPx())
        )
    }
}
