package org.dandelion.classic.commands.model

import org.dandelion.classic.commands.manager.CommandRegistry
import org.dandelion.classic.entity.player.Player

/**
 * CommandExecutor is an interface for entities that can execute commands.
 *
 * Classes implementing this interface must provide a name, a list of permissions, and methods to send messages and check permissions.
 */
interface CommandExecutor {
    /**
     * The name of the executor (e.g., player or console).
     */
    val name: String
    /**
     * The list of permissions assigned to the executor.
     */
    val permissions: List<String>

    /**
     * Sends a message to the executor.
     *
     * @param message The message to send.
     */
    fun sendMessage(message: String)

    /**
     * Checks if the executor has a specific permission.
     *
     * @param permission The permission string to check.
     * @return True if the executor has the permission, false otherwise.
     */
    fun hasPermission(permission: String): Boolean {
        if (permissions.contains("*") || permissions.contains(permission) || (this is Player && this.info.isOperator)) {
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

    /**
     * Sends a command string to the command registry for execution by this executor.
     *
     * @param command The command string to execute.
     */
    fun sendCommand(command: String) {
        CommandRegistry.execute(command, this)
    }
}
