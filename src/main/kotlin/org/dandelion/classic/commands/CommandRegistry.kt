package org.dandelion.classic.commands

import org.dandelion.classic.commands.impl.BanCommand
import org.dandelion.classic.commands.impl.HelpCommand
import org.dandelion.classic.commands.impl.KickCommand
import org.dandelion.classic.commands.impl.PlayerInfoCommand
import org.dandelion.classic.commands.impl.SayCommand
import org.dandelion.classic.commands.impl.SayRawCommand
import org.dandelion.classic.commands.impl.ServerInfoCommand
import org.dandelion.classic.commands.impl.StopCommand
import org.dandelion.classic.commands.impl.UnbanCommand
import org.dandelion.classic.server.Console

object CommandRegistry {
    private val commands = mutableMapOf<String, CommandInfo>()

    internal fun init(){
        register(KickCommand::class.java)
        register(BanCommand::class.java)
        register(UnbanCommand::class.java)
        register(HelpCommand::class.java)
        register(PlayerInfoCommand::class.java)
        register(ServerInfoCommand::class.java)
        register(SayCommand::class.java)
        register(SayRawCommand::class.java)
        register(StopCommand::class.java)
    }
    internal fun shutdown(){
        unregisterAll()
    }

    fun register(clazz: Class<*>): Boolean{
        val commandInfo = CommandProcessor.processCommand(clazz) ?: return false
        commands[commandInfo.name] = commandInfo
        commandInfo.aliases.forEach{ alias ->
            commands[alias] = commandInfo
        }
        Console.debugLog("Command ${commandInfo.name} registered")
        return true
    }
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
    private fun unregisterAll() {
        commands.clear()
    }
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
    fun execute(name: String, executor: CommandExecutor, args: Array<String>) {
        val command = commands[name] ?: commands.values.find { it.aliases.contains(name) }

        if (command == null) {
            executor.sendMessage("Unknown command. Type /help for a list of commands.")
            return
        }

        CommandProcessor.executeCommand(command, executor, args)
    }

    fun getCommands(): List<CommandInfo> = commands.values.distinct()
}