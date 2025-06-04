package org.dandelion.classic

import org.dandelion.classic.data.config.manager.ServerConfigManager
import org.dandelion.classic.data.config.model.ServerConfig
import org.dandelion.classic.data.player.manager.PlayerManager
import org.dandelion.classic.manager.ConnectionManager
import org.dandelion.classic.manager.HeartbeatManager
import org.dandelion.classic.manager.KeyManager
import org.dandelion.classic.packets.manager.PacketManager
import org.dandelion.classic.data.level.io.impl.DandelionLevelSerializer
import org.dandelion.classic.data.level.manager.LevelManager
import java.io.File

object Server {
    private var isRunning = false
    private val protocolVersion: Byte = 0x07
    private val serverSoftware = "Dandelion v0.2.1b"
    private val keyManager = KeyManager()
    private val packetManager = PacketManager
    private val connectionManager = ConnectionManager()
    private val heartbeatManager = HeartbeatManager(ServerConfigManager.serverConfig, { keyManager.getSalt() }, { serverSoftware })

    fun start() {
        if (isRunning) return
        isRunning = true
        Console.startInputLoop()

        Console.log("Server starting...")
        ServerConfigManager.loadAll()

        packetManager.init()
        connectionManager.start()

        LevelManager.loadAll("levels")

        LevelManager.setDefaultJoinLevel(ServerConfigManager.serverConfig.get().serverSettings.defaultLevel)

        if (ServerConfigManager.serverConfig.get().heartbeatSettings.enabled) heartbeatManager.start()
        Console.log("Server started.")

    }

    fun stop() {
        if (!isRunning) return

        isRunning = false
        Console.log("Server stopping...")

        if (ServerConfigManager.serverConfig.get().heartbeatSettings.enabled) heartbeatManager.stop()

        PlayerManager.kickAllPlayers("Server stopped")
        connectionManager.stop()
        Console.stopInputLoop()

        val levelsDir = File("levels")

        if (!levelsDir.exists()) levelsDir.mkdirs()

        val serializer = DandelionLevelSerializer()

        for (level in LevelManager.getAllLevels()) {
            val path = "levels/${level.id}.dlvl"
            serializer.serialize(level, path)
        }

        Console.log("Server stopped.")
        kotlin.system.exitProcess(0)
    }

    fun getConfig(): ServerConfig = ServerConfigManager.serverConfig
    fun getSalt(): String = keyManager.getSalt()
    fun regenerateSalt() = keyManager.regenerate()
    fun getServerSoftware(): String = serverSoftware
    fun isRunning(): Boolean = isRunning
    fun getProtocolVersion() = protocolVersion
    fun sendMessage(message: String, playerId: Byte = 0xff.toByte()) {
        LevelManager.getLevelPlayers(LevelManager.defaultLevel).forEach { it.sendMessage(message, playerId) }
    }
}
