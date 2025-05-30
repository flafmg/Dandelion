package org.dandelion.classic.server.commands.impl

import org.dandelion.classic.server.commands.model.Command
import org.dandelion.classic.server.commands.model.CommandExecutor
import org.dandelion.classic.server.config.manager.ServerConfigManager

class UnbanCommand : Command {
    override val name = "unban"
    override val permission = "dandelion.server.unban"
    override val description = "Unbans a player from the server."
    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("Player required.")
            return
        }
        val playerName = args[0]
        if (!ServerConfigManager.bansConfig.isBanned(playerName)) {
            executor.sendMessage("$playerName is not banned.")
            return
        }
        ServerConfigManager.bansConfig.unban(playerName)
        executor.sendMessage("$playerName has been unbanned.")
    }
}

