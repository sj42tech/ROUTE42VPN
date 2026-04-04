package io.github.sj42tech.route42.tunnel

import android.os.Build
import android.os.Process
import io.github.sj42tech.route42.Route42Application
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.WIFIState
import java.net.InetSocketAddress
import java.security.KeyStore

internal fun TunnelService.resolveConnectionOwner(
    ipProtocol: Int,
    sourceAddress: String,
    sourcePort: Int,
    destinationAddress: String,
    destinationPort: Int,
): ConnectionOwner {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        error("Connection owner lookup requires Android 10+")
    }
    val uid = Route42Application.connectivity.getConnectionOwnerUid(
        ipProtocol,
        InetSocketAddress(sourceAddress, sourcePort),
        InetSocketAddress(destinationAddress, destinationPort),
    )
    if (uid == Process.INVALID_UID) {
        error("Connection owner not found")
    }
    val packageName = packageManager.getPackagesForUid(uid)?.firstOrNull().orEmpty()
    return ConnectionOwner().apply {
        userId = uid
        userName = packageName
        androidPackageName = packageName
    }
}

internal fun readWifiStateSnapshot(): WIFIState? {
    @Suppress("DEPRECATION")
    val wifiInfo = Route42Application.wifi.connectionInfo ?: return null
    var ssid = wifiInfo.ssid ?: return null
    if (ssid == "<unknown ssid>") {
        return WIFIState("", "")
    }
    if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
        ssid = ssid.substring(1, ssid.length - 1)
    }
    return WIFIState(ssid, wifiInfo.bssid.orEmpty())
}

internal fun readSystemCertificatesSnapshot(): StringIterator {
    val certificates = mutableListOf<String>()
    val keyStore = KeyStore.getInstance("AndroidCAStore")
    keyStore.load(null, null)
    val aliases = keyStore.aliases()
    while (aliases.hasMoreElements()) {
        val certificate = keyStore.getCertificate(aliases.nextElement()) ?: continue
        certificates += "-----BEGIN CERTIFICATE-----\n" +
            android.util.Base64.encodeToString(certificate.encoded, android.util.Base64.NO_WRAP) +
            "\n-----END CERTIFICATE-----"
    }
    return SimpleStringIterator(certificates)
}
