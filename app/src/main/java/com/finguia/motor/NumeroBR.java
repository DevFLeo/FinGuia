package com.finguia.motor;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Leitura e exibicao de numeros no padrao brasileiro: 1.234,56.
 *
 * <p>Ponto separa milhar e virgula separa decimal. Todos os valores exibidos
 * no app passam por aqui, e todo campo numerico digitado pelo usuario e lido
 * por {@link #ler(String)}, que aceita tanto o padrao BR quanto o ponto
 * decimal que o teclado numerico do Android as vezes insere.
 */
public final class NumeroBR {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DecimalFormatSymbols SIMBOLOS = DecimalFormatSymbols.getInstance(PT_BR);

    private NumeroBR() {
        // utilitaria
    }

    /**
     * Le um numero digitado pelo usuario.
     *
     * <ul>
     *   <li>Com virgula: ela e o decimal e os pontos sao milhar
     *       ({@code "1.234,56"} = 1234.56).</li>
     *   <li>Sem virgula e com mais de um ponto: sao todos milhar
     *       ({@code "1.234.567"} = 1234567).</li>
     *   <li>Sem virgula e com um ponto seguido de exatamente 3 digitos: milhar
     *       ({@code "1.500"} = 1500), como se escreve no Brasil.</li>
     *   <li>Qualquer outro ponto unico e decimal ({@code "12.5"} = 12.5),
     *       para aceitar o teclado numerico em ingles.</li>
     * </ul>
     *
     * <p>Ignora {@code R$}, {@code %} e espacos.
     *
     * @return o valor, ou {@code null} se o texto estiver vazio ou for invalido
     */
    public static Double ler(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.replace("R$", "")
                .replace("%", "")
                .replace(" ", "") // espaco nao separavel do NumberFormat
                .replace(" ", "")
                .trim();
        if (limpo.isEmpty() || limpo.equals("-") || limpo.equals(",") || limpo.equals(".")) {
            return null;
        }

        String normalizado;
        if (limpo.contains(",")) {
            if (limpo.indexOf(',') != limpo.lastIndexOf(',')) {
                return null; // duas virgulas nao e numero
            }
            normalizado = limpo.replace(".", "").replace(",", ".");
        } else {
            int pontos = limpo.length() - limpo.replace(".", "").length();
            if (pontos > 1) {
                normalizado = limpo.replace(".", "");
            } else if (pontos == 1 && ehMilhar(limpo)) {
                normalizado = limpo.replace(".", "");
            } else {
                normalizado = limpo;
            }
        }

        try {
            double valor = Double.parseDouble(normalizado);
            return Double.isNaN(valor) || Double.isInfinite(valor) ? null : valor;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Como {@link #ler(String)}, mas devolve {@code padrao} quando nao da para ler. */
    public static double lerOu(String texto, double padrao) {
        Double valor = ler(texto);
        return valor != null ? valor : padrao;
    }

    /** {@code 1234.5} com 2 casas vira {@code "1.234,50"}. */
    public static String formatar(double valor, int casas) {
        return formatador(casas, casas).format(normalizarZero(valor, casas));
    }

    /** Formato padrao de valores: sempre 2 casas, {@code "0.000,00"}. */
    public static String formatar(double valor) {
        return formatar(valor, 2);
    }

    /**
     * Entre {@code minimo} e {@code maximo} casas, cortando zeros a direita.
     * Util para cotacao de cripto, onde 0,00001234 importa e 50.000,00 nao.
     */
    public static String formatarFlexivel(double valor, int minimo, int maximo) {
        return formatador(minimo, maximo).format(normalizarZero(valor, maximo));
    }

    /** {@code 1234.5} vira {@code "R$ 1.234,50"}. */
    public static String moeda(double valor) {
        String corpo = formatar(Math.abs(valor), 2);
        return (valor < 0 && !ehZeroNaEscala(valor, 2) ? "-R$ " : "R$ ") + corpo;
    }

    /**
     * Qualquer moeda no padrao BR, como os bancos brasileiros exibem:
     * {@code moeda(1234.5, "US$")} vira {@code "US$ 1.234,50"}.
     */
    public static String moeda(double valor, String sigla) {
        String corpo = formatar(Math.abs(valor), 2);
        boolean negativo = valor < 0 && !ehZeroNaEscala(valor, 2);
        return (negativo ? "-" : "") + sigla + " " + corpo;
    }

    /**
     * Sigla usada na exibicao para um codigo ISO: BRL vira R$, USD vira US$,
     * EUR vira EUR. Codigo desconhecido ou vazio e exibido como veio.
     */
    public static String siglaMoeda(String codigoIso) {
        if (codigoIso == null || codigoIso.isEmpty() || codigoIso.equalsIgnoreCase("BRL")) {
            return "R$";
        }
        if (codigoIso.equalsIgnoreCase("USD")) {
            return "US$";
        }
        return codigoIso.toUpperCase(Locale.ROOT);
    }

    /** Moeda com sinal explicito: {@code "+R$ 10,00"} ou {@code "-R$ 10,00"}. */
    public static String moedaComSinal(double valor) {
        if (ehZeroNaEscala(valor, 2)) {
            return "R$ 0,00";
        }
        return (valor > 0 ? "+" : "") + moeda(valor);
    }

    /** {@code 12.5} vira {@code "12,50%"}. */
    public static String percentual(double valor, int casas) {
        return formatar(valor, casas) + "%";
    }

    /** Percentual com sinal, para variacao: {@code "+3,20%"}. */
    public static String percentualComSinal(double valor, int casas) {
        if (ehZeroNaEscala(valor, casas)) {
            return formatar(0.0, casas) + "%";
        }
        return (valor > 0 ? "+" : "") + percentual(valor, casas);
    }

    /**
     * Valor para preencher um campo editavel: sem milhar, para o usuario
     * poder apagar digito a digito sem tropecar em pontos. {@code 1234.5} vira
     * {@code "1234,50"}; zero vira vazio.
     */
    public static String paraCampo(double valor, int casas) {
        if (ehZeroNaEscala(valor, casas)) {
            return "";
        }
        DecimalFormat f = formatador(casas, casas);
        f.setGroupingUsed(false);
        return f.format(valor);
    }

    // ------------------------------------------------------------------ internos

    private static boolean ehMilhar(String comUmPonto) {
        int ponto = comUmPonto.indexOf('.');
        String depois = comUmPonto.substring(ponto + 1);
        String antes = comUmPonto.substring(0, ponto).replace("-", "");
        return depois.length() == 3 && !antes.isEmpty() && antes.length() <= 3;
    }

    private static DecimalFormat formatador(int minimo, int maximo) {
        // DecimalFormat nao e thread-safe: cria um por chamada, e barato
        DecimalFormat f = new DecimalFormat("#,##0", SIMBOLOS);
        f.setMinimumFractionDigits(minimo);
        f.setMaximumFractionDigits(maximo);
        f.setRoundingMode(RoundingMode.HALF_UP);
        return f;
    }

    private static boolean ehZeroNaEscala(double valor, int casas) {
        return Math.abs(valor) < 0.5 / Math.pow(10, casas);
    }

    /** Evita exibir "-0,00" quando o valor arredonda para zero. */
    private static double normalizarZero(double valor, int casas) {
        return ehZeroNaEscala(valor, casas) ? 0.0 : valor;
    }
}
