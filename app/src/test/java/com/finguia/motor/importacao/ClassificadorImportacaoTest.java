package com.finguia.motor.importacao;

import static org.junit.Assert.assertEquals;

import com.finguia.dados.TipoTransacao;
import com.finguia.motor.SentidoTransacao;
import org.junit.Test;

public class ClassificadorImportacaoTest {

    private static LancamentoExterno conta(double valor, String categoria, String descricao) {
        return new LancamentoExterno("t:" + descricao, 0L, valor, descricao, categoria, null, null, true, false);
    }

    private static LancamentoExterno cartao(double valor, String descricao) {
        return new LancamentoExterno("c:" + descricao, 0L, valor, descricao, "DEBIT", null, null, true, true);
    }

    private static TipoTransacao tipo(LancamentoExterno l) {
        return ClassificadorImportacao.classificar(l);
    }

    @Test
    public void pixPelaDescricao() {
        assertEquals(TipoTransacao.PIX_RECEBIDO, tipo(conta(50, "CREDIT", "PIX RECEBIDO FULANO")));
        assertEquals(TipoTransacao.PIX_ENVIADO, tipo(conta(-50, "DEBIT", "Pix enviado - Maria")));
    }

    @Test
    public void pixPelaCategoriaOpenFinance() {
        assertEquals(TipoTransacao.PIX_ENVIADO, tipo(conta(-10, "PIX", "MARIA SOUZA")));
    }

    @Test
    public void categoriasOfx() {
        assertEquals(TipoTransacao.SAQUE, tipo(conta(-100, "ATM", "RETIRADA AG 1234")));
        assertEquals(TipoTransacao.COMPRA_DEBITO, tipo(conta(-35.9, "POS", "MERCADO CENTRAL")));
        assertEquals(TipoTransacao.DEPOSITO, tipo(conta(2.13, "INT", "RENDIMENTO")));
        assertEquals(TipoTransacao.BOLETO_PAGO, tipo(conta(-200, "PAYMENT", "CONTA DE LUZ")));
    }

    @Test
    public void descricaoTipicaDosExtratos() {
        assertEquals(TipoTransacao.BOLETO_PAGO, tipo(conta(-80, "DEBIT", "PAGTO FATURA CARTAO")));
        assertEquals(TipoTransacao.COMPRA_DEBITO, tipo(conta(-12, "DEBIT", "Compra no débito - PADARIA")));
        assertEquals(TipoTransacao.TRANSFERENCIA_RECEBIDA, tipo(conta(1500, "CREDIT", "TED RECEBIDA EMPRESA")));
        assertEquals(TipoTransacao.ESTORNO, tipo(conta(30, "CREDIT", "Estorno de compra")));
    }

    @Test
    public void tedDentroDeOutraPalavra_naoContaComoTed() {
        // "creditado" nao pode ser lido como TED
        assertEquals(TipoTransacao.TRANSFERENCIA_RECEBIDA, tipo(conta(5, "CREDIT", "VALOR CREDITADO")));
        assertEquals(TipoTransacao.TRANSFERENCIA_ENVIADA, tipo(conta(-5, "FEE", "TARIFA CESTA")));
    }

    @Test
    public void pistaQueContradizOSinal_perdeParaOSinal() {
        // "deposito" sugere entrada, mas o valor saiu
        assertEquals(TipoTransacao.TRANSFERENCIA_ENVIADA, tipo(conta(-40, "DEBIT", "DEPOSITO JUDICIAL")));
    }

    @Test
    public void cartao_compraEPagamentoDaFatura() {
        assertEquals(TipoTransacao.COMPRA_CREDITO, tipo(cartao(-89.9, "LOJA X")));
        // Pagamento da fatura: neutro, senao vira receita falsa
        assertEquals(TipoTransacao.DESCONHECIDO, tipo(cartao(500, "PAGAMENTO EFETUADO")));
        assertEquals(TipoTransacao.ESTORNO, tipo(cartao(20, "LOJA X - AJUSTE")));
    }

    @Test
    public void sentidoSempreBateComOSinal_excetoPagamentoDeFatura() {
        String[] categorias = {"CREDIT", "DEBIT", "ATM", "POS", "DEP", "INT", "PAYMENT", "XFER", "FEE",
                "OTHER", "PIX", "TED", "BOLETO", "CARTAO", "SAQUE", "DEPOSITO", "FOLHA_PAGAMENTO", ""};
        String[] descricoes = {"PIX", "SAQUE", "DEPOSITO", "BOLETO", "COMPRA", "TED", "ESTORNO", "QUALQUER"};
        for (String c : categorias) {
            for (String d : descricoes) {
                for (double v : new double[]{50, -50}) {
                    TipoTransacao t = tipo(conta(v, c, d));
                    SentidoTransacao s = SentidoTransacao.de(t);
                    SentidoTransacao esperado = v > 0 ? SentidoTransacao.ENTRADA : SentidoTransacao.SAIDA;
                    assertEquals(c + "/" + d + "/" + v + " -> " + t, esperado, s);
                }
            }
        }
    }
}
