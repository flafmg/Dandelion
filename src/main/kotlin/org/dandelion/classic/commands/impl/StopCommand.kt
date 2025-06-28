package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.CommandExecutor
import org.dandelion.classic.commands.annotations.Command
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.player.Players
import org.dandelion.classic.server.Server

@Command(name = "stop", description = "Stops the server", usage = "/stop")
class StopCommand {
    @OnExecute
    @RequirePermission("dandelion.server.stop")
    fun execute(executor: CommandExecutor, args: Array<String>) {
        executor.sendMessage("&eShutting down server...")
        Players.kickAll("Shutting down server...")
        Server.shutdown()
    }
}
