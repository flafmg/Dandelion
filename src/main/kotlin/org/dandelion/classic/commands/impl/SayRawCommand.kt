package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.CommandExecutor
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.Command
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.player.Players

@Command(name = "sayraw", description = "Broadcast a raw message to all players", usage = "/sayraw <message>", aliases = ["sayr", "sar"])
class SayRawCommand {
    @OnExecute
    @RequirePermission("dandelion.server.say")
    @ArgRange(min = 1)
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val message = args.joinToString(" ")
        Players.broadcast(message)
    }
}
