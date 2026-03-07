package com.turbofan3360.openeq

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel

import com.turbofan3360.openeq.ui.screens.MainScreen
import com.turbofan3360.openeq.ui.theme.OpenEQTheme
import com.turbofan3360.openeq.audioprocessing.getEqBands
import com.turbofan3360.openeq.audioprocessing.eqFrequenciesToLabels
import com.turbofan3360.openeq.audioprocessing.EQMediaListenerService

class MainActivityViewModel: ViewModel() {
    // State - whether EQ service is enabled or not
    var eqEnabled by mutableStateOf(false)
    // EQ frequency bands in milliHz
    val eqFrequencyBands = getEqBands()
    // String labels for EQ frequency bands
    val eqFrequencyBandsStr = eqFrequenciesToLabels(eqFrequencyBands)
    // State of the sliders (and so EQ levels)
    var eqLevels = mutableStateListOf(*MutableList(eqFrequencyBands.size) {0f}.toTypedArray())
}

class MainActivity : ComponentActivity() {
    val foregroundServiceIntent: Intent by lazy{Intent(this, EQMediaListenerService::class.java)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handles starting the app UI
        setContent {
            val myViewModel: MainActivityViewModel = viewModel()
            OpenEQTheme {
                MainScreen(
                    myViewModel.eqEnabled,
                    eqToggle = {
                        myViewModel.eqEnabled = !myViewModel.eqEnabled
                        // Handles starting/stopping the foreground service to listen for media streams starting
                        if (myViewModel.eqEnabled) {
                            startMediaListenService()
                        }
                        else {
                            stopMediaListenService()
                        }
                               },
                    myViewModel.eqLevels,
                    updateEqLevel = {index:Int, value:Float ->
                        myViewModel.eqLevels[index] = value},
                    frequencyBands = myViewModel.eqFrequencyBandsStr,
                )
            }
        }
    }

    private fun startMediaListenService() {
        // Checking for and requesting notification permission if not already given
        checkNotificationPermission()
        // Starting the foreground service that listens for media streams starting
        this.startForegroundService(foregroundServiceIntent)
    }

    private fun stopMediaListenService() {
        // Stops the foreground service that listens for media streams starting
        stopService(foregroundServiceIntent)
    }

    private fun checkNotificationPermission() {
        // Function to check whether notification permission is given, and request it if not
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Checking notifications are enabled
            val notificationPermission =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)

            // Requesting permission if not enabled
            if (notificationPermission == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
        }
    }
}
