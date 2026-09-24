package com.finguia.ui.configuracoes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.finguia.ui.theme.TemaApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConfiguracoesViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("finguia_prefs", android.content.Context.MODE_PRIVATE)

    private val _ocultarSaldo = MutableStateFlow(prefs.getBoolean("ocultar_saldo", false))
    val ocultarSaldo: StateFlow<Boolean> = _ocultarSaldo.asStateFlow()

    // Sem preferencia salva, fica no escuro: era o unico visual antes do tema claro
    private val _tema = MutableStateFlow(
        TemaApp.entries.firstOrNull { it.name == prefs.getString("tema", null) } ?: TemaApp.ESCURO
    )
    val tema: StateFlow<TemaApp> = _tema.asStateFlow()

    fun toggleOcultarSaldo(valor: Boolean) {
        _ocultarSaldo.value = valor
        prefs.edit().putBoolean("ocultar_saldo", valor).apply()
    }

    fun definirTema(tema: TemaApp) {
        _tema.value = tema
        prefs.edit().putString("tema", tema.name).apply()
    }
}
