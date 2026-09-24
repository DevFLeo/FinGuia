package com.finguia.motor;

import com.finguia.dados.TipoTransacao;

/**
 * Se um tipo de transacao soma, subtrai ou nao mexe no saldo.
 *
 * <p>Fonte unica da regra no lado Kotlin/Java: as telas repetiam a lista de
 * tipos de entrada em seis lugares e tratavam qualquer outro tipo como saida,
 * entao lembretes de boleto (COBRANCA) apareciam como dinheiro saindo e eram
 * subtraidos do grafico de saldo. As consultas SQL do TransacaoDao e o
 * web/esquema.js seguem a mesma divisao.
 */
public enum SentidoTransacao {
    ENTRADA(1),
    SAIDA(-1),
    /** Avisos e nao identificados: aparecem no extrato, mas fora dos totais. */
    NEUTRO(0);

    private final int sinal;

    SentidoTransacao(int sinal) {
        this.sinal = sinal;
    }

    /** +1, -1 ou 0: multiplicado pelo valor, da o efeito no saldo. */
    public int sinal() {
        return sinal;
    }

    /** Efeito do valor no saldo: positivo, negativo ou zero. */
    public double aplicar(double valor) {
        return sinal * valor;
    }

    public static SentidoTransacao de(TipoTransacao tipo) {
        if (tipo == null) {
            return NEUTRO;
        }
        switch (tipo) {
            case PIX_RECEBIDO:
            case TRANSFERENCIA_RECEBIDA:
            case DEPOSITO:
            case ESTORNO:
                return ENTRADA;
            case PIX_ENVIADO:
            case COMPRA_DEBITO:
            case COMPRA_CREDITO:
            case BOLETO_PAGO:
            case TRANSFERENCIA_ENVIADA:
            case SAQUE:
                return SAIDA;
            case COBRANCA:
            case DESCONHECIDO:
            default:
                return NEUTRO;
        }
    }
}
