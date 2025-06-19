package org.dandelion.classic.level

import org.dandelion.classic.level.generator.GeneratorRegistry
import org.dandelion.classic.level.generator.LevelGenerator
import org.dandelion.classic.types.IVec
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.SVec
import java.nio.file.Path

object LevelManager {
    val levels = HashMap<String, Level>()
    var defaultLevel: String = ""


    internal fun init(){
        defaultLevel = "main"
        createLevel("main", 128, 64, 128, 64.0f, 33.0f, 64.0f, 0.0f, 0.0f, GeneratorRegistry.getGenerator("flat")!!, "{}")
    }
    internal fun shutDown(){
        levels.keys.forEach{ key -> unloadLevel(key)}
    }

    /**
     * loads level name
     */
    fun loadLevel(name: String){

    }

    fun loadLevel(level: Level){
        if(levels.containsKey(level.id)){
            println("Level with same id is already registered")
        }
        levels[level.id] = level
        println("level ${level.id} loaded")
    }
    fun unloadLevel(id: String){
        if(!levels.containsKey(id)){
            println("cannot unload level $id because it doesnt exist")
        }
        levels.remove(id)
        println("level $id unloaded")
    }

    fun getLevel(id: String): Level? = levels[id]
    fun getAllLevels(): List<Level> = levels.values.toList()
    fun getDefaultJoinLevel(): Level? = getLevel(defaultLevel)
    fun setDefaultJoinLevel(id: String){
        defaultLevel = id
        if(levels.containsKey(id)){
            println("default level set to: $id")
        } else{
            println("{WARN} level $id does not exists, create or load a level with this id")
        }
    }

    //loads all levels from a folder
    fun loadAllFromFolder(path: String){

    }

    fun createLevel(id: String, size: IVec, spawn: Position, generator: LevelGenerator, params: String): Level?{
        return createLevel(id, size.x, size.y, size.z, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch, generator, params)
    }
    fun createLevel(id: String, size: SVec, spawn: Position, generator: LevelGenerator, params: String): Level?{
        return createLevel(id, size.x, size.y, size.z, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch, generator, params)
    }
    fun createLevel(id: String, sizeX: Int, sizeY: Int, sizeZ: Int, spawnX: Float, spawnY: Float, spawnZ: Float, yaw: Float, pitch: Float, generator: LevelGenerator, params: String): Level?{
        return createLevel(id, sizeX.toShort(), sizeY.toShort(), sizeZ.toShort(), spawnX, spawnY, spawnZ, yaw, pitch, generator, params)
    }

    fun createLevel(id: String, sizeX: Short, sizeY: Short, sizeZ: Short, spawnX: Float, spawnY: Float, spawnZ: Float, yaw: Float, pitch: Float, generator: LevelGenerator, params: String): Level?{
        if(levels.containsKey(id)){
            println("Cannot create level, id already exists")
            return null
        }
        val level = Level(id, SVec(sizeX, sizeY, sizeZ), Position(spawnX, spawnY, spawnZ, yaw, pitch))
        level.generate(generator, params)
        loadLevel(level)
        return level
    }

    fun redirectAll(from: Level, to: Level){
        from.players.keys.forEach{  id ->
            val player = from.getPlayerById(id)!!
            from.removePlayer(id)

            if(!to.trySetId(player)){
                player.kick("Level is full")
            }
        }
    }
}