package io.sj42.vpn.tunnel

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import io.sj42.vpn.MainActivity
import io.sj42.vpn.Route42Application

internal object TunnelForeground {
    const val ChannelId = "vpn_tunnel"
    const val NotificationId = 1001
}

internal fun TunnelService.ensureTunnelForeground(title: String, text: String) {
    createTunnelNotificationChannel()
    val pendingIntent = PendingIntent.getActivity(
        this,
        1,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val notification = NotificationCompat.Builder(this, TunnelForeground.ChannelId)
        .setContentTitle(title)
        .setContentText(text)
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .build()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
            TunnelForeground.NotificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED,
        )
    } else {
        startForeground(TunnelForeground.NotificationId, notification)
    }
}

private fun TunnelService.createTunnelNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        TunnelForeground.ChannelId,
        "VPN tunnel",
        android.app.NotificationManager.IMPORTANCE_LOW,
    )
    Route42Application.notificationManager.createNotificationChannel(channel)
}
