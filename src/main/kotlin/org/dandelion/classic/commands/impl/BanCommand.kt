package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.data.config.manager.ServerConfigManager
import org.dandelion.classic.data.player.manager.PlayerManager

class BanCommand : Command {
    override val name = "ban"
    override val permission = "dandelion.server.ban"
    override val description = "Bans a player from the server."
    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("Player required.")
            return
        }
        val playerName = args[0]
        if (ServerConfigManager.bansConfig.isBanned(playerName)) {
            executor.sendMessage("$playerName is already banned.")
            return
        }
        ServerConfigManager.bansConfig.ban(playerName)
        val player = PlayerManager.getAllPlayers().find { it.userName.equals(playerName, ignoreCase = true) }
        if (player != null) {
            player.kick("You have been banned from this server.")
        }
        executor.sendMessage("$playerName has been banned.")
    }
}

