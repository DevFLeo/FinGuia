package com.finguia.app.ui

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
import com.finguia.app.ui.theme.*

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(20.dp)
    ) {
        // 1. TOP BAR (Settings, Bell, User)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 30.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TopIconButton(Icons.Default.Settings)
            TopIconButton(Icons.Default.Notifications)
            TopIconButton(Icons.Default.Person)
        }

        // 2. DONUT CHART
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            DonutChart(progress = 0.75f) // 75% como no seu HTML
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SALDO TOTAL", color = GrayText, fontSize = 12.sp, letterSpacing = 1.sp)
                Text("R$ 5.240,00", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // 3. BUTTON GRID (3 colunas)
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                AppButton(Modifier.weight(1f), Icons.Default.AddCircle, "Lançar")
                AppButton(Modifier.weight(1f), Icons.Default.Edit, "Editar")
                AppButton(Modifier.weight(1f), Icons.Default.PieChart, "Dashboard")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                AppButton(Modifier.weight(1f), Icons.Default.Calculate, "Calculadora")
                AppButton(Modifier.weight(1f), Icons.Default.AdsClick, "Metas")
                AppButton(Modifier.weight(1f), Icons.Default.Search, "Busca")
            }

            // Botão Largo (Minhas Contas)
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
fun TopIconButton(icon: ImageVector) {
    Surface(
        color = CardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.size(45.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.padding(10.dp))
    }
}

@Composable
fun AppButton(modifier: Modifier, icon: ImageVector, label: String) {
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
            Icon(icon, contentDescription = null, tint = GojoPurple)
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
fun DonutChart(progress: Float) {
    Canvas(modifier = Modifier.size(200.dp)) {
        // Fundo cinza do círculo
        drawArc(
            color = Color(0xFF222222),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 15.dp.toPx())
        )
        // Progresso Roxo
        drawArc(
            color = GojoPurple,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = 15.dp.toPx())
        )
    }
}