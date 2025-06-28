package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.CommandExecutor
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.Command
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.player.PlayerInfo

@Command(name = "unban", description = "unbans a player from the server", usage = "/unban <player>")
class UnbanCommand {
    @OnExecute
    @RequirePermission("dandelion.server.unban")
    @ArgRange(min = 1, max = 1)
    fun execute(executor: CommandExecutor, args: Array<String>){
        val playerName = args[0]
        val info = PlayerInfo.get(playerName)
        if(info == null || !info.banned){
            executor.sendMessage("&cPlayer '&f$playerName&c' is not banned.")
            return
        }
        info.banReason = ""
        info.banned = false
        info.save()
        executor.sendMessage("&aPlayer '&f$playerName&a' was unbanned.")
    }
}