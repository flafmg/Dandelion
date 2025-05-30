package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.data.config.manager.ServerConfigManager

class GetPermsCommand : Command {
    override val name = "getperms"
    override val aliases = listOf("perms")
    override val permission = "dandelion.server.getperms"
    override val description = "Shows the permissions of a player (or yourself)."
    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        val targetName = if (args.isNotEmpty()) args[0] else executor.getName()
        val isSelf = args.isEmpty()
        if (isSelf && executor.isConsole()) {
            executor.sendMessage("Console must specify a player name.")
            return
        }
        val perms = ServerConfigManager.permissionsConfig.getPermissions(targetName)
        if (perms.isEmpty()) {
            executor.sendMessage("No permissions found for $targetName.")
            return
        }
        executor.sendMessage("Permissions for $targetName:")
        perms.sorted().forEach { perm ->
            executor.sendMessage("- $perm")
        }
    }
}

