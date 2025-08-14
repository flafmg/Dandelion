package org.dandelion.classic.commands

import java.util.concurrent.TimeUnit
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.server.MessageRegistry
import org.dandelion.classic.server.Server
import org.dandelion.classic.server.ServerInfo

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

        MessageRegistry.Commands.Server.Info.sendHeader(executor)
        MessageRegistry.Commands.Server.Info.sendSoftware(
            executor,
            ServerInfo.serverSoftware,
        )
        MessageRegistry.Commands.Server.Info.sendUptime(executor, uptime)
        MessageRegistry.Commands.Server.Info.sendPublic(
            executor,
            ServerInfo.isPublic,
        )
    }
}
