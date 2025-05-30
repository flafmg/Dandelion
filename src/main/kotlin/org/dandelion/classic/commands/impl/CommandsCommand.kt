package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.manager.CommandRegistry
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor

class CommandsCommand : Command {
    override val name = "commands"
    override val permission = ""
    override val description = "Lists all available commands."
    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        val all = CommandRegistry.getAll()
        val prefix = CommandRegistry.prefix
        if (all.isEmpty()) {
            executor.sendMessage("No commands registered.")
            return
        }
        val lines = all.map { "> &b$prefix${it.name} &7- ${it.description}" }
        lines.forEach { line ->
            line.chunked(64).forEach { part ->
                executor.sendMessage(part)
            }
        }
    }
}
