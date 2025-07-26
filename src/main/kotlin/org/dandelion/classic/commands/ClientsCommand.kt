package org.dandelion.classic.commands

import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.entity.player.Player

@CommandDef(name = "clients", description = "Lists players by client.")
class ClientsCommand : Command {
    @OnExecute
    fun execute(executor: CommandExecutor,  args: Array<String>) {
        val players = Player.getAllPlayers()
        if (players.isEmpty()) {
            executor.sendMessage("&cNo players online.")
            return
        }
        val grouped = players.groupBy { it.client }
        val msg = StringBuilder()
        grouped.forEach { (client, list) ->
            msg.append("&e$client&f: ")
            msg.append(list.joinToString(", ") { "&7${it.name}" })
            msg.append("\n")
        }
        executor.sendMessage(msg.toString().trimEnd())
    }
}

