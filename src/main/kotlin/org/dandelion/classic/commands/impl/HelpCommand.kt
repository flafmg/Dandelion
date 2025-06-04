package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor

class HelpCommand : Command {
    override val name = "help"
    override val description = "Shows help information."
    override val permission = "dandelion.server.help"

    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        executor.sendMessage("this is not mcgalaxy, use /commands instead")
    }
}

