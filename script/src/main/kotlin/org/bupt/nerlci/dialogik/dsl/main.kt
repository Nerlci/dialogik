package org.bupt.nerlci.dialogik.dsl

class RobotInstance(val conf: RobotConfig) {
    fun start() {
        for (action in conf.welcomeActions) {
            val block = RobotActionBlock()
            block.action()
            println(block.response)
        }
        while (true) {
            val msg = readLine()
            if (msg == null) {
                for (action in conf.goodbyeActions) {
                    val block = RobotActionBlock()
                    block.action()
                    println(block.response)
                    break
                }
            }

            for (i in conf.receiveActions.keys) {
                if (msg!!.matches(i)) {
                    val message = RobotMessage(msg, i.matchEntire(msg)!!.groupValues)
                    val block = RobotActionBlock(message)
                    block.(conf.beforeAction)()
                    val action = conf.receiveActions[i]
                    action?.let { block.it() }
                    block.(conf.afterAction)()
                    println(block.response)
                    break
                }
            }
        }
    }
}

class RobotMessage(val message: String = "", val params: List<String> = listOf()) {

    fun getParam(index: Int): String = params[index]
}

class RobotActionBlock(val message: RobotMessage = RobotMessage()) {
    var response = ""

    fun send(msg: String) {
        response += msg
    }
}

class RobotConfig {
    val welcomeActions = mutableListOf<RobotActionBlock.() -> Unit>()
    var beforeAction: RobotActionBlock.() -> Unit = {}
    val receiveActions = mutableMapOf<Regex, RobotActionBlock.() -> Unit>()
    var afterAction: RobotActionBlock.() -> Unit = {}
    val goodbyeActions = mutableListOf<RobotActionBlock.() -> Unit>()

    fun welcome(block: RobotActionBlock.() -> Unit) {
        welcomeActions.add(block)
    }

    fun receive(msg: String? = null, block: RobotActionBlock.() -> Unit) {
        if (msg == null)
            receiveActions.put("(.*)".toRegex(), block)
        else
            receiveActions.put(msg.toRegex(), block);
    }

    fun before(block: RobotActionBlock.() -> Unit) {
        beforeAction = block;
    }
    fun after(block: RobotActionBlock.() -> Unit) {
        afterAction = block;
    }

    fun goodbye(block: RobotActionBlock.() -> Unit) {
        goodbyeActions.add(block)
    }
}

fun robot(vararg plugins: Plugin, block: RobotConfig.() -> Unit): RobotInstance {
    val conf = RobotConfig()
    conf.block()
    return RobotInstance(conf)
}