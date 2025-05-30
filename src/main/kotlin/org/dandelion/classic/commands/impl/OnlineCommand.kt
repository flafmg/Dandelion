package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.data.player.manager.PlayerManager

class OnlineCommand : Command {
    override val name = "online"
    override val aliases = listOf("o")
    override val description = "Shows the quantity of online players"
    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        val count = PlayerManager.getOnlinePlayerCount()
        executor.sendMessage("Online players: $count")
    }
}
