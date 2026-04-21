package com.finguia.ui.gerenciamento

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.finguia.dados.TransacaoBancaria
import com.finguia.ui.theme.*

@Composable
fun ModalEditarTransacao(
    transacao: TransacaoBancaria,
    onDismiss: () -> Unit,
    onConfirmar: (TransacaoBancaria) -> Unit
) {
    var descricao by remember { mutableStateOf(transacao.descricao) }
    var banco by remember { mutableStateOf(transacao.banco) }
    var valorTexto by remember { mutableStateOf(transacao.valor.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Editar Transação", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = banco,
                    onValueChange = { banco = it },
                    label = { Text("Banco", color = GrayText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição", color = GrayText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors(),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = valorTexto,
                    onValueChange = { valorTexto = it },
                    label = { Text("Valor (R$)", color = GrayText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = GrayText)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val novoValor = valorTexto.replace(",", ".").toDoubleOrNull() ?: transacao.valor
                            onConfirmar(transacao.copy(banco = banco.trim(), descricao = descricao.trim(), valor = novoValor))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GojoPurple)
                    ) {
                        Text("Salvar", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GojoPurple,
    unfocusedBorderColor = GrayText.copy(alpha = 0.3f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = GojoPurple
)
