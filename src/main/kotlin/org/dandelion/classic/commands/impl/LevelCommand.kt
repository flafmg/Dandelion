package org.dandelion.classic.server.commands.impl

import org.dandelion.classic.server.commands.model.Command
import org.dandelion.classic.server.commands.model.CommandExecutor
import org.dandelion.classic.server.data.level.manager.LevelManager
import org.dandelion.classic.server.data.level.generator.manager.GeneratorManager
import org.dandelion.classic.server.data.player.model.Player

class LevelCommand : Command {
    override val name = "level"
    override val description = "Level management commands."
    override val permission = "dandelion.server.level"

    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("Usage: /level <list|go|create> ...")
            return
        }
        when (args[0].lowercase()) {
            "list" -> {
                if (!executor.hasPermission("dandelion.server.level.list")) {
                    executor.sendMessage("&cYou do not have permission to use this command.")
                    return
                }
                handleList(executor)
            }
            "go" -> {
                if (!executor.hasPermission("dandelion.server.level.go")) {
                    executor.sendMessage("&cYou do not have permission to use this command.")
                    return
                }
                handleGo(executor, args.drop(1))
            }
            "create" -> {
                if (!executor.hasPermission("dandelion.server.level.create")) {
                    executor.sendMessage("&cYou do not have permission to use this command.")
                    return
                }
                handleCreate(executor, args.drop(1))
            }
            else -> executor.sendMessage("&cUnknown subcommand. Use /level <list|go|create>")
        }
    }

    private fun handleList(executor: CommandExecutor) {
        val levels = LevelManager.getAllLevels()
        if (levels.isEmpty()) {
            executor.sendMessage("No levels loaded.")
            return
        }
        executor.sendMessage("Loaded levels:")
        for (level in levels) {
            val count = level.getPlayers().size
            executor.sendMessage("- ${level.id} (${count} online)")
        }
    }

    private fun handleGo(executor: CommandExecutor, args: List<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("Usage: /level go <level>")
            return
        }
        val levelName = args[0]
        val level = LevelManager.getLevel(levelName)
        if (level == null) {
            executor.sendMessage("Level '$levelName' does not exist.")
            return
        }
        if (executor !is Player) {
            executor.sendMessage("Only players can use this command.")
            return
        }
        executor.changeLevel(level)
    }

    private fun handleCreate(executor: CommandExecutor, args: List<String>) {
        if (args.size < 9) {
            executor.sendMessage("Usage: /level create <id> <x> <y> <z> <spawnx> <spawny> <spawnz> <generator> <seed>")
            return
        }
        val id = args[0]
        val x = args[1].toShortOrNull()
        val y = args[2].toShortOrNull()
        val z = args[3].toShortOrNull()
        val spawnx = args[4].toFloatOrNull()
        val spawny = args[5].toFloatOrNull()
        val spawnz = args[6].toFloatOrNull()
        val generator = args[7]
        val seed = args[8].toLongOrNull() ?: 0L

        if (x == null || y == null || z == null || spawnx == null || spawny == null || spawnz == null) {
            executor.sendMessage("Invalid coordinates or size.")
            return
        }
        if (LevelManager.getLevel(id) != null) {
            executor.sendMessage("A level with this id already exists.")
            return
        }
        val gen = GeneratorManager.get(generator)
        if (gen == null) {
            executor.sendMessage("Generator '$generator' not found.")
            return
        }
        if (spawnx < 0 || spawnx >= x || spawny < 0 || spawny >= y || spawnz < 0 || spawnz >= z) {
            executor.sendMessage("Spawn coordinates are out of bounds.")
            return
        }
        LevelManager.createLevel(id, x, y, z, spawnx, spawny, spawnz, seed, generator)
        executor.sendMessage("Level '$id' created successfully.")
    }
}

