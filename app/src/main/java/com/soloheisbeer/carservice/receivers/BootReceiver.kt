package com.soloheisbeer.carservice.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soloheisbeer.carservice.services.RestartServiceWorker
import com.soloheisbeer.carservice.services.ShutdownService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, ShutdownService::class.java)
            context?.startService(serviceIntent)

            RestartServiceWorker.startWorker(context!!)
        }
    }
}