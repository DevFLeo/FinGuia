package com.finguia.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DinheiroBRTest {

    @Test
    public void lerCentavos_formatoCompletoComMilhar() {
        assertEquals(Long.valueOf(123456), DinheiroBR.lerCentavos("R$ 1.234,56"));
    }

    @Test
    public void lerCentavos_semEspacoAposCifrao() {
        assertEquals(Long.valueOf(25000), DinheiroBR.lerCentavos("Pix de R$250,00 recebido"));
    }

    @Test
    public void lerCentavos_milharSemCentavos() {
        assertEquals(Long.valueOf(150000), DinheiroBR.lerCentavos("R$ 1.500"));
    }

    @Test
    public void lerCentavos_inteiroSemCentavos() {
        assertEquals(Long.valueOf(5000), DinheiroBR.lerCentavos("R$ 50"));
    }

    @Test
    public void lerCentavos_valorSoltoComCentavos() {
        assertEquals(Long.valueOf(9990), DinheiroBR.lerCentavos("total 99,90 no debito"));
    }

    @Test
    public void lerCentavos_ignoraDataSemCifrao() {
        // "12/05" nao pode virar valor
        assertNull(DinheiroBR.lerCentavos("vence em 12/05"));
    }

    @Test
    public void lerCentavos_textoSemValor() {
        assertNull(DinheiroBR.lerCentavos("Seu extrato esta disponivel"));
        assertNull(DinheiroBR.lerCentavos(null));
    }

    @Test
    public void normalizar_centavosExatos() {
        // 0,29 * 100 em double da 28.999999...; o arredondamento tem que corrigir
        assertEquals(Long.valueOf(29), DinheiroBR.normalizar("0,29"));
        assertEquals(Long.valueOf(1), DinheiroBR.normalizar("0,01"));
    }

    @Test
    public void formatar_casosComuns() {
        assertEquals("R$ 0,00", DinheiroBR.formatar(0));
        assertEquals("R$ 0,05", DinheiroBR.formatar(5));
        assertEquals("R$ 1.234,56", DinheiroBR.formatar(123456));
        assertEquals("R$ 1.000.000,00", DinheiroBR.formatar(100_000_000L));
        assertEquals("-R$ 15,54", DinheiroBR.formatar(-1554));
    }

    @Test
    public void formatarReais_arredondaAntesDeFormatar() {
        assertEquals("R$ 0,30", DinheiroBR.formatarReais(0.1 + 0.2));
    }

    @Test
    public void formatarCompacto_faixas() {
        assertEquals("R$ 999,00", DinheiroBR.formatarCompacto(99_900));
        assertEquals("R$ 1,2 mil", DinheiroBR.formatarCompacto(123_400));
        assertEquals("R$ 3,5 mi", DinheiroBR.formatarCompacto(350_000_000L));
        assertEquals("-R$ 2,0 mil", DinheiroBR.formatarCompacto(-200_000));
    }

    @Test
    public void somar_ehExato() {
        // Em double, somar 0,10 dez vezes nao da 1,00
        long[] parcelas = new long[10];
        java.util.Arrays.fill(parcelas, 10);
        assertEquals(100, DinheiroBR.somar(parcelas));
    }

    @Test(expected = ArithmeticException.class)
    public void somar_estouroLancaExcecao() {
        DinheiroBR.somar(Long.MAX_VALUE, 1);
    }

    @Test
    public void percentual_totalZeroNaoEstoura() {
        assertEquals(0.0, DinheiroBR.percentual(500, 0), 0.0001);
        assertEquals(25.0, DinheiroBR.percentual(250, 1000), 0.0001);
    }
}
