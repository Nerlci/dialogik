package org.bupt.nerlci.dialogik.dsl


import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class MiraiMessage(val syncId: String, val data: JsonObject)
data class MiraiMessageElement(val type: String, val text: String)
data class MiraiContent(val session: String, val target: Long, val messageChain: Array<MiraiMessageElement>)
data class MiraiRespond(val syncId: String, val command: String, val subCommand: String?, val content: MiraiContent)

class MiraiWebsocketHandler() : RobotHandler {
    override var conf: RobotConfig = RobotConfig()
    var syncId = 0

    suspend fun handleMessage(msg: String): String {
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
                println(message)
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
                    println(responseMessage)
                    sendSerialized(responseMessage)
                }
                val result = receiveDeserialized<MiraiMessage>()
                println(result)
            }
        } catch (e: Exception) {
            println("Error while receiving: " + e.localizedMessage)
        }
    }

    override fun start() {
        val host = "localhost"
        val port = 8080
        val verifyKey = "1234567890"
        val qq = 0

        val client = HttpClient {
            install(WebSockets) {
                contentConverter = GsonWebsocketContentConverter()
            }
        }

        runBlocking {
            client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/message?verifyKey=$verifyKey&qq=$qq") {
                println("Connected to server")
                val messageOutputRoutine = launch { outputMessages() }
                messageOutputRoutine.join()
            }
        }

        client.close()
    }

}
