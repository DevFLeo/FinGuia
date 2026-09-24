package com.finguia.service

import com.finguia.dados.TipoTransacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Casos montados a partir dos padroes reais de notificacao dos bancos, com
 * nomes e valores ficticios. Cada grupo corresponde a um tipo de ruido ou de
 * transacao que o app encontrou em uso.
 */
class AnalisadorNotificacaoTest {

    private fun transacao(titulo: String, texto: String, banco: String = "Banco"): Analise.Transacao {
        return when (val r = AnalisadorNotificacao.analisar(titulo, texto, banco)) {
            is Analise.Transacao -> r
            is Analise.Descartada -> fail("esperava transacao, foi descartada: ${r.motivo}") as Nothing
        }
    }

    private fun descartada(titulo: String, texto: String): Analise.Descartada {
        return when (val r = AnalisadorNotificacao.analisar(titulo, texto, "Banco")) {
            is Analise.Descartada -> r
            is Analise.Transacao -> fail("esperava descarte, virou ${r.tipo} de ${r.valor}") as Nothing
        }
    }

    // ------------------------------------------------ transacoes reais

    @Test
    fun pixRecebido_comValorNoTituloEContraparteNoTexto() {
        val t = transacao(
            "Você recebeu R$ 15",
            "O valor que Fulano de Tal te transferiu via Pix já está rendendo."
        )
        assertEquals(TipoTransacao.PIX_RECEBIDO, t.tipo)
        assertEquals(15.0, t.valor, 0.001)
        assertEquals("Fulano de Tal", t.contraparte)
        assertTrue(t.descricao, t.descricao.contains("Fulano de Tal"))
    }

    @Test
    fun pixRecebido_depositoNaCarteira() {
        val t = transacao("Seu dinheiro já está disponível",
            "Você depositou R$ 63 via Pix e o valor já está rendendo na sua conta.")
        assertEquals(TipoTransacao.PIX_RECEBIDO, t.tipo)
        assertEquals(63.0, t.valor, 0.001)
    }

    @Test
    fun pagamentoDebitado_eSaidaComContraparteDoTitulo() {
        val t = transacao("Você pagou Loja Exemplo", "Debitamos R$ 15,54 da sua conta.")
        assertTrue("deve ser saida, foi ${t.tipo}", t.tipo in SAIDAS)
        assertEquals(15.54, t.valor, 0.001)
        assertEquals("Loja Exemplo", t.contraparte)
    }

    @Test
    fun pixEnviado_comDestinatario() {
        val t = transacao("Pix enviado", "Você enviou um Pix de R$ 50,00 para Maria Souza.")
        assertEquals(TipoTransacao.PIX_ENVIADO, t.tipo)
        assertEquals(50.0, t.valor, 0.001)
        assertEquals("Maria Souza", t.contraparte)
    }

    @Test
    fun compraNoCredito() {
        val t = transacao("Compra aprovada", "Compra aprovada no crédito de R$ 99,90 em MERCADO CENTRAL")
        assertEquals(TipoTransacao.COMPRA_CREDITO, t.tipo)
        assertEquals(99.9, t.valor, 0.001)
    }

    @Test
    fun compraAprovadaNoDebito_naoViraCredito() {
        // "compra aprovada" e frase do credito; sem a frase especifica do
        // debito, compras no debito eram registradas como credito
        val t = transacao("BANCO", "Compra aprovada no debito de R$ 45,60 em PADARIA CENTRAL")
        assertEquals(TipoTransacao.COMPRA_DEBITO, t.tipo)
        assertEquals(45.6, t.valor, 0.001)
        assertEquals("PADARIA CENTRAL", t.contraparte)
    }

    @Test
    fun debitoAutomaticoPago_ehBoletoPagoNaoCobranca() {
        val t = transacao("Débito automático", "Débito automático: pagamento efetuado de R$ 89,90")
        assertEquals(TipoTransacao.BOLETO_PAGO, t.tipo)
    }

    @Test
    fun estorno_naoViraPixRecebido() {
        // Antes: "você recebeu" (12 letras) vencia "estorno" (7) e virava Pix
        val t = transacao("Estorno", "Você recebeu um estorno de R$ 30,00 da compra em LOJA X")
        assertEquals(TipoTransacao.ESTORNO, t.tipo)
    }

    @Test
    fun valor_ignoraSaldoEscritoAntesDoValorDaTransacao() {
        val t = transacao("Pix recebido", "Saldo disponível R$ 1.200,00. Pix recebido de R$ 50,00")
        assertEquals(50.0, t.valor, 0.001)
    }

    @Test
    fun valor_ignoraLimiteDisponivel() {
        val t = transacao("Compra aprovada",
            "Compra aprovada no crédito de R$ 42,00. Limite disponível: R$ 3.000,00")
        assertEquals(42.0, t.valor, 0.001)
    }

    // ------------------------------------------ lembretes (neutros)

    @Test
    fun boletoQueVenceAmanha_ehCobranca() {
        val t = transacao("Você tem 1 boleto que vence amanhã",
            "Mantenha suas contas em dia, faça o pagamento de R$ 81,81 para Empresa Exemplo LTDA.")
        assertEquals(TipoTransacao.COBRANCA, t.tipo)
        assertEquals(81.81, t.valor, 0.001)
    }

    @Test
    fun boletoVencendo_ehCobranca() {
        val t = transacao("Você tem boleto vencendo amanhã",
            "O boleto EMPRESA EXEMPLO no valor de R$ 81,81 vence amanhã. Clique aqui para pagar ou agendar.")
        assertEquals(TipoTransacao.COBRANCA, t.tipo)
    }

    @Test
    fun novoBoleto_ehCobranca() {
        val t = transacao("Você tem um novo boleto", "EMPRESA DE INTERNET S A  - R$ 80,00")
        assertEquals(TipoTransacao.COBRANCA, t.tipo)
        assertEquals(80.0, t.valor, 0.001)
    }

    @Test
    fun boletoComDataNoTitulo_naoLeDataComoValor() {
        val t = transacao("Chegou 1 boleto que vence em 10/mai.",
            "Você já pode agendar ou fazer o pagamento de R$ 3.519,18 para Banco Exemplo S.A..")
        assertEquals(TipoTransacao.COBRANCA, t.tipo)
        assertEquals(3519.18, t.valor, 0.001)
    }

    // --------------------------------------------------- ruido

    @Test
    fun ofertaDeRenegociacao_descartada() {
        descartada("Seu cartão pode voltar pra sua rotina",
            "Quite sua dívida à vista e recupere a função crédito. Veja sua proposta no app.")
        descartada("Novo começo com seu cartão",
            "Quite à vista, reative seu cartão e volte a usar o crédito. Toque e veja sua oferta.")
    }

    @Test
    fun descontoOferecido_naoViraPixRecebido() {
        descartada("Temos uma boa notícia",
            "Você recebeu um desconto para quitar sua dívida e evitar que seu nome vá para o Serasa.")
    }

    @Test
    fun smsPromocional_descartado() {
        descartada("Operadora",
            "OFERTA: tem oferta pra voce adiantar o presente de Dia das Maes: R$100 de desconto")
    }

    @Test
    fun cartaoSaiuPraEntrega_descartado() {
        descartada("Oba! Seu cartão saiu pra entrega",
            "É preciso ter alguém no local pra receber o cartão, beleza?")
    }

    @Test
    fun transacaoSemValor_descartada() {
        // Tipo reconhecido mas sem valor: nao ha o que registrar
        val d = descartada("Pix recebido", "Você recebeu um Pix. Abra o app para ver.")
        assertTrue(d.motivo, d.motivo.contains("valor"))
    }

    @Test
    fun textoSemNadaFinanceiro_descartado() {
        descartada("Atualize o app", "Uma nova versão está disponível.")
    }

    // ------------------------------------------------ contraparte

    @Test
    fun contraparte_boletoParaEmpresa() {
        val t = transacao("Você tem 1 boleto que vence amanhã",
            "Mantenha suas contas em dia, faça o pagamento de R$ 81,81 para Empresa Exemplo LTDA.")
        assertEquals("Empresa Exemplo LTDA", t.contraparte)
    }

    @Test
    fun contraparte_ausenteQuandoNaoHaPadrao() {
        val t = transacao("Compra aprovada", "Compra aprovada no crédito de R$ 99,90")
        assertNull(t.contraparte)
    }

    @Test
    fun descricao_usaValorNoPadraoBR() {
        val t = transacao("Pix recebido", "Pix recebido de R$ 1.234,56")
        assertTrue(t.descricao, t.descricao.contains("R$ 1.234,56"))
    }

    private companion object {
        val SAIDAS = setOf(
            TipoTransacao.PIX_ENVIADO, TipoTransacao.COMPRA_DEBITO, TipoTransacao.COMPRA_CREDITO,
            TipoTransacao.BOLETO_PAGO, TipoTransacao.TRANSFERENCIA_ENVIADA, TipoTransacao.SAQUE
        )
    }
}
