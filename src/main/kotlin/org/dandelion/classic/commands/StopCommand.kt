package org.dandelion.classic.commands

import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.server.Server

@CommandDef(name = "stop", description = "Stops the server", usage = "/stop")
class StopCommand: Command {
    @OnExecute
    @RequirePermission("dandelion.server.stop")
    fun execute(executor: CommandExecutor, args: Array<String>) {
        executor.sendMessage("&eShutting down server...")
        Players.kickAll("Shutting down server...")
        Server.shutdown()
    }
}
