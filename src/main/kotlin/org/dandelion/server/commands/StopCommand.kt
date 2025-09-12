package org.dandelion.server.commands

import org.dandelion.server.commands.annotations.CommandDef
import org.dandelion.server.commands.annotations.OnExecute
import org.dandelion.server.commands.annotations.RequirePermission
import org.dandelion.server.commands.model.Command
import org.dandelion.server.commands.model.CommandExecutor
import org.dandelion.server.entity.player.PlayerRegistry
import org.dandelion.server.server.data.MessageRegistry
import org.dandelion.server.server.Server

@CommandDef(name = "stop", description = "Stops the server", usage = "/stop")
class StopCommand : Command {
    @OnExecute
    @RequirePermission("dandelion.command.stop")
    fun execute(executor: CommandExecutor, args: Array<String>) {
        MessageRegistry.Commands.Server.Stop.sendShuttingDown(executor)
        PlayerRegistry.kickAll(MessageRegistry.Commands.Server.Stop.getKickMessage())
        Server.shutdown()
    }
}
