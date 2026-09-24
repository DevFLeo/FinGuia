package com.finguia.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.finguia.dados.TransacaoBancaria
import com.finguia.dados.TransacaoRepository
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

    private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

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
        recuperarPerdidas()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return
        examinar(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    /**
     * Notificações que chegaram enquanto o serviço estava parado (app morto
     * pelo sistema, aparelho reiniciado) continuam na bandeja. Processa só as
     * postadas depois da última que o serviço viu: assim uma transação que o
     * usuário apagou não volta, e nada é gravado duas vezes.
     */
    private fun recuperarPerdidas() {
        val marca = prefs.getLong(CHAVE_ULTIMA_POSTAGEM, 0L)
        if (marca == 0L) {
            // Primeira execução (ou atualização da versão que não tinha a marca):
            // a bandeja pode ter notificações já gravadas com outro horário
            marcarVista(System.currentTimeMillis())
            return
        }
        val ativas = try {
            activeNotifications
        } catch (e: SecurityException) {
            Log.w(TAG, "Sem acesso às notificações ativas: ${e.message}")
            null
        } ?: return

        val perdidas = ativas.filter { it.postTime > marca }.sortedBy { it.postTime }
        if (perdidas.isNotEmpty()) {
            Log.i(TAG, "Recuperando ${perdidas.size} notificação(ões) postada(s) com o serviço parado.")
            perdidas.forEach(::examinar)
        }
    }

    private fun examinar(sbn: StatusBarNotification) {
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
        // Notificação contínua (sincronizando, upload, player): nunca é transação
        if ((notificacao.flags and Notification.FLAG_ONGOING_EVENT) != 0) {
            Log.d(TAG, "[$banco] Ignorada — notificação contínua.")
            return
        }

        val extras = notificacao.extras
        val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val texto = extrairTextoCompleto(extras)

        if (titulo.isBlank() && texto.isBlank()) return
        marcarVista(sbn.postTime)

        // Dedupe: bancos re-postam a mesma notificação várias vezes. Usamos um
        // hash de (pacote, título, texto) e uma janela temporal curta.
        val hash = (pacote + "|" + titulo + "|" + texto).hashCode()
        if (jaProcessadoRecentemente(hash)) {
            Log.d(TAG, "[$banco] Ignorada — duplicata recente.")
            return
        }

        Log.d(TAG, "[$banco] Capturada | Título: $titulo | Texto: $texto")
        processarNotificacao(banco, pacote, titulo, texto, sbn.postTime)
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

    /** Avança a marca d'água usada por [recuperarPerdidas]. Nunca retrocede. */
    private fun marcarVista(postadaEmMs: Long) {
        if (postadaEmMs > prefs.getLong(CHAVE_ULTIMA_POSTAGEM, 0L)) {
            prefs.edit().putLong(CHAVE_ULTIMA_POSTAGEM, postadaEmMs).apply()
        }
    }

    private fun processarNotificacao(
        banco: String,
        pacote: String,
        titulo: String,
        texto: String,
        postadaEmMs: Long
    ) {
        serviceScope.launch {
            // Re-entrega da mesma notificação (mesmo texto e mesmo horário)
            if (repository.existeCaptura(pacote, titulo, texto, postadaEmMs)) {
                Log.d(TAG, "[$banco] Ignorada — já gravada.")
                return@launch
            }

            val transacao = when (val analise = AnalisadorNotificacao.analisar(titulo, texto, banco)) {
                is Analise.Descartada -> {
                    Log.i(TAG, "[$banco] Descartada — ${analise.motivo}. Título='$titulo'")
                    return@launch
                }
                is Analise.Transacao -> TransacaoBancaria(
                    banco = banco,
                    pacoteApp = pacote,
                    tipo = analise.tipo,
                    valor = analise.valor,
                    descricao = analise.descricao,
                    tituloNotificacao = titulo,
                    textoNotificacao = texto,
                    // Hora em que o banco postou, não a do processamento: uma
                    // notificação recuperada horas depois fica com a hora certa
                    timestampMs = postadaEmMs
                )
            }

            val id = repository.salvar(transacao)
            Log.i(TAG, "[$banco] Transação salva (id=$id): ${transacao.descricao}")

            sendBroadcast(
                Intent(ACTION_NOVA_TRANSACAO).apply {
                    setPackage(packageName)
                    putExtra(EXTRA_BANCO, banco)
                    putExtra(EXTRA_DESCRICAO, transacao.descricao)
                    putExtra(EXTRA_VALOR, transacao.valor)
                }
            )
        }
    }

    companion object {
        const val ACTION_NOVA_TRANSACAO = "com.finguia.NOVA_TRANSACAO_BANCARIA"
        const val EXTRA_BANCO      = "extra_banco"
        const val EXTRA_DESCRICAO  = "extra_descricao"
        const val EXTRA_VALOR      = "extra_valor"

        private const val PREFS = "finguia_captura"
        private const val CHAVE_ULTIMA_POSTAGEM = "ultima_postagem_vista_ms"
    }
}
