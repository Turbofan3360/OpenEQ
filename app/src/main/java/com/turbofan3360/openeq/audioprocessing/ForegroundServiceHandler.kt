package com.turbofan3360.openeq.audioprocessing

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ForegroundServiceHandler(context: Context) {
    private val myContext = context
    private val foregroundServiceIntent: Intent by lazy { Intent(myContext, EqForegroundService::class.java) }

    private var latestEqLevels: MutableList<Float> = mutableListOf()
    private var latestGlobalAudio: Boolean = false

    // Class to bind to the foreground service
    private var eqService: EqForegroundService? = null
    private val connection = object : ServiceConnection {
        // FUNCTION EXECUTES ASYNCHRONOUSLY
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as EqForegroundService.LocalBinder
            eqService = binder.getService()

            // Passing required data to service
            eqService?.updateEqLevels(latestEqLevels)
            eqService?.updateTryGlobalAudio(latestGlobalAudio)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            eqService = null
        }
    }

    fun updateEqLevels(
        eqLevels: MutableList<Float>
    ) {
        latestEqLevels = eqLevels
        eqService?.updateEqLevels(latestEqLevels)
    }

    fun updateGlobalAudio(
        globalAudio: Boolean
    ) {
        latestGlobalAudio = globalAudio
        eqService?.updateTryGlobalAudio(latestGlobalAudio)
    }

    fun findMediaListenService(
        onEqEnabled: () -> Unit,
        eqLevels: MutableList<Float>,
        globalAudio: Boolean
    ) {
        // Checks to see if the foreground service is running; if so it re-binds to it
        if (EqForegroundService.isRunning) {
            latestEqLevels = eqLevels
            latestGlobalAudio = globalAudio

            // Binds to the service so new EQ levels can be passed in when the user sets them, updates app state
            myContext.bindService(foregroundServiceIntent, connection, BIND_AUTO_CREATE)
            onEqEnabled()
        }
    }

    fun startMediaListenService(
        activity: Activity,
        eqLevels: MutableList<Float>,
        globalAudio: Boolean
    ): Boolean {
        // Checking for and requesting notification permission if not already given
        val permissionGranted = checkNotificationPermission(activity)

        if (!permissionGranted) {
            return false
        }

        latestEqLevels = eqLevels
        latestGlobalAudio = globalAudio

        // Starting the foreground service that listens for media streams starting
        myContext.startForegroundService(foregroundServiceIntent)
        // Binds to the service so new EQ levels can be passed in when the user sets them
        myContext.bindService(foregroundServiceIntent, connection, BIND_AUTO_CREATE)

        return true
    }

    fun stopMediaListenService() {
        // Unbinds from foreground service
        myContext.unbindService(connection)
        // Stops the foreground service that listens for media streams starting
        myContext.stopService(foregroundServiceIntent)
    }

    fun unbindForegroundService() {
        // Unbinds from foreground service if it's been bound
        if (eqService != null) {
            myContext.unbindService(connection)
        }
    }

    private fun checkNotificationPermission(activity: Activity): Boolean {
        // Function to check whether notification permission is given, and request it if not
        // Below Android 13, is permission automatically granted for notifications
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        // Checking whether notifications are enabled
        var notificationPermission = ActivityCompat.checkSelfPermission(
            myContext,
            Manifest.permission.POST_NOTIFICATIONS
        )

        // Requesting permission if not granted
        if (notificationPermission == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )

            notificationPermission = ContextCompat.checkSelfPermission(
                myContext,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        // If permission granted, return true
        return notificationPermission == PackageManager.PERMISSION_GRANTED
    }
}
