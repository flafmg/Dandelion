package org.dandelion.classic.commands.manager

import org.dandelion.classic.commands.BanCommand
import org.dandelion.classic.commands.ClientsCommand
import org.dandelion.classic.commands.HelpCommand
import org.dandelion.classic.commands.KickCommand
import org.dandelion.classic.commands.LevelCommand
import org.dandelion.classic.commands.OnlineCommand
import org.dandelion.classic.commands.PermissionCommand
import org.dandelion.classic.commands.PlayerInfoCommand
import org.dandelion.classic.commands.SayCommand
import org.dandelion.classic.commands.SayRawCommand
import org.dandelion.classic.commands.ServerInfoCommand
import org.dandelion.classic.commands.StopCommand
import org.dandelion.classic.commands.UnbanCommand
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.server.Console

/**
 * CommandRegistry manages the registration, lookup, and unregistration of commands.
 */
object CommandRegistry {
    private val commands = mutableMapOf<String, CommandInfo>()


    internal fun init(){
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
    }

    /**
     * Registers a command and its aliases in the registry.
     *
     * @param command The command to register.
     * @return True if the command was registered successfully, false otherwise.
     */
    fun register(command: Command): Boolean{
        val commandInfo = CommandProcessor.processCommand(command) ?: return false
        commands[commandInfo.name] = commandInfo
        commandInfo.aliases.forEach{ alias ->
            commands[alias] = commandInfo
        }
        Console.debugLog("Command ${commandInfo.name} registered")
        return true
    }

    /**
     * Unregisters a command and its aliases from the registry.
     *
     * @param commandName The name or alias of the command to unregister.
     * @return True if the command was unregistered successfully, false otherwise.
     */
    fun unregister(commandName: String): Boolean{
        val commandInfo = commands[commandName]
        if(commandInfo == null){
            Console.errLog("Cannot unregister command $commandName, it doesnt exist")
            return false
        }
        commands.remove(commandInfo.name)
        commandInfo.aliases.forEach { alias ->
            commands.remove(alias)
        }
        Console.debugLog("Command ${commandInfo.name} unregistered")
        return true
    }

    /**
     * Unregisters all commands from the registry.
     */
    private fun unregisterAll() {
        commands.clear()
    }

    /**
     * Executes a command line by parsing the command name and arguments, then dispatching to the appropriate command handler.
     *
     * @param commandLine The full command line string entered by the user.
     * @param executor The executor that will run the command (e.g., player or console).
     */
    fun execute(commandLine: String, executor: CommandExecutor){
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

    /**
     * Executes a command by name with the provided arguments and executor.
     *
     * @param name The name or alias of the command to execute.
     * @param executor The executor that will run the command.
     * @param args Arguments to pass to the command.
     */
    fun execute(name: String, executor: CommandExecutor, args: Array<String>) {
        val command = commands[name] ?: commands.values.find { it.aliases.contains(name) }

        if (command == null) {
            executor.sendMessage("Unknown command. Type /help for a list of commands.")
            return
        }

        CommandProcessor.executeCommand(command, executor, args)
    }

    /**
     * Returns a list of all registered commands.
     *
     * @return List of CommandInfo for all registered commands.
     */
    fun getCommands(): List<CommandInfo> = commands.values.distinct()
}