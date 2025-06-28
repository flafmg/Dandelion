package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.CommandExecutor
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.Command
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.player.Players

@Command(name = "say", description = "Broadcast a message to all players", usage = "/say <message>")
class SayCommand {
    @OnExecute
    @RequirePermission("dandelion.server.say")
    @ArgRange(min = 1)
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val message = args.joinToString(" ")
        Players.broadcast("[${executor.name}] $message")
    }
}
