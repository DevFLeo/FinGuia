package com.finguia.ui.formato

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.finguia.motor.MascaraMoeda
import com.finguia.motor.NumeroBR

// Atalhos Kotlin para o motor Java de formatacao (com.finguia.motor).
// Toda tela deve exibir e ler numeros por aqui, no padrao 0.000,00.

/** `1234.5` vira `"R$ 1.234,50"`. */
fun Double.emReais(): String = NumeroBR.moeda(this)

/** `10.0` vira `"+R$ 10,00"`; negativo vira `"-R$ 10,00"`. */
fun Double.emReaisComSinal(): String = NumeroBR.moedaComSinal(this)

/** `1234.5` vira `"1.234,50"` (sem R$). */
fun Double.emNumeroBR(casas: Int = 2): String = NumeroBR.formatar(this, casas)

/** `12.5` vira `"12,50%"`. */
fun Double.emPercentual(casas: Int = 2): String = NumeroBR.percentual(this, casas)

/** `3.2` vira `"+3,20%"`. */
fun Double.emPercentualComSinal(casas: Int = 2): String = NumeroBR.percentualComSinal(this, casas)

/** Le o texto de um campo aceitando `1.234,56`, `1234,56` e `12.5`. Invalido vira null. */
fun String.lerNumeroBR(): Double? = NumeroBR.ler(this)

/**
 * Mascara de dinheiro para OutlinedTextField: o estado guarda so digitos
 * (`"123456"`) e a tela mostra `1.234,56`. Use com [digitosMoeda] no
 * onValueChange e [MascaraMoeda.reais] para obter o valor.
 */
object MascaraMoedaBR : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val exibido = MascaraMoeda.exibir(text.text)
        // Os digitos entram pela direita: qualquer posicao do cursor cai no fim
        val mapa = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = exibido.length
            override fun transformedToOriginal(offset: Int): Int = text.text.length
        }
        return TransformedText(AnnotatedString(exibido), mapa)
    }
}

/** Filtra o que foi digitado num campo com [MascaraMoedaBR]. */
fun digitosMoeda(entrada: String): String = MascaraMoeda.limpar(entrada)
