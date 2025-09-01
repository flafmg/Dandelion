package org.dandelion.classic.level

import java.io.File
import kotlin.time.Duration
import kotlinx.coroutines.*
import org.dandelion.classic.entity.Entity
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.level.generator.LevelGenerator
import org.dandelion.classic.server.Console
import org.dandelion.classic.server.ServerConfig
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.vec.SVec

object Levels {
    private val levels = HashMap<String, Level>()
    var defaultLevelId: String
        get() = ServerConfig.defaultLevel
        set(value) {
            ServerConfig.defaultLevel = value
        }

    var defaultFormat: String
        get() = ServerConfig.levelFormat
        set(value) {
            ServerConfig.levelFormat = value
        }

    var autoSaveInterval: Duration
        get() = ServerConfig.autoSaveInterval
        set(value) {
            ServerConfig.autoSaveInterval = value
        }

    private var saveJob: Job? = null

    internal fun init() {
        startAutoSaveTask()
        loadAllFromDirectory("levels")
    }

    internal fun shutdown() {
        stopAutoSaveTask()
        saveAllLevels()
        unloadAllLevels()
    }

    private fun startAutoSaveTask() {
        saveJob =
            CoroutineScope(Dispatchers.Default).launch {
                while (isActive) {
                    delay(autoSaveInterval.inWholeMilliseconds)
                    saveAllLevels()
                }
            }
    }

    private fun stopAutoSaveTask() {
        saveJob?.cancel()
        saveJob = null
    }

    @JvmStatic
    fun saveAllLevels() {
        val autoSaveLevels = levels.values.filter { it.autoSave }
        autoSaveLevels.forEach { level ->
            try {
                level.save()
                Console.debugLog("Auto-saved level: ${level.id}")
            } catch (e: Exception) {
                Console.warnLog(
                    "Failed to auto-save level ${level.id}: ${e.message}"
                )
            }
        }
        if (autoSaveLevels.isNotEmpty()) {
            Console.debugLog("Auto-saved ${autoSaveLevels.size} levels")
        }
    }

    @JvmStatic
    fun loadLevel(levelId: String): Boolean {
        if (isLevelLoaded(levelId)) {
            Console.warnLog("Level '$levelId' is already loaded")
            return false
        }

        val levelFile =
            File("levels")
                .listFiles { _, name ->
                    name.startsWith("$levelId.") &&
                        (name.endsWith(".dlvl") || name.endsWith(".cw"))
                }
                ?.firstOrNull()
        if (levelFile == null) {
            Console.warnLog("Level $levelId does not exist")
            return false
        }
        val level = Level.load(levelFile)

        return if (level != null) {
            loadLevel(level)
        } else {
            Console.warnLog("Failed to load level '$levelId' from file")
            false
        }
    }

    @JvmStatic
    fun loadLevel(level: Level): Boolean {
        if (isLevelLoaded(level.id)) {
            Console.warnLog("Level '${level.id}' is already loaded")
            return false
        }

        levels[level.id] = level
        Console.log("Successfully loaded level: ${level.id}")
        return true
    }

    @JvmStatic
    fun unloadLevel(levelId: String): Boolean {
        val level = levels[levelId]
        if (level == null) {
            Console.warnLog("Cannot unload level '$levelId' - not found")
            return false
        }

        // Save before unloading if auto-save is enabled
        if (level.autoSave) {
            try {
                level.save()
                Console.debugLog("Saved level before unloading: $levelId")
            } catch (e: Exception) {
                Console.warnLog(
                    "Failed to save level before unloading: ${e.message}"
                )
            }
        }

        // Kick all players from the level
        level.kickAllPlayers("Level is being unloaded")
        levels.remove(levelId)
        Console.log("Successfully unloaded level: $levelId")
        return true
    }

    @JvmStatic
    private fun unloadAllLevels() {
        levels.keys.toList().forEach { levelId -> unloadLevel(levelId) }
        Console.log("Unloaded all levels")
    }

    @JvmStatic
    fun createLevel(
        id: String,
        author: String,
        description: String,
        size: SVec,
        spawn: Position,
        generator: LevelGenerator,
        generatorParams: String = "",
        extraData: String = "",
        timeCreated: Long = System.currentTimeMillis(),
        autoSave: Boolean = true,
    ): Level? {
        if (isLevelLoaded(id)) {
            Console.warnLog("Cannot create level '$id' - ID already exists")
            return null
        }

        val level =
            Level(
                id = id,
                author = author,
                description = description,
                size = size,
                spawn = spawn,
                extraData = extraData,
                timeCreated = timeCreated,
                autoSave = autoSave,
                targetFormat = defaultFormat,
            )

        return try {
            level.generateLevel(generator, generatorParams)
            if (loadLevel(level)) {
                Console.log("Successfully created level: $id")
                level
            } else null
        } catch (e: Exception) {
            Console.warnLog("Failed to create level '$id': ${e.message}")
            null
        }
    }

    @JvmStatic fun getLevel(levelId: String): Level? = levels[levelId]

    @JvmStatic fun getAllLevels(): List<Level> = levels.values.toList()

    @JvmStatic fun getDefaultLevel(): Level? = getLevel(defaultLevelId)

    @JvmStatic
    fun setDefaultLevel(levelId: String) {
        defaultLevelId = levelId
        when {
            isLevelLoaded(levelId) ->
                Console.log("Default level set to: $levelId")
            else ->
                Console.warnLog(
                    "Default level '$levelId' is not loaded - create or load this level first"
                )
        }
        ServerConfig.save()
    }

    @JvmStatic
    fun isLevelLoaded(levelId: String): Boolean = levels.containsKey(levelId)

    @JvmStatic fun getLevelCount(): Int = levels.size

    @JvmStatic
    fun getAllEntities(): List<Entity> =
        levels.values.flatMap { it.getAllEntities() }

    @JvmStatic
    fun getAllPlayers(): List<Player> =
        levels.values.flatMap { it.getPlayers() }

    @JvmStatic
    fun getAllNonPlayerEntities(): List<Entity> =
        levels.values.flatMap { it.getNonPlayerEntities() }

    @JvmStatic
    fun getTotalPlayerCount(): Int = levels.values.sumOf { it.playerCount() }

    @JvmStatic
    fun getTotalEntityCount(): Int = levels.values.sumOf { it.entityCount() }

    @JvmStatic
    fun findLevelContainingPlayer(player: Player): Level? =
        levels.values.find { it.isPlayerInLevel(player) }

    @JvmStatic
    fun findLevelContainingEntity(entity: Entity): Level? =
        levels.values.find { it.isEntityInLevel(entity) }

    @JvmStatic
    fun loadAllFromDirectory(directoryPath: String) {
        Console.log("Loading levels from '$directoryPath' directory")

        val directory = File(directoryPath)
        if (!directory.exists()) {
            Console.log(
                "Levels directory '$directoryPath' not found - creating it"
            )
            directory.mkdirs()
            return
        }

        if (!directory.isDirectory) {
            Console.warnLog("Path '$directoryPath' is not a directory")
            return
        }

        val levelFiles =
            directory.listFiles { _, name ->
                name.endsWith(".dlvl", ignoreCase = true) ||
                    name.endsWith(".cw", true)
            }

        if (levelFiles == null) {
            Console.warnLog(
                "Could not list files in directory '$directoryPath'"
            )
            return
        }

        var loadedCount = 0
        levelFiles.forEach { file ->
            Level.load(file)?.let { level ->
                if (loadLevel(level)) {
                    loadedCount++
                } else {
                    Console.warnLog(
                        "Failed to load level from file: ${file.name}"
                    )
                }
            } ?: Console.warnLog("Failed to load level from file: ${file.name}")
        }

        Console.log(
            "Loaded $loadedCount levels from directory '$directoryPath'"
        )
    }

    @JvmStatic
    fun redirectAllPlayers(fromLevel: Level, toLevel: Level) {
        val playersToRedirect = fromLevel.getPlayers().toList()

        if (playersToRedirect.isEmpty()) {
            Console.debugLog(
                "No players to redirect from level '${fromLevel.id}' to '${toLevel.id}'"
            )
            return
        }

        var redirectedCount = 0
        playersToRedirect.forEach { player ->
            fromLevel.removeEntity(player)
            when {
                toLevel.tryAddEntity(player) -> {
                    toLevel.spawnPlayerInLevel(player)
                    redirectedCount++
                }
                else -> {
                    player.kick("Target level is full")
                    Console.warnLog(
                        "Failed to redirect player '${player.name}' - target level is full"
                    )
                }
            }
        }

        Console.log(
            "Redirected $redirectedCount players from '${fromLevel.id}' to '${toLevel.id}'"
        )
    }

    @JvmStatic
    fun redirectAllPlayers(fromLevelId: String, toLevelId: String): Boolean {
        val fromLevel = getLevel(fromLevelId)
        if (fromLevel == null) {
            Console.warnLog(
                "Cannot redirect players - source level '$fromLevelId' not found"
            )
            return false
        }

        val toLevel = getLevel(toLevelId)
        if (toLevel == null) {
            Console.warnLog(
                "Cannot redirect players - target level '$toLevelId' not found"
            )
            return false
        }

        redirectAllPlayers(fromLevel, toLevel)
        return true
    }

    @JvmStatic
    fun broadcast(message: String, messageTypeId: Byte = 0x00) {
        val totalPlayers = getTotalPlayerCount()

        if (totalPlayers == 0) {
            Console.debugLog("No players online to broadcast message to")
            return
        }

        levels.values.forEach { level ->
            level.broadcast(message, messageTypeId)
        }

        Console.debugLog(
            "Broadcasted message to $totalPlayers players across ${levels.size} levels"
        )
    }

    fun getLevelStatistics(): Map<String, Any> =
        mapOf(
            "totalLevels" to getLevelCount(),
            "totalPlayers" to getTotalPlayerCount(),
            "totalEntities" to getTotalEntityCount(),
            "defaultLevel" to defaultLevelId,
            "autoSaveInterval" to autoSaveInterval.toString(),
            "levels" to
                levels.values.map { level ->
                    mapOf(
                        "id" to level.id,
                        "playerCount" to level.playerCount(),
                        "entityCount" to level.entityCount(),
                        "size" to
                            "${level.size.x}x${level.size.y}x${level.size.z}",
                        "autoSave" to level.autoSave,
                    )
                },
        )
}
