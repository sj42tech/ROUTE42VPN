package io.sj42.vpn.tunnel

internal data class PendingConnection(
    val profileId: String,
    val profileName: String,
    val config: String,
)
