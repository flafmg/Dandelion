package org.dandelion.classic.level

import kotlinx.coroutines.*
import org.dandelion.classic.player.Entity
import org.dandelion.classic.player.Player
import org.dandelion.classic.level.generator.LevelGenerator
import org.dandelion.classic.server.Console
import org.dandelion.classic.server.ServerInfo
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.SVec
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

//TODO: needs to be refactored at some point, do the approach goodly mentioned?
object Levels {
    private val levels = HashMap<String, Level>()
    var defaultLevel: String = ""

    var saveInterval: Duration = 2.minutes
    private var saveJob: Job? = null

    internal fun init(){
        defaultLevel = ServerInfo.defaultLevel
        saveInterval = ServerInfo.autoSaveInterval
        //createLevel("main", "dandelion", "main level", SVec(256,128,256), Position(128f, 64f, 128f), FlatGenerator())
        startSaveTask()
        loadFromFolder("levels")
    }

    internal fun shutdown(){
        saveJob?.cancel()
        saveAll()
        levels.keys.forEach{ key -> unload(key)}
    }

    private fun startSaveTask() {
        saveJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(saveInterval.inWholeMilliseconds)
                saveAll()
            }
        }
    }

    fun saveAll() {
        levels.values.filter { it.autoSave }.forEach { level ->
            level.save()
            Console.debugLog("Auto-saved level: ${level.id}")
        }
    }

    fun load(id: String){
        if(levels.containsKey(id)){
            Console.warnLog("Level with same id is already registered")
            return
        }
        val level = Level.load("levels/$id.dlvl")
        if(level == null){
            Console.warnLog("Level could not be loaded")
            return
        }
        levels[id] = level;
        Console.log("level ${level.id} loaded")
    }

    fun load(level: Level){
        if(levels.containsKey(level.id)){
            Console.warnLog("Level with same id is already registered")
            return
        }
        levels[level.id] = level
        Console.log("level ${level.id} loaded")
    }
    fun unload(id: String){
        if(!levels.containsKey(id)){
            Console.warnLog("cannot unload level $id because it doesnt exist")
        }
        levels.remove(id)
        Console.log("level $id unloaded")
    }

    fun get(id: String): Level? = levels[id]
    fun getAll(): List<Level> = levels.values.toList()
    fun getDefault(): Level? = get(defaultLevel)
    fun setDefault(id: String){
        defaultLevel = id
        if(levels.containsKey(id)){
            Console.log("default level set to: $id")
        } else{
            Console.warnLog("level $id does not exists, create or load a level with this id")
        }
    }

    fun entities(): List<Entity> = levels.values.flatMap { it.getEntities() }
    fun players(): List<Player> = levels.values.flatMap { it.getPlayers() }
    fun nonPlayerEntities(): List<Entity> = levels.values.flatMap { it.getNonPlayerEntities() }

    fun playerCount(): Int = levels.values.sumOf { it.getPlayerCount() }
    fun entityCount(): Int = levels.values.sumOf { it.getEntityCount() }

    fun loadFromFolder(path: String){
        Console.log("Loading levels from $path folder")
        val folder = File(path)
        if (!folder.exists()) {
            Console.log("Levels folder '$path' not found, creating it.")
            folder.mkdirs()
            return
        }
        if (!folder.isDirectory) {
            Console.warnLog("Path '$path' is not a directory.")
            return
        }

        folder.listFiles { _, name -> name.endsWith(".dlvl", ignoreCase = true) }?.forEach { file ->
            val level = Level.load(file)
            if (level != null) {
                load(level)
            } else {
                Console.warnLog("Failed to load level from ${file.name}.")
            }
        }
    }
    
    fun create(
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
        if(levels.containsKey(id)){
            Console.warnLog("Cannot create level, id already exists")
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
        level.generate(generator, generatorParams)
        load(level)
        return level
    }
    

    fun redirectAll(from: Level, to: Level){
        val playersToRedirect = from.getPlayers().toList()
        
        playersToRedirect.forEach { player ->
            from.removeEntity(player.entityId)
            
            if(!to.trySetId(player)){
                if (player is Player) {
                    player.kick("Level is full")
                }
            }
        }
    }
}