package org.dandelion.classic.commands

import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.OnSubCommand
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.level.Levels
import org.dandelion.classic.level.generator.GeneratorRegistry
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.server.Console
import org.dandelion.classic.types.Color
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.SVec
import kotlin.math.ceil

@CommandDef(
    name = "level",
    description = "Manage server levels",
    usage = "/level <subcommand>",
    aliases = ["l", "lvl"]
)
class LevelCommand : Command {

    @OnSubCommand(name = "create", description = "Create a new level", usage = "/level create <id> <description> <sizeX> <sizeY> <sizeZ> <generator> [params]")
    @RequirePermission("dandelion.level.create")
    fun createLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 6) {
            executor.sendMessage("&cUsage: /level create <id> <description> <sizeX> <sizeY> <sizeZ> <generator> [params]")
            return
        }

        val id = args[0]
        val description = args[1]
        val sizeX = args[2].toIntOrNull()
        val sizeY = args[3].toIntOrNull()
        val sizeZ = args[4].toIntOrNull()
        val generatorId = args[5]
        val params = if (args.size > 6) args.slice(6 until args.size).joinToString(" ") else ""

        if (sizeX == null || sizeY == null || sizeZ == null) {
            executor.sendMessage("&cInvalid dimensions. Please provide valid numbers.")
            return
        }
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            executor.sendMessage("&cDimensions must be greater than 0.")
            return
        }

        val generator = GeneratorRegistry.getGenerator(generatorId)
        if (generator == null) {
            executor.sendMessage("&cGenerator '$generatorId' not found.")
            executor.sendMessage("&7Available generators: ${GeneratorRegistry.getAllGenerators().joinToString(", ") { it.id }}")
            return
        }

        val author = when (executor) {
            is Player -> executor.name
            is Console -> "Console"
            else -> "Unknown"
        }
        val spawn = Position(sizeX / 2f, sizeY / 2f, sizeZ / 2f)
        val size = SVec(sizeX.toShort(), sizeY.toShort(), sizeZ.toShort())

        val level = Levels.createLevel(id, author, description, size, spawn, generator, params)
        if (level != null) {
            executor.sendMessage("&aLevel '&7$id&a' created successfully.")
        } else {
            executor.sendMessage("&cFailed to create level '&7$id&c'. Check if the ID already exists.")
        }
    }

    @OnSubCommand(name = "load", description = "Load a level from disk", usage = "/level load <levelId>")
    @RequirePermission("dandelion.level.load")
    @ArgRange(max = 1)
    fun loadLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /level load <levelId>")
            return
        }

        val levelId = args[0]
        if (Levels.loadLevel(levelId)) {
            executor.sendMessage("&aLevel '&7$levelId&a' loaded successfully.")
        } else {
            executor.sendMessage("&cFailed to load level '&7$levelId&c'. Check if the file exists or if the level is already loaded.")
        }
    }

    @OnSubCommand(name = "unload", description = "Unload a level from memory", usage = "/level unload <levelId>")
    @RequirePermission("dandelion.level.load")
    @ArgRange(max = 1)
    fun unloadLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /level unload <levelId>")
            return
        }

        val levelId = args[0]
        if (Levels.unloadLevel(levelId)) {
            executor.sendMessage("&aLevel '&7$levelId&a' unloaded successfully.")
        } else {
            executor.sendMessage("&cFailed to unload level '&7$levelId&c'. Check if the level exists.")
        }
    }

    @OnSubCommand(name = "delete", description = "Delete a level permanently", usage = "/level delete <levelId> [confirm]")
    @RequirePermission("dandelion.level.delete")
    @ArgRange(max = 2)
    fun deleteLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /level delete <levelId> [confirm]")
            return
        }

        val levelId = args[0]
        if (args.size == 1) {
            executor.sendMessage("&c⚠️ This action will permanently remove level '&7$levelId&c' and its file.")
            executor.sendMessage("&cExecute &7'/level delete $levelId confirm' &cto confirm.")
            return
        }

        if (args[1].lowercase() != "confirm") {
            executor.sendMessage("&cInvalid confirmation. Use 'confirm' to proceed.")
            return
        }

        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        level.kickAllPlayers("Level is being deleted")
        if (Levels.unloadLevel(levelId)) {
            val file = java.io.File("levels/$levelId.dlvl")
            if (file.exists() && file.delete()) {
                executor.sendMessage("&aLevel '&7$levelId&a' deleted successfully.")
            } else {
                executor.sendMessage("&cLevel unloaded but failed to delete file.")
            }
        } else {
            executor.sendMessage("&cFailed to delete level '&7$levelId&c'.")
        }
    }

    @OnSubCommand(name = "list", description = "List all loaded levels", usage = "/level list [page]")
    @RequirePermission("dandelion.level.info")
    @ArgRange(max = 1)
    fun listLevels(executor: CommandExecutor, args: Array<String>) {
        val levels = Levels.getAllLevels()
        if (levels.isEmpty()) {
            executor.sendMessage("&cNo levels are currently loaded.")
            return
        }

        val page = if (args.isNotEmpty()) args[0].toIntOrNull() ?: 1 else 1
        val levelsPerPage = 10
        val totalPages = ceil(levels.size.toDouble() / levelsPerPage).toInt()

        if (page < 1 || page > totalPages) {
            executor.sendMessage("&cInvalid page number. Valid range: 1-$totalPages")
            return
        }

        val startIndex = (page - 1) * levelsPerPage
        val endIndex = minOf(startIndex + levelsPerPage, levels.size)

        executor.sendMessage("&eLoaded Levels (Page $page/$totalPages):")
        for (i in startIndex until endIndex) {
            val level = levels[i]
            executor.sendMessage("&f- &7${level.id} &f(${level.playerCount()} players, ${level.size.x}x${level.size.y}x${level.size.z}&f)")
        }
    }

    @OnSubCommand(name = "info", description = "Show detailed information about a level", usage = "/level info <levelId>")
    @RequirePermission("dandelion.level.info")
    @ArgRange(max = 1)
    fun levelInfo(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /level info <levelId>")
            return
        }

        val levelId = args[0]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        executor.sendMessage("&eLevel Information: &7${level.id}")
        executor.sendMessage("&7Author: &b${level.author}")
        executor.sendMessage("&7Description: &b${level.description}")
        executor.sendMessage("&7Size: &b${level.size.x}x${level.size.y}x${level.size.z}")
        executor.sendMessage("&7Spawn: &b${level.spawn.x}, ${level.spawn.y}, ${level.spawn.z}")
        executor.sendMessage("&7Players: &b${level.playerCount()}/${level.getAvailableIds()}")
        executor.sendMessage("&7Entities: &b${level.entityCount()}")
        executor.sendMessage("&7Auto-save: &b${if (level.autoSave) "Enabled" else "Disabled"}")
        executor.sendMessage("&7Weather: &b${getWeatherName(level.getWeatherType())}")
        if (level.getTexturePackUrl().isNotEmpty()) {
            executor.sendMessage("&7Texture Pack: &b${level.getTexturePackUrl()}")
        }
    }

    @OnSubCommand(name = "stats", description = "Show level system statistics", usage = "/level stats")
    @RequirePermission("dandelion.level.info")
    fun levelStats(executor: CommandExecutor, args: Array<String>) {
        val stats = Levels.getLevelStatistics()
        executor.sendMessage("&eLevels Statistics:")
        executor.sendMessage("&7Total Levels: &b${stats["totalLevels"]}")
        executor.sendMessage("&7Total Players: &b${stats["totalPlayers"]}")
        executor.sendMessage("&7Total Entities: &b${stats["totalEntities"]}")
        executor.sendMessage("&7Default Level: &b${stats["defaultLevel"]}")
        executor.sendMessage("&7Auto-save Interval: &b${stats["autoSaveInterval"]}")
    }

    @OnSubCommand(name = "tp", description = "Teleport to a level", usage = "/level tp <levelId> [player]", aliases = ["go"])
    @RequirePermission("dandelion.level.teleport")
    @ArgRange(max = 2)
    fun teleportToLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /level tp <levelId> [player]")
            return
        }

        val levelId = args[0]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        val targetPlayer = if (args.size > 1) {
            val playerName = args[1]
            Players.find(playerName) ?: run {
                executor.sendMessage("&cPlayer '&7$playerName&c' not found.")
                return
            }
        } else {
            if (executor !is Player) {
                executor.sendMessage("&cSpecify a player name.")
                return
            }
            executor
        }

        targetPlayer.joinLevel(level, true)
        if (executor != targetPlayer) {
            executor.sendMessage("&aPlayer '&7${targetPlayer.name}&a' teleported to level '&7$levelId&a'.")
        }
    }

    @OnSubCommand(name = "kick", description = "Kick all players from a level", usage = "/level kick <levelId> [reason]")
    @RequirePermission("dandelion.level.manage")
    fun kickFromLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /level kick <levelId> [reason]")
            return
        }

        val levelId = args[0]
        val reason = if (args.size > 1) args.slice(1 until args.size).joinToString(" ") else "You have been kicked from the level"
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        val playerCount = level.playerCount()
        level.kickAllPlayers(reason)
        executor.sendMessage("&aKicked &7$playerCount &aplayers from level '&7$levelId&a'.")
    }

    @OnSubCommand(name = "redirect", description = "Move all players from one level to another", usage = "/level redirect <fromLevel> <toLevel>")
    @RequirePermission("dandelion.level.manage")
    @ArgRange(max = 2)
    fun redirectPlayers(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 2) {
            executor.sendMessage("&cUsage: /level redirect <fromLevel> <toLevel>")
            return
        }

        val fromLevelId = args[0]
        val toLevelId = args[1]
        if (Levels.redirectAllPlayers(fromLevelId, toLevelId)) {
            executor.sendMessage("&aRedirected all players from '&7$fromLevelId&a' to '&7$toLevelId&a'.")
        } else {
            executor.sendMessage("&cFailed to redirect players. Check if both levels exist.")
        }
    }

    @OnSubCommand(name = "set", description = "Set level properties", usage = "/level set <property> <levelId> <value>")
    @RequirePermission("dandelion.level.edit")
    fun setLevelProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /level set <property> <levelId> <value>")
            executor.sendMessage("&7Properties: &aspawn, autosave, default, description")
            return
        }

        val property = args[0].lowercase()
        when (property) {
            "spawn" -> handleSpawnProperty(executor, args)
            "autosave" -> handleAutoSaveProperty(executor, args)
            "default" -> handleDefaultProperty(executor, args)
            "description" -> handleDescriptionProperty(executor, args)
            else -> {
                executor.sendMessage("&cUnknown property '&7$property&c'.")
                executor.sendMessage("&7Available properties: &aspawn, autosave, default, description")
            }
        }
    }

    @OnSubCommand(name = "env", description = "Modify level environment settings", usage = "/level env <property> <levelId> <value>")
    @RequirePermission("dandelion.level.environment")
    fun setEnvironment(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /level env <property> <levelId> <value>")
            executor.sendMessage("&7Properties: &atexture, weather, blocks, height, fog, speed, fade, offset, colors")
            return
        }

        val property = args[0].lowercase()
        when (property) {
            "texture" -> handleTextureProperty(executor, args)
            "weather" -> handleWeatherProperty(executor, args)
            "blocks" -> handleBlocksProperty(executor, args)
            "height" -> handleHeightProperty(executor, args)
            "fog" -> handleFogProperty(executor, args)
            "speed" -> handleSpeedProperty(executor, args)
            "fade" -> handleFadeProperty(executor, args)
            "offset" -> handleOffsetProperty(executor, args)
            "colors" -> handleColorsProperty(executor, args)
            else -> {
                executor.sendMessage("&cUnknown environment property '&7$property&c'.")
                executor.sendMessage("&7Properties: &atexture, weather, blocks, height, fog, speed, fade, offset, colors")
            }
        }
    }

    @OnSubCommand(name = "save", description = "Save level(s) manually", usage = "/level save <levelId|all>")
    @RequirePermission("dandelion.level.edit")
    @ArgRange(max = 1)
    fun saveLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /level save <levelId|all>")
            return
        }

        val target = args[0].lowercase()
        if (target == "all") {
            Levels.saveAllLevels()
            executor.sendMessage("&aAll levels saved successfully.")
        } else {
            val level = Levels.getLevel(target)
            if (level == null) {
                executor.sendMessage("&cLevel '&7$target&c' not found.")
                return
            }
            try {
                level.save()
                executor.sendMessage("&aLevel '&7$target&a' saved successfully.")
            } catch (e: Exception) {
                executor.sendMessage("&cFailed to save level '&7$target&c': ${e.message}")
            }
        }
    }

    @OnSubCommand(name = "reload", description = "Reload a level from disk", usage = "/level reload <levelId> [confirm]")
    @RequirePermission("dandelion.level.edit")
    @ArgRange(max = 2)
    fun reloadLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /level reload <levelId> [confirm]")
            return
        }

        val levelId = args[0]
        if (args.size == 1) {
            executor.sendMessage("&c⚠ This action will reload level '&7$levelId&c' from disk, losing unsaved changes.")
            executor.sendMessage("&cExecute &7'/level reload $levelId confirm' &cto confirm.")
            return
        }

        if (args[1].lowercase() != "confirm") {
            executor.sendMessage("&cInvalid confirmation. Use 'confirm' to proceed.")
            return
        }

        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        level.kickAllPlayers("Level is being reloaded")
        if (Levels.unloadLevel(levelId) && Levels.loadLevel(levelId)) {
            executor.sendMessage("&aLevel '&7$levelId&a' reloaded successfully.")
        } else {
            executor.sendMessage("&cFailed to reload level '&7$levelId&c'.")
        }
    }

    @OnExecute
    fun showAvailableSubCommands(executor: CommandExecutor, args: Array<String>) {
        val commandInfo = org.dandelion.classic.commands.manager.CommandRegistry.getCommands().find {
            it.name.equals("level", ignoreCase = true)
        } ?: run {
            executor.sendMessage("&cCommand info not found.")
            return
        }

        val availableSubCommands = commandInfo.subCommands.values.filter {
            it.permission.isEmpty() || executor.hasPermission(it.permission)
        }.map { "&7${it.name}&a" }

        if (availableSubCommands.isEmpty()) {
            executor.sendMessage("&cNo subcommands available.")
        } else {
            executor.sendMessage("&eAvailable SubCommands: ${availableSubCommands.joinToString(", ")}")
        }
    }

    private fun handleSpawnProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 2) {
            executor.sendMessage("&cUsage: /level set spawn <levelId> [x] [y] [z]")
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        if (args.size == 2) {
            if (executor !is Player) {
                executor.sendMessage("&cConsole must specify coordinates: /level set spawn <levelId> <x> <y> <z>")
                return
            }
            level.spawn = executor.position
            executor.sendMessage("&aSpawn set to your current position for level '&7$levelId&a'.")
        } else if (args.size == 5) {
            val x = args[2].toFloatOrNull()
            val y = args[3].toFloatOrNull()
            val z = args[4].toFloatOrNull()
            if (x == null || y == null || z == null) {
                executor.sendMessage("&cInvalid coordinates.")
                return
            }
            level.spawn = Position(x, y, z, 0f, 0f)
            executor.sendMessage("&aSpawn set to &7$x, $y, $z &afor level '&7$levelId&a'.")
        } else {
            executor.sendMessage("&cUsage: /level set spawn <levelId> [x] [y] [z]")
        }
    }

    private fun handleAutoSaveProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 3) {
            executor.sendMessage("&cUsage: /level set autosave <levelId> <true|false>")
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        val value = args[2].lowercase()
        when (value) {
            "true", "on", "enabled" -> {
                level.autoSave = true
                executor.sendMessage("&aAuto-save enabled for level '&7$levelId&a'.")
            }
            "false", "off", "disabled" -> {
                level.autoSave = false
                executor.sendMessage("&aAuto-save disabled for level '&7$levelId&a'.")
            }
            else -> executor.sendMessage("&cInvalid value. Use true or false.")
        }
    }

    private fun handleDefaultProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 2) {
            executor.sendMessage("&cUsage: /level set default <levelId>")
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        Levels.setDefaultLevel(levelId)
        executor.sendMessage("&aDefault level set to '&7$levelId&a'.")
    }

    private fun handleDescriptionProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 3) {
            executor.sendMessage("&cUsage: /level set description <levelId> <description>")
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        val description = args.slice(2 until args.size).joinToString(" ")
        executor.sendMessage("&aDescription intended to be updated for level '&7$levelId&a'. (Implementation needed in Level class)")
    }

    private fun showEnvironmentUsage(executor: CommandExecutor, property: String) {
        when (property) {
            "texture" -> executor.sendMessage("&cUsage: /level env texture <levelId> <url|reset>")
            "weather" -> executor.sendMessage("&cUsage: /level env weather <levelId> <sunny|rain|snow>")
            "blocks" -> executor.sendMessage("&cUsage: /level env blocks <levelId> <side|edge> <blockId>")
            "height" -> executor.sendMessage("&cUsage: /level env height <levelId> <edge|clouds> <height>")
            "fog" -> executor.sendMessage("&cUsage: /level env fog <levelId> <distance|exponential> <value>")
            "speed" -> executor.sendMessage("&cUsage: /level env speed <levelId> <clouds|weather> <speed>")
            "fade" -> executor.sendMessage("&cUsage: /level env fade <levelId> <fade>")
            "offset" -> executor.sendMessage("&cUsage: /level env offset <levelId> <offset>")
            "colors" -> {
                executor.sendMessage("&cUsage: /level env colors <levelId> <colorType> <#hex|r g b|reset>")
                executor.sendMessage("&7Examples: &a#ff00ff, 255 0 255, reset")
                executor.sendMessage("&7Types: &asky, cloud, fog, ambient, diffuse, skybox")
            }
            else -> {
                executor.sendMessage("&cUnknown environment property '&7$property&c'.")
                executor.sendMessage("&7Properties: &atexture, weather, blocks, height, fog, speed, fade, offset, colors")
            }
        }
    }

    private fun handleTextureProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 3) {
            executor.sendMessage("&cUsage: /level env texture <levelId> <url|reset>")
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        val value = args[2]
        if (value.lowercase() == "reset") {
            level.setTexturePackUrl("")
            executor.sendMessage("&aTexture pack URL reset for level '&7$levelId&a'.")
        } else {
            level.setTexturePackUrl(value)
            executor.sendMessage("&aTexture pack URL set for level '&7$levelId&a'.")
        }
    }

    private fun handleWeatherProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 3) {
            executor.sendMessage("&cUsage: /level env weather <levelId> <sunny|rain|snow>")
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        val weather = args[2].lowercase()
        val weatherType = when (weather) {
            "sunny", "sun" -> 0.toByte()
            "rain", "raining" -> 1.toByte()
            "snow", "snowing" -> 2.toByte()
            else -> {
                executor.sendMessage("&cInvalid weather type. Use: sunny, rain, snow")
                return
            }
        }
        level.setWeatherType(weatherType)
        executor.sendMessage("&aWeather set to '&7$weather&a' for level '&7$levelId&a'.")
    }

    private fun handleBlocksProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 4) {
            executor.sendMessage("&cUsage: /level env blocks <levelId> <side|edge> <blockId>")
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        val blockType = args[2].lowercase()
        val blockId = args[3].toByteOrNull()
        if (blockId == null) {
            executor.sendMessage("&cInvalid block ID.")
            return
        }

        when (blockType) {
            "side" -> {
                level.setSideBlock(blockId)
                executor.sendMessage("&aSide block set to &7$blockId &afor level '&7$levelId&a'.")
            }
            "edge" -> {
                level.setEdgeBlock(blockId)
                executor.sendMessage("&aEdge block set to &7$blockId &afor level '&7$levelId&a'.")
            }
            else -> {
                executor.sendMessage("&cInvalid block type.")
                executor.sendMessage("&7Types: &aside, edge")
            }
        }
    }

    private fun handleHeightProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 4) {
            executor.sendMessage("&cUsage: /level env height <levelId> <edge|clouds> <height>")
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        val heightType = args[2].lowercase()
        val height = args[3].toIntOrNull()
        if (height == null) {
            executor.sendMessage("&cInvalid height value.")
            return
        }

        when (heightType) {
            "edge" -> {
                level.setEdgeHeight(height)
                executor.sendMessage("&aEdge height set to &7$height &afor level '&7$levelId&a'.")
            }
            "clouds" -> {
                level.setCloudsHeight(height)
                executor.sendMessage("&aClouds height set to &7$height &afor level '&7$levelId&a'.")
            }
            else -> {
                executor.sendMessage("&cInvalid height type.")
                executor.sendMessage("&7Types: &aedge, clouds")
            }
        }
    }

    private fun handleFogProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 4) {
            executor.sendMessage("&cUsage: /level env fog <levelId> <distance|exponential> <value>")
            executor.sendMessage("&7Properties: &adistance, exponential")
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        val subProperty = args[2].lowercase()
        when (subProperty) {
            "distance" -> {
                val distance = args[3].toIntOrNull()
                if (distance == null) {
                    executor.sendMessage("&cInvalid distance value.")
                    return
                }
                level.setMaxFogDistance(distance)
                executor.sendMessage("&aFog distance set to &7$distance &afor level '&7$levelId&a'.")
            }
            "exponential" -> {
                val value = args[3].lowercase()
                val exponential = when (value) {
                    "true", "on", "enabled" -> true
                    "false", "off", "disabled" -> false
                    else -> {
                        executor.sendMessage("&cInvalid value. Use true or false.")
                        return
                    }
                }
                level.setExponentialFog(exponential)
                executor.sendMessage("&aExponential fog ${if (exponential) "&aenabled" else "&cdisabled"} &afor level '&7$levelId&a'.")
            }
            else -> {
                executor.sendMessage("&cInvalid fog property.")
                executor.sendMessage("&7Properties: &adistance, exponential")
            }
        }
    }

    private fun handleSpeedProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 4) {
            executor.sendMessage("&cUsage: /level env speed <levelId> <clouds|weather> <speed>")
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        val speedType = args[2].lowercase()
        val speed = args[3].toIntOrNull()
        if (speed == null) {
            executor.sendMessage("&cInvalid speed value.")
            return
        }

        when (speedType) {
            "clouds" -> {
                level.setCloudsSpeed(speed)
                executor.sendMessage("&aClouds speed set to &7$speed &afor level '&7$levelId&a'.")
            }
            "weather" -> {
                level.setWeatherSpeed(speed)
                executor.sendMessage("&aWeather speed set to &7$speed &afor level '&7$levelId&a'.")
            }
            else -> {
                executor.sendMessage("&cInvalid speed type.")
                executor.sendMessage("&7Types: &aclouds, weather")
            }
        }
    }

    private fun handleFadeProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 3) {
            executor.sendMessage("&cUsage: /level env fade <levelId> <fade>")
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        val fade = args[2].toIntOrNull()
        if (fade == null) {
            executor.sendMessage("&cInvalid fade value.")
            return
        }
        level.setWeatherFade(fade)
        executor.sendMessage("&aWeather fade set to &7$fade &afor level '&7$levelId&a'.")
    }

    private fun handleOffsetProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 3) {
            executor.sendMessage("&cUsage: /level env offset <levelId> <offset>")
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        val offset = args[2].toIntOrNull()
        if (offset == null) {
            executor.sendMessage("&cInvalid offset value.")
            return
        }
        level.setSidesOffset(offset)
        executor.sendMessage("&aSides offset set to &7$offset &afor level '&7$levelId&a'.")
    }

    private fun handleColorsProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 3) {
            executor.sendMessage("&cUsage: /level env colors <levelId> <colorType> <#hex|r g b|reset>")
            executor.sendMessage("&7Examples: &a#ff00ff, 255 0 255, reset")
            executor.sendMessage("&7Types: &asky, cloud, fog, ambient, diffuse, skybox")
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '&7$levelId&c' not found.")
            return
        }

        if (args.size == 4 && args[3].lowercase() == "reset") {
            val colorType = args[2].lowercase()
            when (colorType) {
                "sky" -> level.setSkyColor(null)
                "cloud" -> level.setCloudColor(null)
                "fog" -> level.setFogColor(null)
                "ambient" -> level.setAmbientLightColor(null)
                "diffuse" -> level.setDiffuseLightColor(null)
                "skybox" -> level.setSkyboxColor(null)
                else -> {
                    executor.sendMessage("&cInvalid color type.")
                    executor.sendMessage("&7Types: &asky, cloud, fog, ambient, diffuse, skybox")
                    return
                }
            }
            executor.sendMessage("&a${colorType.capitalize()} color reset for level '&7$levelId&a'.")
        } else if (args.size == 4 && args[3].startsWith("#")) {
            val colorType = args[2].lowercase()
            val hexColor = args[3].substring(1)

            if (hexColor.length != 6 || !hexColor.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                executor.sendMessage("&cInvalid hex color format. Use #RRGGBB (e.g., #ff00ff)")
                return
            }

            try {
                val r = hexColor.substring(0, 2).toInt(16)
                val g = hexColor.substring(2, 4).toInt(16)
                val b = hexColor.substring(4, 6).toInt(16)

                val color = Color(r.toShort(), g.toShort(), b.toShort())
                when (colorType) {
                    "sky" -> level.setSkyColor(color)
                    "cloud" -> level.setCloudColor(color)
                    "fog" -> level.setFogColor(color)
                    "ambient" -> level.setAmbientLightColor(color)
                    "diffuse" -> level.setDiffuseLightColor(color)
                    "skybox" -> level.setSkyboxColor(color)
                    else -> {
                        executor.sendMessage("&cInvalid color type.")
                        executor.sendMessage("&7Types: &asky, cloud, fog, ambient, diffuse, skybox")
                        return
                    }
                }
                executor.sendMessage("&a${colorType.capitalize()} color set to &7#$hexColor &a(&7$r, $g, $b&a) for level '&7$levelId&a'.")
            } catch (e: NumberFormatException) {
                executor.sendMessage("&cInvalid hex color format. Use #RRGGBB (e.g., #ff00ff)")
            }
        } else if (args.size == 6) {
            val colorType = args[2].lowercase()
            val r = args[3].toIntOrNull()
            val g = args[4].toIntOrNull()
            val b = args[5].toIntOrNull()

            if (r == null || g == null || b == null || r !in 0..255 || g !in 0..255 || b !in 0..255) {
                executor.sendMessage("&cInvalid RGB values. Use values between 0-255.")
                return
            }

            val color = Color(r.toShort(), g.toShort(), b.toShort())
            when (colorType) {
                "sky" -> level.setSkyColor(color)
                "cloud" -> level.setCloudColor(color)
                "fog" -> level.setFogColor(color)
                "ambient" -> level.setAmbientLightColor(color)
                "diffuse" -> level.setDiffuseLightColor(color)
                "skybox" -> level.setSkyboxColor(color)
                else -> {
                    executor.sendMessage("&cInvalid color type.")
                    executor.sendMessage("&7Types: &asky, cloud, fog, ambient, diffuse, skybox")
                    return
                }
            }
            executor.sendMessage("&a${colorType.capitalize()} color set to RGB(&7$r, $g, $b&a) for level '&7$levelId&a'.")
        } else {
            executor.sendMessage("&cUsage: /level env colors <levelId> <colorType> <#hex|r g b|reset>")
            executor.sendMessage("&7Examples: &a#ff00ff, 255 0 255, reset")
            executor.sendMessage("&7Types: &asky, cloud, fog, ambient, diffuse, skybox")
        }
    }

    private fun getWeatherName(weatherType: Byte): String {
        return when (weatherType) {
            0.toByte() -> "Sunny"
            1.toByte() -> "Raining"
            2.toByte() -> "Snowing"
            else -> "Unknown"
        }
    }
}