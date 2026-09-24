package com.finguia.ui.theme

import android.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
// Verde e vermelho de valores acompanham o tema (ver Paleta.kt)
val MoneyGreen: Color
    @Composable @ReadOnlyComposable get() = LocalPaleta.current.positivo
val DangerRed = Color(0xFFE74C3C)
val DeepNavy = Color(0xFF1C1B1F)

val GojoPurple = Color(0xFF7D5FFF)

// Tokens de superficie e texto. Eram cores fixas do tema escuro; agora leem a
// paleta ativa, entao as telas que ja usavam DarkBg/CardBg/GrayText passam a
// funcionar no tema claro sem alteracao. So podem ser lidos dentro de @Composable.
val ProfitGreen: Color
    @Composable @ReadOnlyComposable get() = LocalPaleta.current.positivo
val DebtRed: Color
    @Composable @ReadOnlyComposable get() = LocalPaleta.current.negativo
val DarkBg: Color
    @Composable @ReadOnlyComposable get() = LocalPaleta.current.fundo
val CardBg: Color
    @Composable @ReadOnlyComposable get() = LocalPaleta.current.cartao
val GrayText: Color
    @Composable @ReadOnlyComposable get() = LocalPaleta.current.textoFraco

/** Texto principal. Substitui o Color.White fixo, que sumia no tema claro. */
val TextoForte: Color
    @Composable @ReadOnlyComposable get() = LocalPaleta.current.textoForte
val CardElevado: Color
    @Composable @ReadOnlyComposable get() = LocalPaleta.current.cartaoElevado
val BordaSuave: Color
    @Composable @ReadOnlyComposable get() = LocalPaleta.current.borda
val TrilhoProgresso: Color
    @Composable @ReadOnlyComposable get() = LocalPaleta.current.trilho
val DestaqueTopo: Color
    @Composable @ReadOnlyComposable get() = LocalPaleta.current.destaqueTopo

// Cores da Tela Investimentos
// GojoPurple já está definida como cripto.
val Fundos = Color(0xFDA00BC7)
val Coe = Color(0xFD8b1fa5)
val TesouroD = Color(0xFDae27ce)
val RendaV = Color(0xFEA64EBD)
val RendaF = Color(0xFE9400d3)