package dev.anhcraft.vht11server

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*

val GSON = Gson()

fun main() {
    val client = HttpClient {
        install(WebSockets)
    }
    runBlocking {
        client.ws(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/") {
            val join = JsonObject()
            join.addProperty("nickname", "ThuThao")
            send(GSON.toJson(join))
            val report = JsonObject()
            report.addProperty("score", "1232004")
            send(GSON.toJson(report))
            while (true) {
                when (val frame = incoming.receive()) {
                    is Frame.Text -> println(frame.readText())
                }
            }
        }
    }
    client.close()
}
