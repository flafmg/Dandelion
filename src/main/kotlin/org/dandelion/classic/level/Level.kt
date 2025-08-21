package org.dandelion.classic.level

import java.io.File
import kotlin.collections.filter
import org.dandelion.classic.blocks.manager.BlockRegistry
import org.dandelion.classic.blocks.model.Block
import org.dandelion.classic.entity.Entity
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.level.generator.LevelGenerator
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

/**
 * Represents a game level containing blocks and entities
 *
 * @property id The unique identifier for the level.
 * @property author The author of the level.
 * @property description A description of the level.
 * @property size The dimensions of the level as an [SVec] (x, y, z).
 * @property spawn The default spawn position for players in the level.
 * @property extraData Optional extra data associated with the level.
 * @property timeCreated The timestamp (milliseconds since epoch) when the level
 *   was created.
 * @property autoSave Whether the level should be automatically saved
 *   periodically.
 */
class Level(
    val id: String,
    val author: String,
    var description: String,
    val size: SVec,
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

    /** The texture pack URL for this level (from SetMapEnvUrl) */
    var texturePackUrl: String = ""
        /**
         * Sets the texture pack URL for this level
         *
         * @param value The texture pack URL
         */
        set(value) {
            field = value
            if (value.isNotEmpty()) {
                ServerSetMapEnvUrl(value)
                    .send(getPlayers().filter { it.supports("EnvMapAspect") })
            }
        }

    /** The block ID used for map sides (from SetMapEnvProperty) */
    var sideBlock: Byte = 7
        /**
         * Sets the map sides block ID
         *
         * @param value The block ID for map sides
         */
        set(value) {
            field = value
            ServerSetMapEnvProperty(0, value.toInt())
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    /** The block ID used for map edge/horizon (from SetMapEnvProperty) */
    var edgeBlock: Byte = 8
        /**
         * Sets the map edge/horizon block ID
         *
         * @param value The block ID for map edge
         */
        set(value) {
            field = value
            ServerSetMapEnvProperty(1, value.toInt())
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    /** The height of the map edge (from SetMapEnvProperty) */
    var edgeHeight: Int = size.y / 2
        /**
         * Sets the map edge height
         *
         * @param value The edge height
         */
        set(value) {
            field = value
            ServerSetMapEnvProperty(2, value)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    /** The height of the clouds (from SetMapEnvProperty) */
    var cloudsHeight: Int = size.y + 2
        /**
         * Sets the map clouds height
         *
         * @param value The clouds height
         */
        set(value) {
            field = value
            ServerSetMapEnvProperty(3, value)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    /** The maximum fog/view distance (from SetMapEnvProperty) */
    var maxFogDistance: Int = 0
        /**
         * Sets the max fog/view distance
         *
         * @param value The max fog distance
         */
        set(value) {
            field = value
            ServerSetMapEnvProperty(4, value)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    /** The clouds speed multiplied by 256 (from SetMapEnvProperty) */
    var cloudsSpeed: Int = 256
        /**
         * Sets the clouds speed
         *
         * @param value The clouds speed * 256
         */
        set(value) {
            field = value
            ServerSetMapEnvProperty(5, value)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    /** The weather speed multiplied by 256 (from SetMapEnvProperty) */
    var weatherSpeed: Int = 256
        /**
         * Sets the weather speed
         *
         * @param value The weather speed * 256
         */
        set(value) {
            field = value
            ServerSetMapEnvProperty(6, value)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    /** The weather fade multiplied by 128 (from SetMapEnvProperty) */
    var weatherFade: Int = 128
        /**
         * Sets the weather fade
         *
         * @param value The weather fade * 128
         */
        set(value) {
            field = value
            ServerSetMapEnvProperty(7, value)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    /** Whether to use exponential fog (from SetMapEnvProperty) */
    var exponentialFog: Boolean = false
        /**
         * Sets whether to use exponential fog
         *
         * @param value Whether to use exponential fog
         */
        set(value) {
            field = value
            val exponentialFogValue = if (value) 1 else 0
            ServerSetMapEnvProperty(8, exponentialFogValue)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    /**
     * The offset of map sides height from map edge height (from
     * SetMapEnvProperty)
     */
    var sidesOffset: Int = -2
        /**
         * Sets the offset of map sides height from map edge height
         *
         * @param value The sides offset
         */
        set(value) {
            field = value
            ServerSetMapEnvProperty(9, value)
                .send(getPlayers().filter { it.supports("EnvMapAspect") })
        }

    /**
     * The weather type (0 = sunny, 1 = raining, 2 = snowing) (from
     * EnvSetWeatherType)
     */
    var weatherType: Byte = 0
        /**
         * Sets the weather type
         *
         * @param value The weather type (0 = sunny, 1 = raining, 2 = snowing)
         */
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

    /** The sky color, null to reset to default (from EnvSetColor) */
    var skyColor: Color? = null
        /**
         * Sets the sky color
         *
         * @param value The sky color, null to reset to default
         */
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

    /** The cloud color, null to reset to default (from EnvSetColor) */
    var cloudColor: Color? = null
        /**
         * Sets the cloud color
         *
         * @param value The cloud color, null to reset to default
         */
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

    /** The fog color, null to reset to default (from EnvSetColor) */
    var fogColor: Color? = null
        /**
         * Sets the fog color
         *
         * @param value The fog color, null to reset to default
         */
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

    /** The ambient light color, null to reset to default (from EnvSetColor) */
    var ambientLightColor: Color? = null
        /**
         * Sets the ambient light color
         *
         * @param value The ambient light color, null to reset to default
         */
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

    /** The diffuse light color, null to reset to default (from EnvSetColor) */
    var diffuseLightColor: Color? = null
        /**
         * Sets the diffuse light color
         *
         * @param value The diffuse light color, null to reset to default
         */
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

    /** The skybox color, null to reset to default (from EnvSetColor) */
    var skyboxColor: Color? = null
        /**
         * Sets the skybox color
         *
         * @param value The skybox color, null to reset to default
         */
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

    /** The lighting mode for this level (from LightingMode) */
    var lightingMode: LightingMode = LightingMode.CLIENT_LOCAL
        set(value) {
            field = value
            ServerLightingMode(value, lightingModeLocked)
                .send(getPlayers().filter { it.supports("LightingMode") })
        }

    /** Whether the lighting mode is locked to prevent client changes */
    var lightingModeLocked: Boolean = true
        set(value) {
            field = value
            ServerLightingMode(lightingMode, value)
                .send(getPlayers().filter { it.supports("LightingMode") })
        }

    /**
     * Sets the lighting mode for this level
     *
     * @param mode The lighting mode to set
     * @param locked Whether to prevent clients from changing the lighting mode
     */
    fun setLightingMode(mode: LightingMode, locked: Boolean = true) {
        lightingMode = mode
        lightingModeLocked = locked
        ServerLightingMode(mode, locked)
            .send(getPlayers().filter { it.supports("LightingMode") })
    }

    // endregion

    /**
     * Registers a custom block definition for this level.
     *
     * @param block The block instance to register for this level
     */
    fun registerBlockDef(block: Block) {
        BlockRegistry.register(this, block)
    }

    /**
     * Unregisters a custom block definition from this level.
     *
     * @param blockId The ID of the block to unregister from this level
     * @return true if the block was removed, false otherwise
     */
    fun unregisterBlockDef(blockId: UShort): Boolean {
        return BlockRegistry.unregister(this, blockId)
    }

    /**
     * Gets a block definition for this level, with level blocks taking priority
     * over global blocks.
     *
     * @param blockId The ID of the block to retrieve
     * @return The block instance, or null if not found
     */
    fun getBlockDef(blockId: UShort): Block? {
        return BlockRegistry.get(this, blockId)
    }

    /**
     * Sends all env packets and other packets to the player
     *
     * @param player the [Player] that will receive the env update
     */
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

    /**
     * Initializes the pool of available entity IDs (0-254, 255 reserved for
     * player's own view)
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
     * @return The next available [Byte] entity ID, or `null` if none are
     *   available.
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
     * @return `true` if the entity was successfully added, `false` if the level
     *   is full.
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
            Console.warnLog(
                "Attempted to remove entity '${entity.name}' that isn't in level '$id'"
            )
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
     * Gets an non player entity by its ID
     *
     * @param entityId The [Byte] ID of the entity to find.
     * @return The [Entity] instance if found, `null` otherwise.
     */
    fun getNonPlayerEntity(entityId: Byte): Entity? {
        val entity = entities[entityId]
        return if (entity !is Player) entity else null
    }

    /**
     * Gets an entity by its ID
     *
     * @param entityId The [Byte] ID of the entity to find.
     * @return The [Entity] instance if found, `null` otherwise.
     */
    fun getEntity(entityId: Byte): Entity? {
        return entities[entityId]
    }

    /**
     * Gets a player by their entity ID
     *
     * @param entityId The [Byte] ID of the player entity to find.
     * @return The [Player] instance if found, `null` otherwise.
     */
    fun getPlayer(entityId: Byte): Player? {
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
     * Sets multiple blocks at specified positions and notifies all players
     *
     * @param positions A list of [Position] coordinates where blocks should be
     *   set.
     * @param blockTypes A list of [UShort] block type IDs corresponding to each
     *   position.
     */
    fun setBlocks(positions: List<Position>, blockTypes: List<Byte>) {
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
            blockData[blockIndex] = blockType
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
                    UShortArray(chunk.size) { i -> chunk[i].second.toUShort() }
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
                        blockType.toUShort(),
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
                        blockType.toUShort(),
                        useExtendedBlocks = false,
                    )
                classicPlayers.forEach { packet.send(it) }
            }
        }
    }

    /**
     * Sets multiple blocks at specified positions to the same block type using
     * Position coordinates
     *
     * @param positions A list of [Position] coordinates where blocks should be
     *   set.
     * @param blockType The [Byte] block type ID to set at all positions.
     */
    fun setBlocks(positions: List<Position>, blockType: Byte) {
        val blockTypes = List(positions.size) { blockType }
        setBlocks(positions, blockTypes)
    }

    /**
     * Sets a block at the specified position using Position object
     *
     * @param position The [Position] where the block should be set.
     * @param block The [Block] type to set.
     */
    fun setBlock(position: Position, block: Block) {
        setBlock(
            position.x.toInt(),
            position.y.toInt(),
            position.z.toInt(),
            block.id,
        )
    }

    /**
     * Sets a block at the specified position using Position object and byte
     * block type
     *
     * @param position The [Position] where the block should be set.
     * @param blockType The [Byte] block type ID to set.
     */
    fun setBlock(position: Position, blockType: UShort) {
        setBlock(
            position.x.toInt(),
            position.y.toInt(),
            position.z.toInt(),
            blockType,
        )
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
    fun setBlock(position: IVec, blockType: UShort) {
        setBlock(position.x, position.y, position.z, blockType)
    }

    /**
     * Sets a block at the specified coordinates using Short values and Block
     * enum
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
     * Sets a block at the specified coordinates using Short values and byte
     * block type
     *
     * @param x The X coordinate (Short) where the block should be set.
     * @param y The Y coordinate (Short) where the block should be set.
     * @param z The Z coordinate (Short) where the block should be set.
     * @param blockType The [Byte] block type ID to set.
     */
    fun setBlock(x: Short, y: Short, z: Short, blockType: UShort) {
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

    /**
     * Fills a rectangular area with the specified block type
     *
     * @param start The starting [Position] of the area to fill.
     * @param end The ending [Position] of the area to fill.
     * @param block The [Block] type to fill the area with.
     */
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

    /**
     * Fills a rectangular area with the specified block type
     *
     * @param start The starting [Position] of the area to fill.
     * @param end The ending [Position] of the area to fill.
     * @param blockType The [UShort] block type ID to fill the area with.
     */
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

    /**
     * Fills a rectangular area with the specified block type using IVec
     * positions
     *
     * @param start The starting [IVec] coordinates of the area to fill.
     * @param end The ending [IVec] coordinates of the area to fill.
     * @param block The [Block] type to fill the area with.
     */
    fun fillBlocks(start: IVec, end: IVec, block: Block) {
        fillBlocks(start.x, start.y, start.z, end.x, end.y, end.z, block.id)
    }

    /**
     * Fills a rectangular area with the specified block type using IVec
     * positions and UShort value
     *
     * @param start The starting [IVec] coordinates of the area to fill.
     * @param end The ending [IVec] coordinates of the area to fill.
     * @param blockType The [UShort] block type ID to fill the area with.
     */
    fun fillBlocks(start: IVec, end: IVec, blockType: UShort) {
        fillBlocks(start.x, start.y, start.z, end.x, end.y, end.z, blockType)
    }

    /**
     * Fills a rectangular area with the specified block type using Int
     * coordinates and Block enum
     *
     * @param startX The starting X coordinate (Int) of the area to fill.
     * @param startY The starting Y coordinate (Int) of the area to fill.
     * @param startZ The starting Z coordinate (Int) of the area to fill.
     * @param endX The ending X coordinate (Int) of the area to fill.
     * @param endY The ending Y coordinate (Int) of the area to fill.
     * @param endZ The ending Z coordinate (Int) of the area to fill.
     * @param block The [Block] type to fill the area with.
     */
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

    /**
     * Fills a rectangular area with the specified block type
     *
     * @param startX The starting X coordinate (Int) of the area to fill.
     * @param startY The starting Y coordinate (Int) of the area to fill.
     * @param startZ The starting Z coordinate (Int) of the area to fill.
     * @param endX The ending X coordinate (Int) of the area to fill.
     * @param endY The ending Y coordinate (Int) of the area to fill.
     * @param endZ The ending Z coordinate (Int) of the area to fill.
     * @param blockType The [UShort] block type ID to fill the area with.
     */
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
        notifyPlayersOfAreaChange(minX, minY, minZ, maxX, maxY, maxZ)
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

    /**
     * Gets the block type at the specified position using Position object
     *
     * @param position The [Position] to get the block from.
     * @return The [Byte] block type ID at the specified position, or 0x00 if
     *   the position is invalid.
     */
    fun getBlock(position: Position): UShort {
        return getBlock(
            position.x.toInt(),
            position.y.toInt(),
            position.z.toInt(),
        )
    }

    /**
     * Gets the block type at the specified position using IVec
     *
     * @param position The [IVec] coordinates to get the block from.
     * @return The [Byte] block type ID at the specified position, or 0x00 if
     *   the position is invalid.
     */
    fun getBlock(position: IVec): UShort {
        return getBlock(position.x, position.y, position.z)
    }

    /**
     * Gets the block type at the specified coordinates using Short values
     *
     * @param x The X coordinate (Short) to get the block from.
     * @param y The Y coordinate (Short) to get the block from.
     * @param z The Z coordinate (Short) to get the block from.
     * @return The [Byte] block type ID at the specified position, or 0x00 if
     *   the position is invalid.
     */
    fun getBlock(x: Short, y: Short, z: Short): UShort {
        return getBlock(x.toInt(), y.toInt(), z.toInt())
    }

    /**
     * Gets the block type at the specified coordinates
     *
     * @param x The X coordinate (Int) to get the block from.
     * @param y The Y coordinate (Int) to get the block from.
     * @param z The Z coordinate (Int) to get the block from.
     * @return The [UShort] block type ID at the specified position, or 0x00 if
     *   the position is invalid.
     */
    fun getBlock(x: Int, y: Int, z: Int): UShort {
        if (!isValidBlockPosition(x, y, z)) return 0u

        val blockIndex = calculateBlockIndex(x, y, z)
        val low = blockData[blockIndex].toInt() and 0xFF
        val high = blockData2?.get(blockIndex)?.toInt()?.and(0xFF) ?: 0
        return (low or (high shl 8)).toUShort()
    }

    /**
     * Kicks all players from the level
     *
     * @param reason The reason message for kicking the players. Defaults to
     *   "You have been kicked from the level".
     */
    fun kickAllPlayers(reason: String = "You have been kicked from the level") {
        getPlayers().forEach { player -> player.kick(reason) }
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
     * Broadcasts a message with specified message type to all players in the
     * level
     *
     * @param message The message string to broadcast.
     * @param messageTypeId An optional byte identifier for the type of message.
     *   Defaults to `0x00`.
     */
    fun broadcast(message: String, messageTypeId: Byte = 0x00) {
        getPlayers().forEach { player ->
            player.sendMessage(message, messageTypeId)
        }
    }

    /**
     * Generates the level using the specified generator and parameters
     *
     * @param generator The [LevelGenerator] to use for creating the level's
     *   terrain/blocks.
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
            .forEach { entity -> player.mutualSpawn(entity) }
    }

    /**
     * Spawns an entity in the level and shows it to all players
     *
     * @param entity The [Entity] to spawn in the level.
     */
    fun spawnEntityInLevel(entity: Entity) {
        getPlayers().forEach { player -> entity.spawnFor(player) }
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
        serializer.serialize(this, File(path))
    }

    /**
     * Saves the level using the specified serializer to the default location
     *
     * @param serializer The [LevelSerializer] to use for saving.
     */
    fun save(serializer: LevelSerializer) {
        save(serializer, File("levels/$id.dlvl"))
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

    /** Saves the level using the default serializer to the default location */
    fun save() {
        save(DandelionLevelSerializer())
    }

    /**
     * Validates if the given coordinates are within level bounds
     *
     * @param x The X coordinate to validate.
     * @param y The Y coordinate to validate.
     * @param z The Z coordinate to validate.
     * @return `true` if the coordinates are valid within the level, `false`
     *   otherwise.
     */
    private fun isValidBlockPosition(x: Int, y: Int, z: Int): Boolean {
        return x >= 0 &&
            y >= 0 &&
            z >= 0 &&
            x < size.x &&
            y < size.y &&
            z < size.z
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

    /**
     * Notifies all players of changes in a filled area (simplified
     * implementation)
     *
     * @param minX The minimum X coordinate of the changed area.
     * @param minY The minimum Y coordinate of the changed area.
     * @param minZ The minimum Z coordinate of the changed area.
     * @param maxX The maximum X coordinate of the changed area.
     * @param maxY The maximum Y coordinate of the changed area.
     * @param maxZ The maximum Z coordinate of the changed area.
     */
    private fun notifyPlayersOfAreaChange(
        minX: Int,
        minY: Int,
        minZ: Int,
        maxX: Int,
        maxY: Int,
        maxZ: Int,
    ) {
        // TODO: add bulk update block when we add cpe
        for (y in minY..maxY) {
            for (z in minZ..maxZ) {
                for (x in minX..maxX) {
                    val blockType = getBlock(x, y, z)
                    broadcastBlockChange(
                        x.toShort(),
                        y.toShort(),
                        z.toShort(),
                        blockType,
                    )
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
        fun load(deserializer: LevelDeserializer, file: File): Level? =
            deserializer.deserialize(file)

        /**
         * Loads a level using the specified deserializer and file path
         *
         * @param deserializer The [LevelDeserializer] to use for loading.
         * @param path The file path string to load the level from.
         * @return The loaded [Level] instance if successful, `null` otherwise.
         */
        fun load(deserializer: LevelDeserializer, path: String): Level? =
            deserializer.deserialize(File(path))

        /**
         * Loads a level using the default deserializer
         * ([DandelionLevelDeserializer]) and specified file
         *
         * @param file The [File] to load the level from.
         * @return The loaded [Level] instance if successful, `null` otherwise.
         */
        fun load(file: File): Level? =
            DandelionLevelDeserializer().deserialize(file)

        /**
         * Loads a level using the default deserializer
         * ([DandelionLevelDeserializer]) and specified path
         *
         * @param path The file path string to load the level from.
         * @return The loaded [Level] instance if successful, `null` otherwise.
         */
        fun load(path: String): Level? = load(File(path))
    }
}
