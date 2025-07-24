package org.dandelion.classic.commands

import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.ReferSelf
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.player.PlayerInfo
import org.dandelion.classic.player.Players
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

@CommandDef(name = "playerinfo", description = "Shows information about a player", usage = "/playerinfo <player>", aliases = ["info", "pinfo"])
class PlayerInfoCommand: Command {

    @OnExecute
    @ReferSelf()
    @ArgRange(max = 1)
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val playerName = args.getOrNull(0) ?: executor.name
        val info = PlayerInfo.load(playerName)
        if (info == null) {
            executor.sendMessage("&cPlayer '&f$playerName&c' not found.")
            return
        }

        val onlinePlayer = Players.findPlayerByName(playerName)
        var currentTotalPlaytime = info.totalPlaytime
        if (onlinePlayer != null) {
            currentTotalPlaytime += (Date().time - info.lastJoin.time)
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss") //ya americans use MM-dd, we here use dd-MM, so to avoid confusion this uses yyyy-MM-dd
        val playtime = String.format("%d days, %d hours, %d min",
            TimeUnit.MILLISECONDS.toDays(currentTotalPlaytime),
            TimeUnit.MILLISECONDS.toHours(currentTotalPlaytime) % 24,
            TimeUnit.MILLISECONDS.toMinutes(currentTotalPlaytime) % 60
        )

        executor.sendMessage("&e--- Player Info: &f${info.name} &e---")
        executor.sendMessage("&eIs OP: &f${info.isOperator}")
        executor.sendMessage("&eBanned: &f${if (info.isBanned) "&cYes (&f${info.banReason}&c)" else "&aNo"}")
        executor.sendMessage("&eFirst Join: &f${dateFormat.format(info.firstJoin)}")
        executor.sendMessage("&eLast Join: &f${dateFormat.format(info.lastJoin)}")
        executor.sendMessage("&eLast Seen: &f${ if(Players.findPlayerByName(playerName) == null) dateFormat.format(info.lastSeen) else "now"}")
        executor.sendMessage("&ePlaytime: &f$playtime")
        executor.sendMessage("&eJoin Count: &f${info.joinCount}")
    }
}
