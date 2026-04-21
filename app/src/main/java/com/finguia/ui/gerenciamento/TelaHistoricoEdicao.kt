package com.finguia.ui.gerenciamento
// Aba crud com opção de editar, deletar e salvar
// val abas = listOf("Lançar", "Recorrente", "Editar", "Notificação")
// Pasta diferente da original

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finguia.dados.TransacaoBancaria
import com.finguia.ui.theme.*
import com.finguia.ui.transacoes.TransacaoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaHistoricoEdicao(
    viewModel: TransacaoViewModel,
    onVoltar: () -> Unit
) {
    val transacoes by viewModel.todasTransacoes.collectAsState(initial = emptyList())
    var transacaoParaEditar by remember { mutableStateOf<TransacaoBancaria?>(null) }
    var textoBusca by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Registros",
                        style = androidx.compose.ui.text.TextStyle(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = GojoPurple)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkBg,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = DarkBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Barra de Busca
            OutlinedTextField(
                value = textoBusca,
                onValueChange = { textoBusca = it },
                placeholder = { Text("Pesquisar na história...", color = GrayText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GojoPurple) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GojoPurple,
                    unfocusedBorderColor = CardBg,
                    containerColor = CardBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // A Lista do historico
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                val filtradas = transacoes.filter {
                    it.descricao.contains(textoBusca, ignoreCase = true)
                }

                items(filtradas) { transacao ->
                    CardTransacaoEditavel(
                        transacao = transacao,
                        onClique = { transacaoParaEditar = transacao },
                        onDeletar = { viewModel.deletar(transacao.id) }
                    )
                }
            }
        }
    }

    // Modal da edição de transação
    transacaoParaEditar?.let { transacao ->
        ModalEditarTransacao(
            transacao = transacao,
            onDismiss = { transacaoParaEditar = null },
            onConfirmar = { novaTransacao ->
                viewModel.atualizar(novaTransacao) // Certificar se há uma função de atualização no ViewModel
                transacaoParaEditar = null
            }
        )
    }
}

@Composable
private fun CardTransacaoEditavel(
    transacao: TransacaoBancaria,
    onClique: () -> Unit,
    onDeletar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, GojoPurple.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .clickable { onClique() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GojoPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = GojoPurple, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(transacao.descricao, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(transacao.banco, color = GrayText, fontSize = 12.sp)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "R$ ${transacao.valor}", // Use sua função de formatar aqui
                color = if (transacao.valor >= 0) MoneyGreen else DebtRed,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
            IconButton(onClick = onDeletar, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = DebtRed.copy(alpha = 0.6f))
            }
        }
    }
}