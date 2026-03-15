package io.github.sj42tech.route42.tunnel

internal data class PendingConnection(
    val profileId: String,
    val profileName: String,
    val config: String,
)
