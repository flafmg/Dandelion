package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.CommandExecutor
import org.dandelion.classic.commands.annotations.Command
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.server.Server
import org.dandelion.classic.server.ServerInfo
import java.util.concurrent.TimeUnit

@Command(name = "serverinfo", description = "Shows server information", usage = "/serverinfo", aliases = ["sinfo", "server"])
class ServerInfoCommand {

    @OnExecute
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val uptimeMillis = System.currentTimeMillis() - Server.startTime
        val uptime = String.format("%d days, %d hours, %d min",
            TimeUnit.MILLISECONDS.toDays(uptimeMillis),
            TimeUnit.MILLISECONDS.toHours(uptimeMillis) % 24,
            TimeUnit.MILLISECONDS.toMinutes(uptimeMillis) % 60
        )
        executor.sendMessage("&e--- Server Info ---")
        executor.sendMessage("&eSoftware: &f${ServerInfo.serverSoftware}")
        executor.sendMessage("&eUptime: &f$uptime")
        executor.sendMessage("&eCPE Enabled: &f${if (ServerInfo.isCpe) "&aYes" else "&cNo"}")
        executor.sendMessage("&ePublic: &f${if (ServerInfo.isPublic) "&aYes" else "&cNo"}")
    }
}
