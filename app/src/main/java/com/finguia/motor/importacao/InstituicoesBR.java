package com.finguia.motor.importacao;

import java.util.HashMap;
import java.util.Map;

/** Nome dos bancos pelo codigo COMPE (o numero de 3 digitos do banco). */
public final class InstituicoesBR {

    private static final Map<String, String> POR_CODIGO = new HashMap<>();

    static {
        POR_CODIGO.put("001", "Banco do Brasil");
        POR_CODIGO.put("033", "Santander");
        POR_CODIGO.put("041", "Banrisul");
        POR_CODIGO.put("077", "Banco Inter");
        POR_CODIGO.put("102", "XP");
        POR_CODIGO.put("104", "Caixa Econômica");
        POR_CODIGO.put("121", "Agibank");
        POR_CODIGO.put("197", "Stone");
        POR_CODIGO.put("208", "BTG Pactual");
        POR_CODIGO.put("212", "Banco Original");
        POR_CODIGO.put("237", "Bradesco");
        POR_CODIGO.put("260", "Nubank");
        POR_CODIGO.put("290", "PagBank");
        POR_CODIGO.put("323", "Mercado Pago");
        POR_CODIGO.put("336", "C6 Bank");
        POR_CODIGO.put("341", "Itaú");
        POR_CODIGO.put("380", "PicPay");
        POR_CODIGO.put("422", "Safra");
        POR_CODIGO.put("655", "BV");
        POR_CODIGO.put("748", "Sicredi");
        POR_CODIGO.put("756", "Sicoob");
    }

    private InstituicoesBR() {
        // utilitaria
    }

    /**
     * Nome do banco pelo codigo, aceitando "260", "0260" ou "26". Codigo
     * desconhecido devolve null, para quem chama decidir o que exibir.
     */
    public static String nome(String codigo) {
        if (codigo == null) {
            return null;
        }
        String digitos = codigo.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            return null;
        }
        // Remove zeros a esquerda alem de 3 digitos e completa ate 3
        String normalizado = digitos.replaceFirst("^0+(?=\\d{3})", "");
        while (normalizado.length() < 3) {
            normalizado = "0" + normalizado;
        }
        return POR_CODIGO.get(normalizado);
    }
}
