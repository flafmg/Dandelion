package org.dandelion.server.commands.manager

import TeleportCommand
import org.dandelion.server.commands.BanCommand
import org.dandelion.server.commands.BlockCommand
import org.dandelion.server.commands.ClientsCommand
import org.dandelion.server.commands.HelpCommand
import org.dandelion.server.commands.KickCommand
import org.dandelion.server.commands.LevelCommand
import org.dandelion.server.commands.ModelCommand
import org.dandelion.server.commands.OnlineCommand
import org.dandelion.server.commands.PermissionCommand
import org.dandelion.server.commands.PlayerInfoCommand
import org.dandelion.server.commands.PluginCommand
import org.dandelion.server.commands.ReloadCommand
import org.dandelion.server.commands.SayCommand
import org.dandelion.server.commands.SayRawCommand
import org.dandelion.server.commands.ServerInfoCommand
import org.dandelion.server.commands.StopCommand
import org.dandelion.server.commands.UnbanCommand
import org.dandelion.server.commands.model.Command
import org.dandelion.server.commands.model.CommandExecutor
import org.dandelion.server.server.Console

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
        register(ModelCommand())
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
