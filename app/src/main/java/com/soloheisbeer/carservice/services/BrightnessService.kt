package com.soloheisbeer.carservice.services

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.soloheisbeer.carservice.R
import com.soloheisbeer.carservice.utilities.Pwm


class BrightnessService : Service() {

    companion object {
        const val CHANNEL_ID = "BRIGHTNESS_SERVICE_ID"
        const val CHANNEL_NAME = "Brightness service"
        const val NOTIFICATION_ID = 42

        const val MIN_BRIGHTNESS = 1
        const val MAX_BRIGHTNESS = 255
        const val DEFAULT_BRIGHTNESS = MAX_BRIGHTNESS / 2
        const val MIN_DUTY_CYCLE = 100000
        const val MAX_DUTY_CYCLE = 1000000
    }

    private val tag = BrightnessService::class.java.simpleName
    private val contentObserver = object: ContentObserver(Handler(Looper.getMainLooper()))
    {
        override fun onChange(selfChange: Boolean)
        {
            val brightness = Settings.System.getInt(
                contentResolver, Settings.System.SCREEN_BRIGHTNESS, DEFAULT_BRIGHTNESS)
            setDisplayBrightness(brightness)
        }
    }

    private lateinit var pwm0: Pwm

    override fun onCreate() {
        super.onCreate()
        startService(Intent(this, RootFileSystemService::class.java))
        createNotificationChannel()
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(!::pwm0.isInitialized)
            pwm0 = Pwm(0, this, period = MAX_DUTY_CYCLE, polarityInversed = true)
        val brightness = Settings.System.getInt(
            contentResolver, Settings.System.SCREEN_BRIGHTNESS,DEFAULT_BRIGHTNESS)
        setDisplayBrightness(brightness)

        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            false, contentObserver)

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
                e.message?.let { Log.e(tag, it) }
                Log.e(tag, "App not in a valid state to start foreground service")
            }
        }
    }

    private fun createNotification() : Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Brightness Foreground Service")
            .setContentText("Monitor system brightness")
            .setSmallIcon(R.drawable.ic_dashboard_black_24dp)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Channel for brightness foreground service notification";
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun setDisplayBrightness(brightness: Int) {
        if(!::pwm0.isInitialized)
            return
        val dutyCycle = ((MAX_DUTY_CYCLE - MIN_DUTY_CYCLE) * (brightness - MIN_BRIGHTNESS)) /
                (MAX_BRIGHTNESS - MIN_BRIGHTNESS) + MIN_DUTY_CYCLE
        pwm0.setDutyCycle(dutyCycle)
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(contentObserver)
        if(::pwm0.isInitialized)
            pwm0.destruct()
        stopService(Intent(this, RootFileSystemService::class.java))
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}