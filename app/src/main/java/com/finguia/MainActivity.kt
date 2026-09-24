package com.finguia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finguia.ui.FinGuiaApp
import com.finguia.ui.configuracoes.ConfiguracoesViewModel
import com.finguia.ui.theme.FinGuiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Mesmo ViewModel (escopo da Activity) que a tela de Configuracoes usa:
            // trocar o tema la redesenha o app na hora, sem reiniciar
            val configuracoes: ConfiguracoesViewModel = viewModel()
            val tema by configuracoes.tema.collectAsState()
            FinGuiaTheme(tema = tema) {
                FinGuiaApp()
            }
        }
    }
}
