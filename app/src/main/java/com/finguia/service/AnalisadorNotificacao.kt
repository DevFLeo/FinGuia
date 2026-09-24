package com.finguia.service

import com.finguia.dados.TipoTransacao
import com.finguia.motor.DinheiroBR
import com.finguia.motor.NumeroBR
import java.text.Normalizer

/** Resultado de [AnalisadorNotificacao.analisar]: uma transacao ou o motivo do descarte. */
sealed interface Analise {
    data class Transacao(
        val tipo: TipoTransacao,
        val valor: Double,
        val descricao: String,
        /** Quem pagou ou recebeu, quando o texto diz. */
        val contraparte: String?,
    ) : Analise

    data class Descartada(val motivo: String) : Analise
}

/**
 * Analisa o conteúdo textual de notificações bancárias para extrair
 * tipo de transação e valor monetário.
 *
 * Toda comparação de palavras é feita sobre o texto normalizado: minúsculo e
 * sem acentos. SMS e alguns apps escrevem "voce recebeu" sem acento, e sem a
 * normalização essas notificações não casavam com nenhuma palavra-chave.
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
            "enviou dinheiro para você", "você tem um novo pix",
            "te transferiu via pix", "te transferiu",
            // Mercado Pago: "Você depositou R$ X via Pix" = Pix recebido
            // (do ponto de vista deles o usuário "depositou" na conta MP).
            "depositou via pix", "depositou r$", "seu dinheiro já está disponível",
            "dinheiro na sua conta", "caiu na sua conta", "entrou na sua conta"
        ),
        // Avisos: nao e dinheiro se movendo, entao e neutro nos totais
        TipoTransacao.COBRANCA to listOf(
            "fatura vencendo", "fatura disponível", "fatura fechada", "fatura vence",
            "cobrança agendada", "débito automático", "assinatura debitada",
            "mensalidade", "vencimento hoje", "vencimento amanhã", "cobrança",
            // lembretes de boleto
            "boleto que vence", "boleto vencendo", "novo boleto", "chegou 1 boleto",
            "tem boleto", "boleto disponível", "vence amanhã", "vencendo amanhã", "vence hoje"
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
            "débito aprovado", "débito autorizado",
            // mais longas que "compra aprovada" (credito), senao o debito virava credito
            "compra aprovada no débito", "compra autorizada no débito",
            "transação aprovada no débito",
            // "Você pagou LOJA" + "Debitamos R$ X da sua conta"
            "você pagou", "debitamos"
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
            "devolução", "reembolso", "chargeback",
            // mais longa que "você recebeu", senao o estorno virava Pix
            "recebeu um estorno"
        ),
        TipoTransacao.SAQUE to listOf(
            "saque no caixa", "saque realizado", "saque efetuado",
            "retirada em espécie", "retirada"
        ),
        TipoTransacao.DEPOSITO to listOf(
            "depósito identificado", "depósito em conta", "depósito realizado",
            "crédito por depósito"
        )
    ).map { (tipo, palavras) -> tipo to palavras.map(::normalizar) }

    /**
     * Frases que so aparecem em propaganda: descartam a notificacao sempre.
     * Vieram de notificacoes reais que o app gravava como transacao, como
     * ofertas de renegociacao ("quite sua divida a vista") e SMS de operadora.
     */
    private val PROMOCIONAL_FORTE = listOf(
        "quite sua dívida", "quite a dívida", "quite à vista", "quitar sua dívida",
        "saiu pra entrega", "saiu para entrega", "recebeu um desconto", "de desconto",
        "proposta", "cupom", "promoção", "pré-aprovado", "pré aprovado",
        "oferta pra você", "oferta para você", "sua oferta"
    ).map(::normalizar)

    /**
     * Palavras de propaganda que tambem podem aparecer no fim de uma notificacao
     * de transacao real ("Pix recebido! Aproveite..."). So descartam quando o
     * texto nao tem uma frase especifica de transacao.
     */
    private val PROMOCIONAL_FRACO = listOf(
        "oferta", "aproveite", "ganhe ", "convite", "sorteio", "novidade"
    ).map(::normalizar)

    /** Valor logo depois destas palavras e saldo/limite, nao a transacao. */
    private val ANTES_DE_VALOR_IGNORADO = listOf("saldo", "limite", "disponível:").map(::normalizar)

    /** Valor seguido destas palavras e desconto/brinde, nao a transacao. */
    private val DEPOIS_DE_VALOR_IGNORADO = listOf("de desconto", "de cashback", "de bônus").map(::normalizar)

    private val INDICADORES_ENTRADA = listOf(
        "recebido", "recebeu", "creditado", "crédito", "entrou",
        "depositado", "caiu na conta"
    ).map(::normalizar)

    private val INDICADORES_SAIDA = listOf(
        "enviado", "enviou", "debitado", "débito", "pago", "pagou",
        "comprou", "compra", "saiu", "sacado"
    ).map(::normalizar)

    // --------------------------------------------------------------- API

    /**
     * Decide se a notificacao vira transacao. Concentra todas as regras de
     * descarte para que o servico so precise gravar o resultado.
     */
    fun analisar(titulo: String?, texto: String?, banco: String): Analise {
        val original = juntar(titulo, texto)
        val conteudo = normalizar(original)
        if (conteudo.isBlank()) return Analise.Descartada("notificação vazia")

        PROMOCIONAL_FORTE.firstOrNull { conteudo.contains(it) }?.let {
            return Analise.Descartada("propaganda (\"$it\")")
        }

        val (tipo, especifico) = classificar(conteudo)
        if (!especifico) {
            PROMOCIONAL_FRACO.firstOrNull { conteudo.contains(it) }?.let {
                return Analise.Descartada("propaganda (\"${it.trim()}\")")
            }
        }

        val valor = escolherValor(original)
        if (tipo == TipoTransacao.DESCONHECIDO && valor == 0.0) {
            return Analise.Descartada("sem tipo nem valor")
        }
        // Tipo reconhecido sem valor ("Voce recebeu um Pix. Abra o app") nao
        // tem o que registrar; antes virava lancamento de R$ 0,00 no extrato
        if (valor == 0.0) return Analise.Descartada("sem valor")

        val contraparte = extrairContraparte(original, tipo)
        return Analise.Transacao(
            tipo = tipo,
            valor = valor,
            descricao = gerarDescricao(tipo, banco, valor, contraparte),
            contraparte = contraparte
        )
    }

    /**
     * Escolhe o tipo cuja palavra-chave mais longa aparece no conteúdo.
     * Isso resolve colisões como "pix de" (dentro de "você enviou um pix de...")
     * sendo erroneamente pareado com PIX_RECEBIDO quando o correto é PIX_ENVIADO.
     */
    fun identificarTipo(titulo: String?, texto: String?): TipoTransacao =
        classificar(normalizar(juntar(titulo, texto))).first

    /**
     * Extrai o valor monetário da transação. Retorna 0.0 se nenhum valor for
     * encontrado. Veja [escolherValor] para as regras de desempate.
     */
    fun extrairValor(titulo: String?, texto: String?): Double =
        escolherValor(juntar(titulo, texto))

    /**
     * Gera uma descrição legível resumindo a transação capturada. Com a
     * contraparte, a descricao diz quem: "Pix recebido de Fulano".
     */
    fun gerarDescricao(
        tipo: TipoTransacao,
        banco: String,
        valor: Double,
        contraparte: String? = null
    ): String {
        val valorFormatado = NumeroBR.moeda(valor)
        if (contraparte != null) {
            return when (tipo) {
                TipoTransacao.PIX_RECEBIDO           -> "Pix recebido de $contraparte"
                TipoTransacao.PIX_ENVIADO            -> "Pix enviado para $contraparte"
                TipoTransacao.COMPRA_CREDITO         -> "Compra no crédito em $contraparte"
                TipoTransacao.COMPRA_DEBITO          -> "Compra no débito em $contraparte"
                TipoTransacao.BOLETO_PAGO            -> "Boleto pago para $contraparte"
                TipoTransacao.TRANSFERENCIA_RECEBIDA -> "Transferência recebida de $contraparte"
                TipoTransacao.TRANSFERENCIA_ENVIADA  -> "Transferência enviada para $contraparte"
                TipoTransacao.COBRANCA               -> "Boleto de $valorFormatado para $contraparte"
                else -> "${gerarDescricao(tipo, banco, valor)} ($contraparte)"
            }
        }
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

    // ---------------------------------------------------------- internos

    /**
     * Tipo pela palavra-chave mais longa, e se ele veio de uma frase
     * especifica (true) ou so do fallback direcional (false).
     */
    private fun classificar(conteudo: String): Pair<TipoTransacao, Boolean> {
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
        if (melhorTipo != TipoTransacao.DESCONHECIDO) return melhorTipo to true

        // Fallback direcional: quando o texto não casa com nenhum padrão
        // específico mas indica claramente direção, classifica como
        // transferência genérica para não ficar fora dos totais.
        val temEntrada = INDICADORES_ENTRADA.any { conteudo.contains(it) }
        val temSaida = INDICADORES_SAIDA.any { conteudo.contains(it) }
        val tipo = when {
            temEntrada && !temSaida -> TipoTransacao.TRANSFERENCIA_RECEBIDA
            temSaida && !temEntrada -> TipoTransacao.TRANSFERENCIA_ENVIADA
            else -> TipoTransacao.DESCONHECIDO
        }
        return tipo to false
    }

    /**
     * Primeiro valor que nao seja saldo, limite ou desconto. Se todos forem,
     * devolve 0: a notificacao so fala de saldo, nao de uma transacao.
     */
    private fun escolherValor(original: String): Double {
        var fimAnterior = 0
        for (m in REGEX_VALOR.findAll(original)) {
            val inicioJanela = maxOf(fimAnterior, m.range.first - 25)
            val antes = normalizar(original.substring(inicioJanela, m.range.first))
            val depois = normalizar(original.substring(m.range.last + 1, minOf(original.length, m.range.last + 16)))
            fimAnterior = m.range.last + 1

            if (ANTES_DE_VALOR_IGNORADO.any { antes.contains(it) }) continue
            if (DEPOIS_DE_VALOR_IGNORADO.any { depois.trimStart().startsWith(it) }) continue

            val bruto = m.groupValues[1].ifEmpty { m.groupValues[2] }
            val centavos = DinheiroBR.normalizar(bruto) ?: continue
            return DinheiroBR.paraReais(centavos)
        }
        return 0.0
    }

    // Nome: comeca com maiuscula (evita "para pagar"), ate 6 palavras.
    // Pontos internos sao permitidos por causa de "S.A." e "LTDA.".
    private const val NOME = """([A-ZÀ-Ý0-9][\p{L}0-9&'*-]*(?:[ .][\p{L}0-9&'*-]+){0,5}\.?)"""
    // A quebra de linha separa titulo e texto (ver juntar) e tambem encerra o nome
    private const val FIM = """(?=\.?\n|\s+via\b|\s+no valor\b|\s+-\s|[,;!]|\.\s|\.?$)"""

    private val PADROES_ENTRADA = listOf(
        Regex("""que $NOME te transferiu"""),
        Regex("""(?:recebid[oa]|transferência|Pix) de $NOME$FIM"""),
    )
    private val PADROES_SAIDA = listOf(
        Regex("""(?i:você pagou|voce pagou) $NOME$FIM"""),
        Regex("""para $NOME$FIM"""),
        Regex("""\bem ([A-Z0-9][A-Z0-9 .&*'-]{2,40}?)$FIM"""),
    )

    /** Quem pagou ou recebeu, se o texto disser. Nomes genericos sao ignorados. */
    private fun extrairContraparte(original: String, tipo: TipoTransacao): String? {
        val padroes = when (tipo) {
            TipoTransacao.PIX_RECEBIDO, TipoTransacao.TRANSFERENCIA_RECEBIDA,
            TipoTransacao.DEPOSITO -> PADROES_ENTRADA
            TipoTransacao.PIX_ENVIADO, TipoTransacao.TRANSFERENCIA_ENVIADA,
            TipoTransacao.COMPRA_CREDITO, TipoTransacao.COMPRA_DEBITO,
            TipoTransacao.BOLETO_PAGO, TipoTransacao.COBRANCA -> PADROES_SAIDA
            else -> return null
        }
        for (padrao in padroes) {
            val nome = padrao.find(original)?.groupValues?.get(1)?.let(::limparNome) ?: continue
            if (nome.length >= 2 && normalizar(nome) !in NOMES_GENERICOS && !nome.startsWith("R$")) {
                return nome
            }
        }
        return null
    }

    private val NOMES_GENERICOS = setOf("voce", "sua conta", "conta", "app", "o app", "pix")

    private fun limparNome(bruto: String): String {
        var nome = bruto.trim().replace(Regex("""\s+"""), " ")
        // "Empresa LTDA." -> "Empresa LTDA", mas "Banco S.A." fica como esta
        if (nome.endsWith(".") && !Regex("""\b\p{L}\.\p{L}\.$""").containsMatchIn(nome)) {
            nome = nome.dropLast(1)
        }
        return nome.take(40).trim()
    }

    /**
     * Titulo e texto separados por quebra de linha: assim "Voce pagou Loja"
     * no titulo nao emenda com a primeira palavra do texto ao extrair nomes.
     */
    private fun juntar(titulo: String?, texto: String?): String =
        limparTexto("${titulo.orEmpty()}\n${texto.orEmpty()}")

    /** Remove marcas invisiveis de direcao de texto que SMS inserem (U+2066..U+2069). */
    private fun limparTexto(texto: String): String =
        texto.replace(Regex("[⁦-⁩‎‏]"), "").trim()

    /** Minusculo e sem acentos, para comparar palavras sem depender da grafia. */
    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
