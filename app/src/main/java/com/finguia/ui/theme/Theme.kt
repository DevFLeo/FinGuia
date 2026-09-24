package com.finguia.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun esquemaDe(paleta: PaletaFinGuia) = if (paleta.ehEscura) {
    darkColorScheme(
        primary = GojoPurple,
        onPrimary = Color.White,
        secondary = PurpleGrey80,
        tertiary = paleta.positivo,
        background = paleta.fundo,
        onBackground = paleta.textoForte,
        surface = paleta.cartao,
        onSurface = paleta.textoForte,
        surfaceVariant = paleta.cartaoElevado,
        onSurfaceVariant = paleta.textoFraco,
        outline = paleta.borda,
        error = paleta.negativo,
    )
} else {
    lightColorScheme(
        primary = GojoPurple,
        onPrimary = Color.White,
        secondary = PurpleGrey40,
        tertiary = paleta.positivo,
        onTertiary = Color.White,
        background = paleta.fundo,
        onBackground = paleta.textoForte,
        surface = paleta.cartao,
        onSurface = paleta.textoForte,
        surfaceVariant = paleta.cartaoElevado,
        onSurfaceVariant = paleta.textoFraco,
        outline = paleta.borda,
        error = paleta.negativo,
    )
}

/**
 * Tema do app. O padrao e ESCURO, que era o visual unico antes do tema claro
 * existir: quem atualiza o app nao ve nada mudar ate escolher outro tema.
 *
 * As cores dinamicas do Android 12 (tiradas do papel de parede) nao sao usadas:
 * as telas pintam com a paleta da marca, e misturar as duas deixava dialogos
 * e menus com cores que nao combinavam com o resto.
 */
@Composable
fun FinGuiaTheme(
    tema: TemaApp = TemaApp.ESCURO,
    content: @Composable () -> Unit
) {
    val escuro = when (tema) {
        TemaApp.SISTEMA -> isSystemInDarkTheme()
        TemaApp.ESCURO -> true
        TemaApp.CLARO -> false
    }
    val paleta = if (escuro) PaletaEscura else PaletaClara

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = paleta.fundo.toArgb()
            val controle = WindowCompat.getInsetsController(window, view)
            // Icones da barra de status escuros no tema claro, claros no escuro
            controle.isAppearanceLightStatusBars = !escuro
            controle.isAppearanceLightNavigationBars = !escuro
        }
    }

    CompositionLocalProvider(LocalPaleta provides paleta) {
        MaterialTheme(
            colorScheme = esquemaDe(paleta),
            typography = Typography,
            content = content
        )
    }
}
