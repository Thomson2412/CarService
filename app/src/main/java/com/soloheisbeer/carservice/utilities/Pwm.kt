package com.soloheisbeer.carservice.utilities
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.soloheisbeer.carservice.services.RootFileSystemService
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager

class Pwm(
    private val pwm: Int,
    context: Context,
    private val period: Int = 1000000,
    private val initialDutyCycle: Int = period / 2,
    private val polarityInversed: Boolean = false
) {

    private val tag = Pwm::class.java.simpleName
    private var rootFileSystemServiceBinding: IBinder? = null

    private val rootFileSystemServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            rootFileSystemServiceBinding = binder
            exportPWM()
            initPWM()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            rootFileSystemServiceBinding = null
        }
    }

    private lateinit var pwmChipDir: ExtendedFile
    private lateinit var pwmDir: ExtendedFile

    init {
        RootService.bind(
            Intent(
                context,
                RootFileSystemService::class.java
            ),
            rootFileSystemServiceConnection
        )
    }

    fun destruct() {
        RootService.unbind(rootFileSystemServiceConnection)
    }

    private fun exportPWM() {
        pwmChipDir = FileSystemManager.getRemote(rootFileSystemServiceBinding!!)
            .getFile("/sys/class/pwm/pwmchip0/")
        if(!pwmChipDir.exists()) {
            return
        }
        with(pwmChipDir.getChildFile("export").newOutputStream().writer()) {
            write("$pwm")
            close()
        }
    }

    private fun initPWM() {
        pwmDir = pwmChipDir.getChildFile("pwm$pwm/")
        if(!pwmDir.exists() && !pwmDir.isDirectory) {
            error("Failed to init PWM$pwm")
        }

        with(pwmDir.getChildFile("period").newOutputStream().writer()) {
            write("$period")
            close()
        }

        with(pwmDir.getChildFile("duty_cycle").newOutputStream().writer()) {
            write("$initialDutyCycle")
            close()
        }

        if(polarityInversed) {
            with(pwmDir.getChildFile("polarity").newOutputStream().writer()) {
                write("inversed")
                close()
            }
        }

        with(pwmDir.getChildFile("enable").newOutputStream().writer()) {
            write("1")
            close()
        }
    }

    fun setDutyCycle(dutyCycle: Int) {
        if(rootFileSystemServiceBinding == null || !::pwmDir.isInitialized || !pwmDir.exists())
            return
        if(dutyCycle > period)
            return

        with(pwmDir.getChildFile("duty_cycle").newOutputStream().writer()) {
            write("$dutyCycle")
            close()
        }
    }
}