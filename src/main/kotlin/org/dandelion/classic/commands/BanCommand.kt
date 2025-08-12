package org.dandelion.classic.commands

import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.server.MessageRegistry
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.entity.player.PlayerInfo

@CommandDef(name = "ban", description = "bans a player from the server", usage = "/ban <player> [reason]")
class BanCommand: Command {
    @OnExecute
    @RequirePermission("dandelion.server.ban")
    @ArgRange(min = 1)
    fun execute(executor: CommandExecutor, args: Array<String>){
        val playerName = args[0]
        val reason = if (args.size > 1) args.slice(1 until args.size).joinToString(" ") else MessageRegistry.Commands.Server.Ban.getDefaultReason()
        val player = Players.find(playerName)
        if(player == null){
            val info = PlayerInfo.load(playerName)
            if(info == null){
                MessageRegistry.Commands.sendPlayerNotFound(executor, playerName)
                return
            }
            info.setBanned(reason)
            MessageRegistry.Commands.Server.Ban.sendSuccess(executor, playerName, reason)
            return
        }
        player.ban(reason)
        MessageRegistry.Commands.Server.Ban.sendSuccess(executor, player.name, reason)
    }
}