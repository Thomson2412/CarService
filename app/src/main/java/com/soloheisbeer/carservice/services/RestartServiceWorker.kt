package com.soloheisbeer.carservice.services

import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class RestartServiceWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object{
        private val tag = RestartServiceWorker::class.java.simpleName

        fun startWorker(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val workRequest =
                PeriodicWorkRequestBuilder<RestartServiceWorker>(15, TimeUnit.MINUTES)
                    .build()
            workManager.enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.UPDATE, workRequest)
        }
    }

    override fun doWork(): Result {
        val serviceIntent = Intent(applicationContext, ShutdownService::class.java)
        applicationContext.startService(serviceIntent)
        return Result.success()
    }
}