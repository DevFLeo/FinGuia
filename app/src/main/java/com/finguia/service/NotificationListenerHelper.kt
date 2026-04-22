package com.finguia.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationManagerCompat

/**
 * Utilitários para verificar e reiniciar o [NotificationReaderService].
 *
 * Quando o usuário concede a permissão pela primeira vez, ou depois de uma
 * atualização do app, o Android às vezes não reconecta o listener até um
 * reboot. Desligar/religar o componente via PackageManager força a rebind
 * imediata, que é o comportamento que o usuário espera.
 */
object NotificationListenerHelper {

    private const val TAG = "FinGuia-Notif"

    fun listenerAtivo(context: Context): Boolean {
        val habilitados = NotificationManagerCompat.getEnabledListenerPackages(context)
        return habilitados.contains(context.packageName)
    }

    /**
     * Força o sistema a re-bindar nosso NotificationListenerService. Necessário
     * quando a permissão acabou de ser concedida ou o app foi atualizado — o
     * Android mantém o estado do componente em cache, e só uma alteração
     * explícita de componente garante que ele reconecte.
     */
    fun reconectarListener(context: Context) {
        val pm = context.packageManager
        val componente = ComponentName(context, NotificationReaderService::class.java)
        try {
            pm.setComponentEnabledSetting(
                componente,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                componente,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            // Sinaliza ao sistema que deve reconectar o listener agora.
            NotificationListenerService.requestRebind(componente)
            Log.i(TAG, "Rebind do listener solicitado.")
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao solicitar rebind do listener.", e)
        }
    }
}
