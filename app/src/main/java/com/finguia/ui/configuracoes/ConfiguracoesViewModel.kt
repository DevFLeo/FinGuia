package com.finguia.ui.configuracoes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConfiguracoesViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("finguia_prefs", android.content.Context.MODE_PRIVATE)

    private val _ocultarSaldo = MutableStateFlow(prefs.getBoolean("ocultar_saldo", false))
    val ocultarSaldo: StateFlow<Boolean> = _ocultarSaldo.asStateFlow()

    fun toggleOcultarSaldo(valor: Boolean) {
        _ocultarSaldo.value = valor
        prefs.edit().putBoolean("ocultar_saldo", valor).apply()
    }
}
