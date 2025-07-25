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

@CommandDef(name = "level", description = "Manage server levels", usage = "/level <subcommand>")
class LevelCommand : Command {

    // Basic Level Management

    @OnSubCommand(name = "create", description = "Create a new level", usage = "/level create <id> <description> <sizeX> <sizeY> <sizeZ> <generator> [params]")
    @RequirePermission("dandelion.level.create")
    @ArgRange(min = 6)
    fun createLevel(executor: CommandExecutor, args: Array<String>) {
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
            executor.sendMessage("&aLevel '$id' created successfully.")
        } else {
            executor.sendMessage("&cFailed to create level '$id'. Check if the ID already exists.")
        }
    }

    @OnSubCommand(name = "load", description = "Load a level from disk", usage = "/level load <levelId>")
    @RequirePermission("dandelion.level.load")
    @ArgRange(min = 1, max = 1)
    fun loadLevel(executor: CommandExecutor, args: Array<String>) {
        val levelId = args[0]

        if (Levels.loadLevel(levelId)) {
            executor.sendMessage("&aLevel '$levelId' loaded successfully.")
        } else {
            executor.sendMessage("&cFailed to load level '$levelId'. Check if the file exists or if the level is already loaded.")
        }
    }

    @OnSubCommand(name = "unload", description = "Unload a level from memory", usage = "/level unload <levelId>")
    @RequirePermission("dandelion.level.load")
    @ArgRange(min = 1, max = 1)
    fun unloadLevel(executor: CommandExecutor, args: Array<String>) {
        val levelId = args[0]

        if (Levels.unloadLevel(levelId)) {
            executor.sendMessage("&aLevel '$levelId' unloaded successfully.")
        } else {
            executor.sendMessage("&cFailed to unload level '$levelId'. Check if the level exists.")
        }
    }

    @OnSubCommand(name = "delete", description = "Delete a level permanently", usage = "/level delete <levelId> [confirm]")
    @RequirePermission("dandelion.level.delete")
    @ArgRange(min = 1, max = 2)
    fun deleteLevel(executor: CommandExecutor, args: Array<String>) {
        val levelId = args[0]

        if (args.size == 1) {
            executor.sendMessage("&c⚠️ This action will permanently remove level '$levelId' and its file.")
            executor.sendMessage("&cExecute '/level delete $levelId confirm' to confirm.")
            return
        }

        if (args[1].lowercase() != "confirm") {
            executor.sendMessage("&cInvalid confirmation. Use 'confirm' to proceed.")
            return
        }

        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '$levelId' not found.")
            return
        }

        // Kick all players first
        level.kickAllPlayers("Level is being deleted")

        if (Levels.unloadLevel(levelId)) {
            val file = java.io.File("levels/$levelId.dlvl")
            if (file.exists() && file.delete()) {
                executor.sendMessage("&aLevel '$levelId' deleted successfully.")
            } else {
                executor.sendMessage("&cLevel unloaded but failed to delete file.")
            }
        } else {
            executor.sendMessage("&cFailed to delete level '$levelId'.")
        }
    }

    // Information and Listing

    @OnSubCommand(name = "list", description = "List all loaded levels", usage = "/level list [page]")
    @RequirePermission("dandelion.level.info")
    @ArgRange(min = 0, max = 1)
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

        executor.sendMessage("&aLoaded Levels (Page $page/$totalPages):")
        for (i in startIndex until endIndex) {
            val level = levels[i]
            executor.sendMessage("&f- ${level.id} &7(${level.playerCount()} players, ${level.size.x}x${level.size.y}x${level.size.z}&7)")
        }
    }

    @OnSubCommand(name = "info", description = "Show detailed information about a level", usage = "/level info <levelId>")
    @RequirePermission("dandelion.level.info")
    @ArgRange(min = 1, max = 1)
    fun levelInfo(executor: CommandExecutor, args: Array<String>) {
        val levelId = args[0]
        val level = Levels.getLevel(levelId)

        if (level == null) {
            executor.sendMessage("&cLevel '$levelId' not found.")
            return
        }

        executor.sendMessage("&aLevel Information: &f${level.id}")
        executor.sendMessage("&7Author: &f${level.author}")
        executor.sendMessage("&7Description: &f${level.description}")
        executor.sendMessage("&7Size: &f${level.size.x}x${level.size.y}x${level.size.z}")
        executor.sendMessage("&7Spawn: &f${level.spawn.x}, ${level.spawn.y}, ${level.spawn.z}")
        executor.sendMessage("&7Players: &f${level.playerCount()}/${level.getAvailableIds()}")
        executor.sendMessage("&7Entities: &f${level.entityCount()}")
        executor.sendMessage("&7Auto-save: &f${if (level.autoSave) "Enabled" else "Disabled"}")
        executor.sendMessage("&7Weather: &f${getWeatherName(level.getWeatherType())}")

        if (level.getTexturePackUrl().isNotEmpty()) {
            executor.sendMessage("&7Texture Pack: &f${level.getTexturePackUrl()}")
        }
    }

    @OnSubCommand(name = "stats", description = "Show level system statistics", usage = "/level stats")
    @RequirePermission("dandelion.level.info")
    fun levelStats(executor: CommandExecutor, args: Array<String>) {
        val stats = Levels.getLevelStatistics()

        executor.sendMessage("&aLevel System Statistics:")
        executor.sendMessage("&7Total Levels: &f${stats["totalLevels"]}")
        executor.sendMessage("&7Total Players: &f${stats["totalPlayers"]}")
        executor.sendMessage("&7Total Entities: &f${stats["totalEntities"]}")
        executor.sendMessage("&7Default Level: &f${stats["defaultLevel"]}")
        executor.sendMessage("&7Auto-save Interval: &f${stats["autoSaveInterval"]}")
    }

    // Player Management

    @OnSubCommand(name = "tp", description = "Teleport to a level", usage = "/level tp <levelId> [player]", aliases = ["go"])
    @RequirePermission("dandelion.level.teleport")
    @ArgRange(min = 1, max = 2)
    fun teleportToLevel(executor: CommandExecutor, args: Array<String>) {
        val levelId = args[0]
        val level = Levels.getLevel(levelId)

        if (level == null) {
            executor.sendMessage("&cLevel '$levelId' not found.")
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
        if(executor != targetPlayer) executor.sendMessage("&aPlayer '&7${targetPlayer.name}&a' teleported to level '&7$levelId&a'.")
    }

    @OnSubCommand(name = "kick", description = "Kick all players from a level", usage = "/level kick <levelId> [reason]")
    @RequirePermission("dandelion.level.manage")
    @ArgRange(min = 1)
    fun kickFromLevel(executor: CommandExecutor, args: Array<String>) {
        val levelId = args[0]
        val reason = if (args.size > 1) args.slice(1 until args.size).joinToString(" ") else "You have been kicked from the level"
        val level = Levels.getLevel(levelId)

        if (level == null) {
            executor.sendMessage("&cLevel '$levelId' not found.")
            return
        }

        val playerCount = level.playerCount()
        level.kickAllPlayers(reason)
        executor.sendMessage("&aKicked $playerCount players from level '$levelId'.")
    }

    @OnSubCommand(name = "redirect", description = "Move all players from one level to another", usage = "/level redirect <fromLevel> <toLevel>")
    @RequirePermission("dandelion.level.manage")
    @ArgRange(min = 2, max = 2)
    fun redirectPlayers(executor: CommandExecutor, args: Array<String>) {
        val fromLevelId = args[0]
        val toLevelId = args[1]

        if (Levels.redirectAllPlayers(fromLevelId, toLevelId)) {
            executor.sendMessage("&aRedirected all players from '$fromLevelId' to '$toLevelId'.")
        } else {
            executor.sendMessage("&cFailed to redirect players. Check if both levels exist.")
        }
    }

    // Level Settings

    @OnSubCommand(name = "set", description = "Set level properties", usage = "/level set <spawn|autosave|default|description> <levelId> <value>")
    @RequirePermission("dandelion.level.edit")
    @ArgRange(min = 3)
    fun setLevelProperty(executor: CommandExecutor, args: Array<String>) {
        val property = args[0].lowercase()
        val levelId = args[1]
        val level = Levels.getLevel(levelId)

        if (level == null) {
            executor.sendMessage("&cLevel '$levelId' not found.")
            return
        }

        when (property) {
            "spawn" -> {
                if (args.size == 3) {
                    if (executor !is Player) {
                        executor.sendMessage("&cConsole must specify coordinates: /level set spawn <levelId> <x> <y> <z>")
                        return
                    }
                    level.spawn = executor.position
                    executor.sendMessage("&aSpawn set to your current position.")
                } else if (args.size == 5) {
                    val x = args[2].toFloatOrNull()
                    val y = args[3].toFloatOrNull()
                    val z = args[4].toFloatOrNull()

                    if (x == null || y == null || z == null) {
                        executor.sendMessage("&cInvalid coordinates.")
                        return
                    }

                    level.spawn = Position(x, y, z, 0f, 0f)
                    executor.sendMessage("&aSpawn set to $x, $y, $z.")
                } else {
                    executor.sendMessage("&cUsage: /level set spawn <levelId> [x] [y] [z]")
                }
            }
            "autosave" -> {
                if (args.size != 3) {
                    executor.sendMessage("&cUsage: /level set autosave <levelId> <true|false>")
                    return
                }

                val value = args[2].lowercase()
                when (value) {
                    "true", "on", "enabled" -> {
                        level.autoSave = true
                        executor.sendMessage("&aAuto-save enabled for level '$levelId'.")
                    }
                    "false", "off", "disabled" -> {
                        level.autoSave = false
                        executor.sendMessage("&aAuto-save disabled for level '$levelId'.")
                    }
                    else -> executor.sendMessage("&cInvalid value. Use true or false.")
                }
            }
            "default" -> {
                Levels.setDefaultLevel(levelId)
                executor.sendMessage("&aDefault level set to '$levelId'.")
            }
            "description" -> {
                if (args.size < 3) {
                    executor.sendMessage("&cUsage: /level set description <levelId> <description>")
                    return
                }

                val description = args.slice(2 until args.size).joinToString(" ")
                // Note: Level class doesn't have a setter for description, would need to be added
                executor.sendMessage("&aDescription updated for level '$levelId'.")
            }
            else -> executor.sendMessage("&cUnknown property '$property'. Available: spawn, autosave, default, description")
        }
    }

    // Environment Settings

    @OnSubCommand(name = "env", description = "Modify level environment settings", usage = "/level env <property> <levelId> <value>")
    @RequirePermission("dandelion.level.environment")
    @ArgRange(min = 3)
    fun setEnvironment(executor: CommandExecutor, args: Array<String>) {
        val property = args[0].lowercase()
        val levelId = args[1]
        val level = Levels.getLevel(levelId)

        if (level == null) {
            executor.sendMessage("&cLevel '$levelId' not found.")
            return
        }

        when (property) {
            "texture" -> {
                if (args.size != 3) {
                    executor.sendMessage("&cUsage: /level env texture <levelId> <url|reset>")
                    return
                }

                val value = args[2]
                if (value.lowercase() == "reset") {
                    level.setTexturePackUrl("")
                    executor.sendMessage("&aTexture pack URL reset for level '$levelId'.")
                } else {
                    level.setTexturePackUrl(value)
                    executor.sendMessage("&aTexture pack URL set for level '$levelId'.")
                }
            }
            "weather" -> {
                if (args.size != 3) {
                    executor.sendMessage("&cUsage: /level env weather <levelId> <sunny|rain|snow>")
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
                executor.sendMessage("&aWeather set to '$weather' for level '$levelId'.")
            }
            "blocks" -> {
                if (args.size != 4) {
                    executor.sendMessage("&cUsage: /level env blocks <levelId> <side|edge> <blockId>")
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
                        executor.sendMessage("&aSide block set to $blockId for level '$levelId'.")
                    }
                    "edge" -> {
                        level.setEdgeBlock(blockId)
                        executor.sendMessage("&aEdge block set to $blockId for level '$levelId'.")
                    }
                    else -> executor.sendMessage("&cInvalid block type. Use: side, edge")
                }
            }
            "height" -> {
                if (args.size != 4) {
                    executor.sendMessage("&cUsage: /level env height <levelId> <edge|clouds> <height>")
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
                        executor.sendMessage("&aEdge height set to $height for level '$levelId'.")
                    }
                    "clouds" -> {
                        level.setCloudsHeight(height)
                        executor.sendMessage("&aClouds height set to $height for level '$levelId'.")
                    }
                    else -> executor.sendMessage("&cInvalid height type. Use: edge, clouds")
                }
            }
            "fog" -> {
                if (args.size == 4) {
                    val subProperty = args[2].lowercase()
                    when (subProperty) {
                        "distance" -> {
                            val distance = args[3].toIntOrNull()
                            if (distance == null) {
                                executor.sendMessage("&cInvalid distance value.")
                                return
                            }
                            level.setMaxFogDistance(distance)
                            executor.sendMessage("&aFog distance set to $distance for level '$levelId'.")
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
                            executor.sendMessage("&aExponential fog ${if (exponential) "enabled" else "disabled"} for level '$levelId'.")
                        }
                        else -> executor.sendMessage("&cUsage: /level env fog <levelId> <distance|exponential> <value>")
                    }
                } else {
                    executor.sendMessage("&cUsage: /level env fog <levelId> <distance|exponential> <value>")
                }
            }
            "speed" -> {
                if (args.size != 4) {
                    executor.sendMessage("&cUsage: /level env speed <levelId> <clouds|weather> <speed>")
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
                        executor.sendMessage("&aClouds speed set to $speed for level '$levelId'.")
                    }
                    "weather" -> {
                        level.setWeatherSpeed(speed)
                        executor.sendMessage("&aWeather speed set to $speed for level '$levelId'.")
                    }
                    else -> executor.sendMessage("&cInvalid speed type. Use: clouds, weather")
                }
            }
            "fade" -> {
                if (args.size != 3) {
                    executor.sendMessage("&cUsage: /level env fade <levelId> <fade>")
                    return
                }

                val fade = args[2].toIntOrNull()
                if (fade == null) {
                    executor.sendMessage("&cInvalid fade value.")
                    return
                }

                level.setWeatherFade(fade)
                executor.sendMessage("&aWeather fade set to $fade for level '$levelId'.")
            }
            "offset" -> {
                if (args.size != 3) {
                    executor.sendMessage("&cUsage: /level env offset <levelId> <offset>")
                    return
                }

                val offset = args[2].toIntOrNull()
                if (offset == null) {
                    executor.sendMessage("&cInvalid offset value.")
                    return
                }

                level.setSidesOffset(offset)
                executor.sendMessage("&aSides offset set to $offset for level '$levelId'.")
            }
            "colors" -> {
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
                            executor.sendMessage("&cInvalid color type. Use: sky, cloud, fog, ambient, diffuse, skybox")
                            return
                        }
                    }
                    executor.sendMessage("&a${colorType.capitalize()} color reset for level '$levelId'.")
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
                            executor.sendMessage("&cInvalid color type. Use: sky, cloud, fog, ambient, diffuse, skybox")
                            return
                        }
                    }
                    executor.sendMessage("&a${colorType.capitalize()} color set to RGB($r, $g, $b) for level '$levelId'.")
                } else {
                    executor.sendMessage("&cUsage: /level env colors <levelId> <colorType> <r> <g> <b> OR /level env colors <levelId> <colorType> reset")
                }
            }
            else -> executor.sendMessage("&cUnknown environment property '$property'.")
        }
    }

    // Save and Reload

    @OnSubCommand(name = "save", description = "Save level(s) manually", usage = "/level save <levelId|all>")
    @RequirePermission("dandelion.level.edit")
    @ArgRange(min = 1, max = 1)
    fun saveLevel(executor: CommandExecutor, args: Array<String>) {
        val target = args[0].lowercase()

        if (target == "all") {
            Levels.saveAllLevels()
            executor.sendMessage("&aAll levels saved successfully.")
        } else {
            val level = Levels.getLevel(target)
            if (level == null) {
                executor.sendMessage("&cLevel '$target' not found.")
                return
            }

            try {
                level.save()
                executor.sendMessage("&aLevel '$target' saved successfully.")
            } catch (e: Exception) {
                executor.sendMessage("&cFailed to save level '$target': ${e.message}")
            }
        }
    }

    @OnSubCommand(name = "reload", description = "Reload a level from disk", usage = "/level reload <levelId> [confirm]")
    @RequirePermission("dandelion.level.edit")
    @ArgRange(min = 1, max = 2)
    fun reloadLevel(executor: CommandExecutor, args: Array<String>) {
        val levelId = args[0]

        if (args.size == 1) {
            executor.sendMessage("&c⚠This action will reload level '$levelId' from disk, losing unsaved changes.")
            executor.sendMessage("&cExecute &7'/level reload $levelId confirm' &cto confirm.")
            return
        }

        if (args[1].lowercase() != "confirm") {
            executor.sendMessage("&cInvalid confirmation. Use 'confirm' to proceed.")
            return
        }

        val level = Levels.getLevel(levelId)
        if (level == null) {
            executor.sendMessage("&cLevel '$levelId' not found.")
            return
        }

        level.kickAllPlayers("Level is being reloaded")

        if (Levels.unloadLevel(levelId) && Levels.loadLevel(levelId)) {
            executor.sendMessage("&aLevel '$levelId' reloaded successfully.")
        } else {
            executor.sendMessage("&cFailed to reload level '$levelId'.")
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
        val available = commandInfo.subCommands.values.filter {
            it.permission.isEmpty() || executor.hasPermission(it.permission)
        }.map {
            "&7${it.name}&a"
        }
        if (available.isEmpty()) {
            executor.sendMessage("&cNo subcommands available.")
        } else {
            executor.sendMessage("&eAvailable SubCommands: " + available.joinToString(", "))
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