package com.soloheisbeer.carservice.utilities

import com.topjohnwu.superuser.Shell

object Gpio {

    fun setGpio(pin: Int, value: Int) {
        Shell.cmd("su -c gpioset gpiochip0 $pin=$value").exec()
    }

    fun getGpio(pin: Int): Int {
        val result = Shell.cmd("su -c gpioget gpiochip0 $pin").exec()
        if (result.isSuccess && result.out.size > 0) {
            return result.out.joinToString().toInt()
        }
        error("Failed to get gpio value")
    }
}