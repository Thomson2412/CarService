package com.soloheisbeer.carservice.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soloheisbeer.carservice.services.BrightnessService
import com.soloheisbeer.carservice.services.FanService
import com.soloheisbeer.carservice.services.RestartServiceWorker
import com.soloheisbeer.carservice.services.ShutdownService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.startService(Intent(context, ShutdownService::class.java))
            context?.startService(Intent(context, FanService::class.java))
            context?.startService(Intent(context, BrightnessService::class.java))

            RestartServiceWorker.startWorker(context!!)
        }
    }
}