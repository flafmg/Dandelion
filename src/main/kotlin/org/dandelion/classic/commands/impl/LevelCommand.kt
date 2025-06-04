package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.data.level.manager.LevelManager
import org.dandelion.classic.data.level.generator.manager.GeneratorManager
import org.dandelion.classic.data.level.io.impl.DandelionLevelSerializer
import org.dandelion.classic.data.player.model.Player

class LevelCommand : Command {
    override val name = "level"
    override val description = "Level management commands."
    override val permission = "dandelion.server.level"

    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("Usage: /level <list|go|create|unload|load> ...")
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
            "unload" -> {
                if (!executor.hasPermission("dandelion.server.level.unload")) {
                    executor.sendMessage("&cYou do not have permission to use this command.")
                    return
                }
                handleUnload(executor, args.drop(1))
            }
            "load" -> {
                if (!executor.hasPermission("dandelion.server.level.load")) {
                    executor.sendMessage("&cYou do not have permission to use this command.")
                    return
                }
                handleLoad(executor, args.drop(1))
            }
            "setspawn" -> {
                if (!executor.hasPermission("dandelion.server.level.setspawn")) {
                    executor.sendMessage("&cYou do not have permission to use this command.")
                    return
                }
                handleSetSpawn(executor, args.drop(1))
            }
            else -> executor.sendMessage("&cUnknown subcommand. Use /level <list|go|create|unload|load>")
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
            executor.sendMessage("Usage: /level create <id> <x> <y> <z> <spawnx> <spawny> <spawnz> <generator> <seed> [params]")
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
        val params = if (args.size > 9) args.drop(9).joinToString(" ") else ""

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
        LevelManager.createLevel(id, x, y, z, spawnx, spawny, spawnz, seed, generator, params)
        executor.sendMessage("Level '$id' created successfully.")
    }

    private fun handleUnload(executor: CommandExecutor, args: List<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("Usage: /level unload <id>")
            return
        }
        val id = args[0]
        if (LevelManager.getLevel(id) == null) {
            executor.sendMessage("Level '$id' is not loaded.")
            return
        }
        if (LevelManager.getDefaultJoinLevel()?.id == id) {
            executor.sendMessage("You cannot unload the default join level.")
            return
        }
        LevelManager.getLevel(id)?.kickAll("Level unloaded")
        LevelManager.unloadLevel(id)
        executor.sendMessage("Level '$id' unloaded.")
    }

    private fun handleLoad(executor: CommandExecutor, args: List<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("Usage: /level load <id>")
            return
        }
        val id = args[0]
        if (LevelManager.getLevel(id) != null) {
            executor.sendMessage("Level '$id' is already loaded.")
            return
        }
        val path = "levels/$id.dlvl"
        val file = java.io.File(path)
        if (!file.exists()) {
            executor.sendMessage("Level file '$id.dlvl' not found in levels directory.")
            return
        }
        try {
            LevelManager.loadLevel(id)
            executor.sendMessage("Level '$id' loaded.")
        } catch (e: Exception) {
            executor.sendMessage("Failed to load level '$id': ${e.message}")
        }
    }

    private fun handleSetSpawn(executor: CommandExecutor, args: List<String>) {
        if (args.size < 4) {
            executor.sendMessage("Usage: /level setspawn <name> <x> <y> <z>")
            return
        }
        val id = args[0]
        val x = args[1].toFloatOrNull()
        val y = args[2].toFloatOrNull()
        val z = args[3].toFloatOrNull()
        if (x == null || y == null || z == null) {
            executor.sendMessage("Invalid coordinates.")
            return
        }
        val level = LevelManager.getLevel(id)
        if (level == null) {
            executor.sendMessage("Level '$id' not found.")
            return
        }
        level.spawnX = x
        level.spawnY = y
        level.spawnZ = z
        try {
            level.serialize(DandelionLevelSerializer(), "levels/${level.id}.dlvl")
            executor.sendMessage("Spawn point for level '$id' set to ($x, $y, $z) and saved.")
        } catch (e: Exception) {
            executor.sendMessage("Spawn set, but failed to save: ${e.message}")
        }
    }
}
