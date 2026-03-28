// app/src/main/java/com/finguia/app/service/NotificationReaderService.kt
package com.finguia.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationReaderService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            val notification: Notification = it.notification
            val extras = notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE)
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

            Log.d("FinGuia", "Notification Posted: ")
            Log.d("FinGuia", "Package: $packageName")
            Log.d("FinGuia", "Title: $title")
            Log.d("FinGuia", "Text: $text")

            // Função para processar a notificação, filtrar por aplicativos específicos,
            // extrair valores e (ou) salvar no banco de dados ou enviar para alguma API.
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let {
            Log.d("FinGuia", "Notification Removed: ${it.packageName}")
        }
    }
}