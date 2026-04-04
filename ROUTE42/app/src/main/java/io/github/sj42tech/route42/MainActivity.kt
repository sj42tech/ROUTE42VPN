package io.github.sj42tech.route42

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.sj42tech.route42.ui.AppViewModel
import io.github.sj42tech.route42.ui.Route42App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Route42App(viewModel = viewModel(factory = AppViewModel.factory))
        }
    }
}
