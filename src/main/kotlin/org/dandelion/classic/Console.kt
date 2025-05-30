package org.dandelion.classic.server

import org.dandelion.classic.server.commands.model.CommandExecutor
import org.dandelion.classic.server.commands.manager.CommandRegistry

object Console : CommandExecutor {
    override fun sendMessage(message: String) {
        println("[Console] $message")
    }
    override fun getName(): String = "Console"
    override fun hasPermission(permission: String): Boolean = true
    override fun isConsole(): Boolean = true

    fun startInputLoop() {
        while (Server.isRunning()) {
            val input = readLine() ?: break
            if (input.isBlank()) continue
            CommandRegistry.execute(input, this)
        }
    }
}
