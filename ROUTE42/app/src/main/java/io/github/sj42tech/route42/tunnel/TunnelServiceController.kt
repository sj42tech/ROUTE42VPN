package io.github.sj42tech.route42.tunnel

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.sj42tech.route42.AppIds

internal object TunnelServiceController {
    private const val ExtraProfileId = "profile_id"
    private const val ExtraProfileName = "profile_name"
    private const val ExtraConfig = "config"
    private const val ExtraOnlySelectedAppsUseVpn = "only_selected_apps_use_vpn"

    fun isConnectAction(context: Context, action: String?): Boolean = action == AppIds.actionConnect(context.packageName)

    fun isDisconnectAction(context: Context, action: String?): Boolean = action == AppIds.actionDisconnect(context.packageName)

    fun readProfileId(intent: Intent): String? = intent.getStringExtra(ExtraProfileId)

    fun readProfileName(intent: Intent): String? = intent.getStringExtra(ExtraProfileName)

    fun readConfig(intent: Intent): String? = intent.getStringExtra(ExtraConfig)

    fun readOnlySelectedAppsUseVpn(intent: Intent): Boolean =
        intent.getBooleanExtra(ExtraOnlySelectedAppsUseVpn, false)

    fun start(
        context: Context,
        profileId: String,
        profileName: String,
        config: String,
        onlySelectedAppsUseVpn: Boolean,
    ) {
        val intent = Intent(context, TunnelService::class.java).apply {
            action = AppIds.actionConnect(context.packageName)
            putExtra(ExtraProfileId, profileId)
            putExtra(ExtraProfileName, profileName)
            putExtra(ExtraConfig, config)
            putExtra(ExtraOnlySelectedAppsUseVpn, onlySelectedAppsUseVpn)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        context.startService(
            Intent(context, TunnelService::class.java).apply {
                action = AppIds.actionDisconnect(context.packageName)
            },
        )
    }
}
