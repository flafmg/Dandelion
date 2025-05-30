package org.dandelion.classic.server.commands.impl

import org.dandelion.classic.server.Server
import org.dandelion.classic.server.commands.model.Command
import org.dandelion.classic.server.commands.model.CommandExecutor

class StopCommand : Command {
    override val name = "stop"
    override val permission = "dandelion.server.stop"
    override val description = "Stops the server."
    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        executor.sendMessage("Stopping server...")
        Server.stop()
    }
}

