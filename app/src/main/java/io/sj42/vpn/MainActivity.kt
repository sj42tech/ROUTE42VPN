package io.sj42.vpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import io.sj42.vpn.ui.AppViewModel
import io.sj42.vpn.ui.Route42App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Route42App(viewModel = viewModel(factory = AppViewModel.factory))
        }
    }
}
