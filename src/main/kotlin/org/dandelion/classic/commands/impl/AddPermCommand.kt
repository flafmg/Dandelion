package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.data.config.manager.ServerConfigManager
import org.dandelion.classic.data.player.manager.PlayerManager

class AddPermCommand : Command {
    override val name = "addperm"
    override val permission = "dandelion.server.addperm"
    override val description = "Adds a permission to a player."
    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        if (args.size < 2) {
            executor.sendMessage("Player and permission required.")
            return
        }
        val playerName = args[0]
        val perm = args[1]
        if (ServerConfigManager.permissionsConfig.hasPermission(playerName, perm)) {
            executor.sendMessage("$playerName already has permission $perm.")
            return
        }
        ServerConfigManager.permissionsConfig.addPermission(playerName, perm)
        val player = PlayerManager.getAllPlayers().find { it.userName.equals(playerName, ignoreCase = true) }
        player?.setPermission(perm, true)
        executor.sendMessage("Permission $perm added to $playerName.")
    }
}

