package com.finguia.dados

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportadorExtratoTest {

    private val ofx = """
        OFXHEADER:100
        DATA:OFXSGML
        CHARSET:1252

        <OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS>
        <BANKACCTFROM><BANKID>341<ACCTID>999</BANKACCTFROM>
        <BANKTRANLIST>
        <STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260105<TRNAMT>-42.50<FITID>1<MEMO>PIX ENVIADO JOAO</STMTTRN>
        <STMTTRN><TRNTYPE>CREDIT<DTPOSTED>20260106<TRNAMT>100.00<FITID>2<MEMO></STMTTRN>
        </BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>
    """.trimIndent().toByteArray(Charsets.ISO_8859_1)

    private val openFinance = """
        {"data":[{"transactionId":"of-1","completedAuthorisedPaymentType":"LANCAMENTO_FUTURO",
        "creditDebitType":"DEBITO","transactionName":"CONTA DE LUZ","type":"BOLETO",
        "transactionAmount":{"amount":"150.00","currency":"BRL"},
        "transactionDateTime":"2026-02-10T12:00:00Z"}]}
    """.trimIndent().toByteArray(Charsets.UTF_8)

    @Test
    fun detectaOfxPeloConteudo() {
        val leitura = ImportadorExtrato.ler(ofx)
        assertEquals(FormatoExtrato.OFX, leitura.formato)
        assertEquals("Itaú", leitura.instituicao)
        assertEquals(2, leitura.lancamentos.size)
    }

    @Test
    fun detectaOpenFinanceMesmoComBom() {
        val comBom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + openFinance
        assertEquals(FormatoExtrato.OPEN_FINANCE, ImportadorExtrato.ler(comBom).formato)
    }

    @Test
    fun conversao_valorPositivoTipoPelaDirecaoEIdExterno() {
        val l = ImportadorExtrato.ler(ofx).lancamentos
        val pix = ImportadorExtrato.paraTransacao(l[0], FormatoExtrato.OFX)
        assertEquals(TipoTransacao.PIX_ENVIADO, pix.tipo)
        assertEquals(42.5, pix.valor, 0.001)
        assertEquals("Itaú", pix.banco)
        assertEquals("importado.ofx", pix.pacoteApp)
        assertEquals("ofx:999:1", pix.idExterno)
        assertTrue(pix.efetivado)
        assertNull(pix.dataAgendada)
    }

    @Test
    fun conversao_semDescricaoUsaRotuloDoFormato() {
        val credito = ImportadorExtrato.paraTransacao(ImportadorExtrato.ler(ofx).lancamentos[1], FormatoExtrato.OFX)
        assertEquals("Extrato OFX", credito.descricao)
    }

    @Test
    fun conversao_lancamentoFuturoViraAgendado() {
        val l = ImportadorExtrato.ler(openFinance).lancamentos.single()
        val t = ImportadorExtrato.paraTransacao(l, FormatoExtrato.OPEN_FINANCE)
        assertFalse(t.efetivado)
        assertEquals(t.timestampMs, t.dataAgendada)
        assertEquals(TipoTransacao.BOLETO_PAGO, t.tipo)
        assertEquals("Open Finance", t.banco)
    }
}
