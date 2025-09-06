package org.dandelion.server.commands

import org.dandelion.server.commands.annotations.CommandDef
import org.dandelion.server.commands.annotations.OnExecute
import org.dandelion.server.commands.annotations.RequirePermission
import org.dandelion.server.commands.model.Command
import org.dandelion.server.commands.model.CommandExecutor
import org.dandelion.server.server.Server

@CommandDef(name = "reload", description = "Reloads server configuration", usage = "/reload")
class ReloadCommand : Command {
    @OnExecute
    @RequirePermission("dandelion.command.reload")
    fun execute(executor: CommandExecutor, args: Array<String>) {
        Server.reloadConfig()
        executor.sendMessage("&aServer configuration reloaded")
    }
}