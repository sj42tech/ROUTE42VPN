package io.github.sj42tech.route42.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
    LIGHT,
    DARK,
}

fun ThemeMode.isDarkTheme(): Boolean = this == ThemeMode.DARK
