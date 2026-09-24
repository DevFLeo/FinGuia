package com.finguia.motor.importacao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import org.junit.Test;

public class LeitorOpenFinanceTest {

    private static final double D = 1e-9;

    // Formato da resposta de GET /accounts/v2/accounts/{accountId}/transactions
    private static final String RESPOSTA_V2 = "{\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"transactionId\": \"TXpRMU9UQTROMWhZV2xSU1FUazJSMDl\",\n"
            + "      \"completedAuthorisedPaymentType\": \"TRANSACAO_EFETIVADA\",\n"
            + "      \"creditDebitType\": \"DEBITO\",\n"
            + "      \"transactionName\": \"PIX ENVIADO MARIA SOUZA\",\n"
            + "      \"type\": \"PIX\",\n"
            + "      \"transactionAmount\": {\"amount\": \"120.5000\", \"currency\": \"BRL\"},\n"
            + "      \"transactionDateTime\": \"2026-01-05T13:00:00.000Z\",\n"
            + "      \"partieCnpjCpf\": \"43908445778\",\n"
            + "      \"partiePersonType\": \"PESSOA_NATURAL\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"transactionId\": \"abc2\",\n"
            + "      \"completedAuthorisedPaymentType\": \"LANCAMENTO_FUTURO\",\n"
            + "      \"creditDebitType\": \"CREDITO\",\n"
            + "      \"transactionName\": \"SALARIO EMPRESA X\",\n"
            + "      \"type\": \"FOLHA_PAGAMENTO\",\n"
            + "      \"transactionAmount\": {\"amount\": \"3500.00\", \"currency\": \"BRL\"},\n"
            + "      \"transactionDateTime\": \"2026-01-30T09:00:00-03:00\"\n"
            + "    },\n"
            + "    {\"transactionId\": \"sem-valor\", \"creditDebitType\": \"CREDITO\"}\n"
            + "  ],\n"
            + "  \"links\": {\"self\": \"https://api.banco.com.br/open-banking/accounts/v2/...\"},\n"
            + "  \"meta\": {\"totalRecords\": 3, \"totalPages\": 1}\n"
            + "}";

    @Test
    public void respostaV2_debitoFicaNegativoECreditoPositivo() {
        List<LancamentoExterno> l = LeitorOpenFinance.ler(RESPOSTA_V2, "Banco Exemplo").getLancamentos();
        assertEquals(2, l.size());
        assertEquals(-120.5, l.get(0).getValor(), D);
        assertEquals(3500.0, l.get(1).getValor(), D);
        assertEquals("PIX", l.get(0).getCategoria());
        assertEquals("Banco Exemplo", l.get(0).getInstituicao());
    }

    @Test
    public void idPrefixado() {
        LancamentoExterno l = LeitorOpenFinance.ler(RESPOSTA_V2, null).getLancamentos().get(0);
        assertEquals("openfinance:TXpRMU9UQTROMWhZV2xSU1FUazJSMDl", l.getIdExterno());
    }

    @Test
    public void lancamentoFuturo_naoEfetivado() {
        List<LancamentoExterno> l = LeitorOpenFinance.ler(RESPOSTA_V2, null).getLancamentos();
        assertTrue(l.get(0).isEfetivado());
        assertFalse(l.get(1).isEfetivado());
    }

    @Test
    public void itemIncompleto_contadoComoIgnorado() {
        assertEquals(1, LeitorOpenFinance.ler(RESPOSTA_V2, null).getIgnorados());
    }

    @Test
    public void aceitaListaSemEnvelope() {
        String lista = "[{\"transactionId\":\"x\",\"creditDebitType\":\"CREDITO\",\"type\":\"TED\","
                + "\"transactionAmount\":{\"amount\":\"10.00\"},\"transactionDateTime\":\"2026-01-05T13:00:00Z\"}]";
        assertEquals(1, LeitorOpenFinance.ler(lista, null).getLancamentos().size());
    }

    @Test
    public void aceitaCamposDaV1() {
        String v1 = "{\"data\":[{\"transactionId\":\"v1\",\"creditDebitType\":\"DEBITO\",\"type\":\"BOLETO\","
                + "\"amount\":99.9,\"transactionDate\":\"2026-01-05\"}]}";
        LancamentoExterno l = LeitorOpenFinance.ler(v1, null).getLancamentos().get(0);
        assertEquals(-99.9, l.getValor(), D);
        // So a data: meio-dia de Brasilia (15h UTC), nao meia-noite UTC
        assertEquals(utc(2026, 1, 5, 15, 0), l.getDataMs());
    }

    @Test
    public void datas_utcEComFuso() {
        assertEquals(utc(2026, 1, 5, 13, 0), (long) LeitorOpenFinance.lerData("2026-01-05T13:00:00.000Z"));
        assertEquals(utc(2026, 1, 30, 12, 0), (long) LeitorOpenFinance.lerData("2026-01-30T09:00:00-03:00"));
        assertNull(LeitorOpenFinance.lerData("30/01/2026"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void jsonInvalido() {
        LeitorOpenFinance.ler("{isto nao e json", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void jsonSemData() {
        LeitorOpenFinance.ler("{\"links\":{}}", null);
    }

    private static long utc(int ano, int mes, int dia, int hora, int minuto) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(ano, mes - 1, dia, hora, minuto, 0);
        return c.getTimeInMillis();
    }
}
