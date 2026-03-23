package com.turbofan3360.openeq.audioprocessing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.turbofan3360.openeq.MainActivity
import com.turbofan3360.openeq.R

private const val PERMANENT_NOTIFICATION_ID = 1
private const val NOTIFICATION_CHANNEL_ID = "eq_service_channel"

// Foreground service that listens for media streams starting and then attaches equalizers to them
class EQMediaListenerService : Service() {
    // Initializes on first access to variable
    private val notificationManager: NotificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private val mediaStreamStartListener = MediaStreamStartReceiver()
    private val mediaStreamStopListener = MediaStreamStopReceiver()
    private var eqObjects = mutableMapOf<Int, Equalizer>()
    private var eqLevels = mutableListOf<Float>()
    private var tryGlobalMix = false
    private var binder = LocalBinder()

    // ---------------------------------
    // Handles binding to this service
    // ---------------------------------
    inner class LocalBinder : Binder() {
        fun getService() = this@EQMediaListenerService
    }

    override fun onBind(intent: Intent): IBinder {
        // Returns a binder object to interact with this service
        return binder
    }
    // ---------------------------------

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Called when starting the service
        // Calling the function that handles creating the notification
        eqNotification()

        // Registering the broadcast receiver to listen for media streams starting
        ContextCompat.registerReceiver(
            this,
            mediaStreamStartListener,
            IntentFilter("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION"),
            ContextCompat.RECEIVER_EXPORTED
        )
        // Registering the broadcast receiver to listen for media streams stopping
        ContextCompat.registerReceiver(
            this,
            mediaStreamStopListener,
            IntentFilter("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION"),
            ContextCompat.RECEIVER_EXPORTED
        )

        isRunning = true

        return START_STICKY
    }

    override fun onDestroy() {
        // Tidies up everything when stopping the foreground service
        // Deletes notification
        NotificationManagerCompat.from(this).cancel(PERMANENT_NOTIFICATION_ID)
        // Unregistering the broadcast receivers
        this.unregisterReceiver(mediaStreamStartListener)
        this.unregisterReceiver(mediaStreamStopListener)
        // Releasing all EQ objects
        for ((_, eqObj) in eqObjects) {
            delEqualizer(eqObj)
        }
        eqObjects.clear()

        isRunning = false
    }

    // Public function that lets you update the equalizer levels
    // Intended to be called from MainActivity when that is bound to this service
    fun updateEqLevels(
        newEqLevels: MutableList<Float>
    ) {
        eqLevels = newEqLevels

        // Setting all EQ instances to the new levels
        for ((_, eqObj) in eqObjects) {
            setEqualizer(eqObj, eqLevels)
        }
    }

    // Public function that lets you update whether or not the EQ tries to use the global mix
    fun setTryGlobal(value: Boolean) {
        tryGlobalMix = value

        // If the user has enabled global mix EQ, then create a global EQ instance and clear all the others
        if (tryGlobalMix) {
            val globalEq = addEqualizer(0)
            setEqualizer(globalEq, eqLevels)

            // Releasing all EQ objects
            for ((_, eqObj) in eqObjects) {
                delEqualizer(eqObj)
            }
            eqObjects.clear()

            // Adding global EQ
            eqObjects[0] = globalEq
        }

        // If global mix disabled AND global mix EQ object exists - release & remove the global EQ object
        else if (eqObjects.containsKey(0)) {
            delEqualizer(eqObjects[0]!!)
            eqObjects.remove(0)
        }
    }

    private fun eqNotification() {
        // Creating a notification channel to post my notification to
        createEqNotificationChannel(
            getString(R.string.notification_channel_name),
            getString(R.string.notification_channel_info),
        )

        // Creating the intent to happen when notification is tapped
        val tapIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // Creating the notification object for my foreground service notification
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_info))
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // Shows notification on notification channel
        startForeground(PERMANENT_NOTIFICATION_ID, notification)
    }

    private fun createEqNotificationChannel(
        channelName: String,
        channelDescription: String,
    ) {
        // Checking if it already exists (if so, don't re-create it)
        val existingChannel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)

        if (existingChannel == null) {
            // Creates a notification channel that notifications can then be posted to
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                importance
            ).apply {
                description = channelDescription
            }
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        var isRunning = false
    }

    // ---------------------
    //  BROADCAST RECEIVERS
    // ---------------------

    inner class MediaStreamStartReceiver : BroadcastReceiver() {
        // Defining what happens when it detects a media stream starting
        override fun onReceive(context: Context?, intent: Intent?) {
            // If user wants to attach to the global mix - ignore all this
            if (tryGlobalMix) {
                return
            }

            // Getting audio stream ID
            val mediaStreamID = intent?.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)

            // If the ID is valid, creates an equalizer object attached to that stream
            // Saves the equalizer object to the map
            // Then sets the equalizer levels on that EQ object to the current levels
            if (mediaStreamID != null && mediaStreamID != 0 && !eqObjects.containsKey(mediaStreamID)) {
                eqObjects[mediaStreamID] = addEqualizer(mediaStreamID)
                setEqualizer(eqObjects[mediaStreamID]!!, eqLevels)
            }
        }
    }

    inner class MediaStreamStopReceiver : BroadcastReceiver() {
        // Defining what happens when it detects a media stream ending
        override fun onReceive(context: Context?, intent: Intent?) {
            // If user wants to attach to the global mix - ignore all this
            if (tryGlobalMix) {
                return
            }

            // Getting audio stream ID
            val mediaStreamID = intent?.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)

            // If the ID is valid:
            // Gets the EQ object attached to the given media stream, closes the EQ, and removes it from the map
            if (mediaStreamID != null && mediaStreamID != 0) {
                val eqObj = eqObjects[mediaStreamID]

                if (eqObj != null) {
                    delEqualizer(eqObj)
                    eqObjects.remove(mediaStreamID)
                }
            }
        }
    }
}
