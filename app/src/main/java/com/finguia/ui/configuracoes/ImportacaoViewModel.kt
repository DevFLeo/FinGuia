package com.finguia.ui.configuracoes

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finguia.dados.ImportadorExtrato
import com.finguia.dados.ResumoImportacao
import com.finguia.dados.TransacaoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface EstadoImportacao {
    data object Ocioso : EstadoImportacao
    data object Importando : EstadoImportacao
    data class Concluida(val resumo: ResumoImportacao) : EstadoImportacao
    data class Falhou(val mensagem: String) : EstadoImportacao
}

class ImportacaoViewModel(app: Application) : AndroidViewModel(app) {

    private val importador = ImportadorExtrato(TransacaoRepository(app))

    private val _estado = MutableStateFlow<EstadoImportacao>(EstadoImportacao.Ocioso)
    val estado: StateFlow<EstadoImportacao> = _estado.asStateFlow()

    fun importar(uri: Uri) {
        if (_estado.value == EstadoImportacao.Importando) return
        _estado.value = EstadoImportacao.Importando
        viewModelScope.launch {
            _estado.value = try {
                val bytes = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { entrada ->
                        // Extratos reais tem poucos MB; acima disso e arquivo errado (video, backup...)
                        val dados = entrada.readBytes()
                        require(dados.size <= LIMITE_BYTES) { "Arquivo grande demais para um extrato." }
                        dados
                    } ?: error("Não foi possível abrir o arquivo.")
                }
                EstadoImportacao.Concluida(withContext(Dispatchers.IO) { importador.importar(bytes) })
            } catch (e: IllegalArgumentException) {
                EstadoImportacao.Falhou(e.message ?: "Formato não reconhecido.")
            } catch (e: Exception) {
                EstadoImportacao.Falhou("Falha ao importar: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    fun dispensarResultado() {
        _estado.value = EstadoImportacao.Ocioso
    }

    private companion object {
        const val LIMITE_BYTES = 20 * 1024 * 1024
    }
}
