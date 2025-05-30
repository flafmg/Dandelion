package org.dandelion.classic.commands.model

interface CommandExecutor {
    fun sendMessage(message: String)
    fun getName(): String
    fun hasPermission(permission: String): Boolean
    fun isConsole(): Boolean
}

