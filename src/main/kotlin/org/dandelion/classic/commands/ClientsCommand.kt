package org.dandelion.classic.commands

import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.server.MessageRegistry

@CommandDef(name = "clients", description = "Lists players by client.")
class ClientsCommand : Command {
    @OnExecute
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val players = Player.getAllPlayers()
        if (players.isEmpty()) {
            MessageRegistry.Commands.Chat.sendNoPlayersOnline(executor)
            return
        }
        val grouped = players.groupBy { it.client }
        grouped.forEach { (client, list) ->
            val playerNames =
                list.joinToString(", ") {
                    MessageRegistry.Commands.Clients.formatPlayer(it.name)
                }
            MessageRegistry.Commands.Clients.sendClientList(
                executor,
                client,
                playerNames,
            )
        }
    }
}
