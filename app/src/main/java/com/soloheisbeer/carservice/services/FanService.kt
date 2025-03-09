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
import com.soloheisbeer.carservice.utilities.Pwm
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class FanService : Service() {

    companion object {
        const val CHANNEL_ID = "FAN_SERVICE_ID"
        const val CHANNEL_NAME = "Fan service"
        const val NOTIFICATION_ID = 84

        const val DELAY_DURATION_MS = 1000L

        const val MIN_TEMP = 40
        const val MAX_TEMP = 80
        const val MIN_FAN_SPEED = 2000
        const val MAX_FAN_SPEED = 10000
    }

    private val tag = FanService::class.java.simpleName
    private var job: Job? = null

    private lateinit var pwm1: Pwm

    override fun onCreate() {
        super.onCreate()
        startService(Intent(this, RootFileSystemService::class.java))
        createNotificationChannel()
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(!::pwm1.isInitialized)
            pwm1 = Pwm(1, this, MAX_FAN_SPEED)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(job == null) {
            job = CoroutineScope(Dispatchers.Default).launch {
                while (true) {
                    delay(DELAY_DURATION_MS)
                    val result = Shell.cmd("su -c cat /sys/class/thermal/thermal_zone0/temp").exec()
                    if (!result.isSuccess || result.out.size == 0)
                        continue

                    val temp = result.out.joinToString().toInt() / 1000
                    setFanSpeedForTemp(temp)
                    notificationManager.notify(
                        ShutdownService.NOTIFICATION_ID, createNotification(temp)
                    )
                }
            }
        }

        return START_STICKY
    }

    private fun startForeground() {
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                createNotification(0),
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
                e.message?.let { Log.e(tag, it) }
                Log.e(tag, "App not in a valid state to start foreground service")
            }
        }
    }

    private fun createNotification(temp: Int) : Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fan Foreground Service")
            .setContentText("Temp: $temp")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Channel for fan foreground service notification";
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun setFanSpeedForTemp(temp: Int) {
        if(!::pwm1.isInitialized)
            return
        val dutyCycle = ((MAX_FAN_SPEED - MIN_FAN_SPEED) * (temp - MIN_TEMP)) /
                (MAX_TEMP - MIN_TEMP) + MIN_FAN_SPEED
        pwm1.setDutyCycle(dutyCycle)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(::pwm1.isInitialized)
            pwm1.destruct()
        stopService(Intent(this, RootFileSystemService::class.java))
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}