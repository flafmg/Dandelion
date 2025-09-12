package org.dandelion.server.commands

import org.dandelion.server.commands.annotations.ArgRange
import org.dandelion.server.commands.annotations.CommandDef
import org.dandelion.server.commands.annotations.OnExecute
import org.dandelion.server.commands.annotations.RequirePermission
import org.dandelion.server.commands.model.Command
import org.dandelion.server.commands.model.CommandExecutor
import org.dandelion.server.entity.player.data.PlayerInfo
import org.dandelion.server.entity.player.PlayerRegistry
import org.dandelion.server.server.data.MessageRegistry

@CommandDef(
    name = "ban",
    description = "bans a player from the server",
    usage = "/ban <player> [reason]",
)
class BanCommand : Command {
    @OnExecute
    @RequirePermission("dandelion.command.ban")
    @ArgRange(min = 1)
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val playerName = args[0]
        val reason =
            if (args.size > 1) args.slice(1 until args.size).joinToString(" ")
            else MessageRegistry.Commands.Server.Ban.getDefaultReason()
        val player = PlayerRegistry.find(playerName)
        if (player == null) {
            val info = PlayerInfo.load(playerName)
            if (info == null) {
                MessageRegistry.Commands.sendPlayerNotFound(
                    executor,
                    playerName,
                )
                return
            }
            info.setBanned(reason)
            MessageRegistry.Commands.Server.Ban.sendSuccess(
                executor,
                playerName,
                reason,
            )
            return
        }
        player.ban(reason)
        MessageRegistry.Commands.Server.Ban.sendSuccess(
            executor,
            player.name,
            reason,
        )
    }
}
