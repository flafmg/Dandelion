package org.dandelion.classic.server.commands.manager

import org.dandelion.classic.server.commands.impl.*
import org.dandelion.classic.server.commands.model.Command
import org.dandelion.classic.server.commands.model.CommandExecutor
import org.dandelion.classic.server.util.Logger

object CommandRegistry {
    private val commands = mutableMapOf<String, Command>()
    var prefix: String = "/"

    init {
        register(LevelCommand())
        register(OnlineCommand())
        register(KickCommand())
        register(StopCommand())
        register(TeleportCommand())
        register(OpCommand())
        register(DeopCommand())
        register(BanCommand())
        register(UnbanCommand())
        register(AddPermCommand())
        register(RemovePermCommand())
        register(GetPermsCommand())
        register(CommandsCommand())
    }

    fun register(command: Command) {
        commands[command.name.lowercase()] = command
        Logger.debugLog("Command ${command.name.lowercase()} registered")
    }

    fun unregister(command: Command) {
        commands.remove(command.name.lowercase())
        Logger.debugLog("Command ${command.name.lowercase()} unregistered")
    }
    fun execute(raw: String, executor: CommandExecutor) {
        if (!raw.startsWith(prefix)) return
        val split = raw.removePrefix(prefix).trim().split(" ")
        if (split.isEmpty()) return
        val name = split[0].lowercase()
        val args = if (split.size > 1) split.drop(1) else emptyList()
        val command = commands.values.find {
            it.name.lowercase() == name || it.aliases.map { alias -> alias.lowercase() }.contains(name)
        }
        if (command == null) {
            executor.sendMessage("&cUnknown command.")
            return
        }
        if (!executor.hasPermission(command.permission)) {
            executor.sendMessage("&cYou do not have permission to use this command.")
            return
        }
        command.onExecute(executor, args)
    }


    fun getAll(): List<Command> = commands.values.toList()
}
