package com.finguia.ui.configuracoes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.finguia.ui.DestinosApp
import com.finguia.ui.calculadora.AbaCalculadora
import com.finguia.ui.theme.TemaApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Valores que o usuario pode deixar pre-escolhidos em Configuracoes.
 * Os padroes daqui reproduzem o comportamento de antes da tela existir.
 */
data class PadroesApp(
    /** Tela aberta ao iniciar o app. */
    val telaInicial: DestinosApp = DestinosApp.INICIO,
    /** Aba inicial do Lancar: 0 = Lancar, 1 = Recorrente, 2 = Agendado. */
    val abaLancar: Int = 0,
    /** Direcao ja marcada no lancamento avulso e no agendado. */
    val entradaPorPadrao: Boolean = false,
    /** Nome da conta gravado nos lancamentos manuais (coluna banco). */
    val contaManual: String = CONTA_MANUAL_PADRAO,
    /** Calculadora aberta primeiro; null = primeira visivel. */
    val calculadoraInicial: AbaCalculadora? = null,
) {
    companion object {
        const val CONTA_MANUAL_PADRAO = "Manual"

        /** Telas que fazem sentido como ponto de partida. */
        val TELAS_INICIAIS = listOf(
            DestinosApp.INICIO,
            DestinosApp.EXTRATO,
            DestinosApp.LANCAR,
            DestinosApp.DASHBOARD,
            DestinosApp.INVESTIMENTOS,
            DestinosApp.CALCULADORA,
        )

        val ABAS_LANCAR = listOf("Lançar", "Recorrente", "Agendado")
    }
}

class ConfiguracoesViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("finguia_prefs", android.content.Context.MODE_PRIVATE)

    private val _ocultarSaldo = MutableStateFlow(prefs.getBoolean("ocultar_saldo", false))
    val ocultarSaldo: StateFlow<Boolean> = _ocultarSaldo.asStateFlow()

    // Sem preferencia salva, fica no escuro: era o unico visual antes do tema claro
    private val _tema = MutableStateFlow(
        TemaApp.entries.firstOrNull { it.name == prefs.getString("tema", null) } ?: TemaApp.ESCURO
    )
    val tema: StateFlow<TemaApp> = _tema.asStateFlow()

    private val _padroes = MutableStateFlow(lerPadroes())
    val padroes: StateFlow<PadroesApp> = _padroes.asStateFlow()

    fun toggleOcultarSaldo(valor: Boolean) {
        _ocultarSaldo.value = valor
        prefs.edit().putBoolean("ocultar_saldo", valor).apply()
    }

    fun definirTema(tema: TemaApp) {
        _tema.value = tema
        prefs.edit().putString("tema", tema.name).apply()
    }

    fun definirTelaInicial(destino: DestinosApp) =
        salvar(_padroes.value.copy(telaInicial = destino))

    fun definirAbaLancar(indice: Int) =
        salvar(_padroes.value.copy(abaLancar = indice.coerceIn(0, PadroesApp.ABAS_LANCAR.lastIndex)))

    fun definirEntradaPorPadrao(entrada: Boolean) =
        salvar(_padroes.value.copy(entradaPorPadrao = entrada))

    fun definirContaManual(nome: String) =
        salvar(_padroes.value.copy(contaManual = nome))

    fun definirCalculadoraInicial(aba: AbaCalculadora?) =
        salvar(_padroes.value.copy(calculadoraInicial = aba))

    fun restaurarPadroes() = salvar(PadroesApp())

    private fun lerPadroes(): PadroesApp {
        val base = PadroesApp()
        return PadroesApp(
            telaInicial = PadroesApp.TELAS_INICIAIS
                .firstOrNull { it.name == prefs.getString(CHAVE_TELA, null) } ?: base.telaInicial,
            abaLancar = prefs.getInt(CHAVE_ABA_LANCAR, base.abaLancar)
                .coerceIn(0, PadroesApp.ABAS_LANCAR.lastIndex),
            entradaPorPadrao = prefs.getBoolean(CHAVE_ENTRADA, base.entradaPorPadrao),
            contaManual = prefs.getString(CHAVE_CONTA, null) ?: base.contaManual,
            calculadoraInicial = AbaCalculadora.entries
                .firstOrNull { it.name == prefs.getString(CHAVE_CALC, null) },
        )
    }

    private fun salvar(novos: PadroesApp) {
        _padroes.value = novos
        prefs.edit()
            .putString(CHAVE_TELA, novos.telaInicial.name)
            .putInt(CHAVE_ABA_LANCAR, novos.abaLancar)
            .putBoolean(CHAVE_ENTRADA, novos.entradaPorPadrao)
            .putString(CHAVE_CONTA, novos.contaManual)
            .putString(CHAVE_CALC, novos.calculadoraInicial?.name)
            .apply()
    }

    private companion object {
        const val CHAVE_TELA = "padrao_tela_inicial"
        const val CHAVE_ABA_LANCAR = "padrao_aba_lancar"
        const val CHAVE_ENTRADA = "padrao_entrada"
        const val CHAVE_CONTA = "padrao_conta_manual"
        const val CHAVE_CALC = "padrao_calculadora"
    }
}
