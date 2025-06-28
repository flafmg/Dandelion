package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.CommandExecutor
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.Command
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.player.Players

@Command(name = "kick", description = "Kick a player from the server", usage = "/kick <player> [reason]")
class KickCommand {
    @OnExecute
    @RequirePermission("dandelion.server.kick")
    @ArgRange(min = 1)
    fun execute(executor: CommandExecutor, args: Array<String>){
        val playerName = args[0]
        val reason = if (args.size > 1) args.slice(1 until args.size).joinToString(" ") else "You have been kicked"
        val player = Players.byName(playerName)
        if(player == null){
            executor.sendMessage("&cPlayer '&f$playerName&c' not found.")
            return
        }
        player.kick(reason)
        executor.sendMessage("&aPlayer '&f${player.name}&a' has been kicked.")
    }
}