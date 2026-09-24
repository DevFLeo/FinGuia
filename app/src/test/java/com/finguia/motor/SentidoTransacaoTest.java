package com.finguia.motor;

import static org.junit.Assert.assertEquals;

import com.finguia.dados.TipoTransacao;
import java.util.EnumSet;
import org.junit.Test;

public class SentidoTransacaoTest {

    @Test
    public void entradas() {
        for (TipoTransacao t : EnumSet.of(TipoTransacao.PIX_RECEBIDO, TipoTransacao.TRANSFERENCIA_RECEBIDA,
                TipoTransacao.DEPOSITO, TipoTransacao.ESTORNO)) {
            assertEquals(t.name(), SentidoTransacao.ENTRADA, SentidoTransacao.de(t));
        }
    }

    @Test
    public void saidas() {
        for (TipoTransacao t : EnumSet.of(TipoTransacao.PIX_ENVIADO, TipoTransacao.COMPRA_DEBITO,
                TipoTransacao.COMPRA_CREDITO, TipoTransacao.BOLETO_PAGO,
                TipoTransacao.TRANSFERENCIA_ENVIADA, TipoTransacao.SAQUE)) {
            assertEquals(t.name(), SentidoTransacao.SAIDA, SentidoTransacao.de(t));
        }
    }

    @Test
    public void avisosSaoNeutros() {
        assertEquals(SentidoTransacao.NEUTRO, SentidoTransacao.de(TipoTransacao.COBRANCA));
        assertEquals(SentidoTransacao.NEUTRO, SentidoTransacao.de(TipoTransacao.DESCONHECIDO));
        assertEquals(SentidoTransacao.NEUTRO, SentidoTransacao.de(null));
    }

    @Test
    public void todoTipoTemSentidoDefinido() {
        // Um tipo novo no enum tem que ser classificado de proposito
        int total = 0;
        for (TipoTransacao t : TipoTransacao.values()) {
            SentidoTransacao.de(t);
            total++;
        }
        assertEquals(TipoTransacao.values().length, total);
    }

    @Test
    public void saldoAcumuladoIgnoraLembretes() {
        // Regressao do grafico do Painel: lembrete de boleto era subtraido
        double saldo = 0;
        saldo += SentidoTransacao.de(TipoTransacao.PIX_RECEBIDO).aplicar(234.01);
        saldo += SentidoTransacao.de(TipoTransacao.TRANSFERENCIA_ENVIADA).aplicar(15.54);
        saldo += SentidoTransacao.de(TipoTransacao.COBRANCA).aplicar(3519.18);
        saldo += SentidoTransacao.de(TipoTransacao.COBRANCA).aplicar(3519.18);
        assertEquals(218.47, saldo, 0.001);
    }
}
