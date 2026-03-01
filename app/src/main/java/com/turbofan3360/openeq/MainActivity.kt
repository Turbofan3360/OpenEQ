package com.turbofan3360.openeq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel

import com.turbofan3360.openeq.ui.screens.MainScreen
import com.turbofan3360.openeq.ui.theme.OpenEQTheme

class MainActivityViewModel: ViewModel() {
    // State - whether EQ service is enabled or not
    var eqEnabled by mutableStateOf(false)
    // State of the sliders (and so EQ levels)
    var eqLevels = mutableStateListOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
    // EQ frequency bands in Hz
    val eqFrequencyBandsFlt = listOf(
        31.25f, 62.5f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f
    )
    // String labels for EQ frequency bands
    val eqFrequencyBandsStr = listOf(
        "31.25", "62.5", "125", "250", "500", "1K", "2K", "4K", "8K", "16K"
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainActivityViewModel = viewModel()
            OpenEQTheme {
                MainScreen(
                    viewModel.eqEnabled,
                    eqToggle = {viewModel.eqEnabled = !viewModel.eqEnabled},
                    viewModel.eqLevels,
                    updateEqLevel = {index:Int, value:Float -> viewModel.eqLevels[index] = value},
                    viewModel.eqFrequencyBandsStr
                )
            }
        }
    }
}
