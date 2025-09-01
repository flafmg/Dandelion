package org.dandelion.classic.server

import org.dandelion.classic.blocks.manager.BlockRegistry
import org.dandelion.classic.commands.manager.CommandRegistry
import org.dandelion.classic.entity.player.data.PlayerInfoRegistry
import org.dandelion.classic.level.Levels
import org.dandelion.classic.level.generator.GeneratorRegistry
import org.dandelion.classic.network.Connection
import org.dandelion.classic.network.PacketRegistry
import org.dandelion.classic.permission.PermissionRepository
import org.dandelion.classic.plugins.manager.PluginRegistry

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

    fun shutdown() {
        if (!running) return
        running = false

        PluginRegistry.shutdown()
        Heartbeat.shutdown()
        Connection.shutdown()
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
