package org.dandelion.classic.commands.manager

import TeleportCommand
import org.dandelion.classic.commands.BanCommand
import org.dandelion.classic.commands.BlockCommand
import org.dandelion.classic.commands.ClientsCommand
import org.dandelion.classic.commands.HelpCommand
import org.dandelion.classic.commands.KickCommand
import org.dandelion.classic.commands.LevelCommand
import org.dandelion.classic.commands.OnlineCommand
import org.dandelion.classic.commands.PermissionCommand
import org.dandelion.classic.commands.PlayerInfoCommand
import org.dandelion.classic.commands.PluginCommand
import org.dandelion.classic.commands.ReloadCommand
import org.dandelion.classic.commands.SayCommand
import org.dandelion.classic.commands.SayRawCommand
import org.dandelion.classic.commands.ServerInfoCommand
import org.dandelion.classic.commands.StopCommand
import org.dandelion.classic.commands.UnbanCommand
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.server.Console

object CommandRegistry {
    private val commands = mutableMapOf<String, CommandInfo>()

    internal fun init() {
        register(KickCommand())
        register(BanCommand())
        register(UnbanCommand())
        register(HelpCommand())
        register(PlayerInfoCommand())
        register(ServerInfoCommand())
        register(SayCommand())
        register(SayRawCommand())
        register(StopCommand())
        register(LevelCommand())
        register(PermissionCommand())
        register(OnlineCommand())
        register(ClientsCommand())
        register(PluginCommand())
        register(BlockCommand())
        register(TeleportCommand())
        register(ReloadCommand())
    }

    @JvmStatic
    fun register(command: Command): Boolean {
        val commandInfo =
            CommandProcessor.processCommand(command) ?: return false
        commands[commandInfo.name] = commandInfo
        commandInfo.aliases.forEach { alias -> commands[alias] = commandInfo }
        Console.debugLog("Command ${commandInfo.name} registered")
        return true
    }

    @JvmStatic
    fun unregister(commandName: String): Boolean {
        val commandInfo = commands[commandName]
        if (commandInfo == null) {
            Console.errLog(
                "Cannot unregister command $commandName, it doesnt exist"
            )
            return false
        }
        commands.remove(commandInfo.name)
        commandInfo.aliases.forEach { alias -> commands.remove(alias) }
        Console.debugLog("Command ${commandInfo.name} unregistered")
        return true
    }

    private fun unregisterAll() {
        commands.clear()
    }

    @JvmStatic
    fun execute(commandLine: String, executor: CommandExecutor) {
        var cleanCommand = commandLine.trim()
        if (cleanCommand.startsWith("/")) {
            cleanCommand = cleanCommand.drop(1)
        }

        val parts = cleanCommand.split("\\s+".toRegex())
        if (parts.isEmpty() || parts[0].isBlank()) return

        val commandName = parts[0]
        val args = parts.drop(1).toTypedArray()
        execute(commandName, executor, args)
    }

    @JvmStatic
    fun execute(name: String, executor: CommandExecutor, args: Array<String>) {
        val command =
            commands[name] ?: commands.values.find { it.aliases.contains(name) }

        if (command == null) {
            executor.sendMessage(
                "Unknown command. Type /help for a list of commands."
            )
            return
        }

        CommandProcessor.executeCommand(command, executor, args)
    }

    fun getCommands(): List<CommandInfo> = commands.values.distinct()
}
