package org.bupt.nerlci.dialogik.dsl


import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class MiraiMessage(val syncId: String, val data: JsonObject)
data class MiraiMessageElement(val type: String, val text: String)
data class MiraiContent(val session: String, val target: Long, val messageChain: Array<MiraiMessageElement>)
data class MiraiRespond(val syncId: String, val command: String, val subCommand: String?, val content: MiraiContent)
data class MiraiConfig(val host: String, val port: Int, val verifyKey: String, val qq: Long)

class MiraiWebsocketHandler(val miraiConfig: MiraiConfig) : RobotHandler {
    override var conf: RobotConfig = RobotConfig()
    var syncId = 0
    var session = ""

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

    suspend fun DefaultClientWebSocketSession.outputMessages() {
        try {
            val sessionMessage = receiveDeserialized<MiraiMessage>()
            val session = sessionMessage.data.get("session").asString
            while(true) {
                val message = receiveDeserialized<MiraiMessage>()
                val messageChain = message.data.get("messageChain").asJsonArray
                for (i in 0..<messageChain.size()) {
                    val msg = messageChain[i].asJsonObject
                    if (msg.get("type").asString != "Plain") { continue }

                    val text = msg.get("text").asString
                    val response = handleMessage(text)
                    val responseMessage =  MiraiRespond(
                        syncId = syncId.toString(),
                        command = "sendFriendMessage",
                        subCommand = null,
                        content = MiraiContent(
                            session = session,
                            target = message.data.get("sender").asJsonObject.get("id").asLong,
                            messageChain = arrayOf(MiraiMessageElement("Plain", response))
                        )
                    )
                    syncId += 1
                    sendSerialized(responseMessage)
                }
                val result = receiveDeserialized<MiraiMessage>()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error while receiving: " + e.localizedMessage)
        }
    }

    override fun start() {
        val host = miraiConfig.host
        val port = miraiConfig.port
        val verifyKey = miraiConfig.verifyKey
        val qq = miraiConfig.qq

        val client = HttpClient {
            install(WebSockets) {
                contentConverter = GsonWebsocketContentConverter()
            }
        }

        runBlocking {
            client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/message?verifyKey=$verifyKey&qq=$qq") {
                println("Connected to Mirai")
                val messageOutputRoutine = launch { outputMessages() }
                messageOutputRoutine.join()
            }
        }

        client.close()
    }
}
