package com.finguia.motor.importacao;

import com.finguia.dados.TipoTransacao;
import com.finguia.motor.SentidoTransacao;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Decide o TipoTransacao de um lancamento importado.
 *
 * <p>Ordem de confianca: (1) o sinal do valor manda na direcao; (2) a
 * categoria da origem (TRNTYPE do OFX, type do Open Finance) e (3) palavras
 * da descricao refinam o tipo. Se a pista (2) ou (3) apontar um tipo cuja
 * direcao contradiz o sinal, vale o sinal com o tipo generico. Garantia
 * testada: o sentido do tipo escolhido sempre bate com o sinal do valor,
 * exceto os pagamentos de fatura, que sao neutros de proposito.
 */
public final class ClassificadorImportacao {

    // Palavra inteira: "ted" aparece dentro de outras palavras
    private static final Pattern TED_DOC = Pattern.compile("\\b(ted|doc)\\b");

    private ClassificadorImportacao() {
        // utilitaria
    }

    public static TipoTransacao classificar(LancamentoExterno l) {
        boolean entrada = l.ehEntrada();
        String categoria = l.getCategoria().toUpperCase(Locale.ROOT);
        String texto = normalizar(l.getDescricao() + " " + (l.getContraparte() == null ? "" : l.getContraparte()));

        if (l.isCartaoCredito()) {
            if (!entrada) {
                return TipoTransacao.COMPRA_CREDITO;
            }
            // Credito na fatura: pagamento da propria fatura (o dinheiro ja saiu
            // da conta corrente, contar de novo seria receita falsa) ou estorno
            return texto.contains("pagamento") || texto.contains("pgto") || texto.contains("pag fatura")
                    ? TipoTransacao.DESCONHECIDO
                    : TipoTransacao.ESTORNO;
        }

        TipoTransacao pista = pelaDescricao(texto, entrada);
        if (pista == null) {
            pista = pelaCategoria(categoria, entrada);
        }
        if (pista != null && coerente(pista, entrada)) {
            return pista;
        }
        return entrada ? TipoTransacao.TRANSFERENCIA_RECEBIDA : TipoTransacao.TRANSFERENCIA_ENVIADA;
    }

    /** Palavras que os bancos usam no historico do extrato. */
    private static TipoTransacao pelaDescricao(String t, boolean entrada) {
        if (t.contains("estorno") || t.contains("devolucao") || t.contains("reembolso")) {
            return TipoTransacao.ESTORNO;
        }
        if (t.contains("pix")) {
            return entrada ? TipoTransacao.PIX_RECEBIDO : TipoTransacao.PIX_ENVIADO;
        }
        if (t.contains("saque")) {
            return TipoTransacao.SAQUE;
        }
        if (t.contains("boleto") || t.contains("pagto") || t.contains("pagamento de conta")
                || t.contains("pag titulo") || t.contains("convenio")) {
            return TipoTransacao.BOLETO_PAGO;
        }
        if (t.contains("compra") || t.contains("debito visa") || t.contains("debito master")
                || t.contains("cartao debito") || t.contains("elo debito")) {
            return TipoTransacao.COMPRA_DEBITO;
        }
        if (t.contains("deposito")) {
            return TipoTransacao.DEPOSITO;
        }
        if (TED_DOC.matcher(t).find() || t.contains("transf")) {
            return entrada ? TipoTransacao.TRANSFERENCIA_RECEBIDA : TipoTransacao.TRANSFERENCIA_ENVIADA;
        }
        return null;
    }

    /** TRNTYPE do OFX ou type do Open Finance. */
    private static TipoTransacao pelaCategoria(String c, boolean entrada) {
        switch (c) {
            // Open Finance
            case "PIX":
                return entrada ? TipoTransacao.PIX_RECEBIDO : TipoTransacao.PIX_ENVIADO;
            case "TED":
            case "DOC":
            case "TRANSFERENCIA_MESMA_INSTITUICAO":
            case "PORTABILIDADE_SALARIO":
            case "FOLHA_PAGAMENTO":
                return entrada ? TipoTransacao.TRANSFERENCIA_RECEBIDA : TipoTransacao.TRANSFERENCIA_ENVIADA;
            case "BOLETO":
            case "CONVENIO_ARRECADACAO":
                return TipoTransacao.BOLETO_PAGO;
            case "CARTAO":
                return TipoTransacao.COMPRA_DEBITO;
            case "SAQUE":
                return TipoTransacao.SAQUE;
            case "DEPOSITO":
            case "RENDIMENTO_APLIC_FINANCEIRA":
            case "RESGATE_APLIC_FINANCEIRA":
                return TipoTransacao.DEPOSITO;
            // OFX
            case "ATM":
            case "CASH":
                return TipoTransacao.SAQUE;
            case "POS":
                return TipoTransacao.COMPRA_DEBITO;
            case "DEP":
            case "DIRECTDEP":
            case "INT":
            case "DIV":
                return TipoTransacao.DEPOSITO;
            case "PAYMENT":
            case "CHECK":
            case "DIRECTDEBIT":
            case "REPEATPMT":
                return TipoTransacao.BOLETO_PAGO;
            case "XFER":
                return entrada ? TipoTransacao.TRANSFERENCIA_RECEBIDA : TipoTransacao.TRANSFERENCIA_ENVIADA;
            default:
                // CREDIT, DEBIT, FEE, OTHER, tarifas e encargos: so a direcao importa
                return null;
        }
    }

    private static boolean coerente(TipoTransacao tipo, boolean entrada) {
        SentidoTransacao s = SentidoTransacao.de(tipo);
        return entrada ? s == SentidoTransacao.ENTRADA : s == SentidoTransacao.SAIDA;
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{Mn}+", "");
    }
}
