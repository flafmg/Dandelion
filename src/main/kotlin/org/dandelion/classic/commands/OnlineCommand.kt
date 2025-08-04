package org.dandelion.classic.commands

import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.server.MessageRegistry
import org.dandelion.classic.entity.player.Player

@CommandDef(name = "online", description = "Lists all online players.")
class OnlineCommand : Command {
    @OnExecute
    fun execute(executor: CommandExecutor,  args: Array<String>) {
        val players = Player.getAllPlayers()
        if (players.isEmpty()) {
            MessageRegistry.Commands.Chat.sendNoPlayersOnline(executor)
            return
        }
        val names = players.joinToString(", ") { MessageRegistry.Commands.Online.formatPlayer(it.name) }
        MessageRegistry.Commands.Online.sendList(executor, players.size, names)
    }
}
