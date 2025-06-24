package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.CommandExecutor
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.Command
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.entity.PlayerManager

@Command(name = "kick", description = "Kick a player from the server", usage = "/kick <player> [reason]")
class KickCommand {
    @OnExecute
    @RequirePermission("dandelion.server.kick")
    @ArgRange(min = 1)
    fun execute(executor: CommandExecutor, args: Array<String>){
        val playerName = args[0]
        val reason = if(args.size > 1) args.drop(1).joinToString(" ") else "you have been kicked"

        val player = PlayerManager.getPlayerByName(playerName)
        if(player == null){
            executor.sendMessage("&cPlayer not found")
            return
        }

        player.kick(reason)
        executor.sendMessage("&c$playerName was kicked")
    }
}