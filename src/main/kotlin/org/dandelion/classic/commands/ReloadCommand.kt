package org.dandelion.classic.commands

import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.server.Server

@CommandDef(name = "reload", description = "Reloads server configuration", usage = "/reload")
class ReloadCommand : Command {
    @OnExecute
    @RequirePermission("dandelion.server.reload")
    fun execute(executor: CommandExecutor, args: Array<String>) {
        Server.reloadConfig()
        executor.sendMessage("&aServer configuration reloaded")
    }
}