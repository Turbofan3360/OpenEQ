package com.turbofan3360.openeq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import com.turbofan3360.openeq.ui.screens.MainScreen
import com.turbofan3360.openeq.ui.theme.OpenEQTheme

class MainActivity : ComponentActivity() {
    var eqEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Creating EQ on/off state variable

        enableEdgeToEdge()
        setContent {
            OpenEQTheme {
                MainScreen(
                    eqEnabled,
                    eqToggle = {eqEnabled = !eqEnabled}
                )
            }
        }
    }
}
