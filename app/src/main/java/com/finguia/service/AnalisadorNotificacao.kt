package com.finguia.service

import com.finguia.dados.TipoTransacao

/**
 * Analisa o conteúdo textual de notificações bancárias para extrair
 * tipo de transação e valor monetário.
 */
object AnalisadorNotificacao {

    // Regex para valores em formato brasileiro. Prioriza:
    //  1. Valores com R$ e decimais: R$ 1.500,00 | R$250,00 | R$ 0,99
    //  2. Valores com R$ sem decimais: R$ 1.500 | R$ 50
    //  3. Valores soltos no formato 1.234,56 (evita pegar datas tipo 12/05)
    private val REGEX_VALOR = Regex(
        """R\$\s*(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2}|\d{1,3}(?:\.\d{3})+|\d+)""" +
            """|\b(\d{1,3}(?:\.\d{3})*,\d{2})\b""",
        RegexOption.IGNORE_CASE
    )

    // Palavras-chave por tipo. A ordem NÃO define prioridade — o algoritmo
    // escolhe a palavra-chave mais longa (mais específica) que casar, para
    // evitar que termos genéricos como "fatura" ou "pix de" engulam frases
    // específicas como "fatura vencendo" ou "você enviou um pix".
    private val PALAVRAS_TIPO: List<Pair<TipoTransacao, List<String>>> = listOf(
        TipoTransacao.PIX_ENVIADO to listOf(
            "você enviou um pix", "transferência pix enviada", "pix enviado",
            "pix realizado", "pix efetuado", "pix debitado", "pix para ",
            "enviou pix", "pix de saída", "pix de envio"
        ),
        TipoTransacao.PIX_RECEBIDO to listOf(
            "você recebeu um pix", "transferência pix recebida", "pix recebido",
            "recebeu via pix", "recebeu pix", "chegou um pix", "pix creditado",
            "pix de entrada",
            // Mercado Pago, PicPay, PagBank frequentemente omitem "pix"
            "você recebeu", "dinheiro recebido", "transferiu para você",
            "enviou dinheiro para você", "você tem um novo pix"
        ),
        TipoTransacao.COBRANCA to listOf(
            "fatura vencendo", "fatura disponível", "fatura fechada",
            "cobrança agendada", "débito automático", "assinatura debitada",
            "mensalidade", "vencimento hoje", "vencimento amanhã", "cobrança"
        ),
        TipoTransacao.COMPRA_CREDITO to listOf(
            "compra autorizada no crédito", "transação aprovada no crédito",
            "compra aprovada no crédito", "compra no crédito", "compra crédito",
            "crédito aprovado", "crédito autorizado", "parcelado em",
            "compra aprovada"
        ),
        TipoTransacao.COMPRA_DEBITO to listOf(
            "compra realizada no débito", "compra efetuada no débito",
            "transação no débito", "compra no débito", "compra débito",
            "débito aprovado", "débito autorizado"
        ),
        TipoTransacao.BOLETO_PAGO to listOf(
            "pagamento de boleto", "débito de boleto", "boleto compensado",
            "boleto quitado", "boleto pago", "conta quitada", "conta paga",
            "pagamento efetuado", "pagamento realizado"
        ),
        TipoTransacao.TRANSFERENCIA_RECEBIDA to listOf(
            "transferência recebida", "transferência creditada",
            "ted recebido", "ted creditado", "doc recebido",
            "crédito em conta", "depósito recebido"
        ),
        TipoTransacao.TRANSFERENCIA_ENVIADA to listOf(
            "transferência enviada", "transferência realizada",
            "transferência debitada", "ted enviado", "ted efetuado", "doc enviado"
        ),
        TipoTransacao.ESTORNO to listOf(
            "crédito de estorno", "valor estornado", "estorno",
            "devolução", "reembolso", "chargeback"
        ),
        TipoTransacao.SAQUE to listOf(
            "saque no caixa", "saque realizado", "saque efetuado",
            "retirada em espécie", "retirada"
        ),
        TipoTransacao.DEPOSITO to listOf(
            "depósito identificado", "depósito em conta", "depósito realizado",
            "crédito por depósito"
        )
    )

    /**
     * Escolhe o tipo cuja palavra-chave mais longa aparece no conteúdo.
     * Isso resolve colisões como "pix de" (dentro de "você enviou um pix de...")
     * sendo erroneamente pareado com PIX_RECEBIDO quando o correto é PIX_ENVIADO.
     */
    fun identificarTipo(titulo: String?, texto: String?): TipoTransacao {
        val conteudo = "${titulo.orEmpty()} ${texto.orEmpty()}".lowercase()

        var melhorTipo = TipoTransacao.DESCONHECIDO
        var melhorTamanho = 0
        for ((tipo, palavras) in PALAVRAS_TIPO) {
            for (palavra in palavras) {
                if (palavra.length > melhorTamanho && conteudo.contains(palavra)) {
                    melhorTamanho = palavra.length
                    melhorTipo = tipo
                }
            }
        }
        if (melhorTipo != TipoTransacao.DESCONHECIDO) return melhorTipo

        // Fallback direcional: quando o texto não casa com nenhum padrão
        // específico mas indica claramente direção, classifica como
        // transferência genérica para não ficar fora dos totais.
        val indicadoresEntrada = listOf(
            "recebido", "recebeu", "creditado", "crédito", "entrou",
            "depositado", "caiu na conta"
        )
        val indicadoresSaida = listOf(
            "enviado", "enviou", "debitado", "débito", "pago", "pagou",
            "comprou", "compra", "saiu", "sacado"
        )
        val temEntrada = indicadoresEntrada.any { conteudo.contains(it) }
        val temSaida = indicadoresSaida.any { conteudo.contains(it) }
        return when {
            temEntrada && !temSaida -> TipoTransacao.TRANSFERENCIA_RECEBIDA
            temSaida && !temEntrada -> TipoTransacao.TRANSFERENCIA_ENVIADA
            else -> TipoTransacao.DESCONHECIDO
        }
    }

    /**
     * Extrai o primeiro valor monetário encontrado no texto.
     * Retorna 0.0 se nenhum valor for encontrado.
     */
    fun extrairValor(titulo: String?, texto: String?): Double {
        val conteudo = "${titulo.orEmpty()} ${texto.orEmpty()}"
        val match = REGEX_VALOR.find(conteudo) ?: return 0.0

        val valorBruto = match.groupValues[1].ifEmpty { match.groupValues[2] }.trim()
        if (valorBruto.isEmpty()) return 0.0

        // Formato BR: "." é milhar, "," é decimal.
        // Se há vírgula, ela é o separador decimal.
        // Se não há vírgula, removemos os pontos (milhar) e tratamos como inteiro.
        val normalizado = if (valorBruto.contains(',')) {
            valorBruto.replace(".", "").replace(",", ".")
        } else {
            valorBruto.replace(".", "")
        }

        return normalizado.toDoubleOrNull() ?: 0.0
    }

    /**
     * Gera uma descrição legível resumindo a transação capturada.
     */
    fun gerarDescricao(tipo: TipoTransacao, banco: String, valor: Double): String {
        val valorFormatado = "R$ %.2f".format(valor).replace(".", ",")
        return when (tipo) {
            TipoTransacao.PIX_RECEBIDO         -> "Pix recebido de $valorFormatado via $banco"
            TipoTransacao.PIX_ENVIADO          -> "Pix enviado de $valorFormatado via $banco"
            TipoTransacao.COMPRA_CREDITO       -> "Compra no crédito de $valorFormatado em $banco"
            TipoTransacao.COMPRA_DEBITO        -> "Compra no débito de $valorFormatado em $banco"
            TipoTransacao.BOLETO_PAGO          -> "Boleto pago de $valorFormatado via $banco"
            TipoTransacao.TRANSFERENCIA_RECEBIDA -> "Transferência recebida de $valorFormatado em $banco"
            TipoTransacao.TRANSFERENCIA_ENVIADA  -> "Transferência enviada de $valorFormatado via $banco"
            TipoTransacao.ESTORNO              -> "Estorno de $valorFormatado em $banco"
            TipoTransacao.SAQUE                -> "Saque de $valorFormatado em $banco"
            TipoTransacao.DEPOSITO             -> "Depósito de $valorFormatado em $banco"
            TipoTransacao.COBRANCA             -> "Cobrança de $valorFormatado em $banco"
            TipoTransacao.DESCONHECIDO         -> "Movimentação detectada em $banco"
        }
    }
}
