package org.dandelion.classic.commands

import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.OnSubCommand
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.server.MessageRegistry
import org.dandelion.classic.level.Levels
import org.dandelion.classic.level.generator.GeneratorRegistry
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.server.Console
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
            MessageRegistry.Commands.Level.Create.sendUsage(executor)
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
            MessageRegistry.Commands.Level.Create.sendInvalidDimensions(executor)
            return
        }
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            MessageRegistry.Commands.Level.Create.sendInvalidDimensions(executor)
            return
        }

        val generator = GeneratorRegistry.getGenerator(generatorId)
        if (generator == null) {
            MessageRegistry.Commands.Level.Create.sendInvalidGenerator(executor, generatorId)
            MessageRegistry.Commands.Level.Create.sendAvailableGenerators(executor, GeneratorRegistry.getAllGenerators().joinToString(", ") { it.id })
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
            MessageRegistry.Commands.Level.Create.sendSuccess(executor, id)
        } else {
            MessageRegistry.Commands.Level.Create.sendFailed(executor, id)
        }
    }

    @OnSubCommand(name = "load", description = "Load a level from disk", usage = "/level load <levelId>")
    @RequirePermission("dandelion.level.load")
    @ArgRange(max = 1)
    fun loadLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Level.Load.sendUsage(executor)
            return
        }

        val levelId = args[0]
        if (Levels.loadLevel(levelId)) {
            MessageRegistry.Commands.Level.Load.sendSuccess(executor, levelId)
        } else {
            MessageRegistry.Commands.Level.Load.sendFailed(executor, levelId)
        }
    }

    @OnSubCommand(name = "unload", description = "Unload a level from memory", usage = "/level unload <levelId>")
    @RequirePermission("dandelion.level.load")
    @ArgRange(max = 1)
    fun unloadLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Level.Unload.sendUsage(executor)
            return
        }

        val levelId = args[0]
        if (Levels.unloadLevel(levelId)) {
            MessageRegistry.Commands.Level.Unload.sendSuccess(executor, levelId)
        } else {
            MessageRegistry.Commands.Level.Unload.sendFailed(executor, levelId)
        }
    }

    @OnSubCommand(name = "delete", description = "Delete a level permanently", usage = "/level delete <levelId> [confirm]")
    @RequirePermission("dandelion.level.delete")
    @ArgRange(max = 2)
    fun deleteLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Level.Delete.sendUsage(executor)
            return
        }

        val levelId = args[0]
        if (args.size == 1) {
            MessageRegistry.Commands.Level.Delete.sendConfirmMessage(executor, levelId)
            MessageRegistry.Commands.Level.Delete.sendConfirmInstruction(executor, levelId)
            return
        }

        if (args[1].lowercase() != "confirm") {
            MessageRegistry.Commands.Level.Delete.sendInvalidConfirmation(executor)
            return
        }

        val level = Levels.getLevel(levelId)
        if (level == null) {
            MessageRegistry.Commands.Level.Info.sendNotFound(executor, levelId)
            return
        }

        level.kickAllPlayers("Level is being deleted")
        if (Levels.unloadLevel(levelId)) {
            val file = java.io.File("levels/$levelId.dlvl")
            if (file.exists() && file.delete()) {
                MessageRegistry.Commands.Level.Delete.sendSuccess(executor, levelId)
            } else {
                MessageRegistry.Commands.Level.Delete.sendFileDeleteFailed(executor)
            }
        } else {
            MessageRegistry.Commands.Level.Delete.sendFailed(executor, levelId)
        }
    }

    @OnSubCommand(name = "list", description = "List all loaded levels", usage = "/level list [page]")
    @RequirePermission("dandelion.level.info")
    @ArgRange(max = 1)
    fun listLevels(executor: CommandExecutor, args: Array<String>) {
        val levels = Levels.getAllLevels()
        if (levels.isEmpty()) {
            MessageRegistry.Commands.Level.List.sendNoLevels(executor)
            return
        }

        val page = if (args.isNotEmpty()) args[0].toIntOrNull() ?: 1 else 1
        val levelsPerPage = 10
        val totalPages = ceil(levels.size.toDouble() / levelsPerPage).toInt()

        if (page < 1 || page > totalPages) {
            MessageRegistry.Commands.Level.List.sendInvalidPage(executor, totalPages)
            return
        }

        val startIndex = (page - 1) * levelsPerPage
        val endIndex = minOf(startIndex + levelsPerPage, levels.size)

        MessageRegistry.Commands.Level.List.sendHeader(executor, page, totalPages)
        for (i in startIndex until endIndex) {
            val level = levels[i]
            val size = "${level.size.x}x${level.size.y}x${level.size.z}"
            MessageRegistry.Commands.Level.List.sendLevel(executor, level.id, level.playerCount(), size)
        }
    }

    @OnSubCommand(name = "info", description = "Show detailed information about a level", usage = "/level info <levelId>")
    @RequirePermission("dandelion.level.info")
    @ArgRange(max = 1)
    fun levelInfo(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Level.Info.sendUsage(executor)
            return
        }

        val levelId = args[0]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            MessageRegistry.Commands.Level.Info.sendNotFound(executor, levelId)
            return
        }

        MessageRegistry.Commands.Level.Info.sendHeader(executor, level.id)
        MessageRegistry.Commands.Level.Info.sendAuthor(executor, level.author)
        MessageRegistry.Commands.Level.Info.sendDescription(executor, level.description)
        MessageRegistry.Commands.Level.Info.sendSize(executor, "${level.size.x}x${level.size.y}x${level.size.z}")
        MessageRegistry.Commands.Level.Info.sendSpawn(executor, "${level.spawn.x}, ${level.spawn.y}, ${level.spawn.z}")
        MessageRegistry.Commands.Level.Info.sendPlayers(executor, level.playerCount().toString(), level.getAvailableIds().toString())
        MessageRegistry.Commands.Level.Info.sendEntities(executor, level.entityCount().toString())
        MessageRegistry.Commands.Level.Info.sendAutoSave(executor, if (level.autoSave) "Enabled" else "Disabled")
        MessageRegistry.Commands.Level.Info.sendWeather(executor, getWeatherName(level.weatherType))
        if (level.texturePackUrl.isNotEmpty()) {
            MessageRegistry.Commands.Level.Info.sendTexturePack(executor, level.texturePackUrl)
        }
    }

    @OnSubCommand(name = "stats", description = "Show level system statistics", usage = "/level stats")
    @RequirePermission("dandelion.level.info")
    fun levelStats(executor: CommandExecutor, args: Array<String>) {
        val stats = Levels.getLevelStatistics()
        MessageRegistry.Commands.Level.Stats.sendHeader(executor)
        MessageRegistry.Commands.Level.Stats.sendTotalLevels(executor, stats["totalLevels"] ?: 0)
        MessageRegistry.Commands.Level.Stats.sendTotalPlayers(executor, stats["totalPlayers"] ?: 0)
        MessageRegistry.Commands.Level.Stats.sendTotalEntities(executor, stats["totalEntities"] ?: 0)
        MessageRegistry.Commands.Level.Stats.sendDefaultLevel(executor, stats["defaultLevel"] ?: "main")
        MessageRegistry.Commands.Level.Stats.sendAutoSaveInterval(executor, stats["autoSaveInterval"] ?: "5 minutes")
    }

    @OnSubCommand(name = "tp", description = "Teleport to a level", usage = "/level tp <levelId> [player]", aliases = ["go"])
    @RequirePermission("dandelion.level.teleport")
    @ArgRange(max = 2)
    fun teleportToLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Level.Teleport.sendUsage(executor)
            return
        }

        val levelId = args[0]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            MessageRegistry.Commands.Level.Info.sendNotFound(executor, levelId)
            return
        }

        val targetPlayer = if (args.size > 1) {
            val playerName = args[1]
            Players.find(playerName) ?: run {
                MessageRegistry.Commands.sendPlayerNotFound(executor, playerName)
                return
            }
        } else {
            if (executor !is Player) {
                MessageRegistry.Commands.Level.Teleport.sendSpecifyPlayer(executor)
                return
            }
            executor
        }

        targetPlayer.joinLevel(level, true)
        if (executor != targetPlayer) {
            MessageRegistry.Commands.Level.Teleport.sendSuccessOther(executor, targetPlayer.name, levelId)
        } else {
            MessageRegistry.Commands.Level.Teleport.sendSuccessSelf(executor, levelId)
        }
    }

    @OnSubCommand(name = "kick", description = "Kick all players from a level", usage = "/level kick <levelId> [reason]")
    @RequirePermission("dandelion.level.manage")
    fun kickFromLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Level.Kick.sendUsage(executor)
            return
        }

        val levelId = args[0]
        val reason = if (args.size > 1) args.slice(1 until args.size).joinToString(" ") else MessageRegistry.Commands.Level.Kick.getDefaultReason()
        val level = Levels.getLevel(levelId)
        if (level == null) {
            MessageRegistry.Commands.Level.Info.sendNotFound(executor, levelId)
            return
        }

        val playerCount = level.playerCount()
        level.kickAllPlayers(reason)
        MessageRegistry.Commands.Level.Kick.sendSuccess(executor, playerCount, levelId)
    }

    @OnSubCommand(name = "redirect", description = "Move all players from one level to another", usage = "/level redirect <fromLevel> <toLevel>")
    @RequirePermission("dandelion.level.manage")
    @ArgRange(max = 2)
    fun redirectPlayers(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 2) {
            MessageRegistry.Commands.Level.Redirect.sendUsage(executor)
            return
        }

        val fromLevelId = args[0]
        val toLevelId = args[1]
        if (Levels.redirectAllPlayers(fromLevelId, toLevelId)) {
            MessageRegistry.Commands.Level.Redirect.sendSuccess(executor, fromLevelId, toLevelId)
        } else {
            MessageRegistry.Commands.Level.Redirect.sendFailed(executor)
        }
    }

    @OnSubCommand(name = "set", description = "Set level properties", usage = "/level set <property> <levelId> <value>")
    @RequirePermission("dandelion.level.edit")
    fun setLevelProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Level.Set.sendUsage(executor)
            MessageRegistry.Commands.Level.Set.sendProperties(executor)
            return
        }

        val property = args[0].lowercase()
        when (property) {
            "spawn" -> handleSpawnProperty(executor, args)
            "autosave" -> handleAutoSaveProperty(executor, args)
            "default" -> handleDefaultProperty(executor, args)
            "description" -> handleDescriptionProperty(executor, args)
            else -> {
                MessageRegistry.Commands.Level.Set.sendUnknownProperty(executor, property)
                MessageRegistry.Commands.Level.Set.sendProperties(executor)
            }
        }
    }

    @OnSubCommand(name = "env", description = "Modify level environment settings", usage = "/level env <property> <levelId> <value>")
    @RequirePermission("dandelion.level.environment")
    fun setEnvironment(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Level.Env.sendUsage(executor)
            MessageRegistry.Commands.Level.Env.sendProperties(executor)
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
                MessageRegistry.Commands.Level.Env.sendUnknownProperty(executor, property)
                MessageRegistry.Commands.Level.Env.sendProperties(executor)
            }
        }
    }

    @OnSubCommand(name = "save", description = "Save level(s) manually", usage = "/level save <levelId|all>")
    @RequirePermission("dandelion.level.edit")
    @ArgRange(max = 1)
    fun saveLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Level.Save.sendUsage(executor)
            return
        }

        val target = args[0].lowercase()
        if (target == "all") {
            Levels.saveAllLevels()
            MessageRegistry.Commands.Level.Save.sendSuccessAll(executor)
        } else {
            val level = Levels.getLevel(target)
            if (level == null) {
                MessageRegistry.Commands.Level.Info.sendNotFound(executor, target)
                return
            }
            try {
                level.save()
                MessageRegistry.Commands.Level.Save.sendSuccessSingle(executor, target)
            } catch (e: Exception) {
                MessageRegistry.Commands.Level.Save.sendFailed(executor, target, e.message ?: "Unknown error")
            }
        }
    }

    @OnSubCommand(name = "reload", description = "Reload a level from disk", usage = "/level reload <levelId> [confirm]")
    @RequirePermission("dandelion.level.edit")
    @ArgRange(max = 2)
    fun reloadLevel(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Level.Reload.sendUsage(executor)
            return
        }

        val levelId = args[0]
        if (args.size == 1) {
            MessageRegistry.Commands.Level.Reload.sendConfirmMessage(executor, levelId)
            MessageRegistry.Commands.Level.Reload.sendConfirmInstruction(executor, levelId)
            return
        }

        if (args[1].lowercase() != "confirm") {
            MessageRegistry.Commands.Level.Delete.sendInvalidConfirmation(executor)
            return
        }

        val level = Levels.getLevel(levelId)
        if (level == null) {
            MessageRegistry.Commands.Level.Info.sendNotFound(executor, levelId)
            return
        }

        level.kickAllPlayers(MessageRegistry.Commands.Level.Reload.getKickMessage())
        if (Levels.unloadLevel(levelId) && Levels.loadLevel(levelId)) {
            MessageRegistry.Commands.Level.Reload.sendSuccess(executor, levelId)
        } else {
            MessageRegistry.Commands.Level.Reload.sendFailed(executor, levelId)
        }
    }

    @OnExecute
    fun showAvailableSubCommands(executor: CommandExecutor, args: Array<String>) {
        val commandInfo = org.dandelion.classic.commands.manager.CommandRegistry.getCommands().find {
            it.name.equals("level", ignoreCase = true)
        } ?: run {
            MessageRegistry.Commands.sendCommandError(executor)
            return
        }

        val availableSubCommands = commandInfo.subCommands.values.filter {
            it.permission.isEmpty() || executor.hasPermission(it.permission)
        }.map { "${it.name}" }

        if (availableSubCommands.isEmpty()) {
            MessageRegistry.Commands.Level.sendNoSubcommandsAvailable(executor)
        } else {
            MessageRegistry.Commands.Level.sendSubcommandsAvailable(executor, availableSubCommands.joinToString(", "))
        }
    }

    private fun handleSpawnProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 2) {
            MessageRegistry.Commands.Level.Set.Spawn.sendUsage(executor)
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            MessageRegistry.Commands.Level.Info.sendNotFound(executor, levelId)
            return
        }

        if (args.size == 2) {
            if (executor !is Player) {
                MessageRegistry.Commands.Level.Set.Spawn.sendConsoleCoords(executor)
                return
            }
            level.spawn = executor.position.clone()
            MessageRegistry.Commands.Level.Set.Spawn.sendSuccessCurrent(executor, levelId)
        } else if (args.size == 5) {
            val x = args[2].toFloatOrNull()
            val y = args[3].toFloatOrNull()
            val z = args[4].toFloatOrNull()
            if (x == null || y == null || z == null) {
                MessageRegistry.Commands.Level.Set.Spawn.sendInvalidCoords(executor)
                return
            }
            level.spawn = Position(x, y, z, 0f, 0f)
            MessageRegistry.Commands.Level.Set.Spawn.sendSuccessCoords(executor, "$x, $y, $z", levelId)
        } else {
            MessageRegistry.Commands.Level.Set.Spawn.sendUsage(executor)
        }
    }

    private fun handleAutoSaveProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 3) {
            MessageRegistry.Commands.Level.Set.AutoSave.sendUsage(executor)
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            MessageRegistry.Commands.Level.Info.sendNotFound(executor, levelId)
            return
        }

        val value = args[2].lowercase()
        when (value) {
            "true", "on", "enabled" -> {
                level.autoSave = true
                MessageRegistry.Commands.Level.Set.AutoSave.sendEnabled(executor, levelId)
            }
            "false", "off", "disabled" -> {
                level.autoSave = false
                MessageRegistry.Commands.Level.Set.AutoSave.sendDisabled(executor, levelId)
            }
            else -> MessageRegistry.Commands.Level.Set.AutoSave.sendInvalidValue(executor)
        }
    }

    private fun handleDefaultProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 2) {
            MessageRegistry.Commands.Level.Set.Default.sendUsage(executor)
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            MessageRegistry.Commands.Level.Info.sendNotFound(executor, levelId)
            return
        }

        Levels.setDefaultLevel(levelId)
        MessageRegistry.Commands.Level.Set.Default.sendSuccess(executor, levelId)
    }

    private fun handleDescriptionProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 3) {
            executor.sendMessage("&cUsage: /level set description <levelId> <description>")
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            MessageRegistry.Commands.Level.Info.sendNotFound(executor, levelId)
            return
        }

        val description = args.slice(2 until args.size).joinToString(" ")
        executor.sendMessage("&aDescription intended to be updated for level '&7$levelId&a'. (Implementation needed in Level class)")
    }

    private fun handleTextureProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 3) {
            MessageRegistry.Commands.Level.Env.Texture.sendUsage(executor)
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            MessageRegistry.Commands.Level.Info.sendNotFound(executor, levelId)
            return
        }

        val value = args[2]
        if (value.lowercase() == "reset") {
            level.texturePackUrl = ""
            MessageRegistry.Commands.Level.Env.Texture.sendReset(executor, levelId)
        } else {
            level.texturePackUrl = (value)
            MessageRegistry.Commands.Level.Env.Texture.sendSuccess(executor, levelId)
        }
    }

    private fun handleWeatherProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 3) {
            MessageRegistry.Commands.Level.Env.Weather.sendUsage(executor)
            return
        }

        val levelId = args[1]
        val level = Levels.getLevel(levelId)
        if (level == null) {
            MessageRegistry.Commands.Level.Info.sendNotFound(executor, levelId)
            return
        }

        val weather = args[2].lowercase()
        val weatherType = when (weather) {
            "sunny", "sun" -> 0.toByte()
            "rain", "raining" -> 1.toByte()
            "snow", "snowing" -> 2.toByte()
            else -> {
                MessageRegistry.Commands.Level.Env.Weather.sendInvalid(executor)
                return
            }
        }
        level.weatherType = weatherType
        MessageRegistry.Commands.Level.Env.Weather.sendSuccess(executor, weather, levelId)
    }

    private fun handleBlocksProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 4) {
            executor.sendMessage("&cUsage: /level env blocks <levelId> <side|edge> <blockId>")
            return
        }
        // Implementation would continue here
    }

    private fun handleHeightProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 4) {
            executor.sendMessage("&cUsage: /level env height <levelId> <edge|clouds> <height>")
            return
        }
        // Implementation would continue here
    }

    private fun handleFogProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 4) {
            executor.sendMessage("&cUsage: /level env fog <levelId> <distance|exponential> <value>")
            return
        }
        // Implementation would continue here
    }

    private fun handleSpeedProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 4) {
            executor.sendMessage("&cUsage: /level env speed <levelId> <clouds|weather> <speed>")
            return
        }
        // Implementation would continue here
    }

    private fun handleFadeProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 3) {
            executor.sendMessage("&cUsage: /level env fade <levelId> <fade>")
            return
        }
        // Implementation would continue here
    }

    private fun handleOffsetProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 3) {
            executor.sendMessage("&cUsage: /level env offset <levelId> <offset>")
            return
        }
        // Implementation would continue here
    }

    private fun handleColorsProperty(executor: CommandExecutor, args: Array<String>) {
        if (args.size < 3) {
            executor.sendMessage("&cUsage: /level env colors <levelId> <colorType> <#hex|r g b|reset>")
            return
        }
        // Implementation would continue here
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
