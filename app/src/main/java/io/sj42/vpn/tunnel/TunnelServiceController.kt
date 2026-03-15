package io.sj42.vpn.tunnel

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

internal object TunnelServiceController {
    private const val ActionConnect = "io.sj42.vpn.action.CONNECT"
    private const val ActionDisconnect = "io.sj42.vpn.action.DISCONNECT"
    private const val ExtraProfileId = "profile_id"
    private const val ExtraProfileName = "profile_name"
    private const val ExtraConfig = "config"

    fun isConnectAction(action: String?): Boolean = action == ActionConnect

    fun isDisconnectAction(action: String?): Boolean = action == ActionDisconnect

    fun readProfileId(intent: Intent): String? = intent.getStringExtra(ExtraProfileId)

    fun readProfileName(intent: Intent): String? = intent.getStringExtra(ExtraProfileName)

    fun readConfig(intent: Intent): String? = intent.getStringExtra(ExtraConfig)

    fun start(context: Context, profileId: String, profileName: String, config: String) {
        val intent = Intent(context, TunnelService::class.java).apply {
            action = ActionConnect
            putExtra(ExtraProfileId, profileId)
            putExtra(ExtraProfileName, profileName)
            putExtra(ExtraConfig, config)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        context.startService(
            Intent(context, TunnelService::class.java).apply {
                action = ActionDisconnect
            },
        )
    }
}
