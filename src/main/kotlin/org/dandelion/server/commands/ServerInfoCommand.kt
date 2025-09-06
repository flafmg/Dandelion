package org.dandelion.server.commands

import java.util.concurrent.TimeUnit
import org.dandelion.server.commands.annotations.CommandDef
import org.dandelion.server.commands.annotations.OnExecute
import org.dandelion.server.commands.model.Command
import org.dandelion.server.commands.model.CommandExecutor
import org.dandelion.server.entity.player.data.PlayerInfoRegistry
import org.dandelion.server.server.data.MessageRegistry
import org.dandelion.server.server.Server
import org.dandelion.server.server.data.ServerConfig

@CommandDef(
    name = "serverinfo",
    description = "Shows server information",
    usage = "/serverinfo",
    aliases = ["sinfo", "server"],
)
class ServerInfoCommand : Command {

    @OnExecute
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val uptimeMillis = System.currentTimeMillis() - Server.startTime
        val uptime =
            String.format(
                "%d days, %d hours, %d min",
                TimeUnit.MILLISECONDS.toDays(uptimeMillis),
                TimeUnit.MILLISECONDS.toHours(uptimeMillis) % 24,
                TimeUnit.MILLISECONDS.toMinutes(uptimeMillis) % 60,
            )

        val uniquePlayersCount = PlayerInfoRegistry.getPlayerCount()

        MessageRegistry.Commands.Server.Info.sendHeader(executor)
        MessageRegistry.Commands.Server.Info.sendSoftware(
            executor,
            ServerConfig.serverSoftware,
        )
        MessageRegistry.Commands.Server.Info.sendUptime(executor, uptime)
        MessageRegistry.Commands.Server.Info.sendPublic(
            executor,
            ServerConfig.isPublic,
        )
        MessageRegistry.Commands.Server.sendUniquePlayers(executor, uniquePlayersCount)
    }
}
