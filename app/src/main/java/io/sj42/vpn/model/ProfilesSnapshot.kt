package io.sj42.vpn.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfilesSnapshot(
    val profiles: List<ConnectionProfile> = emptyList(),
)
