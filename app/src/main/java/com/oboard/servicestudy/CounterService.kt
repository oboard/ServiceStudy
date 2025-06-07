package com.oboard.servicestudy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CounterService : Service() {
    private var counter = 0
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val CHANNEL_ID = "CounterServiceChannel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.oboard.servicestudy.START"
        const val ACTION_STOP = "com.oboard.servicestudy.STOP"
        const val ACTION_UPDATE_COUNTER = "com.oboard.servicestudy.UPDATE_COUNTER"
        const val EXTRA_COUNTER = "counter"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundService()
            ACTION_STOP -> stopForegroundService()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        serviceJob = serviceScope.launch {
            while (true) {
                counter++
                updateNotification()
                broadcastCounterUpdate()
                delay(1000) // Update every second
            }
        }
    }

    private fun stopForegroundService() {
        serviceJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Counter Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the current counter value"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Counter Service")
            .setContentText("Counter: $counter")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun broadcastCounterUpdate() {
        val intent = Intent(ACTION_UPDATE_COUNTER).apply {
            putExtra(EXTRA_COUNTER, counter)
        }
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
    }
} 