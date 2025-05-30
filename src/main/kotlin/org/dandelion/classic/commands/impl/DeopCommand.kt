package org.dandelion.classic.server.commands.impl

import org.dandelion.classic.server.commands.model.Command
import org.dandelion.classic.server.commands.model.CommandExecutor
import org.dandelion.classic.server.config.manager.ServerConfigManager
import org.dandelion.classic.server.data.player.manager.PlayerManager

class DeopCommand : Command {
    override val name = "deop"
    override val permission = "dandelion.server.deop"
    override val description = "Removes operator status from a player."
    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("Player required.")
            return
        }
        val playerName = args[0]
        if (!ServerConfigManager.opsConfig.isOp(playerName)) {
            executor.sendMessage("$playerName is not an operator.")
            return
        }
        ServerConfigManager.opsConfig.removeOp(playerName)
        val player = PlayerManager.getAllPlayers().find { it.userName.equals(playerName, ignoreCase = true) }
        player?.grantOp(false)
        executor.sendMessage("$playerName is no longer an operator.")
    }
}

