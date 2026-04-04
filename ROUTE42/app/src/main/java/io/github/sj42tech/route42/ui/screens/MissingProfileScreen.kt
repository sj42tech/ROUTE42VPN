package io.github.sj42tech.route42.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun MissingProfileScreen(onBack: () -> Unit) {
    LaunchedEffect(Unit) {
        onBack()
    }
}
