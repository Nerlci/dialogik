package org.bupt.nerlci.dialogik.dsl

interface RobotHandler {
    var conf: RobotConfig
    fun start()
}