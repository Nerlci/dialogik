package org.bupt.nerlci.dialogik.dsl

class ConsoleHandler() : RobotHandler {
    override var conf: RobotConfig = RobotConfig()
    override fun start() {
        val welcomeActions = conf.getWelcomeActions()
        val block = RobotActionBlock()
        block.welcomeActions()
        println(block.response)

        while (true) {
            val msg = readLine()
            if (msg == null) {
                val goodbyeActions = conf.getGoodbyeActions()
                val block = RobotActionBlock()
                block.goodbyeActions()
                println(block.response)
                break
            }

            val beforeAction = conf.getBeforeAction()
            val afterAction = conf.getAfterAction()
            val action = conf.getReceiveAction(msg)
            val message = conf.getRobotMessage(msg)

            val block = RobotActionBlock(message)
            block.beforeAction()
            block.action()
            block.afterAction()
            println(block.response)
            break
        }
    }
}