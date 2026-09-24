package com.finguia.dados

import com.finguia.motor.importacao.ClassificadorImportacao
import com.finguia.motor.importacao.LancamentoExterno
import com.finguia.motor.importacao.LeitorOfx
import com.finguia.motor.importacao.LeitorOpenFinance
import kotlin.math.abs

/** O que aconteceu numa importação, para mostrar ao usuário. */
data class ResumoImportacao(
    val formato: FormatoExtrato,
    val instituicao: String?,
    /** Lançamentos válidos no arquivo. */
    val lidos: Int,
    val importados: Int,
    /** Já estavam no app (importação repetida ou arquivo sobreposto). */
    val jaExistiam: Int,
    /** Linhas do arquivo sem valor ou data válidos. */
    val ignorados: Int,
)

enum class FormatoExtrato(val rotulo: String, val pacote: String) {
    OFX("Extrato OFX", "importado.ofx"),
    OPEN_FINANCE("Open Finance", "importado.openfinance"),
}

/**
 * Importa extratos OFX e respostas da API de Contas do Open Finance para o
 * banco do app. A leitura e a classificação ficam no pacote motor.importacao
 * (Java); aqui ficam a conversão para [TransacaoBancaria] e a deduplicação.
 */
class ImportadorExtrato(private val repository: TransacaoRepository) {

    suspend fun importar(bytes: ByteArray): ResumoImportacao {
        val leitura = ler(bytes)
        // Dentro do próprio arquivo o mesmo id pode repetir (extratos sobrepostos)
        val unicos = leitura.lancamentos.distinctBy { it.idExterno }
        val existentes = repository.idsExternosExistentes(unicos.map { it.idExterno })
        val novos = unicos.filter { it.idExterno !in existentes }

        repository.salvarTodas(novos.map { paraTransacao(it, leitura.formato) })

        return ResumoImportacao(
            formato = leitura.formato,
            instituicao = leitura.instituicao,
            lidos = leitura.lancamentos.size,
            importados = novos.size,
            jaExistiam = leitura.lancamentos.size - novos.size,
            ignorados = leitura.ignorados,
        )
    }

    data class Leitura(
        val formato: FormatoExtrato,
        val lancamentos: List<LancamentoExterno>,
        val instituicao: String?,
        val ignorados: Int,
    )

    companion object {
        /** Banco exibido quando a origem não informa. */
        const val INSTITUICAO_PADRAO_OPEN_FINANCE = "Open Finance"
        const val INSTITUICAO_PADRAO_OFX = "Extrato importado"

        /**
         * Detecta o formato pelo conteúdo, não pela extensão: arquivos baixados
         * pelo celular costumam chegar sem extensão ou como .txt.
         */
        fun ler(bytes: ByteArray): Leitura {
            val inicio = String(bytes, 0, minOf(bytes.size, 64), Charsets.ISO_8859_1)
                .trimStart('﻿', 'ï', '»', '¿', ' ', '\n', '\r', '\t')
            return if (inicio.startsWith("{") || inicio.startsWith("[")) {
                val r = LeitorOpenFinance.ler(String(bytes, Charsets.UTF_8), INSTITUICAO_PADRAO_OPEN_FINANCE)
                Leitura(FormatoExtrato.OPEN_FINANCE, r.lancamentos, INSTITUICAO_PADRAO_OPEN_FINANCE, r.ignorados)
            } else {
                val r = LeitorOfx.ler(bytes)
                Leitura(FormatoExtrato.OFX, r.lancamentos, r.instituicao, r.ignorados)
            }
        }

        fun paraTransacao(l: LancamentoExterno, formato: FormatoExtrato): TransacaoBancaria {
            val tipo = ClassificadorImportacao.classificar(l)
            val instituicao = l.instituicao ?: when (formato) {
                FormatoExtrato.OFX -> INSTITUICAO_PADRAO_OFX
                FormatoExtrato.OPEN_FINANCE -> INSTITUICAO_PADRAO_OPEN_FINANCE
            }
            return TransacaoBancaria(
                banco = instituicao,
                pacoteApp = formato.pacote,
                tipo = tipo,
                // No app o valor é sempre positivo; a direção vem do tipo
                valor = abs(l.valor),
                descricao = l.descricao.ifBlank { l.contraparte ?: formato.rotulo },
                tituloNotificacao = "",
                textoNotificacao = "",
                timestampMs = l.dataMs,
                dataAgendada = if (l.isEfetivado) null else l.dataMs,
                efetivado = l.isEfetivado,
                idExterno = l.idExterno,
            )
        }
    }
}
