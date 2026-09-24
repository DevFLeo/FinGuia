package com.finguia.motor.importacao;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converte transacoes no formato da API de Contas do Open Finance Brasil
 * (GET /accounts/v2/accounts/{accountId}/transactions) em lancamentos do app.
 *
 * <p><b>O que isto faz e o que nao faz.</b> O FinGuia nao chama as APIs do
 * Open Finance: elas so atendem participantes autorizados pelo Banco Central,
 * com certificado ICP-Brasil e mTLS. Esta classe cobre a outra metade: dado o
 * JSON de resposta (obtido por um agregador credenciado, como Pluggy ou Belvo,
 * ou pelo sandbox do ecossistema), transforma cada transacao em
 * {@link LancamentoExterno}. Conectar uma fonte real exige so fornecer esse
 * JSON; o mapeamento e a deduplicacao ja estao prontos e testados.
 *
 * <p>Campos lidos de cada item de {@code data}:
 * {@code transactionId}, {@code completedAuthorisedPaymentType},
 * {@code creditDebitType}, {@code transactionName}, {@code type},
 * {@code transactionAmount.amount} e {@code transactionDateTime}. Tambem
 * aceita os nomes da v1 ({@code amount} numerico e {@code transactionDate}).
 */
public final class LeitorOpenFinance {

    private static final Gson GSON = new Gson();

    // 2026-01-05T10:00:00.123Z | 2026-01-05T10:00:00-03:00 | 2026-01-05
    private static final Pattern DATA_ISO = Pattern.compile(
            "(\\d{4})-(\\d{2})-(\\d{2})(?:[T ](\\d{2}):(\\d{2})(?::(\\d{2})(?:\\.\\d+)?)?)?\\s*(Z|[+-]\\d{2}:?\\d{2})?");

    private LeitorOpenFinance() {
        // utilitaria
    }

    /** Uma transacao como vem da API. Campos publicos para o Gson preencher. */
    public static final class Transacao {
        public String transactionId;
        public String completedAuthorisedPaymentType;
        public String creditDebitType;
        public String transactionName;
        public String type;
        public Valor transactionAmount;
        public String transactionDateTime;
        // v1
        public Double amount;
        public String transactionDate;

        public static final class Valor {
            public String amount;
            public String currency;
        }
    }

    /** Resultado da leitura: lancamentos e quantos itens foram recusados. */
    public static final class Resultado {
        private final List<LancamentoExterno> lancamentos;
        private final int ignorados;

        Resultado(List<LancamentoExterno> lancamentos, int ignorados) {
            this.lancamentos = Collections.unmodifiableList(lancamentos);
            this.ignorados = ignorados;
        }

        public List<LancamentoExterno> getLancamentos() {
            return lancamentos;
        }

        public int getIgnorados() {
            return ignorados;
        }
    }

    /**
     * Le a resposta da API: o objeto completo ({@code {"data": [...]}}) ou
     * so a lista de transacoes.
     *
     * @param instituicao banco da conta consultada, para exibir no extrato
     */
    public static Resultado ler(String json, String instituicao) {
        JsonArray itens;
        try {
            JsonElement raiz = JsonParser.parseString(json);
            if (raiz.isJsonArray()) {
                itens = raiz.getAsJsonArray();
            } else if (raiz.isJsonObject() && raiz.getAsJsonObject().has("data")
                    && raiz.getAsJsonObject().get("data").isJsonArray()) {
                itens = raiz.getAsJsonObject().getAsJsonArray("data");
            } else {
                throw new IllegalArgumentException("JSON sem a lista de transações (campo \"data\").");
            }
        } catch (JsonParseException | IllegalStateException e) {
            throw new IllegalArgumentException("Arquivo não é um JSON válido.", e);
        }

        List<LancamentoExterno> lancamentos = new ArrayList<>();
        int ignorados = 0;
        for (JsonElement item : itens) {
            LancamentoExterno l = item.isJsonObject()
                    ? converter(GSON.fromJson(item, Transacao.class), instituicao)
                    : null;
            if (l == null) {
                ignorados++;
            } else {
                lancamentos.add(l);
            }
        }
        return new Resultado(lancamentos, ignorados);
    }

    /** Uma transacao da API para lancamento; null se faltar o essencial. */
    public static LancamentoExterno converter(Transacao t, String instituicao) {
        if (t == null || t.transactionId == null || t.transactionId.isEmpty()) {
            return null;
        }
        Double valorAbsoluto = valor(t);
        Long data = lerData(t.transactionDateTime != null ? t.transactionDateTime : t.transactionDate);
        if (valorAbsoluto == null || data == null || valorAbsoluto == 0.0) {
            return null;
        }
        // A API manda o valor sempre positivo; a direcao vem de creditDebitType
        double valor = "DEBITO".equalsIgnoreCase(t.creditDebitType)
                ? -Math.abs(valorAbsoluto) : Math.abs(valorAbsoluto);
        boolean efetivado = !"LANCAMENTO_FUTURO".equalsIgnoreCase(t.completedAuthorisedPaymentType);

        return new LancamentoExterno("openfinance:" + t.transactionId, data, valor,
                t.transactionName, t.type, null, instituicao, efetivado);
    }

    private static Double valor(Transacao t) {
        if (t.transactionAmount != null && t.transactionAmount.amount != null) {
            try {
                return Double.parseDouble(t.transactionAmount.amount.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return t.amount;
    }

    /** ISO-8601; sem fuso, assume UTC como a especificacao determina. */
    static Long lerData(String iso) {
        if (iso == null) {
            return null;
        }
        Matcher m = DATA_ISO.matcher(iso.trim());
        if (!m.lookingAt()) {
            return null;
        }
        boolean temHora = m.group(4) != null;
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) - 1, Integer.parseInt(m.group(3)),
                // So a data (v1): meio-dia de Brasilia, para nao virar o dia anterior
                temHora ? Integer.parseInt(m.group(4)) : 15,
                temHora ? Integer.parseInt(m.group(5)) : 0,
                m.group(6) != null ? Integer.parseInt(m.group(6)) : 0);
        long ms = c.getTimeInMillis();
        String fuso = m.group(7);
        if (fuso != null && !fuso.equals("Z")) {
            String digitos = fuso.replace(":", "");
            int sinal = digitos.charAt(0) == '-' ? -1 : 1;
            int horas = Integer.parseInt(digitos.substring(1, 3));
            int minutos = Integer.parseInt(digitos.substring(3, 5));
            ms -= sinal * (horas * 3_600_000L + minutos * 60_000L);
        }
        return ms;
    }
}
