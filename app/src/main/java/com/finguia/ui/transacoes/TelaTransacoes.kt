package com.finguia.ui.transacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finguia.dados.TransacaoBancaria
import com.finguia.dados.TipoTransacao
import com.finguia.ui.formato.corDoSentido
import com.finguia.ui.formato.emReais
import com.finguia.ui.formato.prefixoDoSentido
import com.finguia.ui.formato.sentido
import com.finguia.ui.gerenciamento.ModalEditarTransacao
import com.finguia.ui.theme.*
import com.finguia.ui.theme.TextoForte
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter

private data class TelaMetrics(
    val paddingH: Dp,
    val spacerSm: Dp,
    val spacerMd: Dp,
    val cardPadding: Dp,
    val iconBoxSize: Dp,
    val iconSm: Dp,
    val iconMd: Dp,
    val iconLg: Dp,
    val deleteButtonSize: Dp,
    val deleteIconSize: Dp,
    val fontTitle: TextUnit,
    val fontSubtitle: TextUnit,
    val fontLabel: TextUnit,
    val fontValue: TextUnit,
    val fontBankName: TextUnit,
    val fontDesc: TextUnit,
    val fontDate: TextUnit,
    val fontEmpty: TextUnit,
    val fontEmptySub: TextUnit,
    val cardGap: Dp,
    val resumoGap: Dp,
    val emptyTopPad: Dp,
)

@Composable
private fun rememberMetrics(maxWidthDp: Dp): TelaMetrics {
    val isCompact = maxWidthDp < 400.dp
    val isMedium = maxWidthDp < 600.dp
    return remember(maxWidthDp) {
        when {
            isCompact -> TelaMetrics(
                paddingH = 12.dp, spacerSm = 10.dp, spacerMd = 12.dp,
                cardPadding = 10.dp, iconBoxSize = 34.dp, iconSm = 16.dp,
                iconMd = 20.dp, iconLg = 52.dp, deleteButtonSize = 40.dp,
                deleteIconSize = 22.dp, fontTitle = 18.sp, fontSubtitle = 11.sp,
                fontLabel = 10.sp, fontValue = 14.sp, fontBankName = 13.sp,
                fontDesc = 11.sp, fontDate = 9.sp, fontEmpty = 13.sp,
                fontEmptySub = 11.sp, cardGap = 6.dp, resumoGap = 8.dp,
                emptyTopPad = 40.dp,
            )
            isMedium -> TelaMetrics(
                paddingH = 16.dp, spacerSm = 12.dp, spacerMd = 16.dp,
                cardPadding = 14.dp, iconBoxSize = 40.dp, iconSm = 20.dp,
                iconMd = 24.dp, iconLg = 64.dp, deleteButtonSize = 44.dp,
                deleteIconSize = 24.dp, fontTitle = 22.sp, fontSubtitle = 12.sp,
                fontLabel = 11.sp, fontValue = 16.sp, fontBankName = 14.sp,
                fontDesc = 12.sp, fontDate = 10.sp, fontEmpty = 15.sp,
                fontEmptySub = 12.sp, cardGap = 8.dp, resumoGap = 12.dp,
                emptyTopPad = 60.dp,
            )
            else -> TelaMetrics(
                paddingH = 24.dp, spacerSm = 14.dp, spacerMd = 20.dp,
                cardPadding = 18.dp, iconBoxSize = 48.dp, iconSm = 24.dp,
                iconMd = 28.dp, iconLg = 80.dp, deleteButtonSize = 48.dp,
                deleteIconSize = 26.dp, fontTitle = 26.sp, fontSubtitle = 14.sp,
                fontLabel = 13.sp, fontValue = 18.sp, fontBankName = 16.sp,
                fontDesc = 14.sp, fontDate = 12.sp, fontEmpty = 17.sp,
                fontEmptySub = 14.sp, cardGap = 10.dp, resumoGap = 16.dp,
                emptyTopPad = 80.dp,
            )
        }
    }
}

@Composable
fun TelaTransacoes(viewModel: TransacaoViewModel = viewModel()) {
    val transacoes by viewModel.transacoes.collectAsState()
    val totalReceitas by viewModel.totalReceitas.collectAsState()
    val totalDespesas by viewModel.totalDespesas.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val exportarCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        // UTF-8 BOM
                        os.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        val writer = OutputStreamWriter(os, "UTF-8")
                        writer.write(viewModel.gerarCsv(transacoes))
                        writer.flush()
                    }
                    android.widget.Toast.makeText(context, "Planilha exportada com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Erro ao exportar planilha", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        val m = rememberMetrics(maxWidth)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = m.paddingH)
        ) {
            Spacer(Modifier.height(m.spacerMd))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Extratos Bancários",
                        color = TextoForte,
                        fontSize = m.fontTitle,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Capturado automaticamente via notificações",
                        color = GrayText,
                        fontSize = m.fontSubtitle
                    )
                }
                
                IconButton(onClick = { exportarCsvLauncher.launch("extratos_finguia.csv") }) {
                    Icon(
                        Icons.Default.Download, 
                        contentDescription = "Exportar Planilha", 
                        tint = GojoPurple,
                        modifier = Modifier.size(m.iconMd)
                    )
                }
            }

            Spacer(Modifier.height(m.spacerMd))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(m.resumoGap)
            ) {
                ResumoCard(
                    titulo = "Entradas",
                    valor = totalReceitas,
                    cor = MoneyGreen,
                    icone = Icons.Default.TrendingUp,
                    m = m,
                    modifier = Modifier.weight(1f)
                )
                ResumoCard(
                    titulo = "Saídas",
                    valor = totalDespesas,
                    cor = DebtRed,
                    icone = Icons.Default.TrendingDown,
                    m = m,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(m.spacerMd))

            if (transacoes.isEmpty()) {
                EstadoVazio(m)
            } else {
                var transacaoEditando by remember { mutableStateOf<TransacaoBancaria?>(null) }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(m.cardGap)) {
                    items(transacoes, key = { it.id }) { transacao ->
                        CartaoTransacao(
                            transacao = transacao,
                            m         = m,
                            onDeletar = { viewModel.deletar(transacao.id) },
                            onEditar  = { transacaoEditando = transacao }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }

                transacaoEditando?.let { t ->
                    ModalEditarTransacao(
                        transacao   = t,
                        onDismiss   = { transacaoEditando = null },
                        onConfirmar = { atualizada ->
                            viewModel.atualizar(atualizada)
                            transacaoEditando = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumoCard(
    titulo: String,
    valor: Double,
    cor: Color,
    icone: ImageVector,
    m: TelaMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Row(
            modifier = Modifier.padding(m.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(m.spacerSm)
        ) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(m.iconMd))
            Column {
                Text(titulo, color = GrayText, fontSize = m.fontLabel)
                Text(
                    text = formatarValor(valor),
                    color = cor,
                    fontSize = m.fontValue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CartaoTransacao(
    transacao: TransacaoBancaria,
    m: TelaMetrics,
    onDeletar: () -> Unit,
    onEditar: () -> Unit
) {
    val sentido = transacao.tipo.sentido
    val corValor = corDoSentido(sentido)
    val prefixoValor = prefixoDoSentido(sentido)
    val dataFormatada = SimpleDateFormat("dd/MM/yy HH:mm", Locale("pt", "BR"))
        .format(Date(transacao.timestampMs))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Row(
            modifier = Modifier.padding(m.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(m.iconBoxSize)
                    .background(corValor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconeParaTipo(transacao.tipo),
                    contentDescription = null,
                    tint = corValor,
                    modifier = Modifier.size(m.iconSm)
                )
            }

            Spacer(Modifier.width(m.spacerSm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transacao.banco,
                    color = TextoForte,
                    fontSize = m.fontBankName,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = transacao.descricao,
                    color = GrayText,
                    fontSize = m.fontDesc,
                    maxLines = 2
                )
                Text(
                    text = dataFormatada,
                    color = GrayText.copy(alpha = 0.6f),
                    fontSize = m.fontDate
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefixoValor${formatarValor(transacao.valor)}",
                    color = corValor,
                    fontSize = m.fontBankName,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onEditar, modifier = Modifier.size(m.deleteButtonSize)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = GojoPurple,
                            modifier = Modifier.size(m.deleteIconSize)
                        )
                    }
                    IconButton(onClick = onDeletar, modifier = Modifier.size(m.deleteButtonSize)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Remover",
                            tint = GrayText,
                            modifier = Modifier.size(m.deleteIconSize)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EstadoVazio(m: TelaMetrics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = m.emptyTopPad),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(m.spacerSm)
    ) {
        Icon(
            Icons.Default.NotificationsNone,
            contentDescription = null,
            tint = GrayText,
            modifier = Modifier.size(m.iconLg)
        )
        Text(
            text = "Nenhuma transação capturada ainda",
            color = GrayText,
            fontSize = m.fontEmpty,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "As notificações dos seus bancos serão\ncapturadas automaticamente aqui.",
            color = GrayText.copy(alpha = 0.6f),
            fontSize = m.fontEmptySub,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun iconeParaTipo(tipo: TipoTransacao): ImageVector = when (tipo) {
    TipoTransacao.PIX_RECEBIDO           -> Icons.Default.CallReceived
    TipoTransacao.PIX_ENVIADO            -> Icons.Default.CallMade
    TipoTransacao.COMPRA_CREDITO         -> Icons.Default.CreditCard
    TipoTransacao.COMPRA_DEBITO          -> Icons.Default.ShoppingCart
    TipoTransacao.BOLETO_PAGO            -> Icons.Default.Receipt
    TipoTransacao.TRANSFERENCIA_RECEBIDA -> Icons.Default.MoveToInbox
    TipoTransacao.TRANSFERENCIA_ENVIADA  -> Icons.Default.Outbox
    TipoTransacao.ESTORNO                -> Icons.Default.Undo
    TipoTransacao.SAQUE                  -> Icons.Default.LocalAtm
    TipoTransacao.DEPOSITO               -> Icons.Default.Savings
    TipoTransacao.COBRANCA               -> Icons.Default.Warning
    TipoTransacao.DESCONHECIDO           -> Icons.Default.AccountBalance
}

private fun formatarValor(valor: Double): String =
    valor.emReais()
