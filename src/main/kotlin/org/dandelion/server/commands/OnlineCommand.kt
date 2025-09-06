package org.dandelion.server.commands

import org.dandelion.server.commands.annotations.CommandDef
import org.dandelion.server.commands.annotations.OnExecute
import org.dandelion.server.commands.model.Command
import org.dandelion.server.commands.model.CommandExecutor
import org.dandelion.server.entity.player.Player
import org.dandelion.server.server.data.MessageRegistry

@CommandDef(name = "online", description = "Lists all online players.")
class OnlineCommand : Command {
    @OnExecute
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val players = Player.getAllPlayers()
        if (players.isEmpty()) {
            MessageRegistry.Commands.Chat.sendNoPlayersOnline(executor)
            return
        }
        val names =
            players.joinToString(", ") {
                MessageRegistry.Commands.Online.formatPlayer(it.name)
            }
        MessageRegistry.Commands.Online.sendList(executor, players.size, names)
    }
}
