package com.finguia.motor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MascaraMoedaTest {

    @Test
    public void digitosEntramPelaDireita() {
        assertEquals("0,00", MascaraMoeda.exibir(""));
        assertEquals("0,01", MascaraMoeda.exibir("1"));
        assertEquals("0,12", MascaraMoeda.exibir("12"));
        assertEquals("1,23", MascaraMoeda.exibir("123"));
        assertEquals("12,34", MascaraMoeda.exibir("1234"));
        assertEquals("1.234,56", MascaraMoeda.exibir("123456"));
        assertEquals("1.234.567,89", MascaraMoeda.exibir("123456789"));
    }

    @Test
    public void limpar_removeNaoDigitosEZerosAEsquerda() {
        assertEquals("123456", MascaraMoeda.limpar("R$ 1.234,56"));
        assertEquals("5", MascaraMoeda.limpar("0005"));
        assertEquals("", MascaraMoeda.limpar("000"));
        assertEquals("", MascaraMoeda.limpar(null));
    }

    @Test
    public void limpar_respeitaLimiteDeDigitos() {
        String enorme = "12345678901234567890";
        assertEquals(MascaraMoeda.MAX_DIGITOS, MascaraMoeda.limpar(enorme).length());
    }

    @Test
    public void conversoes() {
        assertEquals(123456L, MascaraMoeda.centavos("123456"));
        assertEquals(1234.56, MascaraMoeda.reais("123456"), 1e-9);
        assertEquals(0.0, MascaraMoeda.reais(""), 1e-9);
    }

    @Test
    public void deReais_abreCampoPreenchido() {
        assertEquals("123456", MascaraMoeda.deReais(1234.56));
        assertEquals("29", MascaraMoeda.deReais(0.29));
        assertEquals("", MascaraMoeda.deReais(0.0));
    }

    @Test
    public void cursorFicaNoFim() {
        assertEquals("1.234,56".length(), MascaraMoeda.cursorExibido("123456"));
    }
}
