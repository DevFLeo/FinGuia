package com.finguia.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finguia.ui.theme.CardBg
import com.finguia.ui.theme.DarkBg
import com.finguia.ui.theme.GojoPurple
import com.finguia.ui.theme.GrayText

@Composable
fun telaHome(
    modifier: Modifier = Modifier,
    aoClicarCriptos: () -> Unit = {},
    aoClicarDashboard: () -> Unit = {},
    aoClicarLancar: () -> Unit = {},
    aoClicarExtrato: () -> Unit = {},
    aoClicarTema: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 26.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BotaoIconeTopo(Icons.Default.Settings)
            BotaoIconeTopo(Icons.Default.Notifications)
            BotaoIconeTopo(Icons.Default.Search)
            BotaoIconeTopo(Icons.Default.Person)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            GraficoRosca(progresso = 0.75f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SALDO TOTAL", color = GrayText, fontSize = 12.sp, letterSpacing = 1.sp)
                Text("R$ 5.240,00", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                BotaoAcao(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Default.AddCircle,
                    rotulo = "Lancar",
                    aoClicar = aoClicarLancar
                )
                BotaoAcao(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Default.Edit,
                    rotulo = "Extrato",
                    aoClicar = aoClicarExtrato
                )
                BotaoAcao(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Default.PieChart,
                    rotulo = "Painel",
                    aoClicar = aoClicarDashboard
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                BotaoAcao(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Default.Calculate,
                    rotulo = "Calculadora"
                )
                BotaoAcao(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Default.AccountBalance,
                    rotulo = "Contas"
                )
                BotaoAcao(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Default.Settings,
                    rotulo = "Tema",
                    aoClicar = aoClicarTema
                )
            }

            Button(
                onClick = aoClicarCriptos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.Black)
                Spacer(Modifier.size(10.dp))
                Text("Criptomoedas", fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GojoPurple)
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text("Minhas Contas", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BotaoIconeTopo(
    icone: ImageVector,
    aoClicar: () -> Unit = {}
) {
    Surface(
        color = CardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .size(45.dp)
            .clickable(onClick = aoClicar)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icone, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun BotaoAcao(
    modifier: Modifier,
    icone: ImageVector,
    rotulo: String,
    aoClicar: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = aoClicar),
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
