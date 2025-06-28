package org.dandelion.classic.server

import org.dandelion.classic.commands.CommandRegistry
import org.dandelion.classic.level.Levels
import org.dandelion.classic.level.generator.GeneratorRegistry
import org.dandelion.classic.network.Connection
import org.dandelion.classic.network.PacketFactory
import org.dandelion.classic.util.Utils
import org.dandelion.classic.util.YamlConfig
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object ServerInfo {
    const val dandelionVersion = "0.1a"
    const val serverSoftware = "Dandelion $dandelionVersion"

    val isCpe = false
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

    internal fun init(){
        if(running) return;
        running = true

        Console.init()
        reloadConfig()
        Salt.regenerate()
        PacketFactory.init()
        Connection.init()
        CommandRegistry.init()
        GeneratorRegistry.init()
        Levels.init()
        Heartbeat.init()
        warns()
        Console.log("Server started")
    }
    fun shutdown(){
        if(!running) return;
        running = false

        Heartbeat.shutdown()
        PacketFactory.shutdown()
        Connection.shutdown()
        CommandRegistry.shutdown()
        GeneratorRegistry.shutdown()
        Levels.shutdown()
        Console.log("Server stopped")
        Console.shutdown()
    }

    fun reloadConfig() {
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
        if(!ServerInfo.isCpe)
            Console.warnLog("Protocol extension is disabled. Any feature relying on cpe will not work!")
        if(!ServerInfo.verifyUsers)
            Console.warnLog("User verification is disabled. Your server will not validate users and will be vulnerable to attacks! Consider enabling it")
    }
    fun restart(){
        shutdown()
        init()
    }

    fun isRunning(): Boolean{
        return running
    }


}
fun main(){
    Server.init()
}