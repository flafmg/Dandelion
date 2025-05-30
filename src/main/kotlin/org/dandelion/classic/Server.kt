package org.dandelion.classic.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.dandelion.classic.server.config.manager.ServerConfigManager
import org.dandelion.classic.server.config.model.ServerConfig
import org.dandelion.classic.server.data.level.generator.manager.GeneratorManager
import org.dandelion.classic.server.data.player.manager.PlayerManager
import org.dandelion.classic.server.manager.ConnectionManager
import org.dandelion.classic.server.manager.HeartbeatManager
import org.dandelion.classic.server.manager.KeyManager
import org.dandelion.classic.server.packets.manager.PacketManager
import org.dandelion.classic.server.util.Logger
import org.dandelion.classic.server.data.level.io.impl.DandelionLevelSerializer
import org.dandelion.classic.server.data.level.io.impl.DandelionLevelDeserializer
import org.dandelion.classic.server.data.level.manager.LevelManager
import org.dandelion.classic.server.data.level.model.Level
import java.io.File

object Server {
    private var isRunning = false
    private val protocolVersion: Byte = 0x07
    private val serverSoftware = "Dandelion v0.2b"
    private val keyManager = KeyManager()
    private val packetManager = PacketManager
    private val connectionManager = ConnectionManager()
    private val heartbeatManager = HeartbeatManager(ServerConfigManager.serverConfig, { keyManager.getSalt() }, { serverSoftware })

    fun start() {
        if (isRunning) return
        isRunning = true
        Logger.log("Server starting...")
        ServerConfigManager.loadAll()

        packetManager.init()
        connectionManager.start()

        LevelManager.loadAll("levels")

        LevelManager.setDefaultJoinLevel(ServerConfigManager.serverConfig.get().serverSettings.defaultLevel)
        if (ServerConfigManager.serverConfig.get().heartbeatSettings.enabled) heartbeatManager.start()
        Logger.log("Server started.")
        Console.startInputLoop()
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        Logger.log("Server stopping...")
        if (ServerConfigManager.serverConfig.get().heartbeatSettings.enabled) heartbeatManager.stop()
        PlayerManager.kickAllPlayers("Server stopped")
        connectionManager.stop()
        val levelsDir = File("levels")
        if (!levelsDir.exists()) levelsDir.mkdirs()
        val serializer = DandelionLevelSerializer()
        for (level in LevelManager.getAllLevels()) {
            val file = File(levelsDir, "${level.id}.dlvl")
            file.writeBytes(level.serialize(serializer))
        }
        Logger.log("Server stopped.")
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
