package org.dandelion.classic.level

import org.dandelion.classic.level.generator.LevelGenerator
import org.dandelion.classic.level.io.DandelionLevelSerializer
import org.dandelion.classic.level.io.DandelionLevelDeserializer
import org.dandelion.classic.level.io.LevelDeserializer
import org.dandelion.classic.level.io.LevelSerializer
import org.dandelion.classic.network.packets.classic.server.ServerDespawnPlayer
import org.dandelion.classic.player.Entity
import org.dandelion.classic.player.Player
import org.dandelion.classic.server.Console
import org.dandelion.classic.types.Block
import org.dandelion.classic.types.IVec
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.SVec
import java.io.File

//TODO: needs to be refactored at some point, do the approach goodly mentioned?
data class Level(
    val id: String,
    val author: String,
    val description: String,

    val size: SVec,
    var spawn: Position,

    var extraData: String = "",
    val timeCreated: Long = System.currentTimeMillis(),
    var autoSave: Boolean = true
){
    var blocks: ByteArray = ByteArray(size.x * size.y * size.z) { 0x00 }

    val availableIds = ArrayDeque<Byte>(256) //we using it as LIFO (stack)
    val entities = HashMap<Byte, Entity>(256)

    init {
        for(id in 0..254){ // - 1 (255) is reserved for own player
            availableIds.addFirst(id.toByte())
        }
    }

    fun isFull(): Boolean {
        return availableIds.isEmpty()
    }
    fun getAvailableSpace(): Int{
        return availableIds.size
    }
    fun getEntityCount(): Int{
        return entities.size
    }
    fun getPlayerCount(): Int{
        return getPlayers().size
    }
    fun getNextAvailableID(): Byte?{
        return availableIds.removeFirstOrNull()
    }

    fun getPlayers(): List<Player> {
        return entities.values.filterIsInstance<Player>()
    }
    fun getNonPlayerEntities(): List<Entity> {
        return entities.values.filter { it !is Player }
    }
    fun getEntities(): List<Entity> {
        return entities.values.toList()
    }

    fun trySetId(entity: Entity): Boolean {
        val id = getNextAvailableID() ?: return false
        entity.entityId = id
        entities[id] = entity
        entity.level = this
        return true
    }
    
    fun removeEntity(entity: Entity) {
        if (!isEntityInLevel(entity)) {
            Console.warnLog("Attempted to remove an entity that isn't in this level")
            return
        }
        removeEntity(entity.entityId)
    }
    fun removeEntity(id: Byte) {
        val entity = entities[id] ?: run {
            Console.warnLog("Attempted to remove an entity that isn't in this level")
            return
        }

        if (entity is Player) {
            getPlayers().filter { it.entityId != id }.forEach { otherPlayer ->
                ServerDespawnPlayer(id).send(otherPlayer.channel)
            }
        }
        availableIds.addFirst(id)
        entities.remove(id)
        entity.entityId = -1
        entity.level = null
    }
    
    fun getEntityById(id: Byte): Entity? {
        return entities[id]
    }
    
    fun getPlayerById(id: Byte): Player? {
        val entity = entities[id]
        return if (entity is Player) entity else null
    }
    
    fun isEntityInLevel(entity: Entity): Boolean {
        return entities.values.any { it === entity }
    }
    
    fun isPlayerInLevel(player: Player): Boolean {
        return entities.values.any { it === player }
    }

    fun setBlock(position: Position, block: Block){
        setBlock(position.x.toInt(), position.y.toInt(), position.z.toInt(), block.id)
    }
    fun setBlock(position: Position, block: Byte){
        setBlock(position.x.toInt(), position.y.toInt(), position.z.toInt(), block)
    }
    fun setBlock(position: IVec, block: Block){
        setBlock(position.x, position.y, position.z, block.id)
    }
    fun setBlock(position: IVec, block: Byte){
        setBlock(position.x, position.y, position.z, block)
    }
    fun setBlock(x: Short, y: Short, z: Short, block: Block){
        setBlock(x, y, z, block.id)
    }
    fun setBlock(x: Short, y: Short, z: Short, block: Byte){
        setBlock(x.toInt(), y.toInt(), z.toInt(), block)
    }
    fun setBlock(x: Int, y: Int, z: Int, block: Block){
        setBlock(x, y, z, block.id)
    }
    fun setBlock(x: Int, y: Int, z: Int, block: Byte){
        val index = x + (z * size.x) + (y * size.x * size.z)
        blocks[index] = block

        if (x >= 0 && y >= 0 && z >= 0 && x < size.x && y < size.y && z < size.z) {
            val xShort = x.toShort()
            val yShort = y.toShort()
            val zShort = z.toShort()
            getPlayers().forEach { player ->
                player.updateBlock(xShort, yShort, zShort, block)
            }
        }
    }
    
    fun fillBlocks(start: Position, end: Position, block: Block){
        fillBlocks(start.x.toInt(), start.y.toInt(), start.z.toInt(), end.x.toInt(), end.y.toInt(), end.z.toInt(), block.id)
    }
    fun fillBlocks(start: Position, end: Position, block: Byte){
        fillBlocks(start.x.toInt(), start.y.toInt(), start.z.toInt(), end.x.toInt(), end.y.toInt(), end.z.toInt(), block)
    }
    fun fillBlocks(start: IVec, end: IVec, block: Block){
        fillBlocks(start.x, start.y, start.z, end.x, end.y, end.z, block.id)
    }
    fun fillBlocks(start: IVec, end: IVec, block: Byte){
        fillBlocks(start.x, start.y, start.z, end.x, end.y, end.z, block)
    }
    fun fillBlocks(startX: Int, startY: Int, startZ: Int, endX: Int, endY: Int, endZ: Int, block: Block){
        fillBlocks(startX, startY, startY, endX, endY, endZ, block.id)
    }
    fun fillBlocks(startX: Int, startY: Int, startZ: Int, endX: Int, endY: Int, endZ: Int, block: Byte){
        val minX = minOf(startX, endX)
        val maxX = maxOf(startX, endX)
        val minY = minOf(startY, endY)
        val maxY = maxOf(startY, endY)
        val minZ = minOf(startZ, endZ)
        val maxZ = maxOf(startZ, endZ)

        if(minX < 0 || maxX >= size.x || minY < 0 || maxY >= size.y || minZ < 0 || maxZ >= size.z) {
            Console.warnLog("Filling out of bounds error")
            return
        }

        for (y in minY..maxY) {
            val yOffset = y * size.x * size.z
            for (z in minZ..maxZ) {
                val zOffset = z * size.x
                for (x in minX..maxX) {
                    val index = x + zOffset + yOffset
                    blocks[index] = block
                }
            }
        }
    }
    
    fun getBlock(position: Position): Byte{
        return getBlock(position.x.toInt(), position.y.toInt(), position.z.toInt())
    }
    fun getBlock(position: IVec): Byte{
        return getBlock(position.x, position.y, position.z)
    }
    fun getBlock(x: Short, y: Short, z: Short): Byte{
        return getBlock(x.toInt(), y.toInt(), z.toInt())
    }
    fun getBlock(x: Int, y: Int, z: Int): Byte{
        val index = x + (z * size.x) + (y * size.x * size.z)
        return blocks[index]
    }

    fun kickAll(reason: String = "you have been kicked"){
        getPlayers().forEach { player -> player.kick(reason) }
    }
    
    fun broadcast(message: String){
        broadcast(message, 0x00)
    }
    
    fun broadcast(message: String, id: Byte = 0x00){
        getPlayers().forEach { player -> player.sendMessage(message, id) }
    }

    fun generate(generator: LevelGenerator, params: String){
        generator.generate(this, params)
    }
    
    fun spawnPlayerInLevel(player: Player){
        getEntities().filter { it.entityId != player.entityId }.forEach { entity ->
            player.spawnEntityMutual(entity)
        }
    }

    fun spawnEntityInLevel(entity: Entity) {
        getPlayers().forEach { player ->
            entity.spawnEntityFor(player)
        }
    }

    fun save(serializer: LevelSerializer, file: File){
        serializer.serialize(this, file)
    }
    fun save(serializer: LevelSerializer, path: String){
        val file = File(path)
        serializer.serialize(this, file)
    }
    fun save(serializer: LevelSerializer){
        val file = File("levels/$id.dlvl")
        serializer.serialize(this, file)
    }
    fun save(file: File){
        save(DandelionLevelSerializer(), file)
    }
    fun save(path: String){
        save(DandelionLevelSerializer(), path)
    }
    fun save(){
        save(DandelionLevelSerializer())
    }
    
    companion object{
        fun load(deserializer: LevelDeserializer, file: File): Level? {
            return deserializer.deserialize(file)
        }
        fun load(deserializer: LevelDeserializer, path: String): Level? {
            val file = File(path)
            return deserializer.deserialize(file)
        }
        fun load(file: File): Level? {
            return DandelionLevelDeserializer().deserialize(file)
        }
        fun load(path: String): Level? {
            val file = File(path)
            return DandelionLevelDeserializer().deserialize(file)
        }
    }
}