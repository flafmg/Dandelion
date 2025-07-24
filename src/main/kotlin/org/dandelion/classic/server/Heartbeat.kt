package org.dandelion.classic.server

import kotlinx.coroutines.*
import org.dandelion.classic.events.HeartbeatSendEvent
import org.dandelion.classic.events.manager.EventDispatcher
import org.dandelion.classic.player.Players
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Heartbeat {
    private var sendLoopJob: Job? = null

    internal fun init(){
        Console.log("Starting heartbeat")
        startSendHeartbeatLoop()
        sendHeartbeat()

    }
    internal fun shutdown(){
        Console.log("Stopping heatbeat")
        try{ sendLoopJob?.cancel()} catch (_: Exception){}
    }
    private fun processHeartbeatPlaceholders(data: String): String{
        return data
            .replace("{server-name}", URLEncoder.encode(ServerInfo.name, "UTF-8"))
            .replace("{server-port}", ServerInfo.port.toString())
            .replace("{players-online}", Players.count().toString())
            .replace("{players-max}", ServerInfo.maxPlayers.toString())
            .replace("{server-public}", ServerInfo.isPublic.toString())
            .replace("{server-salt}", Salt.get())
            .replace("{server-software}", URLEncoder.encode(ServerInfo.serverSoftware, "UTF-8"))
    }

    private fun startSendHeartbeatLoop() {
        sendLoopJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(ServerInfo.heartbeatInterval.inWholeMilliseconds)
                sendHeartbeat()
            }
        }
    }
    private fun sendHeartbeat(){
        var processedHeartbeatString = processHeartbeatPlaceholders(ServerInfo.heartbeatData)
        val event = HeartbeatSendEvent(processedHeartbeatString)
        EventDispatcher.dispatch(event)
        if(event.isCancelled){
            return
        }
        processedHeartbeatString = event.heartbeat

        try {
            val processedData = processedHeartbeatString
            val finalUrl = "${ServerInfo.heartbeatUrl}?$processedData"
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
        } catch (e: Exception){
            Console.errLog("Error sending heartbeat: ${e.message}")
        }
    }

}