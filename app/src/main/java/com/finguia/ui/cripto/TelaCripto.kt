package com.finguia.ui.cripto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
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
import com.finguia.dados.Cripto
import com.finguia.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TelaCripto(
    modifier: Modifier = Modifier,
    viewModel: CriptoViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Text(
            text = "Criptomoedas",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (val s = estado) {
            is EstadoCripto.Carregando -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GojoPurple)
                        Spacer(Modifier.height(12.dp))
                        Text("Buscando preços...", color = GrayText)
                    }
                }
            }
            is EstadoCripto.Erro -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.mensagem, color = DebtRed, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.buscarCriptos() },
                            colors = ButtonDefaults.buttonColors(containerColor = GojoPurple)
                        ) {
                            Text("Tentar novamente")
                        }
                    }
                }
            }
            is EstadoCripto.Sucesso -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(s.criptos) { cripto ->
                        CartaoCripto(cripto)
                    }
                }
            }
        }
    }
}

@Composable
fun CartaoCripto(cripto: Cripto) {
    val variacao = cripto.price_change_percentage_24h ?: 0.0
    val corVariacao = if (variacao >= 0) MoneyGreen else DebtRed
    val iconeVariacao = if (variacao >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown

    val formatoReais = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val precoFormatado = formatoReais.format(cripto.current_price)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cripto.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = cripto.symbol.uppercase(),
                    color = GrayText,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = precoFormatado,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = iconeVariacao,
                        contentDescription = null,
                        tint = corVariacao,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${"%.2f".format(variacao)}%",
                        color = corVariacao,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
