package org.dandelion.classic.commands

import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.OnSubCommand
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
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
            executor.sendMessage("&cNo plugins are currently loaded.")
            return
        }
        executor.sendMessage("&eLoaded plugins:")
        plugins.values.forEach {
            executor.sendMessage("&e${it.info.name} &b(v${it.info.version}): &7${it.info.description}")
        }
    }

    @OnSubCommand(name = "info", description = "Show plugin info", usage = "/plugin info <name>")
    @RequirePermission("dandelion.plugin.manage")
    fun pluginInfo(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /plugin info <name>")
            return
        }
        val plugin = PluginRegistry.getPlugin(args[0])
        if (plugin == null) {
            executor.sendMessage("&cPlugin '&7${args[0]}&c' not found.")
            return
        }
        val info = plugin.info
        executor.sendMessage("&ePlugin Information: &e${info.name}")
        executor.sendMessage("&eVersion: &e${info.version}")
        executor.sendMessage("&eDescription: &7${info.description}")
        executor.sendMessage("&eAuthors: &7${info.authors.joinToString(", ")}")
        if (info.dependencies.isNotEmpty()) {
            executor.sendMessage("&eDependencies: &7${info.dependencies.joinToString(", ") { it.first + (it.second?.let { v -> " (v$v)" } ?: "") }}")
        }
    }

    @OnSubCommand(name = "load", description = "Load a plugin", usage = "/plugin load <name>")
    @RequirePermission("dandelion.plugin.manage")
    fun loadPlugin(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /plugin load <name>")
            return
        }
        val name = args[0]
        if (PluginRegistry.getPlugin(name) != null) {
            executor.sendMessage("&cPlugin '&7$name&c' is already loaded.")
            return
        }
        val success = PluginRegistry.loadPluginByName(name)
        if (success) {
            executor.sendMessage("&aPlugin '&7$name&a' loaded successfully.")
        } else {
            executor.sendMessage("&cFailed to load plugin '&7$name&c'. Check if the jar exists and is valid.")
        }
    }

    @OnSubCommand(name = "unload", description = "Unload a plugin", usage = "/plugin unload <name>")
    @RequirePermission("dandelion.plugin.manage")
    fun unloadPlugin(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /plugin unload <name>")
            return
        }
        val name = args[0]
        val plugin = PluginRegistry.getPlugin(name)
        if (plugin == null) {
            executor.sendMessage("&cPlugin '&7$name&c' not found.")
            return
        }
        val success = PluginRegistry.unloadPluginByName(name)
        if (success) {
            executor.sendMessage("&aPlugin '&7$name&a' unloaded successfully.")
        } else {
            executor.sendMessage("&cFailed to unload plugin '&7$name&c'.")
        }
    }

    @OnSubCommand(name = "reload", description = "Reload a plugin or all plugins", usage = "/plugin reload <name|all>")
    @RequirePermission("dandelion.plugin.manage")
    fun reloadPlugin(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /plugin reload <name|all>")
            return
        }
        val target = args[0]
        if (target.equals("all", ignoreCase = true)) {
            PluginRegistry.shutdown()
            PluginRegistry.init()
            executor.sendMessage("&aAll plugins reloaded successfully.")
        } else {
            val plugin = PluginRegistry.getPlugin(target)
            if (plugin == null) {
                executor.sendMessage("&cPlugin '&7$target&c' not found.")
                return
            }
            // Not implemented: dynamic reload of single plugin
            executor.sendMessage("&cDynamic reload of a single plugin is not supported yet. Use '/plugin reload all'.")
        }
    }

    @OnExecute
    fun showAvailableSubCommands(executor: CommandExecutor, args: Array<String>) {
        executor.sendMessage("&eAvailable SubCommands: &7list, info, load, unload, reload")
    }
}
