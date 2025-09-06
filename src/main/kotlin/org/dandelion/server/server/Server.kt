package org.dandelion.server.server

import org.dandelion.server.blocks.manager.BlockRegistry
import org.dandelion.server.commands.manager.CommandRegistry
import org.dandelion.server.entity.player.data.PlayerInfoRegistry
import org.dandelion.server.level.Levels
import org.dandelion.server.level.generator.GeneratorRegistry
import org.dandelion.server.network.Connection
import org.dandelion.server.network.PacketRegistry
import org.dandelion.server.permission.PermissionRepository
import org.dandelion.server.plugins.manager.PluginRegistry
import org.dandelion.server.server.data.MessageRegistry
import org.dandelion.server.server.data.ServerConfig

object Server {
    private var running = false
    var startTime: Long = 0
        private set

    internal fun init() {
        if (running) return
        running = true
        startTime = System.currentTimeMillis()

        Console.init()
        reloadConfig()
        Salt.regenerate()
        PacketRegistry.init()
        PlayerInfoRegistry.init()
        CommandRegistry.init()
        BlockRegistry.init()
        GeneratorRegistry.init()
        Levels.init()
        Heartbeat.init()
        PermissionRepository.init()
        PluginRegistry.init()
        Connection.init()
        warns()

        val timeTaken = System.currentTimeMillis() - startTime
        Console.log("Server started in ${timeTaken}ms")
    }

    fun shutdown() {
        if (!running) return
        running = false

        Connection.shutdown()
        PluginRegistry.shutdown()
        Heartbeat.shutdown()
        Levels.shutdown()
        Console.log("Server stopped")
        Console.shutdown()
    }

    fun reloadConfig() {
        MessageRegistry.reload()
        ServerConfig.reload()
    }

    private fun warns() {
        Console.warnLog(
            "Dandelion is currently in a highly experimental state. It is not recommended to use this software for production or serious Minecraft servers, as stability and security cannot be guaranteed."
        )
        if (!ServerConfig.verifyUsers)
            Console.warnLog(
                "User verification is disabled. Your server will not validate users and will be vulnerable to attacks! Consider enabling it"
            )
    }

    fun isRunning(): Boolean {
        return running
    }

    fun getSoftware(): String {
        return ServerConfig.serverSoftware
    }
}

fun main() {
    Server.init()
}
