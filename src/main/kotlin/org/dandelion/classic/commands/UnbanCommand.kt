package org.dandelion.classic.commands

import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.server.MessageRegistry
import org.dandelion.classic.entity.player.PlayerInfo

@CommandDef(name = "unban", description = "unbans a player from the server", usage = "/unban <player>")
class UnbanCommand: Command {
    @OnExecute
    @RequirePermission("dandelion.server.unban")
    @ArgRange(min = 1, max = 1)
    fun execute(executor: CommandExecutor, args: Array<String>){
        val playerName = args[0]
        val info = PlayerInfo.load(playerName)
        if(info == null || !info.isBanned){
            MessageRegistry.Commands.Server.Unban.sendNotBanned(executor, playerName)
            return
        }
        info.removeBan()
        MessageRegistry.Commands.Server.Unban.sendSuccess(executor, playerName)
    }
}