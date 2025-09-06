package org.dandelion.server.commands.model

import org.dandelion.server.commands.manager.CommandRegistry

interface CommandExecutor {
    val name: String
    val permissions: Map<String, Boolean>

    fun sendMessage(message: String)

    fun hasPermission(permission: String, default: Boolean = false): Boolean {
        if (permissions.containsKey(permission)) {
            return permissions[permission] == true
        }

        val permissionParts = permission.split('.')
        for (i in permissionParts.size - 1 downTo 1) {
            val wildcard = permissionParts.subList(0, i).joinToString(".") + ".*"
            if (permissions.containsKey(wildcard)) {
                return permissions[wildcard] == true
            }
        }

        if (permissions.containsKey("*")) {
            return permissions["*"] == true
        }

        return default
    }

    fun sendCommand(command: String) {
        CommandRegistry.execute(command, this)
    }
}
