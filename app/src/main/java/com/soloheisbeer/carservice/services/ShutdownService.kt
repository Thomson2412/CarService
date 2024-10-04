package com.soloheisbeer.carservice.services

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.soloheisbeer.carservice.R
import com.soloheisbeer.carservice.utilities.Gpio
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class ShutdownService : Service() {

    companion object {
        const val CHANNEL_ID = "SHUTDOWN_SERVICE_ID"
        const val CHANNEL_NAME = "Shutdown service"
        const val NOTIFICATION_ID = 24

        const val SHUTDOWN_IGNITION_OFF_SEC = 15
    }

    private val tag = ShutdownService::class.java.simpleName
    private var job: Job? = null
    private var ignitionOffCounter = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        job = CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(1000)
                if (isIgnitionOn()) {
                    ignitionOffCounter = 0
                    continue
                }
                if(ignitionOffCounter >= SHUTDOWN_IGNITION_OFF_SEC) {
                    shutdown()
                }
                ignitionOffCounter++
                notificationManager.notify(NOTIFICATION_ID, createNotification())
            }
        }
        return START_STICKY
    }

    private fun startForeground() {
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                createNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                },
            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                e.message?.let { Log.d(tag, it) }
                Log.d(tag, "App not in a valid state to start foreground service")
            }
        }
    }

    private fun createNotification() : Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Shutdown Foreground Service")
            .setContentText("Ignition on: ${isIgnitionOn()}")
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Channel for shutdown foreground service notification";
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun isIgnitionOn(): Boolean {
        return Gpio.getGpio(22) == 0
    }

    private fun shutdown() {
        Shell.cmd("su -c reboot -p").exec()
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}