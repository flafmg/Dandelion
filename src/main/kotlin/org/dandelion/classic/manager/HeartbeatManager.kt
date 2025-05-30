package org.dandelion.classic.manager

import org.dandelion.classic.data.player.manager.PlayerManager
import org.dandelion.classic.util.Logger
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.dandelion.classic.data.config.model.ServerConfig

class HeartbeatManager(private val config: ServerConfig, private val getSalt: () -> String, private val getServerSoftware: () -> String) {
    private val isRunning = AtomicBoolean(false)
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    fun start() {
        if (isRunning.getAndSet(true)) return
        Logger.log("HeartbeatManager started with interval: ${config.get().heartbeatSettings.interval}s")
        scheduler.scheduleAtFixedRate(::heartbeatLoop, 0, config.get().heartbeatSettings.interval.toLong(), TimeUnit.SECONDS)
    }

    private fun heartbeatLoop() {
        if (!isRunning.get()) return
        try {
            sendHeartbeat()
            Logger.debugLog("Heartbeat sent.")
        } catch (e: Exception) {
            Logger.errLog("Error sending heartbeat: ${e.message}")
        }
    }

    private fun sendHeartbeat() {
        val params = mapOf(
            "port" to config.get().serverSettings.port.toString(),
            "max" to config.get().serverSettings.maxPlayers.toString(),
            "name" to URLEncoder.encode(config.get().serverSettings.name, "UTF-8"),
            "public" to config.get().serverSettings.public.toString().replaceFirstChar { it.uppercase() },
            "version" to "7",
            "salt" to getSalt(),
            "users" to PlayerManager.getOnlinePlayerCount(),
            "software" to getServerSoftware(),
        )
        val baseUrl = config.get().heartbeatSettings.url
        val queryString = params.entries.joinToString("&") { (key, value) -> "$key=$value" }
        val urlString = "$baseUrl?$queryString"
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        try { connection.inputStream.bufferedReader().use { it.readText() } } finally { connection.disconnect() }
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        scheduler.shutdownNow()
        try {
            scheduler.awaitTermination(2, TimeUnit.SECONDS)
            Logger.log("HeartbeatManager stopped.")
        } catch (_: InterruptedException) {
            Logger.warnLog("HeartbeatManager termination interrupted.")
        }
    }
}
