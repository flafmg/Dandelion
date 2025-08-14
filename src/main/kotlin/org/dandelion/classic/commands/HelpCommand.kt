package org.dandelion.classic.commands

import kotlin.math.ceil
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.manager.CommandInfo
import org.dandelion.classic.commands.manager.CommandRegistry
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.server.MessageRegistry

@CommandDef(
    name = "commands",
    description = "shows info for all registered commands",
    usage = "/commands [page|command] [subcommand|page]",
    aliases = ["help", "cmd", "cmds"],
)
class HelpCommand : Command {

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

    private fun showPage(
        executor: CommandExecutor,
        commands: List<CommandInfo>,
        page: Int,
    ) {
        val commandsPerPage = 3
        val totalPages =
            ceil(commands.size.toDouble() / commandsPerPage).toInt()

        if (page < 1 || page > totalPages) {
            MessageRegistry.Commands.Help.sendInvalidPage(executor)
            return
        }

        MessageRegistry.Commands.Help.sendHeader(executor, page, totalPages)
        val startIndex = (page - 1) * commandsPerPage
        val endIndex =
            (startIndex + commandsPerPage).coerceAtMost(commands.size)

        for (i in startIndex until endIndex) {
            val command = commands[i]
            showCommandDetails(executor, command)

            if (i < endIndex - 1) {
                MessageRegistry.Commands.Help.sendSeparator(executor)
            }
        }
        MessageRegistry.Commands.Help.sendFooter(executor, page, totalPages)
    }

    private fun showSubCommandInfo(
        executor: CommandExecutor,
        commands: List<CommandInfo>,
        commandName: String,
        subCommandName: String,
    ) {
        val command =
            commands.find {
                it.name.lowercase() == commandName.lowercase() ||
                    it.aliases.any { alias ->
                        alias.lowercase() == commandName.lowercase()
                    }
            }
        if (command == null) {
            MessageRegistry.Commands.Help.sendCommandNotFound(
                executor,
                commandName,
            )
            return
        }

        val subCommand =
            command.subCommands.values.find { subCommandInfo ->
                subCommandInfo.name.lowercase() == subCommandName.lowercase() ||
                    subCommandInfo.aliases.any { alias ->
                        alias.lowercase() == subCommandName.lowercase()
                    }
            }

        if (subCommand == null) {
            MessageRegistry.Commands.Help.sendSubcommandNotFound(
                executor,
                subCommandName,
                commandName,
            )
            return
        }

        val pathStr = subCommand.path.joinToString(" ")
        MessageRegistry.Commands.Help.sendSubcommandInfoHeader(
            executor,
            pathStr,
        )
        MessageRegistry.Commands.Help.Fields.sendSubcommand(
            executor,
            subCommand.name,
        )
        if (subCommand.aliases.isNotEmpty()) {
            MessageRegistry.Commands.Help.Fields.sendAliases(
                executor,
                subCommand.aliases.joinToString(", "),
            )
        }
        MessageRegistry.Commands.Help.Fields.sendDescription(
            executor,
            subCommand.description,
        )
        MessageRegistry.Commands.Help.Fields.sendUsage(
            executor,
            subCommand.usage,
        )
        if (subCommand.permission.isNotEmpty()) {
            MessageRegistry.Commands.Help.Fields.sendPermission(
                executor,
                subCommand.permission,
            )
        }
    }

    private fun showCommandInfo(
        executor: CommandExecutor,
        commands: List<CommandInfo>,
        commandName: String,
        page: Int,
    ) {
        val command =
            commands.find {
                it.name.lowercase() == commandName.lowercase() ||
                    it.aliases.any { alias ->
                        alias.lowercase() == commandName.lowercase()
                    }
            }
        if (command == null) {
            MessageRegistry.Commands.Help.sendCommandNotFound(
                executor,
                commandName,
            )
            return
        }

        if (command.subCommands.isEmpty()) {
            MessageRegistry.Commands.Help.sendCommandInfoHeader(
                executor,
                command.name,
            )
            showCommandDetails(executor, command)
            return
        }

        val uniqueSubCommands =
            command.subCommands.values
                .distinctBy { it.name + it.path.joinToString(" ") }
                .sortedBy { it.path.joinToString(" ") }
        val subCommandsPerPage = 5
        val totalPages =
            ceil(uniqueSubCommands.size.toDouble() / subCommandsPerPage).toInt()

        if (page < 1 || page > totalPages) {
            MessageRegistry.Commands.Help.sendInvalidPage(executor)
            return
        }

        MessageRegistry.Commands.Help.sendSubcommandListHeader(
            executor,
            command.name,
            page,
            totalPages,
        )
        showCommandDetails(executor, command)
        MessageRegistry.Commands.Help.sendSeparator(executor)
        MessageRegistry.Commands.Help.Fields.sendSubcommands(executor)

        val startIndex = (page - 1) * subCommandsPerPage
        val endIndex =
            (startIndex + subCommandsPerPage).coerceAtMost(
                uniqueSubCommands.size
            )

        for (i in startIndex until endIndex) {
            val subCommand = uniqueSubCommands[i]
            val pathStr = subCommand.path.joinToString(" ")
            MessageRegistry.Commands.Help.Fields.sendSubcommand(
                executor,
                pathStr,
            )
            if (subCommand.aliases.isNotEmpty()) {
                MessageRegistry.Commands.Help.Fields.sendAliases(
                    executor,
                    subCommand.aliases.joinToString(", "),
                )
            }
            MessageRegistry.Commands.Help.Fields.sendDescription(
                executor,
                subCommand.description,
            )
            MessageRegistry.Commands.Help.Fields.sendUsage(
                executor,
                subCommand.usage,
            )
            if (subCommand.permission.isNotEmpty()) {
                MessageRegistry.Commands.Help.Fields.sendPermission(
                    executor,
                    subCommand.permission,
                )
            }
            if (i < endIndex - 1) {
                MessageRegistry.Commands.Help.sendSeparator(executor)
            }
        }
        MessageRegistry.Commands.Help.sendFooter(executor, page, totalPages)
    }

    private fun showCommandDetails(
        executor: CommandExecutor,
        command: CommandInfo,
    ) {
        MessageRegistry.Commands.Help.Fields.sendCommand(executor, command.name)
        if (command.aliases.isNotEmpty()) {
            MessageRegistry.Commands.Help.Fields.sendAliases(
                executor,
                command.aliases.joinToString(", "),
            )
        }
        MessageRegistry.Commands.Help.Fields.sendDescription(
            executor,
            command.description,
        )
        MessageRegistry.Commands.Help.Fields.sendUsage(executor, command.usage)
        if (command.permission.isNotEmpty()) {
            MessageRegistry.Commands.Help.Fields.sendPermission(
                executor,
                command.permission,
            )
        }
    }
}
