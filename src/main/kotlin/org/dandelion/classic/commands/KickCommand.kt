package org.dandelion.classic.commands

import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.server.MessageRegistry

@CommandDef(
    name = "kick",
    description = "Kick a player from the server",
    usage = "/kick <player> [reason]",
)
class KickCommand : Command {
    @OnExecute
    @RequirePermission("dandelion.server.kick")
    @ArgRange(min = 1)
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val playerName = args[0]
        val reason =
            if (args.size > 1) args.slice(1 until args.size).joinToString(" ")
            else MessageRegistry.Commands.Server.Kick.getDefaultReason()
        val player = Players.find(playerName)
        if (player == null) {
            MessageRegistry.Commands.sendPlayerNotFound(executor, playerName)
            return
        }
        player.kick(reason)
        MessageRegistry.Commands.Server.Kick.sendSuccess(executor, player.name)
    }
}
