package com.soloheisbeer.carservice

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.soloheisbeer.carservice.databinding.ActivityMainBinding
import com.soloheisbeer.carservice.services.RestartServiceWorker
import com.soloheisbeer.carservice.services.ShutdownService
import com.topjohnwu.superuser.Shell


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shell.enableVerboseLogging = true
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10)
        )

        val result = Shell.cmd("su").exec()

        if (!result.isSuccess) {
            Toast.makeText(this, "Need root access", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val intent = Intent(this, ShutdownService::class.java)
        startService(intent)

        RestartServiceWorker.startWorker(this)
    }
}