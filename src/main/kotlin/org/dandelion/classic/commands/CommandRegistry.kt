package org.dandelion.classic.commands

import org.dandelion.classic.commands.impl.KickCommand
import org.dandelion.classic.server.Console

object CommandRegistry {
    private val commands = mutableMapOf<String, CommandInfo>()

    internal fun init(){
        register(KickCommand::class.java)
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
    fun execute(commandLine: String, executor: CommandExecutor): Boolean {
        var cleanCommand = commandLine.trim()
        if (cleanCommand.startsWith("/")) {
            cleanCommand = cleanCommand.drop(1)
        }

        val parts = cleanCommand.split("\\s+".toRegex())
        if (parts.isEmpty() || parts[0].isBlank()) return false

        val commandName = parts[0]
        val args = parts.drop(1).toTypedArray()
        return execute(commandName, executor, args)
    }
    fun execute(commandName: String, executor: CommandExecutor, args: Array<String>): Boolean {
        Console.debugLog("executing $commandName")
        val commandInfo = commands[commandName] ?: run {
            executor.sendMessage("&cUnknown command")
            return false
        }

        return CommandProcessor.executeCommand(commandInfo, executor, args)
    }
    fun getCommands(): List<CommandInfo> = commands.values.distinct()
}