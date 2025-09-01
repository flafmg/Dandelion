package org.dandelion.classic.level

import java.io.File
import kotlin.collections.filter
import org.dandelion.classic.blocks.manager.BlockRegistry
import org.dandelion.classic.blocks.model.Block
import org.dandelion.classic.entity.Entity
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.level.generator.LevelGenerator
import org.dandelion.classic.level.io.ClassicWorldLevelDeserializer
import org.dandelion.classic.level.io.ClassicWorldLevelSerializer
import org.dandelion.classic.level.io.DandelionLevelDeserializer
import org.dandelion.classic.level.io.DandelionLevelSerializer
import org.dandelion.classic.level.io.LevelDeserializer
import org.dandelion.classic.level.io.LevelSerializer
import org.dandelion.classic.network.packets.classic.server.ServerDespawnPlayer
import org.dandelion.classic.network.packets.classic.server.ServerSetBlock
import org.dandelion.classic.network.packets.cpe.server.ServerBulkBlockUpdate
import org.dandelion.classic.network.packets.cpe.server.ServerEnvColors
import org.dandelion.classic.network.packets.cpe.server.ServerEnvWeatherType
import org.dandelion.classic.network.packets.cpe.server.ServerLightingMode
import org.dandelion.classic.network.packets.cpe.server.ServerSetMapEnvProperty
import org.dandelion.classic.network.packets.cpe.server.ServerSetMapEnvUrl
import org.dandelion.classic.server.Console
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.enums.LightingMode
import org.dandelion.classic.types.extensions.Color
import org.dandelion.classic.types.vec.IVec
import org.dandelion.classic.types.vec.SVec

class Level(
    val id: String,
    val author: String,
    var description: String,
    val size: SVec,
    val targetFormat: String = "dlvl",
    val blockData: ByteArray = ByteArray(size.x * size.y * size.z) { 0x00 },
    var blockData2: ByteArray? = null,
    var spawn: Position = Position(size.x / 2, (size.y / 2) + 1, size.z / 2),
    var extraData: String = "",
    val timeCreated: Long = System.currentTimeMillis(),
    var autoSave: Boolean = true,
) {
    private val MAX_ENTITIES = 255

    internal val availableEntityIds = ArrayDeque<Byte>(MAX_ENTITIES)
    internal val entities = HashMap<Byte, Entity>(MAX_ENTITIES)

    init {
        initializeEntityIdPool()
    }

    // region Environment Control
    var texturePackUrl: String = ""
        set(value) {
            field = value
            if (value.isNotEmpty()) {
                ServerSetMapEnvUrl(value)
                    .send(getPlayers().filter { it.supports("EnvMapAspect") })
            }
        }

    var sideBlock: Short = 7
        set(value) {
            if (value >= 768) {
                Console.errLog("Invalid block ID for sideBlock: $value.")
                return
            }
            field = value
            ServerSetMapEnvProperty(0, value.toInt())
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    var edgeBlock: Short = 8
        set(value) {
            if (value >= 768) {
                Console.errLog("Invalid block ID for edgeBlock: $value")
                return
            }
            field = value
            ServerSetMapEnvProperty(1, value.toInt())
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    var edgeHeight: Int = size.y / 2
        set(value) {
            field = value
            ServerSetMapEnvProperty(2, value)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    var cloudsHeight: Int = size.y + 2
        set(value) {
            field = value
            ServerSetMapEnvProperty(3, value)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    var maxFogDistance: Int = 0
        set(value) {
            field = value
            ServerSetMapEnvProperty(4, value)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    var cloudsSpeed: Int = 256
        set(value) {
            field = value
            ServerSetMapEnvProperty(5, value)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    var weatherSpeed: Int = 256
        set(value) {
            field = value
            ServerSetMapEnvProperty(6, value)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    var weatherFade: Int = 128
        set(value) {
            field = value
            ServerSetMapEnvProperty(7, value)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    var exponentialFog: Boolean = false
        set(value) {
            field = value
            val exponentialFogValue = if (value) 1 else 0
            ServerSetMapEnvProperty(8, exponentialFogValue)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    var sidesOffset: Int = -2
        set(value) {
            field = value
            ServerSetMapEnvProperty(9, value)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    var weatherType: Byte = 0
        set(value) {
            when {
                value in 0..2 -> {
                    field = value
                    ServerEnvWeatherType(value)
                        .send(
                            getPlayers().filter {
                                it.supports("EnvWeatherType")
                            }
                        )
                }
                else -> Console.warnLog("Invalid weather type $value, ignoring")
            }
        }

    var skyColor: Color? = null
        set(value) {
            field = value
            ServerEnvColors(
                    0,
                    value?.red ?: -1,
                    value?.green ?: -1,
                    value?.blue ?: -1,
                )
                .send(getPlayers().filter { it.supports("EnvColors") })
        }

    var cloudColor: Color? = null
        set(value) {
            field = value
            ServerEnvColors(
                    1,
                    value?.red ?: -1,
                    value?.green ?: -1,
                    value?.blue ?: -1,
                )
                .send(getPlayers().filter { it.supports("EnvColors") })
        }

    var fogColor: Color? = null
        set(value) {
            field = value
            ServerEnvColors(
                    2,
                    value?.red ?: -1,
                    value?.green ?: -1,
                    value?.blue ?: -1,
                )
                .send(getPlayers().filter { it.supports("EnvColors") })
        }

    var ambientLightColor: Color? = null
        set(value) {
            field = value
            ServerEnvColors(
                    3,
                    value?.red ?: -1,
                    value?.green ?: -1,
                    value?.blue ?: -1,
                )
                .send(getPlayers().filter { it.supports("EnvColors") })
        }

    var diffuseLightColor: Color? = null
        set(value) {
            field = value
            ServerEnvColors(
                    4,
                    value?.red ?: -1,
                    value?.green ?: -1,
                    value?.blue ?: -1,
                )
                .send(getPlayers().filter { it.supports("EnvColors") })
        }

    var skyboxColor: Color? = null
        set(value) {
            field = value
            ServerEnvColors(
                    5,
                    value?.red ?: -1,
                    value?.green ?: -1,
                    value?.blue ?: -1,
                )
                .send(getPlayers().filter { it.supports("EnvColors") })
        }

    var lightingMode: LightingMode = LightingMode.CLIENT_LOCAL
        set(value) {
            field = value
            ServerLightingMode(value, lightingModeLocked)
                .send(getPlayers().filter { it.supports("LightingMode") })
        }

    var lightingModeLocked: Boolean = true
        set(value) {
            field = value
            ServerLightingMode(lightingMode, value)
                .send(getPlayers().filter { it.supports("LightingMode") })
        }

    fun setLightingMode(mode: LightingMode, locked: Boolean = true) {
        lightingMode = mode
        lightingModeLocked = locked
        ServerLightingMode(mode, locked)
            .send(getPlayers().filter { it.supports("LightingMode") })
    }

    // endregion
    fun registerBlockDef(block: Block) {
        BlockRegistry.register(this, block)
    }

    fun unregisterBlockDef(blockId: UShort): Boolean {
        return BlockRegistry.unregister(this, blockId)
    }

    fun getBlockDef(blockId: UShort): Block? {
        return BlockRegistry.get(this, blockId)
    }

    fun sendAllCustomData(player: Player) {
        if (player.supports("EnvWeatherType")) {
            ServerEnvWeatherType(weatherType).send(player)
        }

        if (player.supports("LightingMode")) {
            ServerLightingMode(lightingMode, lightingModeLocked).send(player)
        }

        listOf(
                0 to skyColor,
                1 to cloudColor,
                2 to fogColor,
                3 to ambientLightColor,
                4 to diffuseLightColor,
                5 to skyboxColor,
            )
            .forEach { (variable, color) ->
                color?.let {
                    if (player.supports("EnvColors")) {
                        ServerEnvColors(
                                variable.toByte(),
                                it.red,
                                it.green,
                                it.blue,
                            )
                            .send(player)
                    }
                }
            }

        if (texturePackUrl.isNotEmpty() && player.supports("EnvMapAspect")) {
            ServerSetMapEnvUrl(texturePackUrl).send(player)
        }

        val exponentialFogValue = if (exponentialFog) 1 else 0
        listOf(
                0 to sideBlock.toInt(),
                1 to edgeBlock.toInt(),
                2 to edgeHeight,
                3 to cloudsHeight,
                4 to maxFogDistance,
                5 to cloudsSpeed,
                6 to weatherSpeed,
                7 to weatherFade,
                8 to exponentialFogValue,
                9 to sidesOffset,
            )
            .forEach { (propertyType, value) ->
                if (player.supports("EnvMapAspect")) {
                    ServerSetMapEnvProperty(propertyType.toByte(), value)
                        .send(player.channel)
                }
            }
    }

    private fun initializeEntityIdPool() {
        for (id in 0 until MAX_ENTITIES) {
            availableEntityIds.addFirst(id.toByte())
        }
    }

    fun isFull(): Boolean {
        return availableEntityIds.isEmpty()
    }

    fun getAvailableIds(): Int {
        return availableEntityIds.size
    }

    fun entityCount(): Int {
        return entities.size
    }

    fun playerCount(): Int {
        return getPlayers().size
    }

    private fun getNextAvailableId(): Byte? {
        return availableEntityIds.removeFirstOrNull()
    }

    private fun freeId(entityId: Byte) {
        availableEntityIds.addFirst(entityId)
    }

    fun getPlayers(): List<Player> {
        return entities.values.filterIsInstance<Player>()
    }

    fun getNonPlayerEntities(): List<Entity> {
        return entities.values.filter { it !is Player }
    }

    fun getAllEntities(): List<Entity> {
        return entities.values.toList()
    }

    fun tryAddEntity(entity: Entity): Boolean {
        entity.level?.removeEntity(entity)

        val entityId = getNextAvailableId() ?: return false

        entity.entityId = entityId
        entities[entityId] = entity
        entity.level = this
        return true
    }

    fun removeEntity(entity: Entity) {
        if (!isEntityInLevel(entity)) {
            Console.warnLog(
                "Attempted to remove entity '${entity.name}' that isn't in level '$id'"
            )
            return
        }
        removeEntityById(entity.entityId)
    }

    fun removeEntityById(entityId: Byte) {
        val entity = entities[entityId]
        if (entity == null) {
            Console.warnLog(
                "Attempted to remove entity with ID $entityId that doesn't exist in level '$id'"
            )
            return
        }

        if (entity is Player) {
            broadcastEntityDespawn(entityId)
        }

        freeId(entityId)
        entities.remove(entityId)
        entity.entityId = -1
        entity.level = null
    }

    private fun broadcastEntityDespawn(despawnedEntityId: Byte) {
        getPlayers()
            .filter { it.entityId != despawnedEntityId }
            .forEach { player ->
                ServerDespawnPlayer(despawnedEntityId).send(player.channel)
            }
    }

    fun getNonPlayerEntity(entityId: Byte): Entity? {
        val entity = entities[entityId]
        return if (entity !is Player) entity else null
    }

    fun getEntity(entityId: Byte): Entity? {
        return entities[entityId]
    }

    fun getPlayer(entityId: Byte): Player? {
        val entity = entities[entityId]
        return if (entity is Player) entity else null
    }

    fun isEntityInLevel(entity: Entity): Boolean {
        return entities.values.any { it === entity }
    }

    fun isPlayerInLevel(player: Player): Boolean {
        return entities.values.any { it === player }
    }

    fun setBlocks(positions: List<Position>, blockTypes: List<UShort>) {
        if (positions.size != blockTypes.size) {
            Console.warnLog(
                "Positions and block types lists must have the same size"
            )
            return
        }

        val validUpdates =
            positions.zip(blockTypes).filter { (position, _) ->
                isValidBlockPosition(
                    position.x.toInt(),
                    position.y.toInt(),
                    position.z.toInt(),
                )
            }
        if (validUpdates.isEmpty()) return
        validUpdates.forEach { (position, blockType) ->
            val blockIndex =
                calculateBlockIndex(
                    position.x.toInt(),
                    position.y.toInt(),
                    position.z.toInt(),
                )
            val lowByte = (blockType.toInt() and 0xFF).toByte()
            blockData[blockIndex] = lowByte
            val high = (blockType.toInt() ushr 8) and 0xFF
            if (high > 0) {
                if (blockData2 == null) {
                    blockData2 = ByteArray(size.x * size.y * size.z) { 0x00 }
                }
                blockData2!![blockIndex] = high.toByte()
            } else {
                blockData2?.set(blockIndex, 0)
            }
        }

        val players = getPlayers()

        val bulkExtPlayers =
            players.filter {
                it.supports("BulkBlockUpdate") && it.supports("ExtendedBlocks")
            }
        val bulkOnlyPlayers =
            players.filter {
                it.supports("BulkBlockUpdate") && !it.supports("ExtendedBlocks")
            }
        val extOnlyPlayers =
            players.filter {
                !it.supports("BulkBlockUpdate") && it.supports("ExtendedBlocks")
            }
        val classicPlayers =
            players.filter {
                !it.supports("BulkBlockUpdate") &&
                    !it.supports("ExtendedBlocks")
            }

        if (bulkExtPlayers.isNotEmpty()) {
            validUpdates.chunked(256).forEach { chunk ->
                val indices =
                    IntArray(chunk.size) { i ->
                        val position = chunk[i].first
                        calculateBlockIndex(
                            position.x.toInt(),
                            position.y.toInt(),
                            position.z.toInt(),
                        )
                    }
                val blocksUShort =
                    UShortArray(chunk.size) { i ->
                        getBlock(
                            chunk[i].first.x.toInt(),
                            chunk[i].first.y.toInt(),
                            chunk[i].first.z.toInt(),
                        )
                    }
                val bulkPacket = ServerBulkBlockUpdate(indices, blocksUShort)
                bulkExtPlayers.forEach { bulkPacket.send(it) }
            }
        }
        if (bulkOnlyPlayers.isNotEmpty()) {
            validUpdates.chunked(256).forEach { chunk ->
                val indices =
                    IntArray(chunk.size) { i ->
                        val position = chunk[i].first
                        calculateBlockIndex(
                            position.x.toInt(),
                            position.y.toInt(),
                            position.z.toInt(),
                        )
                    }
                val blocksUShort =
                    UShortArray(chunk.size) { i -> chunk[i].second }
                val bulkPacket = ServerBulkBlockUpdate(indices, blocksUShort)
                bulkOnlyPlayers.forEach { bulkPacket.send(it) }
            }
        }
        if (extOnlyPlayers.isNotEmpty()) {
            validUpdates.forEach { (position, blockType) ->
                val packet =
                    ServerSetBlock(
                        position.x.toInt().toShort(),
                        position.y.toInt().toShort(),
                        position.z.toInt().toShort(),
                        blockType,
                        useExtendedBlocks = true,
                    )
                extOnlyPlayers.forEach { packet.send(it) }
            }
        }
        if (classicPlayers.isNotEmpty()) {
            validUpdates.forEach { (position, blockType) ->
                val packet =
                    ServerSetBlock(
                        position.x.toInt().toShort(),
                        position.y.toInt().toShort(),
                        position.z.toInt().toShort(),
                        blockType,
                        useExtendedBlocks = false,
                    )
                classicPlayers.forEach { packet.send(it) }
            }
        }
    }

    fun setBlocks(positions: List<Position>, blockType: UShort) {
        val blockTypes = List(positions.size) { blockType }
        setBlocks(positions, blockTypes)
    }

    fun setBlock(position: Position, block: Block) {
        setBlock(
            position.x.toInt(),
            position.y.toInt(),
            position.z.toInt(),
            block.id,
        )
    }

    fun setBlock(position: Position, blockType: UShort) {
        setBlock(
            position.x.toInt(),
            position.y.toInt(),
            position.z.toInt(),
            blockType,
        )
    }

    fun setBlock(position: IVec, block: Block) {
        setBlock(position.x, position.y, position.z, block.id)
    }

    fun setBlock(position: IVec, blockType: UShort) {
        setBlock(position.x, position.y, position.z, blockType)
    }

    fun setBlock(x: Short, y: Short, z: Short, block: Block) {
        setBlock(x, y, z, block.id)
    }

    fun setBlock(x: Short, y: Short, z: Short, blockType: UShort) {
        setBlock(x.toInt(), y.toInt(), z.toInt(), blockType)
    }

    fun setBlock(x: Int, y: Int, z: Int, block: Block) {
        setBlock(x, y, z, block.id)
    }

    fun setBlock(x: Int, y: Int, z: Int, blockType: UShort) {
        if (!isValidBlockPosition(x, y, z)) {
            Console.warnLog(
                "Attempted to set block at invalid position ($x, $y, $z) in level '$id'"
            )
            return
        }

        val blockIndex = calculateBlockIndex(x, y, z)

        blockData[blockIndex] = (blockType.toInt() and 0xFF).toByte()

        val high = (blockType.toInt() ushr 8) and 0xFF
        if (high > 0) {
            if (blockData2 == null) {
                blockData2 = ByteArray(size.x * size.y * size.z) { 0x00 }
            }
            blockData2!![blockIndex] = high.toByte()
        } else {
            blockData2?.set(blockIndex, 0)
        }

        broadcastBlockChange(x.toShort(), y.toShort(), z.toShort(), blockType)
    }

    fun fillBlocks(start: Position, end: Position, block: Block) {
        fillBlocks(
            start.x.toInt(),
            start.y.toInt(),
            start.z.toInt(),
            end.x.toInt(),
            end.y.toInt(),
            end.z.toInt(),
            block.id,
        )
    }

    fun fillBlocks(start: Position, end: Position, blockType: UShort) {
        fillBlocks(
            start.x.toInt(),
            start.y.toInt(),
            start.z.toInt(),
            end.x.toInt(),
            end.y.toInt(),
            end.z.toInt(),
            blockType,
        )
    }

    fun fillBlocks(start: IVec, end: IVec, block: Block) {
        fillBlocks(start.x, start.y, start.z, end.x, end.y, end.z, block.id)
    }

    fun fillBlocks(start: IVec, end: IVec, blockType: UShort) {
        fillBlocks(start.x, start.y, start.z, end.x, end.y, end.z, blockType)
    }

    fun fillBlocks(
        startX: Int,
        startY: Int,
        startZ: Int,
        endX: Int,
        endY: Int,
        endZ: Int,
        block: Block,
    ) {
        fillBlocks(startX, startY, startZ, endX, endY, endZ, block.id)
    }

    fun fillBlocks(
        startX: Int,
        startY: Int,
        startZ: Int,
        endX: Int,
        endY: Int,
        endZ: Int,
        blockType: UShort,
    ) {
        val minX = minOf(startX, endX)
        val maxX = maxOf(startX, endX)
        val minY = minOf(startY, endY)
        val maxY = maxOf(startY, endY)
        val minZ = minOf(startZ, endZ)
        val maxZ = maxOf(startZ, endZ)

        if (!isValidFillArea(minX, minY, minZ, maxX, maxY, maxZ)) {
            Console.warnLog(
                "Attempted to fill blocks outside level bounds in level '$id'"
            )
            return
        }

        performBlockFill(minX, minY, minZ, maxX, maxY, maxZ, blockType)
        notifyPlayersOfAreaChange(minX, minY, minZ, maxX, maxY, maxZ, blockType)
    }

    private fun performBlockFill(
        minX: Int,
        minY: Int,
        minZ: Int,
        maxX: Int,
        maxY: Int,
        maxZ: Int,
        blockType: UShort,
    ) {
        val lowByte = (blockType.toInt() and 0xFF).toByte()
        val highByte = ((blockType.toInt() ushr 8) and 0xFF).toByte()
        val hasHighByte = highByte != 0.toByte()
        if (hasHighByte && blockData2 == null) {
            blockData2 = ByteArray(size.x * size.y * size.z) { 0x00 }
        }
        for (y in minY..maxY) {
            val yOffset = y * size.x * size.z
            for (z in minZ..maxZ) {
                val zOffset = z * size.x
                for (x in minX..maxX) {
                    val index = x + zOffset + yOffset

                    blockData[index] = lowByte
                    if (hasHighByte) {
                        blockData2!![index] = highByte
                    } else {
                        blockData2?.set(index, 0)
                    }
                }
            }
        }
    }

    fun getBlock(position: Position): UShort {
        return getBlock(
            position.x.toInt(),
            position.y.toInt(),
            position.z.toInt(),
        )
    }

    fun getBlock(position: IVec): UShort {
        return getBlock(position.x, position.y, position.z)
    }

    fun getBlock(x: Short, y: Short, z: Short): UShort {
        return getBlock(x.toInt(), y.toInt(), z.toInt())
    }

    fun getBlock(x: Int, y: Int, z: Int): UShort {
        if (!isValidBlockPosition(x, y, z)) return 0u

        val blockIndex = calculateBlockIndex(x, y, z)
        val low = blockData[blockIndex].toInt() and 0xFF
        val high = blockData2?.get(blockIndex)?.toInt()?.and(0xFF) ?: 0
        return (low or (high shl 8)).toUShort()
    }

    fun kickAllPlayers(reason: String = "You have been kicked from the level") {
        getPlayers().forEach { player -> player.kick(reason) }
    }

    fun broadcast(message: String) {
        broadcast(message, 0x00)
    }

    fun broadcast(message: String, messageTypeId: Byte = 0x00) {
        getPlayers().forEach { player ->
            player.sendMessage(message, messageTypeId)
        }
    }

    fun generateLevel(generator: LevelGenerator, parameters: String) {
        generator.generate(this, parameters)
    }

    fun spawnPlayerInLevel(player: Player) {
        getAllEntities()
            .filter { it.entityId != player.entityId }
            .forEach { entity -> player.mutualSpawn(entity) }
    }

    fun spawnEntityInLevel(entity: Entity) {
        getPlayers().forEach { player -> entity.spawnFor(player) }
    }

    fun save(serializer: LevelSerializer, file: File) {
        serializer.serialize(this, file)
    }

    fun save(serializer: LevelSerializer, path: String) {
        serializer.serialize(this, File(path))
    }

    fun save(serializer: LevelSerializer) {
        save(serializer, File("levels/$id.$targetFormat"))
    }

    fun save(file: File) {
        save(DandelionLevelSerializer(), file)
    }

    fun save(path: String) {
        save(DandelionLevelSerializer(), path)
    }

    fun save() {
        val serializer =
            if (targetFormat.equals("cw", true)) ClassicWorldLevelSerializer()
            else DandelionLevelSerializer()
        save(serializer)
    }

    private fun isValidBlockPosition(x: Int, y: Int, z: Int): Boolean {
        return x >= 0 &&
            y >= 0 &&
            z >= 0 &&
            x < size.x &&
            y < size.y &&
            z < size.z
    }

    private fun isValidFillArea(
        minX: Int,
        minY: Int,
        minZ: Int,
        maxX: Int,
        maxY: Int,
        maxZ: Int,
    ): Boolean {
        return minX >= 0 &&
            maxX < size.x &&
            minY >= 0 &&
            maxY < size.y &&
            minZ >= 0 &&
            maxZ < size.z
    }

    private fun calculateBlockIndex(x: Int, y: Int, z: Int): Int {
        return x + (z * size.x) + (y * size.x * size.z)
    }

    private fun broadcastBlockChange(
        x: Short,
        y: Short,
        z: Short,
        blockType: UShort,
    ) {
        getPlayers().forEach { player ->
            player.updateBlock(x, y, z, blockType)
        }
    }

    private fun notifyPlayersOfAreaChange(
        minX: Int,
        minY: Int,
        minZ: Int,
        maxX: Int,
        maxY: Int,
        maxZ: Int,
        blockType: UShort,
    ) {
        val positions = mutableListOf<Position>()
        for (y in minY..maxY) {
            for (z in minZ..maxZ) {
                for (x in minX..maxX) {
                    positions.add(Position(x.toFloat(), y.toFloat(), z.toFloat()))
                }
            }
        }
        if (positions.isNotEmpty()) {
            setBlocks(positions, blockType)
        }
    }

    companion object {
        fun load(deserializer: LevelDeserializer, file: File): Level? =
            deserializer.deserialize(file)

        fun load(deserializer: LevelDeserializer, path: String): Level? =
            deserializer.deserialize(File(path))

        fun load(file: File): Level? {
            if (file.extension == "dlvl")
                return DandelionLevelDeserializer().deserialize(file)
            if (file.extension == "cw")
                return ClassicWorldLevelDeserializer().deserialize(file)

            return null
        }

        fun load(path: String): Level? = load(File(path))
    }
}
