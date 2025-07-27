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
        val lines = mutableListOf<String>()
        grouped.forEach { (client, list) ->
            val line = StringBuilder()
            line.append("&e$client&f: ")
            line.append(list.joinToString(", ") { "&7${it.name}" })
            lines.add(line.toString())
        }
        lines.forEach { executor.sendMessage(it) }
    }
}
