package io.github.sj42tech.route42.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfilesSnapshot(
    val profiles: List<ConnectionProfile> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.DARK,
)
