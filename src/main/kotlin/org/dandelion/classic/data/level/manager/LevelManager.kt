package org.dandelion.classic.data.level.manager

import org.dandelion.classic.data.level.generator.manager.GeneratorManager
import org.dandelion.classic.data.level.model.Level
import org.dandelion.classic.util.Logger
import org.dandelion.classic.data.player.model.Player
import org.dandelion.classic.packets.server.ServerSetBlock
import org.dandelion.classic.data.level.io.impl.DandelionLevelSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dandelion.classic.data.level.io.impl.DandelionLevelDeserializer

object LevelManager {
    private val levels = mutableMapOf<String, Level>()
    private val autoSaveJobs = mutableMapOf<String, Job>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    var defaultLevel: String = ""

    fun addLevel(level: Level) {
        levels[level.id] = level
        Logger.infoLog("Level loaded: ${level.id}")
        startAutoSave(level)
    }

    private fun startAutoSave(level: Level) {
        autoSaveJobs[level.id]?.cancel()
        if (level.autoSaveInterval > 0) {
            val job = coroutineScope.launch {
                val serializer = DandelionLevelSerializer()
                val path = "levels/${level.id}.dlvl"
                while (true) {
                    delay(level.autoSaveInterval * 1000L)
                    try {
                        serializer.serialize(level, path)
                        Logger.debugLog("level ${level.id} auto-saved")
                    } catch (e: Exception) {
                        Logger.errLog("Failed to auto-save level ${level.id}: ${e.message}")
                    }
                }
            }
            autoSaveJobs[level.id] = job
        }
    }

    fun createLevel(id: String, sizeX: Short, sizeY: Short, sizeZ: Short, spawnX: Float = 0f, spawnY: Float = 0f, spawnZ: Float = 0f, seed: Long, generator: String = "flat", params: String = ""){

        if(getLevel(id) != null){
            Logger.errLog("Attempted to generate an level with existing ID.")
            return;
        }
        val gen = GeneratorManager.get(generator)
        if(gen == null){
            Logger.errLog("Generator '$generator' was not found.")
            return
        }
        val level: Level = Level(id, sizeX, sizeY, sizeZ, spawnX, spawnY, spawnZ, seed)

        Logger.log("Generating level '$id'")
        gen.generate(level, params);

        level.serialize(DandelionLevelSerializer(),"levels/${level.id}.dlvl")
        addLevel(level)
    }
    fun getLevel(id: String): Level? = levels[id]
    fun getAllLevels(): List<Level> = levels.values.toList()
    fun getDefaultJoinLevel(): Level? = getLevel(defaultLevel)

    fun setDefaultJoinLevel(id: String) {
        defaultLevel = id
        if(levels.containsKey(id)) {
            Logger.infoLog("Default level set: $id")
        }else{
            Logger.warnLog("Deafault level $id is not present, create or load a level with this id.")
        }
    }

    fun getLevelPlayers(id: String): List<Player> = getLevel(id)?.getPlayers() ?: emptyList()
    fun getLevelFirstAvailableId(id: String): Byte? = getLevel(id)?.getFirstAvailableId()
    fun addPlayerToLevel(levelId: String, player: Player): Boolean {
        val level = getLevel(levelId) ?: return false
        return level.addPlayer(player)
    }
    fun removePlayerFromLevel(levelId: String, playerId: Byte) {
        getLevel(levelId)?.removePlayer(playerId)
    }
    fun sendMessageToLevel(levelId: String, message: String, playerId: Byte = 0xff.toByte()) {
        val level = getLevel(levelId) ?: return
        val players = level.getPlayers()
        players.forEach { player ->
            player.sendMessage(message, playerId)
        }
    }

    fun setBlockAsPlayer(player: Player, x: Short, y: Short, z: Short, blockType: Byte, mode: Byte) {
        val levelId = player.levelId

        //minecraft classic uses a default range of 160 units, one unit is 1/32th of a block
        val dx = (player.posX / 32f) - x
        val dy = (player.posY / 32f) - y
        val dz = (player.posZ / 32f) - z
        val distance = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble())
        val maxDist = 160 // this is temporary until we add cpe

        val finalBlockType = if (mode == 0x00.toByte()) 0x00.toByte() else blockType
        val level = getLevel(levelId) ?: return

        // so people cant add blcoks anywhere just sending a packet, they need to be in range!
        if(distance <= 160.0) {
            level.setBlock(x.toInt(), y.toInt(), z.toInt(), finalBlockType)
        }

        val players = level.getPlayers()
        players.forEach { player ->
            ServerSetBlock(x, y, z, finalBlockType).resolve(player.channel)
        }
    }

    fun loadAll(path: String) {
        val dir = java.io.File(path)
        if (!dir.exists() || !dir.isDirectory) {
            Logger.errLog("Level directory '$path' does not exist or is not a directory.")
            return
        }
        val deserializer = DandelionLevelDeserializer()
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".dlvl") } ?: emptyArray()
        for (file in files) {
            try {
                val level = Level.deserialize(file.path, deserializer)
                addLevel(level)
                Logger.log("Loaded level: ${level.id} from ${file.name}")
            } catch (e: Exception) {
                Logger.errLog("Failed to load level from ${file.name}: ${e.message}")
            }
        }
    }

    fun unloadLevel(id: String) {
        autoSaveJobs[id]?.cancel()
        autoSaveJobs.remove(id)
        levels.remove(id)
        Logger.infoLog("Level unloaded: $id")
    }

    fun loadLevel(id: String) {
        val path = "levels/$id.dlvl"
        val deserializer = DandelionLevelDeserializer()
        val level = Level.deserialize(path, deserializer)
        addLevel(level)
    }

}
