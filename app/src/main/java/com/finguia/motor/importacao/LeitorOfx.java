package com.finguia.motor.importacao;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Le extratos OFX, o formato que os bancos brasileiros oferecem em
 * "exportar extrato" (conta corrente e cartao de credito).
 *
 * <p>Aceita as duas variantes em uso:
 * <ul>
 *   <li>OFX 1.x (SGML): cabecalho "OFXHEADER:100" e tags-folha sem fechamento,
 *       ex. {@code <TRNAMT>-50.00}. Geralmente em Windows-1252.</li>
 *   <li>OFX 2.x (XML): tags fechadas, geralmente UTF-8.</li>
 * </ul>
 *
 * <p>Os lancamentos ficam em blocos {@code <STMTTRN>}, iguais em extrato de
 * conta e de cartao, entao os dois funcionam sem distincao.
 */
public final class LeitorOfx {

    /** Horario de Brasilia, usado quando o arquivo nao informa fuso. */
    private static final int FUSO_PADRAO_HORAS = -3;

    private static final Pattern BLOCO_TRANSACAO =
            Pattern.compile("<STMTTRN>(.*?)</STMTTRN>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // AAAAMMDD[HHMM[SS]][.XXX][[+-h[.m]][:TZ]]
    private static final Pattern DATA_OFX = Pattern.compile(
            "(\\d{4})(\\d{2})(\\d{2})(?:(\\d{2})(\\d{2})(\\d{2})?)?(?:\\.\\d+)?\\s*(?:\\[\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*(?::[^\\]]*)?\\])?");

    private LeitorOfx() {
        // utilitaria
    }

    /** O que foi lido de um arquivo. */
    public static final class Resultado {
        private final List<LancamentoExterno> lancamentos;
        private final String instituicao;
        private final int ignorados;

        Resultado(List<LancamentoExterno> lancamentos, String instituicao, int ignorados) {
            this.lancamentos = Collections.unmodifiableList(lancamentos);
            this.instituicao = instituicao;
            this.ignorados = ignorados;
        }

        public List<LancamentoExterno> getLancamentos() {
            return lancamentos;
        }

        /** Banco do extrato, se o arquivo informar. */
        public String getInstituicao() {
            return instituicao;
        }

        /** Blocos sem valor ou data validos, que nao viraram lancamento. */
        public int getIgnorados() {
            return ignorados;
        }
    }

    /** Le o arquivo em bytes, detectando a codificacao pelo cabecalho. */
    public static Resultado ler(byte[] conteudo) {
        return ler(new String(conteudo, detectarCodificacao(conteudo)));
    }

    public static Resultado ler(String ofx) {
        if (ofx == null || !ofx.toUpperCase(Locale.ROOT).contains("<OFX>")) {
            throw new IllegalArgumentException("Arquivo não parece ser um extrato OFX.");
        }
        String instituicao = campo(ofx, "ORG");
        if (instituicao == null) {
            instituicao = InstituicoesBR.nome(campo(ofx, "BANKID"));
        }
        String conta = campo(ofx, "ACCTID");
        boolean cartao = ofx.toUpperCase(Locale.ROOT).contains("<CCSTMTRS>");

        List<LancamentoExterno> lancamentos = new ArrayList<>();
        int ignorados = 0;
        Matcher blocos = BLOCO_TRANSACAO.matcher(ofx);
        while (blocos.find()) {
            LancamentoExterno l = lerTransacao(blocos.group(1), conta, instituicao, cartao);
            if (l == null) {
                ignorados++;
            } else {
                lancamentos.add(l);
            }
        }
        return new Resultado(lancamentos, instituicao, ignorados);
    }

    private static LancamentoExterno lerTransacao(String bloco, String conta, String instituicao,
                                                  boolean cartao) {
        Double valor = lerValor(campo(bloco, "TRNAMT"));
        Long data = lerData(campo(bloco, "DTPOSTED"));
        if (valor == null || data == null || valor == 0.0) {
            return null;
        }
        String memo = campo(bloco, "MEMO");
        String nome = campo(bloco, "NAME");
        String descricao = memo != null ? memo : (nome != null ? nome : "");
        // NAME costuma ser o favorecido quando MEMO descreve a operacao
        String contraparte = (nome != null && !nome.equalsIgnoreCase(descricao)) ? nome : null;

        String fitid = campo(bloco, "FITID");
        if (fitid == null) {
            // Sem FITID: identificador estavel a partir do conteudo, para
            // reimportar o mesmo arquivo nao duplicar
            fitid = "h" + Integer.toHexString((data + "|" + valor + "|" + descricao).hashCode());
        }
        String id = "ofx:" + (conta != null ? conta + ":" : "") + fitid;

        return new LancamentoExterno(id, data, valor, descricao, campo(bloco, "TRNTYPE"),
                contraparte, instituicao, true, cartao);
    }

    /**
     * Valor de uma tag-folha. Funciona nas duas variantes: le ate o proximo
     * "<" ou quebra de linha, entao pega {@code <X>valor} e {@code <X>valor</X>}.
     */
    static String campo(String texto, String tag) {
        Matcher m = Pattern.compile("<" + tag + ">([^<\\r\\n]*)", Pattern.CASE_INSENSITIVE).matcher(texto);
        if (!m.find()) {
            return null;
        }
        String v = decodificarEntidades(m.group(1).trim());
        return v.isEmpty() ? null : v;
    }

    /**
     * O OFX usa ponto decimal sem milhar ({@code -1500.00}), mas alguns bancos
     * brasileiros gravam virgula ({@code -1500,00}). Se aparecerem os dois, o
     * ultimo separador e o decimal.
     */
    static Double lerValor(String bruto) {
        if (bruto == null) {
            return null;
        }
        String v = bruto.replace(" ", "").replace("+", "");
        int ponto = v.lastIndexOf('.');
        int virgula = v.lastIndexOf(',');
        if (ponto >= 0 && virgula >= 0) {
            v = ponto > virgula ? v.replace(",", "") : v.replace(".", "").replace(',', '.');
        } else if (virgula >= 0) {
            v = v.replace(',', '.');
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Data OFX para milissegundos. Sem fuso, assume horario de Brasilia (os
     * bancos daqui omitem). Sem hora, usa meio-dia: meia-noite em UTC cairia no
     * dia anterior quando exibida no Brasil.
     */
    static Long lerData(String bruto) {
        if (bruto == null) {
            return null;
        }
        Matcher m = DATA_OFX.matcher(bruto.trim());
        if (!m.lookingAt()) {
            return null;
        }
        boolean temHora = m.group(4) != null;
        double fusoHoras = m.group(7) != null ? Double.parseDouble(m.group(7)) : FUSO_PADRAO_HORAS;

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) - 1, Integer.parseInt(m.group(3)),
                temHora ? Integer.parseInt(m.group(4)) : 12,
                temHora ? Integer.parseInt(m.group(5)) : 0,
                m.group(6) != null ? Integer.parseInt(m.group(6)) : 0);
        // A data lida e local ao fuso informado: converte para UTC
        return c.getTimeInMillis() - Math.round(fusoHoras * 3_600_000L);
    }

    /** Codificacao declarada no cabecalho; OFX 1 sem declaracao costuma ser 1252. */
    static Charset detectarCodificacao(byte[] conteudo) {
        int n = Math.min(conteudo.length, 600);
        String cabecalho = new String(conteudo, 0, n, StandardCharsets.ISO_8859_1).toUpperCase(Locale.ROOT);
        if (cabecalho.contains("UTF-8") || cabecalho.contains("CHARSET:UTF")) {
            return StandardCharsets.UTF_8;
        }
        if (cabecalho.contains("<?XML") && !cabecalho.contains("ENCODING=\"ISO") && !cabecalho.contains("1252")) {
            return StandardCharsets.UTF_8;
        }
        if (cabecalho.contains("CHARSET:1252") || cabecalho.contains("WINDOWS-1252")) {
            return Charset.forName("windows-1252");
        }
        if (cabecalho.contains("ISO-8859-1") || cabecalho.contains("CHARSET:8859")) {
            return StandardCharsets.ISO_8859_1;
        }
        return Charset.forName("windows-1252");
    }

    private static String decodificarEntidades(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'").replace("&amp;", "&");
    }
}
