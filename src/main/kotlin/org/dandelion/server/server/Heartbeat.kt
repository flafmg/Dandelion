package org.dandelion.server.server

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.*
import org.dandelion.server.entity.player.PlayerRegistry
import org.dandelion.server.events.HeartbeatSendEvent
import org.dandelion.server.events.manager.EventDispatcher
import org.dandelion.server.server.data.ServerConfig

object Heartbeat {
    private var sendLoopJob: Job? = null

    internal fun init() {
        Console.log("Starting heartbeat")
        startSendHeartbeatLoop()
        sendHeartbeat()
    }

    internal fun shutdown() {
        Console.log("Stopping heatbeat")
        try {
            sendLoopJob?.cancel()
        } catch (_: Exception) {}
    }

    private fun processHeartbeatPlaceholders(data: String): String {
        return data
            .replace(
                "{server-name}",
                URLEncoder.encode(ServerConfig.name, "UTF-8"),
            )
            .replace("{server-port}", ServerConfig.port.toString())
            .replace("{players-online}", PlayerRegistry.count().toString())
            .replace("{players-max}", ServerConfig.maxPlayers.toString())
            .replace("{server-public}", ServerConfig.isPublic.toString())
            .replace("{server-salt}", Salt.get())
            .replace(
                "{server-software}",
                URLEncoder.encode(ServerConfig.serverSoftware, "UTF-8"),
            )
    }

    private fun startSendHeartbeatLoop() {
        sendLoopJob =
            CoroutineScope(Dispatchers.Default).launch {
                while (isActive) {
                    delay(ServerConfig.heartbeatInterval.inWholeMilliseconds)
                    sendHeartbeat()
                }
            }
    }

    private fun sendHeartbeat() {
        var processedHeartbeatString =
            processHeartbeatPlaceholders(ServerConfig.heartbeatData)
        val event = HeartbeatSendEvent(processedHeartbeatString)
        EventDispatcher.dispatch(event)
        if (event.isCancelled) {
            return
        }
        processedHeartbeatString = event.heartbeat

        try {
            val processedData = processedHeartbeatString
            val finalUrl = "${ServerConfig.heartbeatUrl}?$processedData"
            val url = URL(finalUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            try {
                connection.inputStream.bufferedReader().use {
                    val response = it.readText()
                    if (response.startsWith("http")) {
                        Console.debugLog("Server URL: $response")
                    } else {
                        Console.debugLog("Heartbeat error: $response")
                    }
                }
            } finally {
                connection.disconnect()
                Console.debugLog("heartbeat sent to $finalUrl")
            }
        } catch (e: Exception) {
            Console.errLog("Error sending heartbeat: ${e.message}")
        }
    }
}
