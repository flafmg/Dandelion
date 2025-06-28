package org.dandelion.classic.commands

import org.dandelion.classic.commands.annotations.*
import org.dandelion.classic.player.Player
import org.dandelion.classic.server.Console
import java.lang.reflect.Method

data class SubCommandInfo(
    val name: String,
    val aliases: List<String>,
    val description: String,
    val usage: String,
    val permission: String,
    val method: Method,
    val path: List<String>
)
data class CommandInfo(
    val name: String,
    val aliases: List<String>,
    val description: String,
    val usage: String,
    val permission: String,
    val clazz: Class<*>,
    val instance: Any,
    val executeMethod: Method?,
    val subCommands: Map<String, SubCommandInfo>
)
internal object CommandProcessor {
    fun processCommand(clazz: Class<*>): CommandInfo?{
        val commandAnnotation = clazz.getAnnotation(Command::class.java)
        if(commandAnnotation == null){
            Console.errLog("Could not register command '${clazz.name}', it doesnt have @Command annotation")
            return null
        }
        val instance = clazz.getDeclaredConstructor().newInstance()
        val executeMethods = clazz.declaredMethods.filter { it.isAnnotationPresent(OnExecute::class.java) }
        if (executeMethods.size > 1) {
            Console.errLog("Could not register command '${clazz.name}', it contains more than one @OnExecute")
        }
        val executeMethod = executeMethods.firstOrNull()
        val subCommands = mutableMapOf<String, SubCommandInfo>()

        processSubCommands(clazz.declaredMethods.toList(), subCommands, emptyList())

        val permission = executeMethod?.getAnnotation(RequirePermission::class.java)?.permission ?: ""

        return CommandInfo(
            name = commandAnnotation.name,
            aliases = commandAnnotation.aliases.toList(),
            description = commandAnnotation.description,
            usage = commandAnnotation.usage,
            clazz = clazz,
            instance = instance,
            executeMethod = executeMethod,
            subCommands = subCommands,
            permission = permission
        )
    }

    private fun processSubCommands(methods: List<Method>, subCommands: MutableMap<String, SubCommandInfo>, path: List<String>){
        methods.filter { it.isAnnotationPresent(OnSubCommand::class.java) }.forEach { method ->
            val annotation = method.getAnnotation(OnSubCommand::class.java)
            val fullPath = path + annotation.name
            val key = fullPath.joinToString(" ")
            val permission = method.getAnnotation(RequirePermission::class.java)?.permission ?: ""

            val subCommandInfo = SubCommandInfo(
                name = annotation.name,
                aliases = annotation.aliases.toList(),
                description = annotation.description,
                usage = annotation.usage,
                method = method,
                path = fullPath,
                permission = permission
            )

            subCommands[key] = subCommandInfo
            annotation.aliases.forEach { alias ->
                val aliasPath = path + alias
                val aliasKey = aliasPath.joinToString(" ")
                subCommands[aliasKey] = subCommandInfo.copy(path = aliasPath)
            }
        }
    }
    fun executeCommand(
        commandInfo: CommandInfo,
        executor: CommandExecutor,
        args: Array<String>
    ): Boolean {
        if (args.isNotEmpty()) {
            val subCommandPath = findSubCommand(commandInfo.subCommands, args)
            if (subCommandPath != null) {
                val remainingArgs = args.drop(subCommandPath.path.size).toTypedArray()
                return executeMethod(subCommandPath.method, commandInfo.instance, executor, remainingArgs, subCommandPath.permission)
            }
        }

        if (commandInfo.executeMethod != null) {
            return executeMethod(commandInfo.executeMethod, commandInfo.instance, executor, args, commandInfo.permission)
        }

        executor.sendMessage("&cCommand not found. Available subcommands: ${commandInfo.subCommands.keys.joinToString(", ")}")
        return false
    }

    private fun findSubCommand(subCommands: Map<String, SubCommandInfo>, args: Array<String>): SubCommandInfo? {
        for (i in args.size downTo 1) {
            val path = args.take(i).joinToString(" ")
            subCommands[path]?.let { return it }
        }
        return null
    }

    private fun executeMethod(
        method: Method,
        instance: Any,
        executor: CommandExecutor,
        args: Array<String>,
        permission: String
    ): Boolean {
        if (!validatePermissions(permission, executor)) return false
        if (!validateExecutorType(method, executor)) return false
        val processedArgs = processReferSelf(method, executor, args)
        if (!validateArguments(method, executor, processedArgs)) return false

        try {
            method.invoke(instance, executor, processedArgs)
            return true
        } catch (e: Exception) {
            executor.sendMessage("&cError executing command: ${e.message}")
            return false
        }
    }

    private fun processReferSelf(method: Method, executor: CommandExecutor, args: Array<String>): Array<String> {
        val referSelf = method.getAnnotation(ReferSelf::class.java) ?: return args

        if (executor !is Player) return args

        val argPosition = referSelf.argPosition

        if (argPosition >= args.size) {
            val newArgs = args.toMutableList()
            while (newArgs.size <= argPosition) {
                newArgs.add("")
            }
            newArgs[argPosition] = executor.name
            return newArgs.toTypedArray()
        }

        return args
    }

    private fun validatePermissions(permission: String, executor: CommandExecutor): Boolean {
        if (permission.isNotEmpty() && !executor.hasPermission(permission)) {
            executor.sendMessage("&cYou don't have permission to execute this command")
            return false
        }
        return true
    }

    private fun validateExecutorType(method: Method, executor: CommandExecutor): Boolean {
        if (method.isAnnotationPresent(RequirePlayer::class.java) && executor !is Player) {
            executor.sendMessage("&cThis command can only be executed by players")
            return false
        }
        if (method.isAnnotationPresent(RequireConsole::class.java) && executor !is Console) {
            executor.sendMessage("&cThis command can only be executed by console")
            return false
        }
        return true
    }

    private fun validateArguments(method: Method, executor: CommandExecutor, args: Array<String>): Boolean {
        val argRange = method.getAnnotation(ArgRange::class.java)
        if (argRange != null) {
            when {
                args.size < argRange.min -> {
                    executor.sendMessage("&cThis command expects at least ${argRange.min} arguments")
                    return false
                }
                args.size > argRange.max -> {
                    executor.sendMessage("&cThis command expects at most ${argRange.max} arguments")
                    return false
                }
            }
        }
        return true
    }
}