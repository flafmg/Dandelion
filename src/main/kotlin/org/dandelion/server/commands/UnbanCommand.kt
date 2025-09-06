package org.dandelion.server.commands

import org.dandelion.server.commands.annotations.ArgRange
import org.dandelion.server.commands.annotations.CommandDef
import org.dandelion.server.commands.annotations.OnExecute
import org.dandelion.server.commands.annotations.RequirePermission
import org.dandelion.server.commands.model.Command
import org.dandelion.server.commands.model.CommandExecutor
import org.dandelion.server.entity.player.data.PlayerInfo
import org.dandelion.server.server.data.MessageRegistry

@CommandDef(
    name = "unban",
    description = "unbans a player from the server",
    usage = "/unban <player>",
)
class UnbanCommand : Command {
    @OnExecute
    @RequirePermission("dandelion.command.unban")
    @ArgRange(min = 1, max = 1)
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val playerName = args[0]
        val info = PlayerInfo.load(playerName)
        if (info == null || !info.isBanned) {
            MessageRegistry.Commands.Server.Unban.sendNotBanned(
                executor,
                playerName,
            )
            return
        }
        info.removeBan()
        MessageRegistry.Commands.Server.Unban.sendSuccess(executor, playerName)
    }
}
