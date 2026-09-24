package com.finguia.motor.importacao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import org.junit.Test;

public class LeitorOfxTest {

    private static final double D = 1e-9;

    // OFX 1.x (SGML), como exportado pela maioria dos bancos brasileiros
    private static final String OFX_SGML =
            "OFXHEADER:100\r\n"
            + "DATA:OFXSGML\r\n"
            + "VERSION:102\r\n"
            + "ENCODING:USASCII\r\n"
            + "CHARSET:1252\r\n"
            + "\r\n"
            + "<OFX>\r\n"
            + "<BANKMSGSRSV1><STMTTRNRS><STMTRS>\r\n"
            + "<CURDEF>BRL\r\n"
            + "<BANKACCTFROM>\r\n"
            + "<BANKID>0260\r\n"
            + "<ACCTID>123456-7\r\n"
            + "</BANKACCTFROM>\r\n"
            + "<BANKTRANLIST>\r\n"
            + "<STMTTRN>\r\n"
            + "<TRNTYPE>DEBIT\r\n"
            + "<DTPOSTED>20260105100000[-3:BRT]\r\n"
            + "<TRNAMT>-50.00\r\n"
            + "<FITID>abc-1\r\n"
            + "<MEMO>Compra no débito - PADARIA SÃO JOÃO\r\n"
            + "</STMTTRN>\r\n"
            + "<STMTTRN>\r\n"
            + "<TRNTYPE>CREDIT\r\n"
            + "<DTPOSTED>20260106\r\n"
            + "<TRNAMT>1500.00\r\n"
            + "<FITID>abc-2\r\n"
            + "<NAME>EMPRESA EXEMPLO LTDA\r\n"
            + "<MEMO>Transferência recebida\r\n"
            + "</STMTTRN>\r\n"
            + "</BANKTRANLIST>\r\n"
            + "</STMTRS></STMTTRNRS></BANKMSGSRSV1>\r\n"
            + "</OFX>\r\n";

    // OFX 2.x (XML)
    private static final String OFX_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<?OFX OFXHEADER=\"200\" VERSION=\"220\"?>\n"
            + "<OFX><SIGNONMSGSRSV1><SONRS><FI><ORG>Banco Exemplo</ORG></FI></SONRS></SIGNONMSGSRSV1>\n"
            + "<CREDITCARDMSGSRSV1><CCSTMTTRNRS><CCSTMTRS>\n"
            + "<CCACCTFROM><ACCTID>4111</ACCTID></CCACCTFROM>\n"
            + "<BANKTRANLIST>\n"
            + "<STMTTRN><TRNTYPE>DEBIT</TRNTYPE><DTPOSTED>20260210</DTPOSTED>"
            + "<TRNAMT>-89.90</TRNAMT><FITID>x1</FITID><MEMO>Loja &amp; Cia</MEMO></STMTTRN>\n"
            + "</BANKTRANLIST></CCSTMTRS></CCSTMTTRNRS></CREDITCARDMSGSRSV1></OFX>";

    @Test
    public void sgml_leLancamentosComSinalECategoria() {
        List<LancamentoExterno> l = LeitorOfx.ler(OFX_SGML).getLancamentos();
        assertEquals(2, l.size());

        assertEquals(-50.0, l.get(0).getValor(), D);
        assertFalse(l.get(0).ehEntrada());
        assertEquals("DEBIT", l.get(0).getCategoria());

        assertEquals(1500.0, l.get(1).getValor(), D);
        assertTrue(l.get(1).ehEntrada());
        assertEquals("EMPRESA EXEMPLO LTDA", l.get(1).getContraparte());
        assertFalse("extrato de conta corrente", l.get(0).isCartaoCredito());
    }

    @Test
    public void sgml_bancoPeloCodigoCompe() {
        assertEquals("Nubank", LeitorOfx.ler(OFX_SGML).getInstituicao());
    }

    @Test
    public void idExternoIncluiContaEFitid() {
        List<LancamentoExterno> l = LeitorOfx.ler(OFX_SGML).getLancamentos();
        assertEquals("ofx:123456-7:abc-1", l.get(0).getIdExterno());
    }

    @Test
    public void bytesEmWindows1252_mantemAcentos() {
        byte[] bytes = OFX_SGML.getBytes(Charset.forName("windows-1252"));
        List<LancamentoExterno> l = LeitorOfx.ler(bytes).getLancamentos();
        assertEquals("Compra no débito - PADARIA SÃO JOÃO", l.get(0).getDescricao());
    }

    @Test
    public void xml_cartaoDeCreditoComEntidadesEOrg() {
        byte[] bytes = OFX_XML.getBytes(StandardCharsets.UTF_8);
        LeitorOfx.Resultado r = LeitorOfx.ler(bytes);
        assertEquals("Banco Exemplo", r.getInstituicao());
        assertEquals(1, r.getLancamentos().size());
        assertEquals("Loja & Cia", r.getLancamentos().get(0).getDescricao());
        assertEquals(-89.9, r.getLancamentos().get(0).getValor(), D);
        assertTrue("fatura de cartao (CCSTMTRS)", r.getLancamentos().get(0).isCartaoCredito());
    }

    @Test
    public void data_comFusoInformado() {
        // 05/01/2026 10:00 em -3 = 13:00 UTC
        long ms = LeitorOfx.lerData("20260105100000[-3:BRT]");
        assertEquals(utc(2026, 1, 5, 13, 0), ms);
    }

    @Test
    public void data_semHoraCaiAoMeioDiaDeBrasilia() {
        // Meia-noite UTC seria 21h do dia anterior no Brasil
        long ms = LeitorOfx.lerData("20260106");
        assertEquals(utc(2026, 1, 6, 15, 0), ms);
    }

    @Test
    public void data_comFracaoDeSegundoEFusoPositivo() {
        long ms = LeitorOfx.lerData("20260105100000.000[+1:CET]");
        assertEquals(utc(2026, 1, 5, 9, 0), ms);
    }

    @Test
    public void data_invalida() {
        assertNull(LeitorOfx.lerData("ontem"));
        assertNull(LeitorOfx.lerData(null));
    }

    @Test
    public void valor_pontoVirgulaESinais() {
        assertEquals(-1500.0, LeitorOfx.lerValor("-1500.00"), D);
        assertEquals(-1500.0, LeitorOfx.lerValor("-1500,00"), D);
        assertEquals(1234.56, LeitorOfx.lerValor("1.234,56"), D);
        assertEquals(1234.56, LeitorOfx.lerValor("1,234.56"), D);
        assertEquals(10.0, LeitorOfx.lerValor("+10.00"), D);
        assertNull(LeitorOfx.lerValor("abc"));
    }

    @Test
    public void semFitid_geraIdEstavel() {
        String semFitid = OFX_SGML.replace("<FITID>abc-1\r\n", "");
        String id1 = LeitorOfx.ler(semFitid).getLancamentos().get(0).getIdExterno();
        String id2 = LeitorOfx.ler(semFitid).getLancamentos().get(0).getIdExterno();
        assertEquals("reimportar o mesmo arquivo deve dar o mesmo id", id1, id2);
        assertNotEquals("ofx:123456-7:abc-2", id1);
    }

    @Test
    public void blocoSemValor_ehIgnoradoEContado() {
        String quebrado = OFX_SGML.replace("<TRNAMT>-50.00", "<TRNAMT>");
        LeitorOfx.Resultado r = LeitorOfx.ler(quebrado);
        assertEquals(1, r.getLancamentos().size());
        assertEquals(1, r.getIgnorados());
    }

    @Test(expected = IllegalArgumentException.class)
    public void arquivoQueNaoEOfx() {
        LeitorOfx.ler("data;valor\n01/01;10,00");
    }

    @Test
    public void instituicoes_codigosEmVariosFormatos() {
        assertEquals("Nubank", InstituicoesBR.nome("260"));
        assertEquals("Nubank", InstituicoesBR.nome("0260"));
        assertEquals("Banco do Brasil", InstituicoesBR.nome("1"));
        assertNull(InstituicoesBR.nome("999"));
        assertNull(InstituicoesBR.nome(null));
    }

    private static long utc(int ano, int mes, int dia, int hora, int minuto) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(ano, mes - 1, dia, hora, minuto, 0);
        return c.getTimeInMillis();
    }
}
