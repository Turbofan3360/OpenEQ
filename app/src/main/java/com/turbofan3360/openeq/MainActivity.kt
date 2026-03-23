package com.turbofan3360.openeq

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.turbofan3360.openeq.appdata.DatabaseHandler
import com.turbofan3360.openeq.audioprocessing.EQMediaListenerService
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
    var eqLevels = mutableStateListOf(*MutableList(eqFrequencyBands.size) { 0f }.toTypedArray())

    // List of preset ID strings
    var presetIdStrings = mutableStateListOf<String>()
}

class MainActivity : ComponentActivity() {
    val myViewModel: MainActivityViewModel by viewModels()

    val sharedPref: SharedPreferences by lazy { getPreferences(MODE_PRIVATE) }
    private val appDb by lazy { DatabaseHandler() }

    private val foregroundServiceIntent: Intent by lazy { Intent(this, EQMediaListenerService::class.java) }

    // Class to bind to the foreground service
    private var eqService: EQMediaListenerService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as EQMediaListenerService.LocalBinder
            eqService = binder.getService()

            // Calls the service to pass the needed data
            eqService?.updateEqLevels(myViewModel.eqLevels)
            eqService?.setTryGlobal(myViewModel.tryGlobalAudio)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            eqService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Calls the function to initialize the database with stored app data
        appDataInit()

        // Calls the function to initialize stored app settings
        appSettingsInit()

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

        // Handles starting the app UI
        setContent {
            OpenEQTheme {
                MainScreen(
                    eqEnabled = myViewModel.eqEnabled,
                    eqToggle = {
                        // Handles starting/stopping the foreground service to handle EQ
                        // If EQ not already enabled, then pressing the button means the user wants to enable it
                        if (!myViewModel.eqEnabled) {
                            // Started depends on whether or not the user allowed notifications
                            // No notifications -> foreground service can't run
                            val started = startMediaListenService()
                            // If service started, the eqEnabled = true, and vice versa
                            myViewModel.eqEnabled = started
                        } else {
                            stopMediaListenService()
                            myViewModel.eqEnabled = false
                        }
                    },

                    tryGlobal = myViewModel.tryGlobalAudio,
                    setGlobal = {
                        if (myViewModel.globalAudioAllowed) {
                            // Tries to attach to the global audio mix in the foreground service
                            myViewModel.tryGlobalAudio = it
                            eqService?.setTryGlobal(myViewModel.tryGlobalAudio)
                        } else {
                            // If device doesn't support global EQ (it's technically deprecated)
                            // Showing error message:
                            Toast.makeText(
                                this,
                                getString(R.string.global_mix_error_toast_message),
                                Toast.LENGTH_LONG
                            )
                                .show()
                            myViewModel.tryGlobalAudio = false
                        }

                        // Saves app settings
                        appSettingsSave()
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

    private fun appSettingsInit() {
        // Grabs app settings from shared preferences (much simpler than the more modern data store)
        // Currently only setting stored this way is whether attached to global audio mix or not

        val globalAudioEnabled = sharedPref.getBoolean(getString(R.string.shared_preferences_global_mix_key), false)
        myViewModel.tryGlobalAudio = globalAudioEnabled
    }

    private fun appSettingsSave() {
        // Saves app settings to the shared preferences
        sharedPref.edit {
            putBoolean(getString(R.string.shared_preferences_global_mix_key), myViewModel.tryGlobalAudio)
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
        // Checking for no user input
        if (id.isBlank() || myViewModel.presetIdStrings.contains(id)) {
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
        // Checking string ID is valid
        if (id.isBlank() || !myViewModel.presetIdStrings.contains(id)) {
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
        // Checking string ID is valid
        if (id.isBlank() || !myViewModel.presetIdStrings.contains(id)) {
            return
        }

        // Launches coroutine to update preset in database to current EQ levels
        lifecycleScope.launch {
            appDb.updatePreset(id, myViewModel.eqLevels)
        }
    }

    // -----------------------------------------------

    private fun findMediaListenService() {
        // Checks to see if the foreground service is running; if so it re-binds to it
        if (EQMediaListenerService.isRunning) {
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

    // -----------------------------------------------

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
