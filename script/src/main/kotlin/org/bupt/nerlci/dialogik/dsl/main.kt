package org.bupt.nerlci.dialogik.dsl

class RobotInstance(val conf: RobotConfig, val handler: RobotHandler) {
    fun start() {
        handler.conf = conf
        handler.start()
    }
}

data class RobotMessage(val text: String = "", val params: List<String> = listOf()) {

    fun getParam(index: Int): String = params[index]
}

class RobotActionBlock(val message: RobotMessage = RobotMessage()) {
    var response = ""

    fun send(msg: String) {
        response += msg
    }
}

class RobotConfig {
    private val welcomeActions = mutableListOf<RobotActionBlock.() -> Unit>()
    private var beforeAction: RobotActionBlock.() -> Unit = {}
    private val receiveActions = mutableMapOf<Regex, RobotActionBlock.() -> Unit>()
    private var afterAction: RobotActionBlock.() -> Unit = {}
    private val goodbyeActions = mutableListOf<RobotActionBlock.() -> Unit>()

    fun welcome(block: RobotActionBlock.() -> Unit) {
        welcomeActions.add(block)
    }

    fun receive(msg: String = ".*", block: RobotActionBlock.() -> Unit) {
        receiveActions.put(msg.toRegex(), block)
    }

    fun before(block: RobotActionBlock.() -> Unit) {
        beforeAction = block
    }
    fun after(block: RobotActionBlock.() -> Unit) {
        afterAction = block
    }

    fun goodbye(block: RobotActionBlock.() -> Unit) {
        goodbyeActions.add(block)
    }

    fun getWelcomeActions(): RobotActionBlock.() -> Unit {
        return {
            for (action in welcomeActions) {
                action()
            }
        }
    }

    fun getGoodbyeActions(): RobotActionBlock.() -> Unit {
        return {
            for (action in goodbyeActions) {
                action()
            }
        }
    }

    fun getReceiveAction(msg: String): RobotActionBlock.() -> Unit {
        for (i in receiveActions.keys) {
            if (msg.matches(i)) {
                return receiveActions[i]!!
            }
        }
        return {}
    }

    fun getRobotMessage(msg: String): RobotMessage {
        for (i in receiveActions.keys) {
            if (msg.matches(i)) {
                val params = i.matchEntire(msg)!!.groupValues // TODO: 修改正则表达式匹配，使其更加合适
                return RobotMessage(msg, params)
            }
        }
        return RobotMessage(msg)
    }

    fun getBeforeAction(): RobotActionBlock.() -> Unit {
        return beforeAction
    }

    fun getAfterAction(): RobotActionBlock.() -> Unit {
        return afterAction
    }
}

fun robot(handler: RobotHandler = ConsoleHandler(), block: RobotConfig.() -> Unit): RobotInstance {
    val conf = RobotConfig()
    conf.block()
    return RobotInstance(conf, handler)
}

fun robot(handler: RobotHandler = ConsoleHandler(), conf: RobotConfig): RobotInstance {
    return RobotInstance(conf, handler)
}