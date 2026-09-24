package com.finguia.ui.componentes

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import com.finguia.dados.TransacaoBancaria
import com.finguia.ui.formato.emReais
import com.finguia.ui.theme.DebtRed
import com.finguia.ui.theme.GrayText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pede confirmação antes de apagar uma transação. Devolve a função que as
 * telas chamam no ícone de lixeira; o diálogo aparece sozinho.
 *
 * Existe porque a lixeira apagava na hora: um toque acidental perdia o
 * lançamento de vez (a captura não regrava notificações já vistas). O teste
 * de estresse com toques aleatórios apagou 2 de 14 transações assim.
 */
@Composable
fun rememberConfirmacaoExclusao(aoConfirmar: (TransacaoBancaria) -> Unit): (TransacaoBancaria) -> Unit {
    var pendente by remember { mutableStateOf<TransacaoBancaria?>(null) }

    pendente?.let { transacao ->
        val data = remember(transacao.timestampMs) {
            SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")).format(Date(transacao.timestampMs))
        }
        AlertDialog(
            onDismissRequest = { pendente = null },
            title = { Text("Excluir lançamento?") },
            text = {
                Text(
                    "${transacao.descricao}\n${transacao.valor.emReais()} · $data\n\n" +
                        "Esta ação não pode ser desfeita."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    aoConfirmar(transacao)
                    pendente = null
                }) { Text("Excluir", color = DebtRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendente = null }) { Text("Cancelar", color = GrayText) }
            }
        )
    }
    return { pendente = it }
}
