package org.dandelion.classic.commands

import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.commands.manager.CommandInfo
import org.dandelion.classic.commands.manager.CommandRegistry
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.model.Command
import kotlin.math.ceil

@CommandDef(name = "commands", description = "shows info for all registered commands", usage = "/commands [page|command] [subcommand|page]", aliases = ["help", "cmd", "cmds"])
class HelpCommand: Command {

    @OnExecute
    @ArgRange(max = 2)
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val commands = CommandRegistry.getCommands()
        when (args.size) {
            0 -> showPage(executor, commands, 1)
            1 -> {
                val arg = args[0]
                val pageNumber = arg.toIntOrNull()
                if (pageNumber != null) {
                    showPage(executor, commands, pageNumber)
                } else {
                    showCommandInfo(executor, commands, arg, 1)
                }
            }
            2 -> {
                val firstArg = args[0]
                val secondArg = args[1]
                val pageNumber = secondArg.toIntOrNull()

                if (pageNumber != null) {
                    showCommandInfo(executor, commands, firstArg, pageNumber)
                } else {
                    showSubCommandInfo(executor, commands, firstArg, secondArg)
                }
            }
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

    private fun showSubCommandInfo(executor: CommandExecutor, commands: List<CommandInfo>, commandName: String, subCommandName: String) {
        val command = commands.find {
            it.name.lowercase() == commandName.lowercase() ||
                    it.aliases.any { alias -> alias.lowercase() == commandName.lowercase() }
        }
        if (command == null) {
            executor.sendMessage("&cCommand '&f$commandName&c' not found.")
            return
        }

        val subCommand = command.subCommands.values.find { subCommandInfo ->
            subCommandInfo.name.lowercase() == subCommandName.lowercase() ||
                    subCommandInfo.aliases.any { alias -> alias.lowercase() == subCommandName.lowercase() }
        }

        if (subCommand == null) {
            executor.sendMessage("&cSubcommand '&f$subCommandName&c' not found for command '&f$commandName&c'.")
            return
        }

        val pathStr = subCommand.path.joinToString(" ")
        executor.sendMessage("&e--- Subcommand Info: &f$pathStr &e---")
        executor.sendMessage("&eSubcommand: &f${subCommand.name}")
        if (subCommand.aliases.isNotEmpty()) {
            executor.sendMessage("&eAliases: &f${subCommand.aliases.joinToString(", ")}")
        }
        executor.sendMessage("&eDescription: &f${subCommand.description}")
        executor.sendMessage("&eUsage: &f${subCommand.usage}")
        if (subCommand.permission.isNotEmpty()) {
            executor.sendMessage("&ePermission: &f${subCommand.permission}")
        }
    }

    private fun showCommandInfo(executor: CommandExecutor, commands: List<CommandInfo>, commandName: String, page: Int) {
        val command = commands.find {
            it.name.lowercase() == commandName.lowercase() ||
                    it.aliases.any { alias -> alias.lowercase() == commandName.lowercase() }
        }
        if (command == null) {
            executor.sendMessage("&cCommand '&f$commandName&c' not found.")
            return
        }

        if (command.subCommands.isEmpty()) {
            executor.sendMessage("&e--- Command Info: &f${command.name} &e---")
            showCommandDetails(executor, command)
            return
        }

        val uniqueSubCommands = command.subCommands.values.distinctBy { it.name + it.path.joinToString(" ") }
            .sortedBy { it.path.joinToString(" ") }
        val subCommandsPerPage = 5
        val totalPages = ceil(uniqueSubCommands.size.toDouble() / subCommandsPerPage).toInt()

        if (page < 1 || page > totalPages) {
            executor.sendMessage("&cInvalid page number.")
            return
        }

        executor.sendMessage("&e--- Command Info: &f${command.name} &e(${page}/${totalPages}) ---")
        showCommandDetails(executor, command)
        executor.sendMessage("&8---")
        executor.sendMessage("&eSubcommands:")

        val startIndex = (page - 1) * subCommandsPerPage
        val endIndex = (startIndex + subCommandsPerPage).coerceAtMost(uniqueSubCommands.size)

        for (i in startIndex until endIndex) {
            val subCommand = uniqueSubCommands[i]
            val pathStr = subCommand.path.joinToString(" ")
            executor.sendMessage("&eSubcommand: &f$pathStr")
            if (subCommand.aliases.isNotEmpty()) {
                executor.sendMessage("&eAliases: &f${subCommand.aliases.joinToString(", ")}")
            }
            executor.sendMessage("&eDescription: &f${subCommand.description}")
            executor.sendMessage("&eUsage: &f${subCommand.usage}")
            if (subCommand.permission.isNotEmpty()) {
                executor.sendMessage("&ePermission: &f${subCommand.permission}")
            }
            if (i < endIndex - 1) {
                executor.sendMessage("&8---")
            }
        }
        executor.sendMessage("&e--- Page $page of $totalPages ---")
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