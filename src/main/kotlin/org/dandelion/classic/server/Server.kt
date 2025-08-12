package org.dandelion.classic.server

import org.dandelion.classic.blocks.manager.BlockRegistry
import org.dandelion.classic.commands.manager.CommandRegistry
import org.dandelion.classic.level.Levels
import org.dandelion.classic.level.generator.GeneratorRegistry
import org.dandelion.classic.network.Connection
import org.dandelion.classic.network.PacketRegistry
import org.dandelion.classic.permission.PermissionRepository
import org.dandelion.classic.plugins.manager.PluginRegistry
import org.dandelion.classic.util.Utils
import org.dandelion.classic.util.YamlConfig
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object ServerInfo {
    const val dandelionVersion = "0.1.1-Dev"
    const val serverSoftware = "Dandelion $dandelionVersion"

    var isPublic = true
        internal set
    var port = 25566
        internal set
    var name = "a classic minecraft server"
        internal set
    var motd = "powered by dandelion"
        internal set
    var maxPlayers = 255
        internal set
    var verifyUsers = true
        internal set
    var debugMode = false
        internal set

    var defaultLevel = "main"
        internal set
    var autoSaveInterval: Duration = 120.seconds
        internal set

    var heartbeatEnabled: Boolean = true
        internal set
    var heartbeatUrl: String = "http://www.classicube.net/server/heartbeat/"
        internal set
    var heartbeatData: String = "name={server-name}&port={server-port}&users={players-online}&max={players-max}&public={server-public}&salt={server-salt}&software={server-software}"
        internal set
    var heartbeatInterval: Duration = 45.seconds
        internal set

    val salt: String
        get() = Salt.get()
}

object Server {
    private var running = false;
    private lateinit var config: YamlConfig
    var startTime: Long = 0
        private set

    internal fun init(){
        if(running) return;
        running = true
        startTime = System.currentTimeMillis()

        Console.init()
        reloadConfig()
        Salt.regenerate()
        PacketRegistry.init()
        Connection.init()
        CommandRegistry.init()
        BlockRegistry.init()
        GeneratorRegistry.init()
        Levels.init()
        Heartbeat.init()
        PermissionRepository.init()
        PluginRegistry.init()
        warns()
        Console.log("Server started")
    }
    fun shutdown(){
        if(!running) return;
        running = false

        PluginRegistry.shutdown()
        Heartbeat.shutdown()
        Connection.shutdown()
        Levels.shutdown()
        Console.log("Server stopped")
        Console.shutdown()
    }

    fun reloadConfig() {
        MessageRegistry.init()

        val configFile = File("config.yml")
        if (!configFile.exists()) {
            Utils.copyResourceTo("config.yml", "config.yml")
        }
        config = YamlConfig.load(configFile)

        ServerInfo.name = config.getString("server.name", ServerInfo.name)
        ServerInfo.motd = config.getString("server.motd", ServerInfo.motd)
        ServerInfo.port = config.getInt("ServerInfo.port", ServerInfo.port)
        ServerInfo.maxPlayers = config.getInt("server.max-players", ServerInfo.maxPlayers)
        ServerInfo.isPublic = config.getBoolean("server.public", ServerInfo.isPublic)
        ServerInfo.verifyUsers = config.getBoolean("server.verify-users", ServerInfo.verifyUsers)
        ServerInfo.debugMode = config.getBoolean("server.debug-mode", ServerInfo.debugMode)

        ServerInfo.defaultLevel = config.getString("level.default-level", ServerInfo.defaultLevel)
        ServerInfo.autoSaveInterval = config.getInt("level.auto-save-interval", ServerInfo.autoSaveInterval.inWholeSeconds.toInt()).seconds

        ServerInfo.heartbeatEnabled = config.getBoolean("heartbeat.enabled", ServerInfo.heartbeatEnabled)
        ServerInfo.heartbeatUrl = config.getString("heartbeat.url", ServerInfo.heartbeatUrl)
        ServerInfo.heartbeatData = config.getString("heartbeat.data", ServerInfo.heartbeatData)
        ServerInfo.heartbeatInterval = config.getInt("heartbeat.interval", ServerInfo.heartbeatInterval.inWholeSeconds.toInt()).seconds
    }
    private fun warns(){
        Console.warnLog("Dandelion is currently in a highly experimental state. It is not recommended to use this software for production or serious Minecraft servers, as stability and security cannot be guaranteed.")
        if(!ServerInfo.verifyUsers)
            Console.warnLog("User verification is disabled. Your server will not validate users and will be vulnerable to attacks! Consider enabling it")
    }

    fun isRunning(): Boolean{
        return running
    }

    fun getSoftware(): String{
        return ServerInfo.serverSoftware
    }
}
fun main(){
    Server.init()
}
