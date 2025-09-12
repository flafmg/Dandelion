package org.dandelion.server.commands

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import org.dandelion.server.commands.annotations.ArgRange
import org.dandelion.server.commands.annotations.CommandDef
import org.dandelion.server.commands.annotations.OnExecute
import org.dandelion.server.commands.annotations.ReferSelf
import org.dandelion.server.commands.model.Command
import org.dandelion.server.commands.model.CommandExecutor
import org.dandelion.server.entity.player.data.PlayerInfo
import org.dandelion.server.entity.player.PlayerRegistry
import org.dandelion.server.server.data.MessageRegistry

@CommandDef(
    name = "playerinfo",
    description = "Shows information about a player",
    usage = "/playerinfo <player>",
    aliases = ["info", "pinfo"],
)
class PlayerInfoCommand : Command {

    @OnExecute
    @ReferSelf()
    @ArgRange(max = 1)
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val playerName = args.getOrNull(0) ?: executor.name
        val info = PlayerInfo.load(playerName)
        if (info == null) {
            MessageRegistry.Commands.sendPlayerNotFound(executor, playerName)
            return
        }

        val onlinePlayer = PlayerRegistry.find(playerName)
        var currentTotalPlaytime = info.totalPlaytime
        if (onlinePlayer != null) {
            currentTotalPlaytime += (Date().time - info.lastJoin.time)
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val playtime =
            String.format(
                "%d days, %d hours, %d min",
                TimeUnit.MILLISECONDS.toDays(currentTotalPlaytime),
                TimeUnit.MILLISECONDS.toHours(currentTotalPlaytime) % 24,
                TimeUnit.MILLISECONDS.toMinutes(currentTotalPlaytime) % 60,
            )

        MessageRegistry.Commands.Player.Info.sendHeader(executor, info.name)
        MessageRegistry.Commands.Player.Info.sendClient(
            executor,
            onlinePlayer?.client
                ?: MessageRegistry.Commands.Player.Info.getUnknownClient(),
        )
        MessageRegistry.Commands.Player.Info.sendBannedStatus(
            executor,
            info.isBanned,
            info.banReason,
        )
        MessageRegistry.Commands.Player.Info.sendFirstJoin(
            executor,
            dateFormat.format(info.firstJoin),
        )
        MessageRegistry.Commands.Player.Info.sendLastJoin(
            executor,
            dateFormat.format(info.lastJoin),
        )
        MessageRegistry.Commands.Player.Info.sendLastSeen(
            executor,
            dateFormat.format(info.lastSeen),
            PlayerRegistry.find(playerName) != null,
        )
        MessageRegistry.Commands.Player.Info.sendPlaytime(executor, playtime)
        MessageRegistry.Commands.Player.Info.sendJoinCount(
            executor,
            info.joinCount,
        )
    }
}
