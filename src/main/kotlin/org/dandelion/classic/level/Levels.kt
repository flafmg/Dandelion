package org.dandelion.classic.level
import kotlinx.coroutines.*
import org.dandelion.classic.level.generator.LevelGenerator
import org.dandelion.classic.entity.Entity
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.server.Console
import org.dandelion.classic.server.ServerInfo
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.SVec
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
/**
 * Manages all loaded levels in the server
 */
object Levels {
    private val levels = HashMap<String, Level>()
    private var defaultLevelId: String = "unknowId"
    private var autoSaveInterval: Duration = 2.minutes
    private var saveJob: Job? = null
    /**
     * Initializes the level manager with server configuration
     */
    internal fun init() {
        defaultLevelId = ServerInfo.defaultLevel
        autoSaveInterval = ServerInfo.autoSaveInterval
        startAutoSaveTask()
        loadAllFromDirectory("levels")
    }
    /**
     * Shuts down the level manager, saving and unloading all levels
     */
    internal fun shutdown() {
        stopAutoSaveTask()
        saveAllLevels()
        unloadAllLevels()
    }
    /**
     * Starts the automatic save task for all levels
     */
    private fun startAutoSaveTask() {
        saveJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(autoSaveInterval.inWholeMilliseconds)
                saveAllLevels()
            }
        }
    }
    /**
     * Stops the automatic save task
     */
    private fun stopAutoSaveTask() {
        saveJob?.cancel()
        saveJob = null
    }
    /**
     * Saves all levels that have auto-save enabled
     */
    fun saveAllLevels() {
        val autoSaveLevels = levels.values.filter { it.autoSave }
        autoSaveLevels.forEach { level ->
            try {
                level.save()
                Console.debugLog("Auto-saved level: ${level.id}")
            } catch (e: Exception) {
                Console.warnLog("Failed to auto-save level ${level.id}: ${e.message}")
            }
        }
        if (autoSaveLevels.isNotEmpty()) {
            Console.debugLog("Auto-saved ${autoSaveLevels.size} levels")
        }
    }
    /**
     * Loads a level from file by ID
     *
     * @param levelId The ID of the level to load.
     * @return `true` if the level was successfully loaded, `false` otherwise.
     */
    fun loadLevel(levelId: String): Boolean {
        if (isLevelLoaded(levelId)) {
            Console.warnLog("Level '$levelId' is already loaded")
            return false
        }
        val levelFile = File("levels/$levelId.dlvl")
        val level = Level.load(levelFile) ?: run {
            Console.warnLog("Failed to load level '$levelId' from file")
            return false
        }
        return loadLevel(level)
    }
    /**
     * Loads a level instance into the manager
     *
     * @param level The [Level] instance to load.
     * @return `true` if the level was successfully loaded, `false` otherwise.
     */
    fun loadLevel(level: Level): Boolean {
        if (isLevelLoaded(level.id)) {
            Console.warnLog("Level '${level.id}' is already loaded")
            return false
        }
        levels[level.id] = level
        Console.log("Successfully loaded level: ${level.id}")
        return true
    }
    /**
     * Unloads a level by ID, saving it first if auto-save is enabled
     *
     * @param levelId The ID of the level to unload.
     * @return `true` if the level was successfully unloaded, `false` otherwise.
     */
    fun unloadLevel(levelId: String): Boolean {
        val level = levels[levelId] ?: run {
            Console.warnLog("Cannot unload level '$levelId' - not found")
            return false
        }
        // Save before unloading if auto-save is enabled
        if (level.autoSave) {
            try {
                level.save()
                Console.debugLog("Saved level before unloading: $levelId")
            } catch (e: Exception) {
                Console.warnLog("Failed to save level before unloading: ${e.message}")
            }
        }
        // Kick all players from the level
        level.kickAllPlayers("Level is being unloaded")
        levels.remove(levelId)
        Console.log("Successfully unloaded level: $levelId")
        return true
    }
    /**
     * Unloads all levels
     */
    private fun unloadAllLevels() {
        val levelIds = levels.keys.toList()
        levelIds.forEach { levelId ->
            unloadLevel(levelId)
        }
        Console.log("Unloaded all levels")
    }
    /**
     * Creates a new level with the specified parameters
     *
     * @param id The unique identifier for the new level.
     * @param author The author of the level.
     * @param description A description of the level.
     * @param size The dimensions of the level as an [SVec] (x, y, z).
     * @param spawn The default spawn position for players in the level as a [Position].
     * @param generator The [LevelGenerator] to use for creating the level's terrain/blocks.
     * @param generatorParams Optional parameters for the level generator.
     * @param extraData Optional extra data associated with the level.
     * @param timeCreated The timestamp (milliseconds since epoch) when the level was created. Defaults to the current time.
     * @param autoSave Whether the level should be automatically saved periodically. Defaults to `true`.
     * @return The newly created [Level] instance if successful, `null` otherwise.
     */
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
        autoSave: Boolean = true
    ): Level? {
        if (isLevelLoaded(id)) {
            Console.warnLog("Cannot create level '$id' - ID already exists")
            return null
        }
        val level = Level(
            id = id,
            author = author,
            description = description,
            size = size,
            spawn = spawn,
            extraData = extraData,
            timeCreated = timeCreated,
            autoSave = autoSave
        )
        try {
            level.generateLevel(generator, generatorParams)
            loadLevel(level)
            Console.log("Successfully created level: $id")
            return level
        } catch (e: Exception) {
            Console.warnLog("Failed to create level '$id': ${e.message}")
            return null
        }
    }
    /**
     * Gets a loaded [Level] by ID
     *
     * @param levelId The ID of the level to retrieve.
     * @return The [Level] instance if found, `null` otherwise.
     */
    fun getLevel(levelId: String): Level? {
        return levels[levelId]
    }
    /**
     * Gets all loaded levels
     *
     * @return A list of all currently loaded [Level] instances.
     */
    fun getAllLevels(): List<Level> {
        return levels.values.toList()
    }
    /**
     * Gets the default level
     *
     * @return The default [Level] instance if loaded, `null` otherwise.
     */
    fun getDefaultLevel(): Level? {
        return getLevel(defaultLevelId)
    }
    /**
     * Sets the default level ID
     *
     * @param levelId The ID of the level to set as the default.
     */
    fun setDefaultLevel(levelId: String) {
        defaultLevelId = levelId
        if (isLevelLoaded(levelId)) {
            Console.log("Default level set to: $levelId")
        } else {
            Console.warnLog("Default level '$levelId' is not loaded - create or load this level first")
        }
    }
    /**
     * Gets the current default level ID
     *
     * @return The ID of the currently configured default level.
     */
    fun getDefaultLevelId(): String {
        return defaultLevelId
    }
    /**
     * Checks if a level is currently loaded
     *
     * @param levelId The ID of the level to check.
     * @return `true` if the level is loaded, `false` otherwise.
     */
    fun isLevelLoaded(levelId: String): Boolean {
        return levels.containsKey(levelId)
    }
    /**
     * Gets the number of loaded levels
     *
     * @return The total count of currently loaded levels.
     */
    fun getLevelCount(): Int {
        return levels.size
    }
    /**
     * Gets all entities across all levels
     *
     * @return A flat list of all [Entity] instances present in any loaded level.
     */
    fun getAllEntities(): List<Entity> {
        return levels.values.flatMap { it.getAllEntities() }
    }
    /**
     * Gets all players across all levels
     *
     * @return A flat list of all [Player] instances present in any loaded level.
     */
    fun getAllPlayers(): List<Player> {
        return levels.values.flatMap { it.getPlayers() }
    }
    /**
     * Gets all non-player entities across all levels
     *
     * @return A flat list of all non-player [Entity] instances present in any loaded level.
     */
    fun getAllNonPlayerEntities(): List<Entity> {
        return levels.values.flatMap { it.getNonPlayerEntities() }
    }
    /**
     * Gets the total player count across all levels
     *
     * @return The sum of player counts across all loaded levels.
     */
    fun getTotalPlayerCount(): Int {
        return levels.values.sumOf { it.playerCount() }
    }
    /**
     * Gets the total entity count across all levels
     *
     * @return The sum of entity counts (players and non-players) across all loaded levels.
     */
    fun getTotalEntityCount(): Int {
        return levels.values.sumOf { it.entityCount() }
    }
    /**
     * Finds a player by their entity ID across all levels
     *
     * @param entityId The unique byte ID of the player entity to find.
     * @return The [Player] instance if found in any level, `null` otherwise.
     */
    fun findPlayerById(entityId: Byte): Player? {
        levels.values.forEach { level ->
            val player = level.findPlayerById(entityId)
            if (player != null) return player
        }
        return null
    }
    /**
     * Finds an entity by its ID across all levels
     *
     * @param entityId The unique byte ID of the entity to find.
     * @return The [Entity] instance if found in any level, `null` otherwise.
     */
    fun findEntityById(entityId: Byte): Entity? {
        levels.values.forEach { level ->
            val entity = level.findEntityById(entityId)
            if (entity != null) return entity
        }
        return null
    }
    /**
     * Finds which level contains the specified player
     *
     * @param player The [Player] instance to search for.
     * @return The [Level] instance containing the player, or `null` if the player is not found in any loaded level.
     */
    fun findLevelContainingPlayer(player: Player): Level? {
        return levels.values.find { level ->
            level.isPlayerInLevel(player)
        }
    }
    /**
     * Finds which level contains the specified entity
     *
     * @param entity The [Entity] instance to search for.
     * @return The [Level] instance containing the entity, or `null` if the entity is not found in any loaded level.
     */
    fun findLevelContainingEntity(entity: Entity): Level? {
        return levels.values.find { level ->
            level.isEntityInLevel(entity)
        }
    }
    /**
     * Loads all level files from the specified directory
     *
     * @param directoryPath The path to the directory containing `.dlvl` files.
     */
    fun loadAllFromDirectory(directoryPath: String) {
        Console.log("Loading levels from '$directoryPath' directory")
        val directory = File(directoryPath)
        if (!directory.exists()) {
            Console.log("Levels directory '$directoryPath' not found - creating it")
            directory.mkdirs()
            return
        }
        if (!directory.isDirectory) {
            Console.warnLog("Path '$directoryPath' is not a directory")
            return
        }
        val levelFiles = directory.listFiles { _, name ->
            name.endsWith(".dlvl", ignoreCase = true)
        } ?: run {
            Console.warnLog("Could not list files in directory '$directoryPath'")
            return
        }
        var loadedCount = 0
        levelFiles.forEach { file ->
            val level = Level.load(file)
            if (level != null && loadLevel(level)) {
                loadedCount++
            } else {
                Console.warnLog("Failed to load level from file: ${file.name}")
            }
        }
        Console.log("Loaded $loadedCount levels from directory '$directoryPath'")
    }
    /**
     * Redirects all players from one level to another
     *
     * @param fromLevel The source [Level] from which players will be moved.
     * @param toLevel The target [Level] to which players will be moved.
     */
    fun redirectAllPlayers(fromLevel: Level, toLevel: Level) {
        val playersToRedirect = fromLevel.getPlayers().toList()
        if (playersToRedirect.isEmpty()) {
            Console.debugLog("No players to redirect from level '${fromLevel.id}' to '${toLevel.id}'")
            return
        }
        var redirectedCount = 0
        playersToRedirect.forEach { player ->
            fromLevel.removeEntity(player)
            if (toLevel.tryAddEntity(player)) {
                toLevel.spawnPlayerInLevel(player)
                redirectedCount++
            } else {
                player.kick("Target level is full")
                Console.warnLog("Failed to redirect player '${player.name}' - target level is full")
            }
        }
        Console.log("Redirected $redirectedCount players from '${fromLevel.id}' to '${toLevel.id}'")
    }
    /**
     * Redirects all players from a level by ID to another level by ID
     *
     * @param fromLevelId The ID of the source level.
     * @param toLevelId The ID of the target level.
     * @return `true` if the redirection process was initiated, `false` if either level was not found.
     */
    fun redirectAllPlayers(fromLevelId: String, toLevelId: String): Boolean {
        val fromLevel = getLevel(fromLevelId) ?: run {
            Console.warnLog("Cannot redirect players - source level '$fromLevelId' not found")
            return false
        }
        val toLevel = getLevel(toLevelId) ?: run {
            Console.warnLog("Cannot redirect players - target level '$toLevelId' not found")
            return false
        }
        redirectAllPlayers(fromLevel, toLevel)
        return true
    }
    /**
     * Broadcasts a message to all players across all levels
     *
     * @param message The message string to broadcast.
     * @param messageTypeId An optional byte identifier for the type of message. Defaults to `0x00`.
     */
    fun broadcastToAllPlayers(message: String, messageTypeId: Byte = 0x00) {
        val totalPlayers = getTotalPlayerCount()
        if (totalPlayers == 0) {
            Console.debugLog("No players online to broadcast message to")
            return
        }
        levels.values.forEach { level ->
            level.broadcast(message, messageTypeId)
        }
        Console.debugLog("Broadcasted message to $totalPlayers players across ${levels.size} levels")
    }
    /**
     * Gets level statistics for monitoring
     *
     * @return A map containing various statistics about loaded levels, including total counts, default level, auto-save interval, and per-level details.
     */
    fun getLevelStatistics(): Map<String, Any> {
        return mapOf(
            "totalLevels" to getLevelCount(),
            "totalPlayers" to getTotalPlayerCount(),
            "totalEntities" to getTotalEntityCount(),
            "defaultLevel" to defaultLevelId,
            "autoSaveInterval" to autoSaveInterval.toString(),
            "levels" to levels.values.map { level ->
                mapOf(
                    "id" to level.id,
                    "playerCount" to level.playerCount(),
                    "entityCount" to level.entityCount(),
                    "size" to "${level.size.x}x${level.size.y}x${level.size.z}",
                    "autoSave" to level.autoSave
                )
            }
        )
    }
}