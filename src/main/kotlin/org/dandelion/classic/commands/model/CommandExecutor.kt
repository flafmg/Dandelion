package org.dandelion.classic.commands.model

import org.dandelion.classic.commands.manager.CommandRegistry

interface CommandExecutor {
    val name: String
    val permissions: List<String>

    fun sendMessage(message: String)

    fun hasPermission(permission: String): Boolean {
        if (permissions.contains("*") || permissions.contains(permission)) {
            return true
        }

        val permissionParts = permission.split('.')
        for (i in 1 until permissionParts.size) {
            val wildcard = permissionParts.subList(0, i).joinToString(".") + ".*"
            if (permissions.contains(wildcard)) {
                return true
            }
        }

        return false
    }
    fun sendCommand(command: String){
        CommandRegistry.execute(command, this)
    }
}