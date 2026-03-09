package com.turbofan3360.openeq

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.content.ServiceConnection
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Build
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel

import com.turbofan3360.openeq.ui.screens.MainScreen
import com.turbofan3360.openeq.ui.theme.OpenEQTheme
import com.turbofan3360.openeq.audioprocessing.getEqBands
import com.turbofan3360.openeq.audioprocessing.eqFrequenciesToLabels
import com.turbofan3360.openeq.audioprocessing.EQMediaListenerService
import com.turbofan3360.openeq.audioprocessing.getEqRange

class MainActivityViewModel: ViewModel() {
    // State - whether EQ service is enabled or not
    var eqEnabled by mutableStateOf(false)
    // EQ frequency bands in milliHz
    val eqFrequencyBands = getEqBands()
    // String labels for EQ frequency bands
    val eqFrequencyBandsStr = eqFrequenciesToLabels(eqFrequencyBands)
    // Supported range of EQ bands (in dB)
    val eqRange = getEqRange()
    // State of the sliders (and so EQ levels)
    var eqLevels = mutableStateListOf(*MutableList(eqFrequencyBands.size) {0f}.toTypedArray())
}

class MainActivity : ComponentActivity() {
    private val foregroundServiceIntent: Intent by lazy{Intent(this, EQMediaListenerService::class.java)}
    val myViewModel: MainActivityViewModel by viewModels()

    // Class to bind to the foreground service
    private var eqService: EQMediaListenerService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as EQMediaListenerService.LocalBinder
            eqService = binder.getService()

            // Calls the service to set the current EQ levels
            eqService?.updateEqLevels(myViewModel.eqLevels)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            eqService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handles starting the app UI
        setContent {
            OpenEQTheme {
                MainScreen(
                    myViewModel.eqEnabled,
                    eqToggle = {
                        // Handles starting/stopping the foreground service to handle EQ
                        // If EQ not already enabled, then pressing the button means the user wants to enable it
                        if (!myViewModel.eqEnabled) {
                            // Started depends on whether or not the user allowed notifications
                            // No notifications -> foreground service can't run
                            val started = startMediaListenService()
                            // If service started, the eqEnabled = true, and vice versa
                            myViewModel.eqEnabled = started
                        }
                        else {
                            stopMediaListenService()
                            myViewModel.eqEnabled = false
                        }
                               },
                    myViewModel.eqLevels,
                    updateEqLevel = {index:Int, value:Float ->
                        myViewModel.eqLevels[index] = value
                        // Passing updated EQ levels to the foreground service managing EQ objects
                        eqService?.updateEqLevels(myViewModel.eqLevels)
                                    },
                    frequencyBands = myViewModel.eqFrequencyBandsStr,
                    eqRange = myViewModel.eqRange
                )
            }
        }
    }

    override fun onDestroy() {
        // Unbinds from the foreground service if it's bound
        if (eqService != null) {
            unbindService(connection)
        }
        // Calls the onDestroy() of the parent class to properly destroy the activity
        super.onDestroy()
    }

    private fun startMediaListenService(): Boolean {
        // Checking for and requesting notification permission if not already given
        val permissionGranted = checkNotificationPermission()

        if (!permissionGranted) {
            return false
        }

        // Starting the foreground service that listens for media streams starting
        this.startForegroundService(foregroundServiceIntent)
        // Binds to the service so new EQ levels can be passed in when the user sets them
        bindService(foregroundServiceIntent, connection, Context.BIND_AUTO_CREATE)

        return true
    }

    private fun stopMediaListenService() {
        // Unbinds from foreground service
        unbindService(connection)
        // Stops the foreground service that listens for media streams starting
        stopService(foregroundServiceIntent)
    }

    private fun checkNotificationPermission(): Boolean {
        // Function to check whether notification permission is given, and request it if not
        // Below Android 13, is permission automatically granted for notifications
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        // Checking whether notifications are enabled
        val notificationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)

        // Requesting permission if not enabled
        if (notificationPermission == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        // If permission still denied, return false
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            return false
        }

        return true
    }
}
