package dev.anhcraft.vht11server

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.SendChannel
import java.time.Duration

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

val GSON = Gson()
val scores: HashMap<String, Int> = HashMap()
val clients: HashMap<String, SendChannel<Frame>> = HashMap()

fun Application.module() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/") {
            var nickname: String? = null
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val req = GSON.fromJson(frame.readText(), JsonObject::class.java)
                        if (nickname == null) {
                            nickname = req.getAsJsonPrimitive("nickname").asString
                            if (scores.containsKey(nickname)) {
                                println("Someone is trying to play as $nickname who has already joined!")
                                this.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "playerExisted"))
                                break
                            } else {
                                //
                                val out = JsonObject()
                                out.addProperty("type", "bulkUpdate")
                                val arr = JsonArray()
                                scores.map { e ->
                                    val item = JsonObject()
                                    item.addProperty("player", e.key)
                                    item.addProperty("score", e.value)
                                    item
                                }.forEach { element: JsonObject? -> arr.add(element) }
                                out.add("data", arr)
                                outgoing.send(Frame.Text(GSON.toJson(out)))
                                //
                                scores[nickname] = 0
                                clients[nickname] = outgoing
                                println("$nickname has just joined!")
                            }
                        } else {
                            val score = req.getAsJsonPrimitive("score").asInt
                            scores[nickname] = score
                            println("$nickname has reported a new score: $score")
                            //
                            val out = JsonObject()
                            out.addProperty("type", "update")
                            out.addProperty("player", nickname)
                            out.addProperty("score", score)
                            val outStr = GSON.toJson(out)
                            clients.entries.filter { it.key != nickname }.forEach { it.value.send(Frame.Text(outStr)) }
                        }
                    }
                }
            }

            println("$nickname has quited")
            scores.remove(nickname)
            clients.remove(nickname)
            //
            val out = JsonObject()
            out.addProperty("type", "quit")
            out.addProperty("player", nickname)
            val outStr = GSON.toJson(out)
            clients.values.forEach { it.send(Frame.Text(outStr)) }
        }
    }
}
