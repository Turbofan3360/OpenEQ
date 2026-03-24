package com.turbofan3360.openeq

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.turbofan3360.openeq.appdata.DatabaseHandler
import com.turbofan3360.openeq.appdata.SharedPreferencesSettings
import com.turbofan3360.openeq.audioprocessing.EqForegroundService
import com.turbofan3360.openeq.audioprocessing.eqFrequenciesToLabels
import com.turbofan3360.openeq.audioprocessing.getEqBands
import com.turbofan3360.openeq.audioprocessing.getEqRange
import com.turbofan3360.openeq.audioprocessing.globalEqAllowed
import com.turbofan3360.openeq.ui.screens.MainScreen
import com.turbofan3360.openeq.ui.theme.OpenEQTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivityViewModel : ViewModel() {
    // State - whether EQ service is enabled or not
    var eqEnabled by mutableStateOf(false)

    // Whether the device supports global audio EQ
    val globalAudioAllowed = globalEqAllowed()

    // Whether to try and attach the EQ to the global audio mix
    var tryGlobalAudio by mutableStateOf(false)

    // EQ frequency bands in milliHz
    val eqFrequencyBands = getEqBands()

    // String labels for EQ frequency bands
    val eqFrequencyBandsStr = eqFrequenciesToLabels(eqFrequencyBands)

    // Supported range of EQ bands (in dB)
    val eqRange = getEqRange()

    // State of the sliders (and so EQ levels)
    var eqLevels = List(eqFrequencyBands.size) { 0f }.toMutableStateList()

    // List of preset ID strings
    var presetIdStrings = mutableStateListOf<String>()
}

class MainActivity : ComponentActivity() {
    val myViewModel: MainActivityViewModel by viewModels()

    val appSettings by lazy { SharedPreferencesSettings(this) }
    private val appDb by lazy { DatabaseHandler() }

    private val foregroundServiceIntent: Intent by lazy { Intent(this, EqForegroundService::class.java) }

    // Class to bind to the foreground service
    private var eqService: EqForegroundService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as EqForegroundService.LocalBinder
            eqService = binder.getService()

            // Calls the service to pass the needed data
            eqService?.updateEqLevels(myViewModel.eqLevels)
            eqService?.updateTryGlobalAudio(myViewModel.tryGlobalAudio)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            eqService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Function that handles initialization of app state (database, shared preferences, finding foreground service)
        appDataInit()

        // Handles starting the app UI
        setContent {
            OpenEQTheme {
                MainScreen(
                    eqEnabled = myViewModel.eqEnabled,
                    eqToggle = {
                        // Handles starting/stopping the EQ foreground service
                        // If EQ not already enabled, enable it:
                        if (!myViewModel.eqEnabled) {
                            // Started depends on whether user allowed notifications (notification permission required)
                            val started = startMediaListenService()
                            myViewModel.eqEnabled = started
                        } else {
                            stopMediaListenService()
                            myViewModel.eqEnabled = false
                        }
                    },

                    tryGlobal = myViewModel.tryGlobalAudio,
                    setGlobal = {
                        // Toggles whether to attach EQ to the global audio mix (not supported on all devices)
                        if (myViewModel.globalAudioAllowed) {
                            myViewModel.tryGlobalAudio = it
                            eqService?.updateTryGlobalAudio(myViewModel.tryGlobalAudio)
                        } else {
                            // If device doesn't support global EQ, show an error message
                            Toast.makeText(
                                this,
                                getString(R.string.global_mix_error_toast_message),
                                Toast.LENGTH_LONG
                            )
                                .show()
                            myViewModel.tryGlobalAudio = false
                        }

                        // Saves app settings
                        appSettings.appSaveBoolean(
                            getString(R.string.shared_preferences_global_mix_key),
                            myViewModel.tryGlobalAudio
                        )
                    },

                    eqLevels = myViewModel.eqLevels,
                    updateEqLevel = { index: Int, value: Float ->
                        myViewModel.eqLevels[index] = value
                        // Passing updated EQ levels to the foreground service managing EQ objects
                        eqService?.updateEqLevels(myViewModel.eqLevels)
                    },

                    frequencyBands = myViewModel.eqFrequencyBandsStr,
                    eqRange = myViewModel.eqRange,

                    presetIds = myViewModel.presetIdStrings,
                    onPresetSelect = { presetId -> loadPreset(presetId) },
                    onPresetSave = { presetId -> newPreset(presetId) },
                    onPresetDelete = { presetId -> deletePreset(presetId) },
                    onPresetUpdate = { presetId -> updatePreset(presetId) }
                )
            }
        }
    }

    override fun onDestroy() {
        // Saves the latest EQ levels to the database and waits for the operation to complete
        runBlocking {
            appDb.updatePreset("latest_eq_levels", myViewModel.eqLevels)
        }

        // Unbinds from the foreground service if it's bound
        if (eqService != null) {
            unbindService(connection)
        }
        // Calls the onDestroy() of the parent class to properly destroy the activity
        super.onDestroy()
    }

    private fun appDataInit() {
        // Calls the function to initialize the database with stored app data
        appDatabaseInit()

        // Calls the function to initialize stored app settings
        myViewModel.tryGlobalAudio = appSettings.getAppSettingBoolean(
            getString(R.string.shared_preferences_global_mix_key),
            false
        )

        // Getting preset IDs from the database
        lifecycleScope.launch {
            val strings = appDb.getAllPresetIds()

            if (strings != null) {
                myViewModel.presetIdStrings.clear()
                myViewModel.presetIdStrings.addAll(strings)
            }
        }

        // Re-binds to the foreground service if it was left running upon last app destruction
        findMediaListenService()
    }

    private fun appDatabaseInit() {
        // Starts the app database to access stored preset info
        // WARNING: DO NOT START THE DATABASE AGAIN ANYWHERE ELSE IN THE APP
        appDb.buildDatabase(this)
        // Launching coroutine to get the EQ levels from previous app close and save them in the view model
        lifecycleScope.launch {
            val values = appDb.getPreset("latest_eq_levels")
            // Checking if a "latest_eq_levels" preset already exists, if so setting my EQ levels to it
            if (values != null) {
                myViewModel.eqLevels.clear()
                myViewModel.eqLevels.addAll(values)
            }
            // If not - need to create one
            else {
                appDb.addPreset("latest_eq_levels", myViewModel.eqLevels.toList())
            }
        }
    }

    private fun loadPreset(id: String) {
        // Launches a coroutine to load the user-selected preset into the EQ levels state
        lifecycleScope.launch {
            val presetVals = appDb.getPreset(id)

            if (presetVals != null) {
                myViewModel.eqLevels.clear()
                myViewModel.eqLevels.addAll(presetVals)

                // Sending updated EQ levels to the foreground service from the main thread
                eqService?.updateEqLevels(myViewModel.eqLevels)
            }
        }
    }

    private fun newPreset(id: String) {
        if (!validatePresetId(id)) {
            return
        }

        // Launches a coroutine to save the current EQ levels as a new preset in the database
        lifecycleScope.launch {
            appDb.addPreset(id, myViewModel.eqLevels)
            // Once preset added to database, adds the new preset ID to the list of preset ID string
            myViewModel.presetIdStrings += id
        }
    }

    private fun deletePreset(id: String) {
        if (!validatePresetId(id)) {
            return
        }

        // Launches coroutine to remove preset from database
        lifecycleScope.launch {
            appDb.deletePreset(id)
            // Once added to the database, can remove the preset ID string from the list
            myViewModel.presetIdStrings.remove(id)

            // Clearing all EQ levels
            for (i in 0..<myViewModel.eqLevels.size) {
                myViewModel.eqLevels[i] = 0f
            }

            // Sending updated EQ levels to the foreground service from the main thread
            eqService?.updateEqLevels(myViewModel.eqLevels)
        }
    }

    private fun updatePreset(id: String) {
        if (!validatePresetId(id)) {
            return
        }

        // Launches coroutine to update preset in database to current EQ levels
        lifecycleScope.launch {
            appDb.updatePreset(id, myViewModel.eqLevels)
        }
    }

    private fun validatePresetId(id: String): Boolean {
        // Checks given preset ID is valid
        if (id.isBlank() || !myViewModel.presetIdStrings.contains(id)) {
            return false
        }
        return true
    }

    // -----------------------------------------------

    private fun findMediaListenService() {
        // Checks to see if the foreground service is running; if so it re-binds to it
        if (EqForegroundService.isRunning) {
            // Binds to the service so new EQ levels can be passed in when the user sets them, updates app state
            bindService(foregroundServiceIntent, connection, BIND_AUTO_CREATE)
            myViewModel.eqEnabled = true
        }
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
        bindService(foregroundServiceIntent, connection, BIND_AUTO_CREATE)

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
        var notificationPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )

        // Requesting permission if not granted
        if (notificationPermission == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )

            notificationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        }

        // If permission granted, return true
        return notificationPermission == PackageManager.PERMISSION_GRANTED
    }
}
