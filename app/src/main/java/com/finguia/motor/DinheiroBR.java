package com.finguia.motor;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dinheiro em centavos (long), para nao acumular erro de ponto flutuante.
 *
 * <p>Somar dezenas de {@code double} em reais desvia: 0.1 + 0.2 nao da 0.3 em
 * binario, e o extrato do FinGuia soma centenas de lancamentos. Em centavos
 * inteiros a soma e exata, e a conversao para reais acontece so na exibicao.
 *
 * <p>Classe utilitaria pura: sem dependencia de Android, coberta por testes
 * unitarios de host.
 */
public final class DinheiroBR {

    private DinheiroBR() {
        // utilitaria
    }

    /**
     * Valor monetario em texto brasileiro. Aceita, nesta ordem de preferencia:
     * com R$ e centavos, com R$ e apenas milhar, e valores soltos com centavos.
     * O grupo 1 sempre carrega os digitos.
     */
    private static final Pattern PADRAO_VALOR = Pattern.compile(
            "R\\$\\s*(\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+,\\d{2}|\\d{1,3}(?:\\.\\d{3})+|\\d+)"
                    + "|\\b(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\b",
            Pattern.CASE_INSENSITIVE);

    /** Converte reais para centavos arredondando para o centavo mais proximo. */
    public static long paraCentavos(double reais) {
        return Math.round(reais * 100.0);
    }

    /** Converte centavos para reais. Use apenas na borda de exibicao. */
    public static double paraReais(long centavos) {
        return centavos / 100.0;
    }

    /**
     * Le um valor monetario em formato brasileiro e devolve centavos.
     *
     * <p>No formato BR o ponto e separador de milhar e a virgula e decimal:
     * {@code "R$ 1.234,56"} vira {@code 123456}. Sem virgula, os pontos sao
     * milhar: {@code "R$ 1.500"} vira {@code 150000}.
     *
     * @return os centavos, ou {@code null} se nao houver valor reconhecivel
     */
    public static Long lerCentavos(String texto) {
        if (texto == null) {
            return null;
        }
        Matcher m = PADRAO_VALOR.matcher(texto);
        if (!m.find()) {
            return null;
        }
        String bruto = m.group(1) != null ? m.group(1) : m.group(2);
        return normalizar(bruto);
    }

    /**
     * Converte apenas os digitos ja isolados de um valor BR em centavos.
     * Exposto porque o analisador de notificacoes escolhe qual ocorrencia usar
     * antes de converter.
     *
     * @return os centavos, ou {@code null} se o texto nao for numerico
     */
    public static Long normalizar(String valorBruto) {
        if (valorBruto == null) {
            return null;
        }
        String limpo = valorBruto.trim();
        if (limpo.isEmpty()) {
            return null;
        }
        // Com virgula ela e o decimal; sem virgula os pontos sao milhar
        String normalizado = limpo.contains(",")
                ? limpo.replace(".", "").replace(",", ".")
                : limpo.replace(".", "");
        try {
            return paraCentavos(Double.parseDouble(normalizado));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Formata centavos como {@code "R$ 1.234,56"}. */
    public static String formatar(long centavos) {
        boolean negativo = centavos < 0;
        long absoluto = Math.abs(centavos);
        long reais = absoluto / 100;
        long resto = absoluto % 100;
        StringBuilder sb = new StringBuilder();
        if (negativo) {
            sb.append('-');
        }
        sb.append("R$ ").append(agruparMilhar(reais)).append(',');
        if (resto < 10) {
            sb.append('0');
        }
        sb.append(resto);
        return sb.toString();
    }

    /** Formata reais como {@code "R$ 1.234,56"}, convertendo para centavos antes. */
    public static String formatarReais(double reais) {
        return formatar(paraCentavos(reais));
    }

    /**
     * Forma curta para caber em cartao e rotulo de grafico:
     * {@code "R$ 1,2 mil"}, {@code "R$ 3,5 mi"}. Abaixo de mil usa o formato cheio.
     */
    public static String formatarCompacto(long centavos) {
        long absoluto = Math.abs(centavos);
        String sinal = centavos < 0 ? "-" : "";
        if (absoluto >= 100_000_000L) { // a partir de R$ 1 milhao
            return sinal + "R$ " + umaCasa(absoluto / 100_000_000.0) + " mi";
        }
        if (absoluto >= 100_000L) { // a partir de R$ 1 mil
            return sinal + "R$ " + umaCasa(absoluto / 100_000.0) + " mil";
        }
        return formatar(centavos);
    }

    /** Soma exata de centavos, sem risco de estouro silencioso. */
    public static long somar(long... centavos) {
        long total = 0;
        for (long c : centavos) {
            total = Math.addExact(total, c);
        }
        return total;
    }

    /**
     * Percentual de {@code parte} sobre {@code total}, de 0 a 100.
     * Total zero devolve 0 em vez de estourar.
     */
    public static double percentual(long parte, long total) {
        if (total == 0) {
            return 0.0;
        }
        return (parte * 100.0) / total;
    }

    private static String umaCasa(double valor) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f", valor);
    }

    private static String agruparMilhar(long reais) {
        String digitos = Long.toString(reais);
        StringBuilder sb = new StringBuilder();
        int contador = 0;
        for (int i = digitos.length() - 1; i >= 0; i--) {
            sb.append(digitos.charAt(i));
            contador++;
            if (contador % 3 == 0 && i > 0) {
                sb.append('.');
            }
        }
        return sb.reverse().toString();
    }
}
