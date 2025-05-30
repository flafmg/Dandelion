package org.dandelion.classic.server.commands.impl

import org.dandelion.classic.server.commands.model.Command
import org.dandelion.classic.server.commands.model.CommandExecutor
import org.dandelion.classic.server.config.manager.ServerConfigManager
import org.dandelion.classic.server.data.player.manager.PlayerManager

class OpCommand : Command {
    override val name = "op"
    override val permission = "dandelion.server.op"
    override val description = "Grants operator status to a player."
    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("Player required.")
            return
        }
        val playerName = args[0]
        if (ServerConfigManager.opsConfig.isOp(playerName)) {
            executor.sendMessage("$playerName is already an operator.")
            return
        }
        ServerConfigManager.opsConfig.addOp(playerName)
        val player = PlayerManager.getAllPlayers().find { it.userName.equals(playerName, ignoreCase = true) }
        player?.grantOp(true)
        executor.sendMessage("$playerName is now an operator.")
    }
}

