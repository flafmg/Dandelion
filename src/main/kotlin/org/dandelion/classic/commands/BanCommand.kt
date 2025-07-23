package org.dandelion.classic.commands

import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.player.Players
import org.dandelion.classic.player.PlayerInfo

@CommandDef(name = "ban", description = "bans a player from the server", usage = "/ban <player> [reason]")
class BanCommand: Command {
    @OnExecute
    @RequirePermission("dandelion.server.ban")
    @ArgRange(min = 1)
    fun execute(executor: CommandExecutor, args: Array<String>){
        val playerName = args[0]
        val reason = if (args.size > 1) args.slice(1 until args.size).joinToString(" ") else "You have been banned"
        val player = Players.byName(playerName)
        if(player == null){
            val info = PlayerInfo.get(playerName)
            if(info == null){
                executor.sendMessage("&cPlayer '&f$playerName&c' not found.")
                return
            }
            info.banned = true
            info.banReason = reason
            info.save()
            executor.sendMessage("&aPlayer '&f$playerName&a' has been banned for '&f$reason&a'.")
            return
        }
        player.ban(reason)
        executor.sendMessage("&aPlayer '&f${player.name}&a' has been banned for '&f$reason&a'.")
    }
}