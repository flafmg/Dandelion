package org.dandelion.classic.commands.impl

import org.dandelion.classic.Server
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor

class SoftwareCommand : Command {
    override val name = "software"
    override val description = "Shows the software name and version."
    override val permission = "dandelion.server.software"
    override val aliases = listOf("s")

    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        executor.sendMessage("Using: &e${Server.getServerSoftware()}")
    }
}

