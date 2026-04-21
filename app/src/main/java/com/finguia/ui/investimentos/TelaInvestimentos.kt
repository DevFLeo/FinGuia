package com.finguia.ui.investimentos
// Fundos , COE , Tesouro Direto , Renda Variavel , Renda fixa ,

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finguia.ui.theme.*

// ─────────────────────────────────────────────
// DEFINIÇÃO DE CORES
// ─────────────────────────────────────────────
val Fundos = Color(0xFDA00BC7)
val Coe = Color(0xFD8B1FA5)
val TesouroD = Color(0xFDAE27CE)
val RendaV = Color(0xFEA64EBD)
val RendaF = Color(0xFE9400D3)
val Cripto = GojoPurple

// ─────────────────────────────────────────────
// COMEÇO DO CODIGO
// ─────────────────────────────────────────────

data class ItemInvestimento(
    val nome: String,
    val valor: Double,
    val cor: Color,
    val porcentagem: Float // Para o gráfico de pizza/donut
)

@Composable
fun TelaInvestimentos(
    modifier: Modifier = Modifier
) {
    // Dados fictícios para teste (colocar o banco de dados)
    val listaInvestimentos = remember {
        listOf(
            ItemInvestimento("Fundos", 1500.0, Fundos, 0.3f),
            ItemInvestimento("COE", 800.0, Coe, 0.15f),
            ItemInvestimento("Tesouro Direto", 2500.0, TesouroD, 0.25f),
            ItemInvestimento("Renda Variável", 1200.0, RendaV, 0.12f),
            ItemInvestimento("Renda Fixa", 3000.0, RendaF, 0.13f),
            ItemInvestimento("Cripto", 500.0, Cripto, 0.05f)
        )
    }

    Scaffold(
        topBar = { TopBarInvestimentos() },
        containerColor = DarkBg
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(Modifier.height(20.dp)) }
            item {
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GraficoDonutInvestimentos(listaInvestimentos)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", color = GrayText, fontSize = 14.sp)
                        Text("R$ 9.500,00", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
            items(listaInvestimentos) { item ->
                CardCategoriaInvestimento(item)
                Spacer(Modifier.height(16.dp))
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

/**
 * Componente que desenha o gráfico circular usando as cores fornecidas.
 */
@Composable
fun GraficoDonutInvestimentos(itens: List<ItemInvestimento>) {
    Canvas(modifier = Modifier.size(200.dp)) {
        var startAngle = -90f // Começa no topo (12h)
        val strokeWidth = 45f

        itens.forEach { item ->
            val sweepAngle = item.porcentagem * 360f
            drawArc(
                color = item.cor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

/**
 * O Card individual com a barra vertical colorida, seguindo o design do Inter.
 */
@Composable
fun CardCategoriaInvestimento(item: ItemInvestimento) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Barra Vertical Colorida
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(item.cor)
        )

        Spacer(Modifier.width(16.dp))

        // Nome da Categoria
        Text(
            text = item.nome,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "R$ •••••",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Barra de navegação superior com abas "Brasil" e "Global".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarInvestimentos() {
    Column(modifier = Modifier.background(DarkBg)) {
        CenterAlignedTopAppBar(
            title = { Text("INVESTIMENTOS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { /* Voltar */ }) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = GojoPurple)
                }
            },
            actions = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp))
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp))
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBg)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Brasil", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(40.dp).height(2.dp).background(GojoPurple)) // Indicador de aba ativa
            }
            Text("Global", color = GrayText)
        }
        Divider(color = GrayText.copy(alpha = 0.2f), thickness = 0.5.dp, modifier = Modifier.padding(top = 8.dp))
    }
}