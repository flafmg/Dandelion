package org.dandelion.classic.level
import org.dandelion.classic.blocks.model.Block
import org.dandelion.classic.level.generator.LevelGenerator
import org.dandelion.classic.level.io.DandelionLevelSerializer
import org.dandelion.classic.level.io.DandelionLevelDeserializer
import org.dandelion.classic.level.io.LevelDeserializer
import org.dandelion.classic.level.io.LevelSerializer
import org.dandelion.classic.network.packets.classic.server.ServerDespawnPlayer
import org.dandelion.classic.entity.Entity
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.server.Console
import org.dandelion.classic.types.Color
import org.dandelion.classic.types.IVec
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.SVec
import java.io.File
import kotlin.collections.filter


/**
 * Represents a game level containing blocks and entities
 *
 * @property id The unique identifier for the level.
 * @property author The author of the level.
 * @property description A description of the level.
 * @property size The dimensions of the level as an [SVec] (x, y, z).
 * @property spawn The default spawn position for players in the level.
 * @property extraData Optional extra data associated with the level.
 * @property timeCreated The timestamp (milliseconds since epoch) when the level was created.
 * @property autoSave Whether the level should be automatically saved periodically.
 */
class Level(
    val id: String,
    val author: String,
    val description: String,
    val size: SVec,
    var spawn: Position,
    var extraData: String = "",
    val timeCreated: Long = System.currentTimeMillis(),
    var autoSave: Boolean = true
) {
    private val MAX_ENTITIES = 255
    internal var blocks: ByteArray = ByteArray(size.x * size.y * size.z) { 0x00 }
    internal val availableEntityIds = ArrayDeque<Byte>(MAX_ENTITIES)
    internal val entities = HashMap<Byte, Entity>(MAX_ENTITIES)

    // Environment properties from SetMapEnvUrl packet
    internal var texturePackUrl: String = ""

    // Environment properties from SetMapEnvProperty packet
    internal var sideBlock: Byte = 7
    internal var edgeBlock: Byte = 8
    internal var edgeHeight: Int = size.y / 2
    internal var cloudsHeight: Int = size.y + 2
    internal var maxFogDistance: Int = 0
    internal var cloudsSpeed: Int = 256
    internal var weatherSpeed: Int = 256
    internal var weatherFade: Int = 128
    internal var exponentialFog: Boolean = false
    internal var sidesOffset: Int = -2

    // Environment property from EnvSetWeatherType packet
    internal var weatherType: Byte = 0

    // Environment properties fom EnvSetColor packet
    internal var skyColor: Color? = null
    internal var cloudColor: Color? = null
    internal var fogColor: Color? = null
    internal var ambientLightColor: Color? = null
    internal var diffuseLightColor: Color? = null
    internal var skyboxColor: Color? = null

    init {
        initializeEntityIdPool()
    }

    /**
     * Sets the texture pack URL for this level (from SetMapEnvUrl)
     *
     * @param url The texture pack URL
     */
    fun setTexturePackUrl(url: String) {
        texturePackUrl = url
    }

    /**
     * Gets the texture pack URL for this level (from SetMapEnvUrl)
     *
     * @return The texture pack URL
     */
    fun getTexturePackUrl(): String = texturePackUrl

    /**
     * Sets the map sides block ID (from SetMapEnvProperty)
     *
     * @param blockId The block ID for map sides
     */
    fun setSideBlock(blockId: Byte) {
        sideBlock = blockId
    }

    /**
     * Gets the map sides block ID (from SetMapEnvProperty)
     *
     * @return The block ID for map sides
     */
    fun getSideBlock(): Byte = sideBlock

    /**
     * Sets the map edge/horizon block ID (from SetMapEnvProperty)
     *
     * @param blockId The block ID for map edge
     */
    fun setEdgeBlock(blockId: Byte) {
        edgeBlock = blockId
    }

    /**
     * Gets the map edge/horizon block ID (from SetMapEnvProperty)
     *
     * @return The block ID for map edge
     */
    fun getEdgeBlock(): Byte = edgeBlock

    /**
     * Sets the map edge height (from SetMapEnvProperty)
     *
     * @param height The edge height
     */
    fun setEdgeHeight(height: Int) {
        edgeHeight = height
    }

    /**
     * Gets the map edge height (from SetMapEnvProperty)
     *
     * @return The edge height
     */
    fun getEdgeHeight(): Int = edgeHeight

    /**
     * Sets the map clouds height (from SetMapEnvProperty)
     *
     * @param height The clouds height
     */
    fun setCloudsHeight(height: Int) {
        cloudsHeight = height
    }

    /**
     * Gets the map clouds height (from SetMapEnvProperty)
     *
     * @return The clouds height
     */
    fun getCloudsHeight(): Int = cloudsHeight

    /**
     * Sets the max fog/view distance (from SetMapEnvProperty)
     *
     * @param distance The max fog distance
     */
    fun setMaxFogDistance(distance: Int) {
        maxFogDistance = distance
    }

    /**
     * Gets the max fog/view distance (from SetMapEnvProperty)
     *
     * @return The max fog distance
     */
    fun getMaxFogDistance(): Int = maxFogDistance

    /**
     * Sets the clouds speed (from SetMapEnvProperty)
     *
     * @param speed The clouds speed * 256
     */
    fun setCloudsSpeed(speed: Int) {
        cloudsSpeed = speed
    }

    /**
     * Gets the clouds speed (from SetMapEnvProperty)
     *
     * @return The clouds speed * 256
     */
    fun getCloudsSpeed(): Int = cloudsSpeed

    /**
     * Sets the weather speed (from SetMapEnvProperty)
     *
     * @param speed The weather speed * 256
     */
    fun setWeatherSpeed(speed: Int) {
        weatherSpeed = speed
    }

    /**
     * Gets the weather speed (from SetMapEnvProperty)
     *
     * @return The weather speed * 256
     */
    fun getWeatherSpeed(): Int = weatherSpeed

    /**
     * Sets the weather fade (from SetMapEnvProperty)
     *
     * @param fade The weather fade * 128
     */
    fun setWeatherFade(fade: Int) {
        weatherFade = fade
    }

    /**
     * Gets the weather fade (from SetMapEnvProperty)
     *
     * @return The weather fade * 128
     */
    fun getWeatherFade(): Int = weatherFade

    /**
     * Sets whether to use exponential fog (from SetMapEnvProperty)
     *
     * @param useExponential Whether to use exponential fog
     */
    fun setExponentialFog(useExponential: Boolean) {
        exponentialFog = useExponential
    }

    /**
     * Gets whether to use exponential fog (from SetMapEnvProperty)
     *
     * @return Whether to use exponential fog
     */
    fun getExponentialFog(): Boolean = exponentialFog

    /**
     * Sets the offset of map sides height from map edge height (from SetMapEnvProperty)
     *
     * @param offset The sides offset
     */
    fun setSidesOffset(offset: Int) {
        sidesOffset = offset
    }

    /**
     * Gets the offset of map sides height from map edge height (from SetMapEnvProperty)
     *
     * @return The sides offset
     */
    fun getSidesOffset(): Int = sidesOffset

    /**
     * Sets the weather type (from EnvSetWeatherType)
     *
     * @param type The weather type (0 = sunny, 1 = raining, 2 = snowing)
     */
    fun setWeatherType(type: Byte) {
        if (type in 0..2) {
            weatherType = type
        } else {
            Console.warnLog("Invalid weather type $type, ignoring")
        }
    }

    /**
     * Gets the weather type (from EnvSetWeatherType)
     *
     * @return The weather type (0 = sunny, 1 = raining, 2 = snowing)
     */
    fun getWeatherType(): Byte = weatherType

    /**
     * Sets the sky color (from EnvSetColor)
     *
     * @param color The sky color, null to reset to default
     */
    fun setSkyColor(color: Color?) {
        skyColor = color
    }

    /**
     * Gets the sky color (from EnvSetColor)
     *
     * @return The sky color, null means default
     */
    fun getSkyColor(): Color? = skyColor

    /**
     * Sets the cloud color (from EnvSetColor)
     *
     * @param color The cloud color, null to reset to default
     */
    fun setCloudColor(color: Color?) {
        cloudColor = color
    }

    /**
     * Gets the cloud color (from EnvSetColor)
     *
     * @return The cloud color, null means default
     */
    fun getCloudColor(): Color? = cloudColor

    /**
     * Sets the fog color (from EnvSetColor)
     *
     * @param color The fog color, null to reset to default
     */
    fun setFogColor(color: Color?) {
        fogColor = color
    }

    /**
     * Gets the fog color (from EnvSetColor)
     *
     * @return The fog color, null means default
     */
    fun getFogColor(): Color? = fogColor

    /**
     * Sets the ambient light color (from EnvSetColor)
     *
     * @param color The ambient light color, null to reset to default
     */
    fun setAmbientLightColor(color: Color?) {
        ambientLightColor = color
    }

    /**
     * Gets the ambient light color (from EnvSetColor)
     *
     * @return The ambient light color, null means default
     */
    fun getAmbientLightColor(): Color? = ambientLightColor

    /**
     * Sets the diffuse light color (from EnvSetColor)
     *
     * @param color The diffuse light color, null to reset to default
     */
    fun setDiffuseLightColor(color: Color?) {
        diffuseLightColor = color
    }

    /**
     * Gets the diffuse light color (from EnvSetColor)
     *
     * @return The diffuse light color, null means default
     */
    fun getDiffuseLightColor(): Color? = diffuseLightColor

    /**
     * Sets the skybox color (from EnvSetColor)
     *
     * @param color The skybox color, null to reset to default
     */
    fun setSkyboxColor(color: Color?) {
        skyboxColor = color
    }

    /**
     * Gets the skybox color (from EnvSetColor)
     *
     * @return The skybox color, null means default
     */
    fun getSkyboxColor(): Color? = skyboxColor

    /**
     * Initializes the pool of available entity IDs (0-254, 255 reserved for player's own view)
     */
    private fun initializeEntityIdPool() {
        for (id in 0 until MAX_ENTITIES) {
            availableEntityIds.addFirst(id.toByte())
        }
    }
    /**
     * Checks if the level has reached maximum entity capacity
     *
     * @return `true` if the level is full, `false` otherwise.
     */
    fun isFull(): Boolean {
        return availableEntityIds.isEmpty()
    }
    /**
     * Gets the number of available entity slots
     *
     * @return The number of entity IDs currently available for assignment.
     */
    fun getAvailableIds(): Int {
        return availableEntityIds.size
    }
    /**
     * Gets the total number of entities in the level
     *
     * @return The count of all entities currently in the level.
     */
    fun entityCount(): Int {
        return entities.size
    }
    /**
     * Gets the number of players currently in the level
     *
     * @return The count of player entities currently in the level.
     */
    fun playerCount(): Int {
        return getPlayers().size
    }
    /**
     * Gets the next available entity ID from the pool
     *
     * @return The next available [Byte] entity ID, or `null` if none are available.
     */
    private fun getNextAvailableId(): Byte? {
        return availableEntityIds.removeFirstOrNull()
    }
    /**
     * Returns an entity ID to the available pool
     *
     * @param entityId The [Byte] entity ID to return to the pool.
     */
    private fun freeId(entityId: Byte) {
        availableEntityIds.addFirst(entityId)
    }
    /**
     * Gets all players currently in the level
     *
     * @return A list of all [Player] entities in the level.
     */
    fun getPlayers(): List<Player> {
        return entities.values.filterIsInstance<Player>()
    }
    /**
     * Gets all non-player entities in the level
     *
     * @return A list of all entities in the level that are not players.
     */
    fun getNonPlayerEntities(): List<Entity> {
        return entities.values.filter { it !is Player }
    }
    /**
     * Gets all entities in the level
     *
     * @return A list of all [Entity] instances in the level.
     */
    fun getAllEntities(): List<Entity> {
        return entities.values.toList()
    }
    /**
     * Attempts to assign an entity ID to an entity and add it to the level
     *
     * @param entity The [Entity] to add to the level.
     * @return `true` if the entity was successfully added, `false` if the level is full.
     */
    fun tryAddEntity(entity: Entity): Boolean {
        entity.level?.removeEntity(entity)
        val entityId = getNextAvailableId() ?: return false
        entity.entityId = entityId
        entities[entityId] = entity
        entity.level = this
        return true
    }
    /**
     * Removes an entity from the level by entity reference
     *
     * @param entity The [Entity] instance to remove.
     */
    fun removeEntity(entity: Entity) {
        if (!isEntityInLevel(entity)) {
            Console.warnLog("Attempted to remove entity '${entity.name}' that isn't in level '$id'")
            return
        }
        removeEntityById(entity.entityId)
    }
    /**
     * Removes an entity from the level by entity ID
     *
     * @param entityId The [Byte] ID of the entity to remove.
     */
    fun removeEntityById(entityId: Byte) {
        val entity = entities[entityId] ?: run {
            Console.warnLog("Attempted to remove entity with ID $entityId that doesn't exist in level '$id'")
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
    /**
     * Notifies all other players when a player despawns
     *
     * @param despawnedEntityId The [Byte] ID of the entity that despawned.
     */
    private fun broadcastEntityDespawn(despawnedEntityId: Byte) {
        getPlayers()
            .filter { it.entityId != despawnedEntityId }
            .forEach { player ->
                ServerDespawnPlayer(despawnedEntityId).send(player.channel)
            }
    }
    /**
     * Gets an entity by its ID
     *
     * @param entityId The [Byte] ID of the entity to find.
     * @return The [Entity] instance if found, `null` otherwise.
     */
    fun findEntityById(entityId: Byte): Entity? {
        return entities[entityId]
    }
    /**
     * Gets a player by their entity ID
     *
     * @param entityId The [Byte] ID of the player entity to find.
     * @return The [Player] instance if found, `null` otherwise.
     */
    fun findPlayerById(entityId: Byte): Player? {
        val entity = entities[entityId]
        return if (entity is Player) entity else null
    }
    /**
     * Checks if an entity is present in this level
     *
     * @param entity The [Entity] instance to check.
     * @return `true` if the entity is in this level, `false` otherwise.
     */
    fun isEntityInLevel(entity: Entity): Boolean {
        return entities.values.any { it === entity }
    }
    /**
     * Checks if a player is present in this level
     *
     * @param player The [Player] instance to check.
     * @return `true` if the player is in this level, `false` otherwise.
     */
    fun isPlayerInLevel(player: Player): Boolean {
        return entities.values.any { it === player }
    }
    /**
     * Sets a block at the specified position using Position object
     *
     * @param position The [Position] where the block should be set.
     * @param block The [Block] type to set.
     */
    fun setBlock(position: Position, block: Block) {
        setBlock(position.x.toInt(), position.y.toInt(), position.z.toInt(), block.id)
    }
    /**
     * Sets a block at the specified position using Position object and byte block type
     *
     * @param position The [Position] where the block should be set.
     * @param blockType The [Byte] block type ID to set.
     */
    fun setBlock(position: Position, blockType: Byte) {
        setBlock(position.x.toInt(), position.y.toInt(), position.z.toInt(), blockType)
    }
    /**
     * Sets a block at the specified position using IVec and Block enum
     *
     * @param position The [IVec] coordinates where the block should be set.
     * @param block The [Block] type to set.
     */
    fun setBlock(position: IVec, block: Block) {
        setBlock(position.x, position.y, position.z, block.id)
    }
    /**
     * Sets a block at the specified position using IVec and byte block type
     *
     * @param position The [IVec] coordinates where the block should be set.
     * @param blockType The [Byte] block type ID to set.
     */
    fun setBlock(position: IVec, blockType: Byte) {
        setBlock(position.x, position.y, position.z, blockType)
    }
    /**
     * Sets a block at the specified coordinates using Short values and Block enum
     *
     * @param x The X coordinate (Short) where the block should be set.
     * @param y The Y coordinate (Short) where the block should be set.
     * @param z The Z coordinate (Short) where the block should be set.
     * @param block The [Block] type to set.
     */
    fun setBlock(x: Short, y: Short, z: Short, block: Block) {
        setBlock(x, y, z, block.id)
    }
    /**
     * Sets a block at the specified coordinates using Short values and byte block type
     *
     * @param x The X coordinate (Short) where the block should be set.
     * @param y The Y coordinate (Short) where the block should be set.
     * @param z The Z coordinate (Short) where the block should be set.
     * @param blockType The [Byte] block type ID to set.
     */
    fun setBlock(x: Short, y: Short, z: Short, blockType: Byte) {
        setBlock(x.toInt(), y.toInt(), z.toInt(), blockType)
    }
    /**
     * Sets a block at the specified coordinates using Int values and Block enum
     *
     * @param x The X coordinate (Int) where the block should be set.
     * @param y The Y coordinate (Int) where the block should be set.
     * @param z The Z coordinate (Int) where the block should be set.
     * @param block The [Block] type to set.
     */
    fun setBlock(x: Int, y: Int, z: Int, block: Block) {
        setBlock(x, y, z, block.id)
    }
    /**
     * Sets a block at the specified coordinates and notifies all players
     *
     * @param x The X coordinate (Int) where the block should be set.
     * @param y The Y coordinate (Int) where the block should be set.
     * @param z The Z coordinate (Int) where the block should be set.
     * @param blockType The [Byte] block type ID to set.
     */
    fun setBlock(x: Int, y: Int, z: Int, blockType: Byte) {
        if (!isValidBlockPosition(x, y, z)) {
            Console.warnLog("Attempted to set block at invalid position ($x, $y, $z) in level '$id'")
            return
        }
        val blockIndex = calculateBlockIndex(x, y, z)
        blocks[blockIndex] = blockType
        broadcastBlockChange(x.toShort(), y.toShort(), z.toShort(), blockType)
    }
    /**
     * Fills a rectangular area with the specified block type
     *
     * @param start The starting [Position] of the area to fill.
     * @param end The ending [Position] of the area to fill.
     * @param block The [Block] type to fill the area with.
     */
    fun fillBlocks(start: Position, end: Position, block: Block) {
        fillBlocks(start.x.toInt(), start.y.toInt(), start.z.toInt(), end.x.toInt(), end.y.toInt(), end.z.toInt(), block.id)
    }
    /**
     * Fills a rectangular area with the specified block type using byte value
     *
     * @param start The starting [Position] of the area to fill.
     * @param end The ending [Position] of the area to fill.
     * @param blockType The [Byte] block type ID to fill the area with.
     */
    fun fillBlocks(start: Position, end: Position, blockType: Byte) {
        fillBlocks(start.x.toInt(), start.y.toInt(), start.z.toInt(), end.x.toInt(), end.y.toInt(), end.z.toInt(), blockType)
    }
    /**
     * Fills a rectangular area with the specified block type using IVec positions
     *
     * @param start The starting [IVec] coordinates of the area to fill.
     * @param end The ending [IVec] coordinates of the area to fill.
     * @param block The [Block] type to fill the area with.
     */
    fun fillBlocks(start: IVec, end: IVec, block: Block) {
        fillBlocks(start.x, start.y, start.z, end.x, end.y, end.z, block.id)
    }
    /**
     * Fills a rectangular area with the specified block type using IVec positions and byte value
     *
     * @param start The starting [IVec] coordinates of the area to fill.
     * @param end The ending [IVec] coordinates of the area to fill.
     * @param blockType The [Byte] block type ID to fill the area with.
     */
    fun fillBlocks(start: IVec, end: IVec, blockType: Byte) {
        fillBlocks(start.x, start.y, start.z, end.x, end.y, end.z, blockType)
    }
    /**
     * Fills a rectangular area with the specified block type using Int coordinates and Block enum
     *
     * @param startX The starting X coordinate (Int) of the area to fill.
     * @param startY The starting Y coordinate (Int) of the area to fill.
     * @param startZ The starting Z coordinate (Int) of the area to fill.
     * @param endX The ending X coordinate (Int) of the area to fill.
     * @param endY The ending Y coordinate (Int) of the area to fill.
     * @param endZ The ending Z coordinate (Int) of the area to fill.
     * @param block The [Block] type to fill the area with.
     */
    fun fillBlocks(startX: Int, startY: Int, startZ: Int, endX: Int, endY: Int, endZ: Int, block: Block) {
        fillBlocks(startX, startY, startZ, endX, endY, endZ, block.id)
    }
    /**
     * Fills a rectangular area with the specified block type
     *
     * @param startX The starting X coordinate (Int) of the area to fill.
     * @param startY The starting Y coordinate (Int) of the area to fill.
     * @param startZ The starting Z coordinate (Int) of the area to fill.
     * @param endX The ending X coordinate (Int) of the area to fill.
     * @param endY The ending Y coordinate (Int) of the area to fill.
     * @param endZ The ending Z coordinate (Int) of the area to fill.
     * @param blockType The [Byte] block type ID to fill the area with.
     */
    fun fillBlocks(startX: Int, startY: Int, startZ: Int, endX: Int, endY: Int, endZ: Int, blockType: Byte) {
        val minX = minOf(startX, endX)
        val maxX = maxOf(startX, endX)
        val minY = minOf(startY, endY)
        val maxY = maxOf(startY, endY)
        val minZ = minOf(startZ, endZ)
        val maxZ = maxOf(startZ, endZ)
        if (!isValidFillArea(minX, minY, minZ, maxX, maxY, maxZ)) {
            Console.warnLog("Attempted to fill blocks outside level bounds in level '$id'")
            return
        }
        performBlockFill(minX, minY, minZ, maxX, maxY, maxZ, blockType)
        notifyPlayersOfAreaChange(minX, minY, minZ, maxX, maxY, maxZ)
    }
    /**
     * Performs the actual block filling operation
     *
     * @param minX The minimum X coordinate of the area to fill.
     * @param minY The minimum Y coordinate of the area to fill.
     * @param minZ The minimum Z coordinate of the area to fill.
     * @param maxX The maximum X coordinate of the area to fill.
     * @param maxY The maximum Y coordinate of the area to fill.
     * @param maxZ The maximum Z coordinate of the area to fill.
     * @param blockType The [Byte] block type ID to fill the area with.
     */
    private fun performBlockFill(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int, blockType: Byte) {
        for (y in minY..maxY) {
            val yOffset = y * size.x * size.z
            for (z in minZ..maxZ) {
                val zOffset = z * size.x
                for (x in minX..maxX) {
                    val index = x + zOffset + yOffset
                    blocks[index] = blockType
                }
            }
        }
    }
    /**
     * Gets the block type at the specified position using Position object
     *
     * @param position The [Position] to get the block from.
     * @return The [Byte] block type ID at the specified position, or 0x00 if the position is invalid.
     */
    fun getBlock(position: Position): Byte {
        return getBlock(position.x.toInt(), position.y.toInt(), position.z.toInt())
    }
    /**
     * Gets the block type at the specified position using IVec
     *
     * @param position The [IVec] coordinates to get the block from.
     * @return The [Byte] block type ID at the specified position, or 0x00 if the position is invalid.
     */
    fun getBlock(position: IVec): Byte {
        return getBlock(position.x, position.y, position.z)
    }
    /**
     * Gets the block type at the specified coordinates using Short values
     *
     * @param x The X coordinate (Short) to get the block from.
     * @param y The Y coordinate (Short) to get the block from.
     * @param z The Z coordinate (Short) to get the block from.
     * @return The [Byte] block type ID at the specified position, or 0x00 if the position is invalid.
     */
    fun getBlock(x: Short, y: Short, z: Short): Byte {
        return getBlock(x.toInt(), y.toInt(), z.toInt())
    }
    /**
     * Gets the block type at the specified coordinates
     *
     * @param x The X coordinate (Int) to get the block from.
     * @param y The Y coordinate (Int) to get the block from.
     * @param z The Z coordinate (Int) to get the block from.
     * @return The [Byte] block type ID at the specified position, or 0x00 if the position is invalid.
     */
    fun getBlock(x: Int, y: Int, z: Int): Byte {
        if (!isValidBlockPosition(x, y, z)) {
            return 0x00
        }
        val blockIndex = calculateBlockIndex(x, y, z)
        return blocks[blockIndex]
    }
    /**
     * Kicks all players from the level
     *
     * @param reason The reason message for kicking the players. Defaults to "You have been kicked from the level".
     */
    fun kickAllPlayers(reason: String = "You have been kicked from the level") {
        getPlayers().forEach { player ->
            player.kick(reason)
        }
    }
    /**
     * Broadcasts a message to all players in the level
     *
     * @param message The message string to broadcast.
     */
    fun broadcast(message: String) {
        broadcast(message, 0x00)
    }
    /**
     * Broadcasts a message with specified message type to all players in the level
     *
     * @param message The message string to broadcast.
     * @param messageTypeId An optional byte identifier for the type of message. Defaults to `0x00`.
     */
    fun broadcast(message: String, messageTypeId: Byte = 0x00) {
        getPlayers().forEach { player ->
            player.sendMessage(message, messageTypeId)
        }
    }
    /**
     * Generates the level using the specified generator and parameters
     *
     * @param generator The [LevelGenerator] to use for creating the level's terrain/blocks.
     * @param parameters Optional parameters for the level generator.
     */
    fun generateLevel(generator: LevelGenerator, parameters: String) {
        generator.generate(this, parameters)
    }
    /**
     * Spawns a player in the level and shows them all existing entities
     *
     * @param player The [Player] to spawn in the level.
     */
    fun spawnPlayerInLevel(player: Player) {
        getAllEntities()
            .filter { it.entityId != player.entityId }
            .forEach { entity ->
                player.mutualSpawn(entity)
            }
    }
    /**
     * Spawns an entity in the level and shows it to all players
     *
     * @param entity The [Entity] to spawn in the level.
     */
    fun spawnEntityInLevel(entity: Entity) {
        getPlayers().forEach { player ->
            entity.spawnFor(player)
        }
    }
    /**
     * Saves the level using the specified serializer and file
     *
     * @param serializer The [LevelSerializer] to use for saving.
     * @param file The [File] to save the level to.
     */
    fun save(serializer: LevelSerializer, file: File) {
        serializer.serialize(this, file)
    }
    /**
     * Saves the level using the specified serializer and file path
     *
     * @param serializer The [LevelSerializer] to use for saving.
     * @param path The file path string to save the level to.
     */
    fun save(serializer: LevelSerializer, path: String) {
        val file = File(path)
        serializer.serialize(this, file)
    }
    /**
     * Saves the level using the specified serializer to the default location
     *
     * @param serializer The [LevelSerializer] to use for saving.
     */
    fun save(serializer: LevelSerializer) {
        val file = File("levels/$id.dlvl")
        serializer.serialize(this, file)
    }
    /**
     * Saves the level using the default serializer and specified file
     *
     * @param file The [File] to save the level to.
     */
    fun save(file: File) {
        save(DandelionLevelSerializer(), file)
    }
    /**
     * Saves the level using the default serializer and specified path
     *
     * @param path The file path string to save the level to.
     */
    fun save(path: String) {
        save(DandelionLevelSerializer(), path)
    }
    /**
     * Saves the level using the default serializer to the default location
     */
    fun save() {
        save(DandelionLevelSerializer())
    }
    /**
     * Validates if the given coordinates are within level bounds
     *
     * @param x The X coordinate to validate.
     * @param y The Y coordinate to validate.
     * @param z The Z coordinate to validate.
     * @return `true` if the coordinates are valid within the level, `false` otherwise.
     */
    private fun isValidBlockPosition(x: Int, y: Int, z: Int): Boolean {
        return x >= 0 && y >= 0 && z >= 0 && x < size.x && y < size.y && z < size.z
    }
    /**
     * Validates if the fill area is within level bounds
     *
     * @param minX The minimum X coordinate of the area.
     * @param minY The minimum Y coordinate of the area.
     * @param minZ The minimum Z coordinate of the area.
     * @param maxX The maximum X coordinate of the area.
     * @param maxY The maximum Y coordinate of the area.
     * @param maxZ The maximum Z coordinate of the area.
     * @return `true` if the area is valid within the level, `false` otherwise.
     */
    private fun isValidFillArea(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int): Boolean {
        return minX >= 0 && maxX < size.x && minY >= 0 && maxY < size.y && minZ >= 0 && maxZ < size.z
    }
    /**
     * Calculates the block index for the given coordinates
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param z The Z coordinate.
     * @return The calculated index in the blocks array.
     */
    private fun calculateBlockIndex(x: Int, y: Int, z: Int): Int {
        return x + (z * size.x) + (y * size.x * size.z)
    }
    /**
     * Notifies all players of a block change
     *
     * @param x The X coordinate (Short) of the changed block.
     * @param y The Y coordinate (Short) of the changed block.
     * @param z The Z coordinate (Short) of the changed block.
     * @param blockType The new [Byte] block type ID at the position.
     */
    private fun broadcastBlockChange(x: Short, y: Short, z: Short, blockType: Byte) {
        getPlayers().forEach { player ->
            player.updateBlock(x, y, z, blockType)
        }
    }
    /**
     * Notifies all players of changes in a filled area (simplified implementation)
     *
     * @param minX The minimum X coordinate of the changed area.
     * @param minY The minimum Y coordinate of the changed area.
     * @param minZ The minimum Z coordinate of the changed area.
     * @param maxX The maximum X coordinate of the changed area.
     * @param maxY The maximum Y coordinate of the changed area.
     * @param maxZ The maximum Z coordinate of the changed area.
     */
    private fun notifyPlayersOfAreaChange(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int) {
        //TODO: add bulk update block when we add cpe
        for (y in minY..maxY) {
            for (z in minZ..maxZ) {
                for (x in minX..maxX) {
                    val blockType = getBlock(x, y, z)
                    broadcastBlockChange(x.toShort(), y.toShort(), z.toShort(), blockType)
                }
            }
        }
    }
    companion object {
        /**
         * Loads a level using the specified deserializer and file
         *
         * @param deserializer The [LevelDeserializer] to use for loading.
         * @param file The [File] to load the level from.
         * @return The loaded [Level] instance if successful, `null` otherwise.
         */
        fun load(deserializer: LevelDeserializer, file: File): Level? {
            return deserializer.deserialize(file)
        }
        /**
         * Loads a level using the specified deserializer and file path
         *
         * @param deserializer The [LevelDeserializer] to use for loading.
         * @param path The file path string to load the level from.
         * @return The loaded [Level] instance if successful, `null` otherwise.
         */
        fun load(deserializer: LevelDeserializer, path: String): Level? {
            val file = File(path)
            return deserializer.deserialize(file)
        }
        /**
         * Loads a level using the default deserializer ([DandelionLevelDeserializer]) and specified file
         *
         * @param file The [File] to load the level from.
         * @return The loaded [Level] instance if successful, `null` otherwise.
         */
        fun load(file: File): Level? {
            return DandelionLevelDeserializer().deserialize(file)
        }
        /**
         * Loads a level using the default deserializer ([DandelionLevelDeserializer]) and specified path
         *
         * @param path The file path string to load the level from.
         * @return The loaded [Level] instance if successful, `null` otherwise.
         */
        fun load(path: String): Level? {
            val file = File(path)
            return DandelionLevelDeserializer().deserialize(file)
        }
    }
}