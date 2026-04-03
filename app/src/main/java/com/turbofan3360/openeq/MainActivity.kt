package com.turbofan3360.openeq

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import com.turbofan3360.openeq.appdata.RoomDatabaseHandler
import com.turbofan3360.openeq.appdata.SharedPreferencesSettings
import com.turbofan3360.openeq.audioprocessing.ForegroundServiceHandler
import com.turbofan3360.openeq.audioprocessing.eqFrequenciesToLabels
import com.turbofan3360.openeq.audioprocessing.getEqBands
import com.turbofan3360.openeq.audioprocessing.getEqRange
import com.turbofan3360.openeq.audioprocessing.globalEqAllowed
import com.turbofan3360.openeq.ui.screens.MainScreen
import com.turbofan3360.openeq.ui.theme.OpenEQTheme
import kotlinx.coroutines.runBlocking

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    val context = getApplication<Application>()

    // State - whether EQ service is enabled or not
    var eqEnabled by mutableStateOf(false)

    // Whether the device supports global audio EQ
    val globalAudioAllowed = globalEqAllowed()

    // Whether to try and attach the EQ to the global audio mix
    var tryGlobalAudio by mutableStateOf(false)

    // EQ frequency bands in milliHz
    val eqFrequencyBands = getEqBands(context)

    // String labels for EQ frequency bands
    val eqFrequencyBandsStr = eqFrequenciesToLabels(eqFrequencyBands)

    // Supported range of EQ bands (in dB)
    val eqRange = getEqRange(context)

    // State of the sliders (and so EQ levels)
    var eqLevels = List(eqFrequencyBands.size) { 0f }.toMutableStateList()
}

class MainActivity : ComponentActivity() {
    val myViewModel: MainActivityViewModel by viewModels()

    val appSettings by lazy { SharedPreferencesSettings(this, "app_settings") }
    private val foregroundServiceHandler by lazy { ForegroundServiceHandler(this) }

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
                    eqToggle = ::toggleEq,

                    tryGlobal = myViewModel.tryGlobalAudio,
                    toggleGlobal = ::toggleGlobalAudio,

                    eqLevels = myViewModel.eqLevels,
                    // Saving EQ levels + passing them to the foreground service managing EQ objects
                    updateEqLevel = { index: Int, value: Float ->
                        myViewModel.eqLevels[index] = value
                        foregroundServiceHandler.updateEqLevels(myViewModel.eqLevels)
                    },

                    frequencyBands = myViewModel.eqFrequencyBandsStr,
                    eqRange = myViewModel.eqRange,

                    onPresetUpdate = { presetId ->
                        RoomDatabaseHandler.updatePreset(presetId, myViewModel.eqLevels, lifecycleScope)
                    },
                    onPresetSave = { presetId ->
                        RoomDatabaseHandler.addPreset(presetId, myViewModel.eqLevels, lifecycleScope)
                    },
                    onPresetSelect = { presetId ->
                        RoomDatabaseHandler.getPreset(
                            presetId,
                            lifecycleScope
                        ) { presetVals ->
                            // Updating the EQ levels to the retrieved values
                            myViewModel.eqLevels.clear()
                            myViewModel.eqLevels.addAll(presetVals)
                        }
                    },
                    onPresetDelete = { presetId ->
                        RoomDatabaseHandler.deletePreset(
                            presetId,
                            lifecycleScope
                        ) {
                            // Clearing the EQ levels
                            for (i in 0..<myViewModel.eqLevels.size) {
                                myViewModel.eqLevels[i] = 0f
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        // Saves the latest EQ levels to the database (blocking to ensure completion)
        val retJob = RoomDatabaseHandler.updatePreset(
            getString(R.string.db_key_recent_eq_levels),
            myViewModel.eqLevels,
            lifecycleScope
        )

        // Waiting for preset update job to complete
        runBlocking {
            retJob?.join()
        }

        RoomDatabaseHandler.dbInitialized = false

        // Unbinds from the foreground service if it's bound
        foregroundServiceHandler.unbindForegroundService()
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

        // Re-binds to the foreground service if it was left running upon last app destruction
        foregroundServiceHandler.findMediaListenService(
            { myViewModel.eqEnabled = true },
            myViewModel.eqLevels,
            myViewModel.tryGlobalAudio
        )
    }

    private fun appDatabaseInit() {
        // Starts the app database to access stored preset info
        RoomDatabaseHandler.buildDatabase("preset-database", this)

        // Checking if a "latest_eq_levels" preset already exists; if so setting my EQ levels to it
        if (RoomDatabaseHandler.idStrings.contains(getString(R.string.db_key_recent_eq_levels))) {
            RoomDatabaseHandler.getPreset(
                getString(R.string.db_key_recent_eq_levels),
                lifecycleScope
            ) { values ->
                myViewModel.eqLevels.clear()
                myViewModel.eqLevels.addAll(values)
            }
        } else {
            // Otherwise, one needs to be created
            RoomDatabaseHandler.addPreset(
                getString(R.string.db_key_recent_eq_levels),
                myViewModel.eqLevels,
                lifecycleScope
            )
        }
    }

    private fun toggleEq() {
        // Handles starting/stopping the EQ foreground service
        // If EQ not already enabled, enable it:
        if (!myViewModel.eqEnabled) {
            // Started depends on whether user allowed notifications (notification permission required)
            val started = foregroundServiceHandler.startMediaListenService(
                this,
                myViewModel.eqLevels,
                myViewModel.tryGlobalAudio
            )

            myViewModel.eqEnabled = started
        } else {
            foregroundServiceHandler.stopMediaListenService()
            myViewModel.eqEnabled = false
        }
    }

    private fun toggleGlobalAudio() {
        // Toggles whether to attach EQ to the global audio mix (not supported on all devices)
        if (myViewModel.globalAudioAllowed) {
            myViewModel.tryGlobalAudio = !myViewModel.tryGlobalAudio

            foregroundServiceHandler.updateGlobalAudio(myViewModel.tryGlobalAudio)
        } else {
            // If device doesn't support global EQ, show an error message
            Toast.makeText(
                this,
                getString(R.string.attach_global_mix_error_message),
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
    }
}
