package com.finguia.motor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class NumeroBRTest {

    private static final double D = 1e-9;

    // --------------------------------------------------------------- leitura

    @Test
    public void ler_padraoBrasileiroCompleto() {
        assertEquals(1234.56, NumeroBR.ler("1.234,56"), D);
    }

    @Test
    public void ler_regressaoCampoZerado() {
        // Bug antigo: "1.234,56".replace(",", ".") virava "1.234.56",
        // toDoubleOrNull() devolvia null e a calculadora usava 0.
        assertEquals(1234.56, NumeroBR.ler("1.234,56"), D);
        assertEquals(1000000.0, NumeroBR.ler("1.000.000,00"), D);
    }

    @Test
    public void ler_soVirgula() {
        assertEquals(10.75, NumeroBR.ler("10,75"), D);
    }

    @Test
    public void ler_pontoComTresDigitosEhMilhar() {
        assertEquals(1500.0, NumeroBR.ler("1.500"), D);
    }

    @Test
    public void ler_variosPontosSaoMilhar() {
        assertEquals(1234567.0, NumeroBR.ler("1.234.567"), D);
    }

    @Test
    public void ler_pontoDecimalDoTecladoIngles() {
        assertEquals(12.5, NumeroBR.ler("12.5"), D);
        assertEquals(0.99, NumeroBR.ler("0.99"), D);
    }

    @Test
    public void ler_ignoraCifraoPercentualEEspacos() {
        assertEquals(1234.56, NumeroBR.ler("R$ 1.234,56"), D);
        assertEquals(13.25, NumeroBR.ler(" 13,25 % "), D);
        assertEquals(50.0, NumeroBR.ler("R$ 50,00"), D);
    }

    @Test
    public void ler_negativo() {
        assertEquals(-15.54, NumeroBR.ler("-15,54"), D);
    }

    @Test
    public void ler_invalidosDevolvemNull() {
        assertNull(NumeroBR.ler(null));
        assertNull(NumeroBR.ler(""));
        assertNull(NumeroBR.ler("   "));
        assertNull(NumeroBR.ler("-"));
        assertNull(NumeroBR.ler(","));
        assertNull(NumeroBR.ler("abc"));
        assertNull(NumeroBR.ler("1,2,3"));
    }

    @Test
    public void lerOu_usaPadraoQuandoInvalido() {
        assertEquals(0.0, NumeroBR.lerOu("", 0.0), D);
        assertEquals(7.0, NumeroBR.lerOu("x", 7.0), D);
        assertEquals(3.5, NumeroBR.lerOu("3,5", 0.0), D);
    }

    // ------------------------------------------------------------ exibicao

    @Test
    public void formatar_padraoZeroPontoZeroZeroZeroVirgulaZeroZero() {
        assertEquals("0,00", NumeroBR.formatar(0.0));
        assertEquals("1.234,56", NumeroBR.formatar(1234.56));
        assertEquals("1.000.000,00", NumeroBR.formatar(1_000_000.0));
    }

    @Test
    public void formatar_arredondaMeioParaCima() {
        assertEquals("0,13", NumeroBR.formatar(0.125));
        assertEquals("2,68", NumeroBR.formatar(2.675000001));
    }

    @Test
    public void formatar_naoExibeMenosZero() {
        assertEquals("0,00", NumeroBR.formatar(-0.001));
        assertEquals("R$ 0,00", NumeroBR.moeda(-0.001));
    }

    @Test
    public void formatarFlexivel_cortaZerosADireita() {
        assertEquals("0,00001234", NumeroBR.formatarFlexivel(0.00001234, 2, 8));
        assertEquals("50.000,00", NumeroBR.formatarFlexivel(50000.0, 2, 8));
    }

    @Test
    public void moeda_comESemSinal() {
        assertEquals("R$ 1.234,56", NumeroBR.moeda(1234.56));
        assertEquals("-R$ 15,54", NumeroBR.moeda(-15.54));
        assertEquals("+R$ 10,00", NumeroBR.moedaComSinal(10.0));
        assertEquals("-R$ 10,00", NumeroBR.moedaComSinal(-10.0));
        assertEquals("R$ 0,00", NumeroBR.moedaComSinal(0.0));
    }

    @Test
    public void percentual_formatos() {
        assertEquals("12,50%", NumeroBR.percentual(12.5, 2));
        assertEquals("+3,2%", NumeroBR.percentualComSinal(3.2, 1));
        assertEquals("-0,75%", NumeroBR.percentualComSinal(-0.75, 2));
        assertEquals("0,00%", NumeroBR.percentualComSinal(0.0001, 2));
    }

    @Test
    public void paraCampo_semMilharParaEdicao() {
        assertEquals("1234,50", NumeroBR.paraCampo(1234.5, 2));
        assertEquals("", NumeroBR.paraCampo(0.0, 2));
    }

    @Test
    public void calculadoraCientifica_resultadoExibidoVoltaIgualNoMMais() {
        // A calculadora exibe com formatarFlexivel(r, 0, 8) e o M+ le o texto
        // exibido com ler(). Antes o resultado saia em Locale.US: 1,234 era
        // exibido "1.234" e voltaria como mil duzentos e trinta e quatro.
        double[] resultados = {1.234, 1500.0, 1234567.0, 0.5, 0.00001234, -42.75, 12.345};
        for (double r : resultados) {
            String exibido = NumeroBR.formatarFlexivel(r, 0, 8);
            assertEquals("ida e volta de " + r + " via \"" + exibido + "\"", r, NumeroBR.ler(exibido), D);
        }
        assertEquals("1,234", NumeroBR.formatarFlexivel(1.234, 0, 8));
        assertEquals("1.500", NumeroBR.formatarFlexivel(1500.0, 0, 8));
    }

    @Test
    public void idaEVolta() {
        double[] valores = {0.01, 9.99, 1234.56, 1_000_000.0, 98765.43};
        for (double v : valores) {
            assertEquals(v, NumeroBR.ler(NumeroBR.formatar(v)), D);
            assertEquals(v, NumeroBR.ler(NumeroBR.moeda(v)), D);
        }
    }
}
