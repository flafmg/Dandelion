package org.dandelion.classic.server.commands.impl

import org.dandelion.classic.server.commands.model.Command
import org.dandelion.classic.server.commands.model.CommandExecutor
import org.dandelion.classic.server.data.player.manager.PlayerManager

class KickCommand : Command {
    override val name = "kick"
    override val permission = "dandelion.server.kick"
    override val description = "Kicks a player from the server."
    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("Player required.")
            return
        }
        val targetName = args[0]
        val reason = if (args.size > 1) args.drop(1).joinToString(" ") else "Kicked by an operator"
        val target = PlayerManager.getAllPlayers().find { it.userName.equals(targetName, ignoreCase = true) }
        if (target == null) {
            executor.sendMessage("Player not found.")
            return
        }
        target.kick(reason)
        executor.sendMessage("Player $targetName was kicked.")
    }
}

