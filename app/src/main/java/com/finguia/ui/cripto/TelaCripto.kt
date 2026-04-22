package com.finguia.ui.cripto

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.finguia.dados.Cripto
import com.finguia.dados.TrendingCoinItem
import com.finguia.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

private val formatoReais = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

@Composable
fun TelaCripto(
    modifier: Modifier = Modifier,
    viewModel: CriptoViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        when (val s = estado) {
            is EstadoCripto.Carregando -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GojoPurple)
                        Spacer(Modifier.height(12.dp))
                        Text("Buscando preços...", color = GrayText, fontSize = 14.sp)
                    }
                }
            }

            is EstadoCripto.Erro -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text(s.mensagem, color = DebtRed, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.buscarCriptos() },
                            colors = ButtonDefaults.buttonColors(containerColor = GojoPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Tentar novamente", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            is EstadoCripto.Sucesso -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item { CabecalhoCripto() }

                    if (s.dados.trending.isNotEmpty()) {
                        item { SecaoTrending(s.dados.trending) }
                    }

                    if (s.dados.maioresAltas.isNotEmpty()) {
                        item { SecaoMaioresAltas(s.dados.maioresAltas) }
                    }

                    item {
                        Text(
                            text = "Ranking por Market Cap",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    items(s.dados.ranking) { cripto ->
                        CartaoCripto(cripto)
                    }
                }
            }
        }
    }
}

@Composable
private fun CabecalhoCripto() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1040), DarkBg),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                text = "Mercado Cripto",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Preços em tempo real · BRL",
                color = GrayText,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SecaoTrending(trending: List<TrendingCoinItem>) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Whatshot,
                contentDescription = null,
                tint = Color(0xFFFF6B35),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Em Alta Agora",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(trending) { coin ->
                CartaoTrending(coin)
            }
        }
    }
}

@Composable
private fun CartaoTrending(coin: TrendingCoinItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(120.dp)
            .border(
                width = 1.dp,
                color = Color(0xFF2A2A3E),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = coin.thumb,
                contentDescription = coin.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = coin.symbol.uppercase(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = coin.name,
                color = GrayText,
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (coin.market_cap_rank != null) {
                Text(
                    text = "#${coin.market_cap_rank}",
                    color = GojoPurple,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SecaoMaioresAltas(gainers: List<Cripto>) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = MoneyGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Maiores Altas 24h",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(gainers) { cripto ->
                CartaoGainer(cripto)
            }
        }
    }
}

@Composable
private fun CartaoGainer(cripto: Cripto) {
    val variacao = cripto.price_change_percentage_24h ?: 0.0

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2818)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(130.dp)
            .border(
                width = 1.dp,
                color = Color(0xFF1A4A2E),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = cripto.image,
                contentDescription = cripto.name,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = cripto.symbol.uppercase(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatoReais.format(cripto.current_price),
                color = GrayText,
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .background(Color(0x3A2ECC71), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "+${"%.2f".format(variacao)}%",
                    color = MoneyGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CartaoCripto(cripto: Cripto) {
    val variacao = cripto.price_change_percentage_24h ?: 0.0
    val corVariacao = if (variacao >= 0) MoneyGreen else DebtRed
    val iconeVariacao = if (variacao >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown
    val bgVariacao = if (variacao >= 0) Color(0x1A2ECC71) else Color(0x1AFF4D4D)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cripto.market_cap_rank != null) {
                Text(
                    text = "${cripto.market_cap_rank}",
                    color = GrayText,
                    fontSize = 11.sp,
                    modifier = Modifier.width(22.dp)
                )
            }

            AsyncImage(
                model = cripto.image,
                contentDescription = cripto.name,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cripto.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = cripto.symbol.uppercase(),
                    color = GrayText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatoReais.format(cripto.current_price),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(bgVariacao, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = iconeVariacao,
                            contentDescription = null,
                            tint = corVariacao,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "${"%.2f".format(variacao)}%",
                            color = corVariacao,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
