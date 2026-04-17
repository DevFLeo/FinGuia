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

    // Escopo de coroutine vinculado ao ciclo de vida do serviço
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var repository: TransacaoRepository

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

        // Filtra apenas notificações de bancos e apps financeiros conhecidos
        if (!BancoConfig.ehBancoConhecido(pacote)) return

        val banco = BancoConfig.nomeBanco(pacote) ?: return
        val extras = sbn.notification.extras
        val titulo = extras.getString(Notification.EXTRA_TITLE)
        val texto = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        Log.d(TAG, "[$banco] Notificação capturada | Título: $titulo | Texto: $texto")

        processarNotificacao(banco, pacote, titulo, texto)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Ignoramos remoções — a transação já foi salva ao chegar
    }

    /**
     * Analisa e persiste a notificação bancária capturada.
     * Notificações sem valor monetário identificável são descartadas,
     * exceto para tipos de transação como COBRANÇA e DESCONHECIDO com banco válido.
     */
    private fun processarNotificacao(
        banco: String,
        pacote: String,
        titulo: String?,
        texto: String?
    ) {
        serviceScope.launch {
            val tipo = AnalisadorNotificacao.identificarTipo(titulo, texto)
            val valor = AnalisadorNotificacao.extrairValor(titulo, texto)

            // Descarta notificações sem valor e sem tipo reconhecido
            // (ex: promoções, avisos genéricos sem contexto financeiro)
            if (tipo == TipoTransacao.DESCONHECIDO && valor == 0.0) {
                Log.d(TAG, "[$banco] Notificação descartada — sem tipo nem valor reconhecido.")
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

            // Envia broadcast para a UI atualizar em tempo real
            sendBroadcast(
                Intent(ACTION_NOVA_TRANSACAO).apply {
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
