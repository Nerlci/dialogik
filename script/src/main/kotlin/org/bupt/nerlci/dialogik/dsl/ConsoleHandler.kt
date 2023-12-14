package org.bupt.nerlci.dialogik.dsl

class ConsoleHandler() : RobotHandler {
    override var conf: RobotConfig = RobotConfig()

    fun handleMessage(msg: String): String {
        val beforeAction = conf.getBeforeAction()
        val afterAction = conf.getAfterAction()
        val action = conf.getReceiveAction(msg)
        val robotMessage = conf.getRobotMessage(msg)

        val block = RobotActionBlock(robotMessage)
        block.beforeAction()
        block.action()
        block.afterAction()
        return block.response
    }

    override fun start() {
        val welcomeActions = conf.getWelcomeActions()
        val block = RobotActionBlock()
        block.welcomeActions()
        println(block.response)

        while (true) {
            val msg = readlnOrNull()
            if (msg == null) {
                val goodbyeActions = conf.getGoodbyeActions()
                val block = RobotActionBlock()
                block.goodbyeActions()
                println(block.response)
                break
            }

            val response = handleMessage(msg)
            println(response)
        }
    }
}