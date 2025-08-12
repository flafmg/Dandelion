package org.dandelion.classic.commands

import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.OnSubCommand
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.server.MessageRegistry
import org.dandelion.classic.plugins.manager.PluginRegistry

@CommandDef(
    name = "plugin",
    description = "Manage server plugins.",
    usage = "/plugin <subcommand>",
    aliases = ["pl"]
)
class PluginCommand : Command {

    @OnSubCommand(name = "list", description = "List all loaded plugins", usage = "/plugin list")
    @RequirePermission("dandelion.plugin.list")
    fun listPlugins(executor: CommandExecutor, args: Array<String>) {
        val plugins = PluginRegistry.getAllPlugins()
        if (plugins.isEmpty()) {
            MessageRegistry.Commands.Plugin.List.sendNoPlugins(executor)
            return
        }
        MessageRegistry.Commands.Plugin.List.sendHeader(executor)
        plugins.values.forEach {
            MessageRegistry.Commands.Plugin.List.sendPlugin(executor, it.info.name, it.info.version, it.info.description)
        }
    }

    @OnSubCommand(name = "info", description = "Show plugin info", usage = "/plugin info <name>")
    @RequirePermission("dandelion.plugin.manage")
    fun pluginInfo(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Plugin.Info.sendUsage(executor)
            return
        }
        val plugin = PluginRegistry.getPlugin(args[0])
        if (plugin == null) {
            MessageRegistry.Commands.Plugin.Info.sendNotFound(executor, args[0])
            return
        }
        val info = plugin.info
        MessageRegistry.Commands.Plugin.Info.sendHeader(executor, info.name)
        MessageRegistry.Commands.Plugin.Info.sendVersion(executor, info.version)
        MessageRegistry.Commands.Plugin.Info.sendDescription(executor, info.description)
        MessageRegistry.Commands.Plugin.Info.sendAuthors(executor, info.authors.joinToString(", "))
        if (info.dependencies.isNotEmpty()) {
            val deps = info.dependencies.joinToString(", ") { it.first + (it.second?.let { v -> " (v$v)" } ?: "") }
            MessageRegistry.Commands.Plugin.Info.sendDependencies(executor, deps)
        }
    }

    @OnSubCommand(name = "load", description = "Load a plugin", usage = "/plugin load <name>")
    @RequirePermission("dandelion.plugin.manage")
    fun loadPlugin(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Plugin.Load.sendUsage(executor)
            return
        }
        val name = args[0]
        val success = PluginRegistry.loadPluginByName(name)
        if (success) {
            MessageRegistry.Commands.Plugin.Load.sendSuccess(executor, name)
        } else {
            MessageRegistry.Commands.Plugin.Load.sendFailed(executor, name)
        }
    }

    @OnSubCommand(name = "unload", description = "Unload a plugin", usage = "/plugin unload <name>")
    @RequirePermission("dandelion.plugin.manage")
    fun unloadPlugin(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Plugin.Unload.sendUsage(executor)
            return
        }
        val name = args[0]
        val success = PluginRegistry.unloadPluginByName(name)
        if (success) {
            MessageRegistry.Commands.Plugin.Unload.sendSuccess(executor, name)
        } else {
            MessageRegistry.Commands.Plugin.Unload.sendFailed(executor, name)
        }
    }

    @OnSubCommand(name = "reload", description = "Reload a plugin or all plugins", usage = "/plugin reload <name|all>")
    @RequirePermission("dandelion.plugin.manage")
    fun reloadPlugin(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Plugin.Reload.sendUsage(executor)
            return
        }
        val target = args[0]
        if (target.equals("all", ignoreCase = true)) {
            PluginRegistry.shutdown()
            PluginRegistry.init()
            MessageRegistry.Commands.Plugin.Reload.sendSuccessAll(executor)
        } else {
            val success = PluginRegistry.reloadPluginByName(target)
            if (success) {
                MessageRegistry.Commands.Plugin.Reload.sendSuccessSingle(executor, target)
            } else {
                MessageRegistry.Commands.Plugin.Reload.sendFailed(executor, target)
            }
        }
    }

    @OnExecute
    fun showAvailableSubCommands(executor: CommandExecutor, args: Array<String>) {
        MessageRegistry.Commands.Plugin.sendSubcommandsAvailable(executor)
    }
}