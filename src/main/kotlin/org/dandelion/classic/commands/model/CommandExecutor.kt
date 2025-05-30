package org.dandelion.classic.server.commands.model

interface CommandExecutor {
    fun sendMessage(message: String)
    fun getName(): String
    fun hasPermission(permission: String): Boolean
    fun isConsole(): Boolean
}

