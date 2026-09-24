package com.finguia.motor.importacao;

/**
 * Um lancamento lido de fora do app (extrato OFX ou API Open Finance), antes
 * de virar TransacaoBancaria. Imutavel e sem dependencia de Android.
 *
 * <p>O valor carrega o sinal da origem: positivo entrou, negativo saiu. O tipo
 * de transacao do app e decidido depois, na camada Kotlin, combinando esse
 * sinal com a {@link #getCategoria() categoria} e a descricao.
 */
public final class LancamentoExterno {

    private final String idExterno;
    private final long dataMs;
    private final double valor;
    private final String descricao;
    private final String categoria;
    private final String contraparte;
    private final String instituicao;
    private final boolean efetivado;
    private final boolean cartaoCredito;

    public LancamentoExterno(String idExterno, long dataMs, double valor, String descricao,
                             String categoria, String contraparte, String instituicao,
                             boolean efetivado) {
        this(idExterno, dataMs, valor, descricao, categoria, contraparte, instituicao, efetivado, false);
    }

    public LancamentoExterno(String idExterno, long dataMs, double valor, String descricao,
                             String categoria, String contraparte, String instituicao,
                             boolean efetivado, boolean cartaoCredito) {
        if (idExterno == null || idExterno.isEmpty()) {
            throw new IllegalArgumentException("idExterno obrigatorio");
        }
        this.idExterno = idExterno;
        this.dataMs = dataMs;
        this.valor = valor;
        this.descricao = descricao == null ? "" : descricao.trim();
        this.categoria = categoria == null ? "" : categoria.trim();
        this.contraparte = vazioParaNull(contraparte);
        this.instituicao = vazioParaNull(instituicao);
        this.efetivado = efetivado;
        this.cartaoCredito = cartaoCredito;
    }

    /** Identificador na origem, ja com prefixo: "ofx:..." ou "openfinance:...". */
    public String getIdExterno() {
        return idExterno;
    }

    public long getDataMs() {
        return dataMs;
    }

    /** Positivo entrou, negativo saiu. */
    public double getValor() {
        return valor;
    }

    public boolean ehEntrada() {
        return valor > 0;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Classificacao da origem: TRNTYPE do OFX (DEBIT, POS, ATM...) ou type do Open Finance (PIX, TED...). */
    public String getCategoria() {
        return categoria;
    }

    /** Quem pagou ou recebeu, quando a origem informa. Pode ser null. */
    public String getContraparte() {
        return contraparte;
    }

    /** Banco de origem, quando a origem informa. Pode ser null. */
    public String getInstituicao() {
        return instituicao;
    }

    /** false = lancamento futuro (agendado), fora dos totais ate efetivar. */
    public boolean isEfetivado() {
        return efetivado;
    }

    /**
     * Veio de fatura de cartao de credito. Muda a leitura do sinal: la, o
     * lancamento positivo costuma ser o pagamento da fatura, nao uma receita.
     */
    public boolean isCartaoCredito() {
        return cartaoCredito;
    }

    private static String vazioParaNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Override
    public String toString() {
        return idExterno + " " + valor + " " + descricao;
    }
}
