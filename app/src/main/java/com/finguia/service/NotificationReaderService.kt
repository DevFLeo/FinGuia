package com.finguia.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.finguia.dados.TransacaoBancaria
import com.finguia.dados.TransacaoRepository
import com.finguia.dados.TipoTransacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Serviço de escuta de notificações do sistema.
 *
 * Captura automaticamente notificações de todos os aplicativos bancários
 * registrados em [BancoConfig], analisa o conteúdo via [AnalisadorNotificacao]
 * e persiste as transações detectadas no banco de dados Room.
 *
 * Requer que o usuário conceda a permissão de acesso a notificações em:
 * Configurações → Privacidade → Acesso a Notificações → FinGuia
 */
class NotificationReaderService : NotificationListenerService() {

    private val TAG = "FinGuia-Notif"

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var repository: TransacaoRepository

    // Cache de hashes recentes para suprimir re-postagens do mesmo evento.
    // Bancos frequentemente re-postam a notificação (expandindo, atualizando
    // contador etc.) e cada re-post dispara onNotificationPosted novamente.
    private val hashesRecentes = ArrayDeque<Pair<Int, Long>>()
    private val JANELA_DEDUPE_MS = 60_000L
    private val MAX_HASHES = 50

    override fun onCreate() {
        super.onCreate()
        repository = TransacaoRepository(applicationContext)
        Log.i(TAG, "Serviço de captura de notificações iniciado.")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.i(TAG, "Serviço de captura de notificações encerrado.")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Listener conectado ao sistema de notificações.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val pacote = sbn.packageName
        if (!BancoConfig.ehBancoConhecido(pacote)) return

        val banco = BancoConfig.nomeBanco(pacote) ?: return
        val notificacao = sbn.notification ?: return

        // Ignora resumos de grupo — trazem apenas "3 novas movimentações"
        // sem valor nem contexto, sujando o banco com lixo.
        if ((notificacao.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            Log.d(TAG, "[$banco] Ignorada — resumo de grupo.")
            return
        }

        val extras = notificacao.extras
        val titulo = extras.getString(Notification.EXTRA_TITLE).orEmpty()
        val texto = extrairTextoCompleto(extras)

        if (titulo.isBlank() && texto.isBlank()) return

        // Dedupe: bancos re-postam a mesma notificação várias vezes. Usamos um
        // hash de (pacote, título, texto) e uma janela temporal curta.
        val hash = (pacote + "|" + titulo + "|" + texto).hashCode()
        if (jaProcessadoRecentemente(hash)) {
            Log.d(TAG, "[$banco] Ignorada — duplicata recente.")
            return
        }

        Log.d(TAG, "[$banco] Capturada | Título: $titulo | Texto: $texto")
        processarNotificacao(banco, pacote, titulo, texto)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    /**
     * Concatena todos os campos textuais relevantes da notificação.
     * Bancos costumam colocar o valor em EXTRA_BIG_TEXT enquanto EXTRA_TEXT
     * contém só o teaser — ignorar BIG_TEXT perde justamente o valor.
     */
    private fun extrairTextoCompleto(extras: android.os.Bundle): String {
        val partes = mutableListOf<String>()

        extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.let { partes += it }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.let { partes += it }
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.let { partes += it }
        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.let { partes += it }
        extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()?.let { partes += it }

        // EXTRA_TEXT_LINES: listagem de linhas (usada por InboxStyle).
        val linhas = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        linhas?.forEach { it?.toString()?.let { s -> partes += s } }

        return partes.distinct().joinToString(" ").trim()
    }

    private fun jaProcessadoRecentemente(hash: Int): Boolean {
        val agora = System.currentTimeMillis()
        // Remove entradas fora da janela
        while (hashesRecentes.isNotEmpty() && agora - hashesRecentes.first().second > JANELA_DEDUPE_MS) {
            hashesRecentes.removeFirst()
        }
        if (hashesRecentes.any { it.first == hash }) return true
        hashesRecentes.addLast(hash to agora)
        while (hashesRecentes.size > MAX_HASHES) hashesRecentes.removeFirst()
        return false
    }

    private fun processarNotificacao(
        banco: String,
        pacote: String,
        titulo: String?,
        texto: String?
    ) {
        serviceScope.launch {
            val tipo = AnalisadorNotificacao.identificarTipo(titulo, texto)
            val valor = AnalisadorNotificacao.extrairValor(titulo, texto)

            if (tipo == TipoTransacao.DESCONHECIDO && valor == 0.0) {
                Log.w(
                    TAG,
                    "[$banco] Descartada — sem tipo/valor. Título='$titulo' Texto='$texto'"
                )
                return@launch
            }

            val descricao = AnalisadorNotificacao.gerarDescricao(tipo, banco, valor)

            val transacao = TransacaoBancaria(
                banco = banco,
                pacoteApp = pacote,
                tipo = tipo,
                valor = valor,
                descricao = descricao,
                tituloNotificacao = titulo.orEmpty(),
                textoNotificacao = texto.orEmpty()
            )

            val id = repository.salvar(transacao)
            Log.i(TAG, "[$banco] Transação salva (id=$id): $descricao")

            sendBroadcast(
                Intent(ACTION_NOVA_TRANSACAO).apply {
                    setPackage(packageName)
                    putExtra(EXTRA_BANCO, banco)
                    putExtra(EXTRA_DESCRICAO, descricao)
                    putExtra(EXTRA_VALOR, valor)
                }
            )
        }
    }

    companion object {
        const val ACTION_NOVA_TRANSACAO = "com.finguia.NOVA_TRANSACAO_BANCARIA"
        const val EXTRA_BANCO      = "extra_banco"
        const val EXTRA_DESCRICAO  = "extra_descricao"
        const val EXTRA_VALOR      = "extra_valor"
    }
}
