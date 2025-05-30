package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.data.player.manager.PlayerManager
import org.dandelion.classic.data.level.manager.LevelManager
import org.dandelion.classic.util.Logger

class SayCommand : Command {
    override val name = "say"
    override val description = "Sends a message to everyone in the chat."
    override val permission = "dandelion.server.say"
    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("Usage: /say <message>")
            return
        }
        val message = args.joinToString(" ")
        LevelManager.getAllLevels().forEach { it.sendMessage(message) }
        Logger.log(message)
    }
}
