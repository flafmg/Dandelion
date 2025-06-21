package org.dandelion.classic.level

import kotlinx.coroutines.*
import org.dandelion.classic.entity.Entity
import org.dandelion.classic.entity.Player
import org.dandelion.classic.level.generator.GeneratorRegistry
import org.dandelion.classic.level.generator.LevelGenerator
import org.dandelion.classic.server.Console
import org.dandelion.classic.types.IVec
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.SVec
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object LevelManager {
    private val levels = HashMap<String, Level>()
    var defaultLevel: String = ""

    var autoSaveInterval: Duration = 2.minutes
    private var autoSaveJob: Job? = null

    internal fun init(){
        defaultLevel = "main"
        startAutoSaveLoop()
        loadLevel("main")

        val mainLevel = getLevel("main")
        if (mainLevel != null) {
            val npcEntity = Entity("TestNPC", "main")
            npcEntity.sendToLevel(mainLevel)
        }
    }

    internal fun shutDown(){
        autoSaveJob?.cancel()
        autoSave()
        levels.keys.forEach{ key -> unloadLevel(key)}
    }

    private fun startAutoSaveLoop() {
        autoSaveJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(autoSaveInterval.inWholeMilliseconds)
                autoSave()
            }
        }
    }

    fun autoSave() {
        levels.values.filter { it.autoSave }.forEach { level ->
            level.save()
            Console.log("Auto-saved level: ${level.id}")
        }
    }

    fun loadLevel(id: String){
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

    fun loadLevel(level: Level){
        if(levels.containsKey(level.id)){
            Console.warnLog("Level with same id is already registered")
            return
        }
        levels[level.id] = level
        Console.log("level ${level.id} loaded")
    }
    fun unloadLevel(id: String){
        if(!levels.containsKey(id)){
            Console.warnLog("cannot unload level $id because it doesnt exist")
        }
        levels.remove(id)
        Console.log("level $id unloaded")
    }

    fun getLevel(id: String): Level? = levels[id]
    fun getAllLevels(): List<Level> = levels.values.toList()
    fun getDefaultJoinLevel(): Level? = getLevel(defaultLevel)
    fun setDefaultJoinLevel(id: String){
        defaultLevel = id
        if(levels.containsKey(id)){
            Console.log("default level set to: $id")
        } else{
            Console.warnLog("level $id does not exists, create or load a level with this id")
        }
    }

    fun getAllEntities(): List<Entity> = levels.values.flatMap { it.getEntities() }
    fun getAllPlayers(): List<Player> = levels.values.flatMap { it.getPlayers() }
    fun getAllNonPlayerEntities(): List<Entity> = levels.values.flatMap { it.getNonPlayerEntities() }
    
    fun getOnlinePlayerCount(): Int = levels.values.sumOf { it.getPlayerCount() }
    fun getEntityCount(): Int = levels.values.sumOf { it.getEntityCount() }

    fun loadAllFromFolder(path: String){

    }
    
    fun createLevel(
        id: String,
        author: String,
        description: String,
        size: SVec,
        spawn: Position,
        extraData: String = "",
        timeCreated: Long = System.currentTimeMillis(),
        autoSave: Boolean = true,
        generator: LevelGenerator, 
        generatorParams: String
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
        loadLevel(level)
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