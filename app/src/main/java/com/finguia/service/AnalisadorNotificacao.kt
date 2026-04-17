package com.finguia.service

import com.finguia.dados.TipoTransacao

/**
 * Analisa o conteúdo textual de notificações bancárias para extrair
 * tipo de transação e valor monetário.
 */
object AnalisadorNotificacao {

    // Regex para capturar valores monetários em formato brasileiro
    // Ex: R$ 1.500,00 | R$250,00 | 1.200,50 | R$ 0,99
    private val REGEX_VALOR = Regex(
        """R\$\s*([\d.,]+)|([\d]{1,3}(?:\.\d{3})*(?:,\d{2}))""",
        RegexOption.IGNORE_CASE
    )

    // Palavras-chave mapeadas para tipos de transação (lowercase para comparação)
    private val PALAVRAS_TIPO: List<Pair<TipoTransacao, List<String>>> = listOf(
        TipoTransacao.PIX_RECEBIDO to listOf(
            "pix recebido", "você recebeu um pix", "recebeu via pix",
            "transferência pix recebida", "pix de ", "recebeu pix",
            "chegou um pix", "pix creditado"
        ),
        TipoTransacao.PIX_ENVIADO to listOf(
            "pix enviado", "você enviou um pix", "pix realizado",
            "transferência pix enviada", "pix efetuado", "pix debitado",
            "pix para ", "enviou pix"
        ),
        TipoTransacao.COMPRA_CREDITO to listOf(
            "compra no crédito", "compra crédito", "crédito aprovado",
            "compra aprovada", "parcelado em", "fatura", "crédito autorizado",
            "compra autorizada no crédito", "transação aprovada no crédito"
        ),
        TipoTransacao.COMPRA_DEBITO to listOf(
            "compra no débito", "compra débito", "débito aprovado",
            "compra efetuada no débito", "transação no débito",
            "compra realizada no débito", "débito autorizado"
        ),
        TipoTransacao.BOLETO_PAGO to listOf(
            "boleto pago", "pagamento de boleto", "boleto compensado",
            "boleto quitado", "pagamento efetuado", "conta paga",
            "pagamento realizado", "conta quitada", "débito de boleto"
        ),
        TipoTransacao.TRANSFERENCIA_RECEBIDA to listOf(
            "transferência recebida", "ted recebido", "doc recebido",
            "crédito em conta", "depósito recebido", "ted creditado",
            "transferência creditada"
        ),
        TipoTransacao.TRANSFERENCIA_ENVIADA to listOf(
            "transferência enviada", "ted enviado", "doc enviado",
            "transferência realizada", "ted efetuado", "transferência debitada"
        ),
        TipoTransacao.ESTORNO to listOf(
            "estorno", "devolução", "reembolso", "chargeback",
            "cancelamento", "crédito de estorno", "valor estornado"
        ),
        TipoTransacao.SAQUE to listOf(
            "saque realizado", "retirada", "saque no caixa",
            "saque efetuado", "retirada em espécie"
        ),
        TipoTransacao.DEPOSITO to listOf(
            "depósito realizado", "depósito em conta", "depósito identificado",
            "crédito por depósito"
        ),
        TipoTransacao.COBRANCA to listOf(
            "cobrança", "fatura vencendo", "fatura disponível",
            "débito automático", "cobrança agendada", "vencimento",
            "mensalidade", "assinatura debitada"
        )
    )

    /**
     * Tenta identificar o tipo de transação com base no texto completo da notificação.
     * Combina título + texto para maximizar a detecção.
     */
    fun identificarTipo(titulo: String?, texto: String?): TipoTransacao {
        val conteudo = "${titulo.orEmpty()} ${texto.orEmpty()}".lowercase()

        for ((tipo, palavras) in PALAVRAS_TIPO) {
            if (palavras.any { conteudo.contains(it) }) {
                return tipo
            }
        }
        return TipoTransacao.DESCONHECIDO
    }

    /**
     * Extrai o primeiro valor monetário encontrado no texto.
     * Retorna 0.0 se nenhum valor for encontrado.
     */
    fun extrairValor(titulo: String?, texto: String?): Double {
        val conteudo = "${titulo.orEmpty()} ${texto.orEmpty()}"
        val match = REGEX_VALOR.find(conteudo) ?: return 0.0

        // Grupo 1 = com prefixo R$, Grupo 2 = apenas número formatado
        val valorBruto = (match.groupValues[1].ifEmpty { match.groupValues[2] })
            .trim()
            .replace(".", "")   // remove separador de milhar
            .replace(",", ".") // troca vírgula decimal por ponto

        return valorBruto.toDoubleOrNull() ?: 0.0
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
