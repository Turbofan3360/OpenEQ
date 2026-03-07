package com.turbofan3360.openeq.audioprocessing

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission

import com.turbofan3360.openeq.MainActivity
import com.turbofan3360.openeq.R

private const val NOTIFICATION_ID = 1

// Foreground service that listens for media streams starting and then attaches equalizers to them
class EQMediaListenerService: Service() {
    // Initialises on first access to variable
    val notificationManager: NotificationManager by lazy{getSystemService(NOTIFICATION_SERVICE) as NotificationManager}
    val mediaStreamStartListener = MediaStreamStartReceiver()
    val mediaStreamStopListener = MediaStreamStopReceiver()
    var audioStreamIDs = mutableListOf<Int>()

    override fun onBind(intent: Intent): IBinder? {
        // Required method; not used in this service
        return null
    }

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

        return START_STICKY
    }

    override fun onDestroy() {
        // Tidies up everything when stopping the foreground service
        // Deletes notification
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        // Unregistering the broadcast receivers
        this.unregisterReceiver(mediaStreamStartListener)
        this.unregisterReceiver(mediaStreamStopListener)
    }

    private fun eqNotification() {
        if (checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }

        // Creating a notification channel to post my notification to
        createEqNotificationChannel(
            getString(R.string.notification_channel_name),
            getString(R.string.notification_channel_info),
        )

        // Creating the intent to happen when notification is tapped
        val tapIntent: PendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        // Creating the notification object for my foreground service notification
        val notification = NotificationCompat.Builder(this, NOTIFICATION_ID.toString())
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_info))
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // Shows notification on notification channel
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createEqNotificationChannel(
        channelName: String,
        channelDescription: String,
    ){
        // Checking if it already exists (if so, don't re-create it)
        val existingChannel = notificationManager.getNotificationChannel(NOTIFICATION_ID.toString())

        if (existingChannel == null) {
            // Creates a notification channel that notifications can then be posted to
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(
                NOTIFICATION_ID.toString(),
                channelName,
                importance
            ).apply {
                description = channelDescription
            }
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }
    }

    // ---------------------
    //  BROADCAST RECEIVERS
    // ---------------------

    inner class MediaStreamStartReceiver: BroadcastReceiver() {
        // Defining what happens when it detects a media stream starting
        override fun onReceive(context: Context?, intent: Intent?) {
            // Getting audio stream ID
            val mediaStreamID = intent?.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)

            // If the ID isn't null, adds the ID to a list tracking audio streams
            if(mediaStreamID != null) {
                this@EQMediaListenerService.audioStreamIDs += mediaStreamID
            }
        }
    }

    inner class MediaStreamStopReceiver: BroadcastReceiver() {
        // Defining what happens when it detects a media stream ending
        override fun onReceive(context: Context?, intent: Intent?) {
            // Getting audio stream ID
            val mediaStreamID = intent?.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)

            // If the ID isn't null, removes the ID from the list tracking audio streams
            if (mediaStreamID != null) {
                this@EQMediaListenerService.audioStreamIDs.remove(mediaStreamID)
            }
        }
    }
}