package com.finguia // Verifique se seu pacote é este ou com.finguia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.finguia.ui.FinGuiaApp // Importando o Maestro
import com.finguia.ui.theme.FinGuiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinGuiaTheme {
                // Chamamos apenas a função principal de UI
                FinGuiaApp()
            }
        }
    }
}