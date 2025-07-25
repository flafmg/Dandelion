package org.dandelion.classic.entity

import org.dandelion.classic.blocks.model.Block
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.events.PlayerBlockInteractionEvent
import org.dandelion.classic.events.manager.EventDispatcher
import org.dandelion.classic.level.Level
import org.dandelion.classic.network.packets.classic.server.*
import org.dandelion.classic.types.Position
import org.dandelion.classic.util.toFShort
import kotlin.math.sqrt

/**
 * Base entity class representing any interactive object in the game world.
 * Handles position management, movement updates, and basic entity interactions.
 *
 * @property name The name of the entity.
 * @property levelId The ID of the level the entity is associated with.
 * @property entityId The unique byte identifier for this entity within its level.
 * @property position The current [Position] of the entity.
 */
open class Entity(
    val name: String,
    var levelId: String = "",
    var entityId: Byte = -1,
    val position: Position = Position(0f, 0f, 0f, 0f, 0f),
) {
    var level: Level? = null

    companion object {
        private const val MAX_RELATIVE_MOVEMENT = 3.96875f
        private const val MIN_RELATIVE_MOVEMENT = -4.0f
        private const val MAX_BLOCK_INTERACTION_DISTANCE = 6.0
    }

    //region Position Management

    /**
     * Updates entity position using a Position object
     *
     * @param position The new [Position] to teleport the entity to.
     */
    open fun teleportTo(position: Position) {
        teleportTo(position.x, position.y, position.z, position.yaw, position.pitch)
    }

    /**
     * Updates entity position using integer coordinates (maintains current rotation)
     *
     * @param x The new X coordinate (Int).
     * @param y The new Y coordinate (Int).
     * @param z The new Z coordinate (Int).
     */
    open fun teleportTo(x: Int, y: Int, z: Int) {
        teleportTo(x.toFloat(), y.toFloat(), z.toFloat(), position.yaw, position.pitch)
    }

    /**
     * Updates entity position using float coordinates (maintains current rotation)
     *
     * @param x The new X coordinate (Float).
     * @param y The new Y coordinate (Float).
     * @param z The new Z coordinate (Float).
     */
    open fun teleportTo(x: Float, y: Float, z: Float) {
        teleportTo(x, y, z, position.yaw, position.pitch)
    }

    /**
     * Updates entity position and rotation with absolute positioning
     *
     * @param x The new X coordinate (Float).
     * @param y The new Y coordinate (Float).
     * @param z The new Z coordinate (Float).
     * @param yaw The new yaw rotation (Float).
     * @param pitch The new pitch rotation (Float).
     */
    open fun teleportTo(x: Float, y: Float, z: Float, yaw: Float, pitch: Float) {
        position.set(x, y , z, yaw, pitch)
        broadcastAbsolutePositionUpdate(x, y - toFShort(22)/*fixes weird half block step*/, z, yaw.toInt().toByte(), pitch.toInt().toByte())
    }

    /**
     * Updates only entity rotation
     *
     * @param yaw The new yaw rotation (Float).
     * @param pitch The new pitch rotation (Float).
     */
    open fun rotateTo(yaw: Float, pitch: Float) {
        teleportTo(position.x, position.y, position.z, yaw, pitch)
    }

    /**
     * Smoothly updates entity position and orientation with optimized packet sending
     *
     * @param newX The new X coordinate (Float).
     * @param newY The new Y coordinate (Float).
     * @param newZ The new Z coordinate (Float).
     * @param newYaw The new yaw rotation (Float).
     * @param newPitch The new pitch rotation (Float).
     * @param forceAbsolute Whether to force an absolute position update. Defaults to `false`.
     */
    open fun updatePositionAndOrientation(
        newX: Float,
        newY: Float,
        newZ: Float,
        newYaw: Float,
        newPitch: Float,
        forceAbsolute: Boolean = false
    ) {
        val deltaX = newX - position.x
        val deltaY = newY - position.y
        val deltaZ = newZ - position.z
        val deltaYaw = newYaw - position.yaw
        val deltaPitch = newPitch - position.pitch

        val hasMoved = deltaX != 0f || deltaY != 0f || deltaZ != 0f
        val hasRotated = deltaYaw != 0f || deltaPitch != 0f

        val shouldUseAbsolute = forceAbsolute ||
                deltaX < MIN_RELATIVE_MOVEMENT || deltaX > MAX_RELATIVE_MOVEMENT ||
                deltaY < MIN_RELATIVE_MOVEMENT || deltaY > MAX_RELATIVE_MOVEMENT ||
                deltaZ < MIN_RELATIVE_MOVEMENT || deltaZ > MAX_RELATIVE_MOVEMENT

        when {
            shouldUseAbsolute && (hasMoved || hasRotated) -> {
                broadcastAbsolutePositionUpdate(newX, newY, newZ, newYaw.toInt().toByte(), newPitch.toInt().toByte())
            }
            hasMoved && hasRotated -> {
                broadcastRelativePositionAndOrientationUpdate(deltaX, deltaY, deltaZ, newYaw.toInt().toByte(), newPitch.toInt().toByte())
            }
            hasMoved -> {
                broadcastRelativePositionUpdate(deltaX, deltaY, deltaZ)
            }
            hasRotated -> {
                broadcastOrientationUpdate(newYaw.toInt().toByte(), newPitch.toInt().toByte())
            }
        }

        position.set(newX, newY, newZ, newYaw, newPitch)
    }

    //endregion

    //region Network Broadcasting

    /**
     * Broadcasts absolute position and orientation to all players in the same level
     *
     * @param x The X coordinate (Float) to broadcast.
     * @param y The Y coordinate (Float) to broadcast.
     * @param z The Z coordinate (Float) to broadcast.
     * @param yaw The yaw rotation (Byte) to broadcast.
     * @param pitch The pitch rotation (Byte) to broadcast.
     */
    protected open fun broadcastAbsolutePositionUpdate(x: Float, y: Float, z: Float, yaw: Byte, pitch: Byte) {
        getOtherPlayersInLevel().forEach { player ->
            ServerSetPositionAndOrientation(entityId, x, y, z, yaw, pitch).send(player.channel)
        }
    }

    /**
     * Broadcasts relative position and orientation changes to all players in the same level
     *
     * @param dx The change in X coordinate (Float).
     * @param dy The change in Y coordinate (Float).
     * @param dz The change in Z coordinate (Float).
     * @param yaw The new yaw rotation (Byte).
     * @param pitch The new pitch rotation (Byte).
     */
    protected open fun broadcastRelativePositionAndOrientationUpdate(dx: Float, dy: Float, dz: Float, yaw: Byte, pitch: Byte) {
        getOtherPlayersInLevel().forEach { player ->
            ServerPositionAndOrientationUpdate(entityId, dx, dy, dz, yaw, pitch).send(player.channel)
        }
    }

    /**
     * Broadcasts relative position changes to all players in the same level
     *
     * @param dx The change in X coordinate (Float).
     * @param dy The change in Y coordinate (Float).
     * @param dz The change in Z coordinate (Float).
     */
    protected open fun broadcastRelativePositionUpdate(dx: Float, dy: Float, dz: Float) {
        getOtherPlayersInLevel().forEach { player ->
            ServerPositionUpdate(entityId, dx, dy, dz).send(player.channel)
        }
    }

    /**
     * Broadcasts orientation changes to all players in the same level
     *
     * @param yaw The new yaw rotation (Byte).
     * @param pitch The new pitch rotation (Byte).
     */
    protected open fun broadcastOrientationUpdate(yaw: Byte, pitch: Byte) {
        getOtherPlayersInLevel().forEach { player ->
            ServerOrientationUpdate(entityId, yaw, pitch).send(player.channel)
        }
    }

    //endregion

    //region Entity Management

    /**
     * Spawns this entity for a specific target entity
     *
     * @param target The [Entity] for which this entity should be spawned.
     */
    open fun spawnFor(target: Entity) {
        if (target is Player) {
            ServerSpawnPlayer(
                entityId,
                name,
                position.x,
                position.y,
                position.z,
                position.yaw.toInt().toByte(),
                position.pitch.toInt().toByte()
            ).send(target.channel)
        }
    }

    /**
     * Spawns this entity for all players in the current level
     */
    open fun globalSpawn() {
        level?.getPlayers()?.forEach { player ->
            if (player.entityId != entityId) {
                spawnFor(player)
            }
        }
    }

    /**
     * Mutually spawns two entities for each other
     *
     * @param other The other [Entity] to spawn mutually with this one.
     */
    open fun mutualSpawn(other: Entity) {
        this.spawnFor(other)
        other.spawnFor(this)
    }

    /**
     * Despawns this entity for a specific target entity
     *
     * @param target The [Entity] for which this entity should be despawned.
     */
    open fun despawnFor(target: Entity) {
        if (target is Player) {
            ServerDespawnPlayer(entityId).send(target.channel)
        }
    }

    /**
     * Despawns this entity for all players in the current level
     */
    open fun globalDespawn() {
        level?.getPlayers()?.forEach { player ->
            if (player.entityId != entityId) {
                despawnFor(player)
            }
        }
    }

    /**
     * Mutually despawns two entities for each other
     *
     * @param other The other [Entity] to despawn mutually with this one.
     */
    open fun mutualDespawn(other: Entity) {
        this.despawnFor(other)
        other.despawnFor(this)
    }

    //endregion

    //region Level Management

    /**
     * Moves entity to a new level
     *
     * @param level The [Level] to move the entity to.
     * @param notifyJoin Whether to notify the join (implementation-dependent). Defaults to `false`.
     */
    open fun joinLevel(level: Level, notifyJoin: Boolean = false) {
        if (!level.tryAddEntity(this)) {
            return
        }
        this.level = level
        teleportTo(level.spawn)
        level.spawnEntityInLevel(this)
    }

    //endregion

    //region Communication

    /**
     * Sends a message as this entity to all players in the level
     *
     * @param message The message string to broadcast.
     */
    open fun sendMessageAs(message: String) {
        level?.broadcast("$name: &7$message")
    }

    //endregion

    //region Block Interaction

    /**
     * Attempts to place or destroy a block as this entity
     *
     * @param x The X coordinate (Short) of the block to interact with.
     * @param y The Y coordinate (Short) of the block to interact with.
     * @param z The Z coordinate (Short) of the block to interact with.
     * @param blockType The [Byte] block type ID to place (ignored if destroying).
     * @param isDestroying Whether the interaction is to destroy the block (`true`) or place it (`false`).
     */
    open fun interactWithBlock(x: Short, y: Short, z: Short, blockType: Byte, isDestroying: Boolean) {
        val currentLevel = level ?: return
        if (!isWithinInteractionRange(x.toFloat(), y.toFloat(), z.toFloat())) {
            return
        }

        val finalBlockType = if (isDestroying) Block.get(0)?.id ?: 0 else blockType
        val blockAtPos = Block.get(currentLevel.getBlock(x, y, z))

        if(Block.get(finalBlockType) == null){
            return
        }

        if (this is Player) {
            val event = PlayerBlockInteractionEvent(
                this,
                blockAtPos!!,
                Block.get( finalBlockType)!!,
                Position(x.toFloat(), y.toFloat(), z.toFloat()),
                level!!
            )
            EventDispatcher.dispatch(event)
            if(event.isCancelled){
                ServerSetBlock(x, y, z, blockAtPos.id).send(this)
                return
            }
        }

        currentLevel.setBlock(x, y, z, finalBlockType)
        broadcastBlockUpdate(x, y, z, finalBlockType)
    }

    /**
     * Updates a block for this entity (override in subclasses for specific behavior)
     *
     * @param x The X coordinate (Short) of the block to update.
     * @param y The Y coordinate (Short) of the block to update.
     * @param z The Z coordinate (Short) of the block to update.
     * @param block The new [Byte] block type ID.
     */
    open fun updateBlock(x: Short, y: Short, z: Short, block: Byte) {
        // Default implementation does nothing - override in Player class
    }

    /**
     * Broadcasts block update to all players in the level
     *
     * @param x The X coordinate (Short) of the updated block.
     * @param y The Y coordinate (Short) of the updated block.
     * @param z The Z coordinate (Short) of the updated block.
     * @param block The new [Byte] block type ID.
     */
    protected fun broadcastBlockUpdate(x: Short, y: Short, z: Short, block: Byte) {
        level?.getPlayers()?.forEach { player ->
            player.updateBlock(x, y, z, block)
        }
    }

    //endregion

    //region Utility Methods

    /**
     * Gets all players in the same level excluding this entity
     *
     * @return A list of [Player] instances in the same level, excluding this entity.
     */
    protected fun getOtherPlayersInLevel(): List<Player> {
        return level?.getPlayers()?.filter { it.entityId != entityId } ?: emptyList()
    }

    /**
     * Checks if the given coordinates are within interaction range
     *
     * @param x The X coordinate (Float) to check.
     * @param y The Y coordinate (Float) to check.
     * @param z The Z coordinate (Float) to check.
     * @return `true` if the coordinates are within interaction range, `false` otherwise.
     */
    protected fun isWithinInteractionRange(x: Float, y: Float, z: Float): Boolean {
        val dx = position.x - x
        val dy = position.y - y
        val dz = position.z - z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)
        return distance <= MAX_BLOCK_INTERACTION_DISTANCE
    }
    //endregion
}