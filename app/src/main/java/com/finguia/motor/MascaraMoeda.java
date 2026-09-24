package com.finguia.motor;

/**
 * Mascara de digitacao de valores no estilo dos apps de banco: o usuario so
 * digita numeros e eles entram pela direita, como centavos.
 *
 * <pre>
 *   digita 1     ->       0,01
 *   digita 12    ->       0,12
 *   digita 1234  ->      12,34
 *   digita 123456 ->  1.234,56
 * </pre>
 *
 * <p>Assim nunca ha duvida sobre ponto ou virgula: o campo guarda so digitos
 * e a exibicao e sempre 0.000,00. A parte visual fica numa VisualTransformation
 * do Compose; esta classe tem apenas a regra, testavel sem Android.
 */
public final class MascaraMoeda {

    /** 13 digitos = R$ 99.999.999.999,99, bem acima de qualquer uso real. */
    public static final int MAX_DIGITOS = 13;

    private MascaraMoeda() {
        // utilitaria
    }

    /**
     * Filtra o que o usuario digitou para ficar so com digitos significativos:
     * remove tudo que nao e digito, zeros a esquerda e o excesso acima de
     * {@link #MAX_DIGITOS}.
     */
    public static String limpar(String entrada) {
        if (entrada == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entrada.length(); i++) {
            char c = entrada.charAt(i);
            if (c >= '0' && c <= '9') {
                if (sb.length() == 0 && c == '0') {
                    continue; // zero a esquerda nao conta
                }
                sb.append(c);
            }
        }
        if (sb.length() > MAX_DIGITOS) {
            sb.setLength(MAX_DIGITOS);
        }
        return sb.toString();
    }

    /** Digitos ja limpos para o texto exibido: {@code "123456"} vira {@code "1.234,56"}. */
    public static String exibir(String digitos) {
        return DinheiroBR.formatar(centavos(digitos)).substring(3); // tira "R$ "
    }

    /** Digitos para centavos. Vazio vale zero. */
    public static long centavos(String digitos) {
        String limpo = limpar(digitos);
        if (limpo.isEmpty()) {
            return 0L;
        }
        return Long.parseLong(limpo);
    }

    /** Digitos para reais, para entregar ao restante do app. */
    public static double reais(String digitos) {
        return DinheiroBR.paraReais(centavos(digitos));
    }

    /** Caminho inverso, para abrir um campo ja preenchido: 1234.56 vira {@code "123456"}. */
    public static String deReais(double reais) {
        long c = Math.abs(DinheiroBR.paraCentavos(reais));
        return c == 0 ? "" : Long.toString(c);
    }

    /**
     * Posicao do cursor no texto exibido. Como os digitos entram pela direita,
     * o cursor fica sempre no fim; a VisualTransformation do Compose usa isto
     * no OffsetMapping.
     */
    public static int cursorExibido(String digitos) {
        return exibir(digitos).length();
    }
}
