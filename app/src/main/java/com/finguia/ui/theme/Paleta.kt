package com.finguia.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Tema escolhido pelo usuario em Configuracoes. */
enum class TemaApp(val rotulo: String) {
    SISTEMA("Seguir o sistema"),
    ESCURO("Escuro"),
    CLARO("Claro"),
}

/**
 * Cores de superficie e texto que mudam entre o tema escuro e o claro.
 *
 * As telas nao leem esta classe direto: usam DarkBg, CardBg, GrayText,
 * TextoForte etc. (Color.kt), que consultam a paleta ativa. Assim o tema
 * troca sem precisar mexer em cada tela.
 */
@Immutable
data class PaletaFinGuia(
    val ehEscura: Boolean,
    /** Fundo das telas. */
    val fundo: Color,
    /** Cartoes sobre o fundo. */
    val cartao: Color,
    /** Cartao dentro de cartao, chips e abas inativas. */
    val cartaoElevado: Color,
    val borda: Color,
    /** Parte vazia de barras de progresso. */
    val trilho: Color,
    /** Cor do topo do degrade dos cabecalhos de investimento. */
    val destaqueTopo: Color,
    /** Texto principal: titulos, valores, rotulos. */
    val textoForte: Color,
    /** Texto secundario: legendas, dicas, placeholders. */
    val textoFraco: Color,
    /** Dinheiro entrando e lucro. */
    val positivo: Color,
    /** Dinheiro saindo e prejuizo. */
    val negativo: Color,
)

val PaletaEscura = PaletaFinGuia(
    ehEscura = true,
    fundo = Color(0xFF0A0A0C),
    cartao = Color(0xFF16161E),
    cartaoElevado = Color(0xFF1E1E2E),
    borda = Color(0xFF2A2A3E),
    trilho = Color(0xFF222222),
    destaqueTopo = Color(0xFF1A1040),
    textoForte = Color.White,
    textoFraco = Color(0xFF888888),
    positivo = Color(0xFF2ECC71),
    negativo = Color(0xFFFF4D4D),
)

// Verde e vermelho mais escuros que no tema escuro: #2ECC71 sobre branco
// tem contraste perto de 2:1 e os valores ficariam dificeis de ler.
val PaletaClara = PaletaFinGuia(
    ehEscura = false,
    fundo = Color(0xFFF4F4F8),
    cartao = Color(0xFFFFFFFF),
    cartaoElevado = Color(0xFFECEBF3),
    borda = Color(0xFFDCDAE6),
    trilho = Color(0xFFE3E1EC),
    destaqueTopo = Color(0xFFE6E0FF),
    textoForte = Color(0xFF16161E),
    textoFraco = Color(0xFF6B6B78),
    positivo = Color(0xFF15803D),
    negativo = Color(0xFFC62828),
)

val LocalPaleta = staticCompositionLocalOf { PaletaEscura }
