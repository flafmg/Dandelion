package org.dandelion.classic.commands

import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.commands.manager.CommandInfo
import org.dandelion.classic.commands.manager.CommandRegistry
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.model.Command
import kotlin.math.ceil

@CommandDef(name = "commands", description = "shows info for all registered commands", usage = "/commands [page|command]", aliases = ["help", "cmd", "cmds"])
class HelpCommand: Command {

    @OnExecute
    @ArgRange(max = 1)
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val commands = CommandRegistry.getCommands()
        if (args.isNotEmpty()) {
            val arg = args[0]
            val pageNumber = arg.toIntOrNull()

            if (pageNumber != null) {
                showPage(executor, commands, pageNumber)
            } else {
                showCommandInfo(executor, commands, arg)
            }

        } else {
            showPage(executor, commands, 1)
        }
    }

    private fun showPage(executor: CommandExecutor, commands: List<CommandInfo>, page: Int) {
        val commandsPerPage = 3
        val totalPages = ceil(commands.size.toDouble() / commandsPerPage).toInt()

        if (page < 1 || page > totalPages) {
            executor.sendMessage("&cInvalid page number.")
            return
        }

        executor.sendMessage("&e--- Showing help page ($page/$totalPages) ---")
        val startIndex = (page - 1) * commandsPerPage
        val endIndex = (startIndex + commandsPerPage).coerceAtMost(commands.size)

        for (i in startIndex until endIndex) {
            val command = commands[i]
            showCommandDetails(executor, command)

            if (i < endIndex - 1) {
                executor.sendMessage("&8---")
            }
        }
        executor.sendMessage("&e--- Page $page of $totalPages ---")
    }

    private fun showCommandInfo(executor: CommandExecutor, commands: List<CommandInfo>, commandName: String) {
        val command = commands.find {
            it.name.lowercase() == commandName.lowercase() ||
            it.aliases.any { alias -> alias.lowercase() == commandName.lowercase() }
        }
        if (command == null) {
            executor.sendMessage("&cCommand '&f$commandName&c' not found.")
            return
        }

        executor.sendMessage("&e--- Command Info: &f${command.name} &e---")
        showCommandDetails(executor, command)
    }

    private fun showCommandDetails(executor: CommandExecutor, command: CommandInfo) {
        executor.sendMessage("&eCommand: &f${command.name}")
        if (command.aliases.isNotEmpty()) {
            executor.sendMessage("&eAliases: &f${command.aliases.joinToString(", ")}")
        }
        executor.sendMessage("&eDescription: &f${command.description}")
        executor.sendMessage("&eUsage: &f${command.usage}")
        if (command.permission.isNotEmpty()) {
            executor.sendMessage("&ePermission: &f${command.permission}")
        }
    }
}
