package com.finguia.ui.transacoes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finguia.dados.CategoriaCustom
import com.finguia.dados.CategoriaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriaViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = CategoriaRepository(app)

    val categorias: StateFlow<List<CategoriaCustom>> = repository
        .listarTodas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun inserir(categoria: CategoriaCustom) {
        viewModelScope.launch { repository.salvar(categoria) }
    }

    fun deletar(id: Long) {
        viewModelScope.launch { repository.deletar(id) }
    }
}
