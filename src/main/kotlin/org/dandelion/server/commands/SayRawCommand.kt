package org.dandelion.server.commands

import org.dandelion.server.commands.annotations.ArgRange
import org.dandelion.server.commands.annotations.CommandDef
import org.dandelion.server.commands.annotations.OnExecute
import org.dandelion.server.commands.annotations.RequirePermission
import org.dandelion.server.commands.model.Command
import org.dandelion.server.commands.model.CommandExecutor
import org.dandelion.server.entity.player.Players

@CommandDef(
    name = "sayraw",
    description = "Broadcast a raw message to all players",
    usage = "/sayraw <message>",
    aliases = ["sayr", "sar"],
)
class SayRawCommand : Command {
    @OnExecute
    @RequirePermission("dandelion.command.say")
    @ArgRange(min = 1)
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val message = args.joinToString(" ")
        Players.broadcastMessage(message)
    }
}
