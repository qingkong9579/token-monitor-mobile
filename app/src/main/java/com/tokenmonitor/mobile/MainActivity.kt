package com.tokenmonitor.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tokenmonitor.mobile.ui.MainScreen
import com.tokenmonitor.mobile.ui.theme.TokenMonitorTheme
import com.tokenmonitor.mobile.vm.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val state by vm.state.collectAsState()
            TokenMonitorTheme(darkTheme = state.settings.darkTheme) {
                MainScreen(vm)
            }
        }
    }
}
