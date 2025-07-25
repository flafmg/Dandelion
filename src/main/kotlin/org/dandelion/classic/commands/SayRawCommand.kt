package org.dandelion.classic.commands

import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.entity.player.Players

@CommandDef(name = "sayraw", description = "Broadcast a raw message to all players", usage = "/sayraw <message>", aliases = ["sayr", "sar"])
class SayRawCommand: Command {
    @OnExecute
    @RequirePermission("dandelion.server.say")
    @ArgRange(min = 1)
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val message = args.joinToString(" ")
        Players.broadcastMessage(message)
    }
}
