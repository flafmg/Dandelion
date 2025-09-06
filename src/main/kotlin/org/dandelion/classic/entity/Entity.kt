package org.dandelion.classic.entity

import kotlin.math.sqrt
import org.dandelion.classic.blocks.model.Block
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.events.PlayerBlockInteractionEvent
import org.dandelion.classic.events.manager.EventDispatcher
import org.dandelion.classic.level.Level
import org.dandelion.classic.network.packets.classic.server.*
import org.dandelion.classic.network.packets.cpe.server.ServerChangeModel
import org.dandelion.classic.network.packets.cpe.server.ServerExtAddEntity2
import org.dandelion.classic.network.packets.cpe.server.ServerExtEntityTeleport
import org.dandelion.classic.network.packets.cpe.server.ServerSetEntityProperty
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.enums.EntityModel
import org.dandelion.classic.types.enums.MoveMode

open class Entity(
    val name: String,
    var levelId: String = "",
    var entityId: Byte = -1,
    val position: Position = Position(0f, 0f, 0f, 0f, 0f),
) {
    var level: Level? = null
    var displayName: String = name
        set(value) {
            if (field != value) {
                field = value
                refreshEntityForPlayers()
            }
        }

    var skin: String = name
        set(value) {
            if (field != value) {
                field = value
                refreshEntityForPlayers()
            }
        }

    var model: String = EntityModel.HUMANOID.string
        set(value) {
            if (field != value) {
                field = value
                broadcastModelChange(value)
            }
        }

    var modelRotationX: Int = 0
        set(value) {
            if (field != value) {
                field = value
                broadcastEntityProperty(0, value)
            }
        }

    var modelRotationY: Int = 0
        set(value) {
            if (field != value) {
                field = value
                broadcastEntityProperty(1, value)
            }
        }

    var modelRotationZ: Int = 0
        set(value) {
            if (field != value) {
                field = value
                broadcastEntityProperty(2, value)
            }
        }

    companion object {
        private const val MAX_RELATIVE_MOVEMENT = 3.96875f
        private const val MIN_RELATIVE_MOVEMENT = -4.0f
    }

    // region Position Management
    open fun teleportTo(position: Position) {
        teleportTo(
            position.x,
            position.y,
            position.z,
            position.yaw,
            position.pitch,
        )
    }

    open fun teleportTo(x: Int, y: Int, z: Int) {
        teleportTo(
            x.toFloat(),
            y.toFloat(),
            z.toFloat(),
            position.yaw,
            position.pitch,
        )
    }

    open fun teleportTo(x: Float, y: Float, z: Float) {
        teleportTo(x, y, z, position.yaw, position.pitch)
    }

    open fun teleportTo(
        x: Float,
        y: Float,
        z: Float,
        yaw: Float,
        pitch: Float,
        moveMode: MoveMode = MoveMode.INSTANT,
        interpolateOrientation: Boolean = false,
    ) {
        val actualUsePosition =
            x != this.position.x || y != this.position.y || z != this.position.z
        val actualUseOrientation =
            yaw != this.position.yaw || pitch != this.position.pitch

        position.set(x, y, z, yaw, pitch)
        broadcastPositionUpdate(
            x,
            y,
            z,
            yaw,
            pitch,
            moveMode,
            actualUsePosition,
            actualUseOrientation,
            interpolateOrientation,
        )
    }

    open fun rotateTo(yaw: Float, pitch: Float) {
        teleportTo(position.x, position.y, position.z, yaw, pitch)
    }

    open fun updatePositionAndOrientation(
        newX: Float,
        newY: Float,
        newZ: Float,
        newYaw: Float,
        newPitch: Float,
        forceAbsolute: Boolean = false,
    ) {
        val deltaX = newX - position.x
        val deltaY = newY - position.y
        val deltaZ = newZ - position.z
        val deltaYaw = newYaw - position.yaw
        val deltaPitch = newPitch - position.pitch

        val hasMoved = deltaX != 0f || deltaY != 0f || deltaZ != 0f
        val hasRotated = deltaYaw != 0f || deltaPitch != 0f

        val shouldUseAbsolute =
            forceAbsolute ||
                deltaX < MIN_RELATIVE_MOVEMENT ||
                deltaX > MAX_RELATIVE_MOVEMENT ||
                deltaY < MIN_RELATIVE_MOVEMENT ||
                deltaY > MAX_RELATIVE_MOVEMENT ||
                deltaZ < MIN_RELATIVE_MOVEMENT ||
                deltaZ > MAX_RELATIVE_MOVEMENT

        when {
            shouldUseAbsolute && (hasMoved || hasRotated) -> {
                broadcastAbsolutePositionUpdate(
                    newX,
                    newY,
                    newZ,
                    newYaw,
                    newPitch,
                )
            }
            hasMoved && hasRotated -> {
                broadcastRelativePositionAndOrientationUpdate(
                    deltaX,
                    deltaY,
                    deltaZ,
                    newYaw,
                    newPitch,
                )
            }
            hasMoved -> {
                broadcastRelativePositionUpdate(deltaX, deltaY, deltaZ)
            }
            hasRotated -> {
                broadcastOrientationUpdate(
                    newYaw,
                    newPitch,
                )
            }
        }

        position.set(newX, newY, newZ, newYaw, newPitch)
    }

    // endregion

    // region Network Broadcasting

    protected open fun broadcastAbsolutePositionUpdate(
        x: Float,
        y: Float,
        z: Float,
        yaw: Float,
        pitch: Float,
    ) {
        getOtherPlayersInLevel().forEach { player ->
            ServerSetPositionAndOrientation(entityId, x, y, z, yaw, pitch)
                .send(player.channel)
        }
    }

    protected open fun broadcastPositionUpdate(
        x: Float,
        y: Float,
        z: Float,
        yaw: Float,
        pitch: Float,
        moveMode: MoveMode,
        usePosition: Boolean,
        useOrientation: Boolean,
        interpolateOrientation: Boolean,
    ) {
        getOtherPlayersInLevel().forEach { player ->
            if (player.supports("ExtEntityTeleport")) {
                ServerExtEntityTeleport(
                        entityId = entityId,
                        usePosition = usePosition,
                        moveMode = moveMode,
                        useOrientation = useOrientation,
                        interpolateOrientation = interpolateOrientation,
                        x = x,
                        y = y,
                        z = z,
                        yaw = yaw,
                        pitch = pitch,
                    )
                    .send(player.channel)
            } else {
                if (
                    moveMode == MoveMode.RELATIVE_SMOOTH ||
                        moveMode == MoveMode.RELATIVE_SEAMLESS
                ) {
                    ServerSetPositionAndOrientation(
                            this.entityId,
                            if (usePosition) this.position.x + x
                            else this.position.x,
                            if (usePosition) this.position.y + y
                            else this.position.y,
                            if (usePosition) this.position.z + z
                            else this.position.z,
                            if (useOrientation)
                                this.position.yaw + yaw
                            else this.position.yaw,
                            if (useOrientation)
                                this.position.pitch + pitch
                            else this.position.pitch,
                        )
                        .send(player.channel)
                } else {
                    ServerSetPositionAndOrientation(
                            this.entityId,
                            if (usePosition) x else this.position.x,
                            if (usePosition) y else this.position.y,
                            if (usePosition) z else this.position.z,
                            if (useOrientation) yaw
                            else this.position.yaw,
                            if (useOrientation) pitch
                            else this.position.pitch,
                        )
                        .send(player.channel)
                }
            }
        }
    }

    protected open fun broadcastRelativePositionAndOrientationUpdate(
        dx: Float,
        dy: Float,
        dz: Float,
        yaw: Float,
        pitch: Float,
    ) {
        getOtherPlayersInLevel().forEach { player ->
            ServerPositionAndOrientationUpdate(entityId, dx, dy, dz, yaw, pitch)
                .send(player.channel)
        }
    }

    protected open fun broadcastRelativePositionUpdate(
        dx: Float,
        dy: Float,
        dz: Float,
    ) {
        getOtherPlayersInLevel().forEach { player ->
            ServerPositionUpdate(entityId, dx, dy, dz).send(player.channel)
        }
    }

    protected open fun broadcastOrientationUpdate(yaw: Float, pitch: Float) {
        getOtherPlayersInLevel().forEach { player ->
            ServerOrientationUpdate(entityId, yaw, pitch).send(player.channel)
        }
    }

    // endregion

    // region Model Management
    protected open fun broadcastModelChange(modelName: String) {
        getOtherPlayersInLevel().forEach { player ->
            if (player.supports("ChangeModel")) {
                ServerChangeModel(entityId, modelName).send(player.channel)
            }
        }
    }

    protected open fun broadcastEntityProperty(
        propertyType: Byte,
        propertyValue: Int,
    ) {
        getOtherPlayersInLevel().forEach { player ->
            if (player.supports("EntityProperty")) {
                ServerSetEntityProperty(entityId, propertyType, propertyValue)
                    .send(player.channel)
            }
        }
    }

    protected open fun sendEntityPropertiesTo(player: Player) {
        if (player.supports("EntityProperty")) {
            if (modelRotationX != 0) {
                ServerSetEntityProperty(entityId, 0, modelRotationX)
                    .send(player.channel)
            }
            if (modelRotationY != 0) {
                ServerSetEntityProperty(entityId, 1, modelRotationY)
                    .send(player.channel)
            }
            if (modelRotationZ != 0) {
                ServerSetEntityProperty(entityId, 2, modelRotationZ)
                    .send(player.channel)
            }
        }
    }

    // endregion

    // region Entity Management
    open fun spawnFor(target: Entity) {
        (target as? Player)?.let { player ->
            if (player.supportsCpe && player.supports("ExtPlayerList")) {
                ServerExtAddEntity2(
                        entityId,
                        displayName,
                        skin,
                        position.x,
                        position.y,
                        position.z,
                        position.yaw,
                        position.pitch,
                    )
                    .send(player.channel)
            } else {
                ServerSpawnPlayer(
                        entityId,
                        name,
                        position.x,
                        position.y,
                        position.z,
                        position.yaw,
                        position.pitch,
                    )
                    .send(player.channel)
            }

            // Send model change packet after spawning to ensure visual consistency
            if (
                player.supports("ChangeModel") &&
                    model != EntityModel.HUMANOID.string
            ) {
                ServerChangeModel(entityId, model).send(player.channel)
            }

            // Send entity properties to the player
            sendEntityPropertiesTo(player)
        }
    }

    open fun globalSpawn() {
        level
            ?.getPlayers()
            ?.filter { it.entityId != entityId }
            ?.forEach(::spawnFor)
    }

    open fun mutualSpawn(other: Entity) {
        this.spawnFor(other)
        other.spawnFor(this)
    }

    open fun despawnFor(target: Entity) {
        (target as? Player)?.let { player ->
            ServerDespawnPlayer(entityId).send(player.channel)
        }
    }

    open fun globalDespawn() {
        level
            ?.getPlayers()
            ?.filter { it.entityId != entityId }
            ?.forEach(::despawnFor)
    }

    open fun mutualDespawn(other: Entity) {
        this.despawnFor(other)
        other.despawnFor(this)
    }

    // endregion

    // region Level Management
    open fun joinLevel(level: Level, notifyJoin: Boolean = false) {
        if (!level.tryAddEntity(this)) return

        this.level = level
        teleportTo(level.spawn)
        level.spawnEntityInLevel(this)
    }

    // endregion

    // region Communication
    open fun sendMessageAs(message: String) {
        level?.broadcast("$name: &7$message")
    }

    // endregion

    // region Block Interaction
    open fun interactWithBlock(
        x: Short,
        y: Short,
        z: Short,
        blockType: UShort,
        isDestroying: Boolean,
    ) {
        val currentLevel = level ?: return

        if (!isWithinInteractionRange(x.toFloat(), y.toFloat(), z.toFloat()))
            return

        val finalBlockType =
            if (isDestroying) Block.get(0.toUShort())?.id ?: 0.toUShort()
            else blockType
        val blockAtPos = Block.get(currentLevel.getBlock(x, y, z)) ?: return

        if (Block.get(finalBlockType) == null) return

        if (this is Player) {
            val event =
                PlayerBlockInteractionEvent(
                    this,
                    blockAtPos,
                    Block.get(finalBlockType)!!,
                    Position(x.toFloat(), y.toFloat(), z.toFloat()),
                    currentLevel,
                )
            EventDispatcher.dispatch(event)

            if (event.isCancelled) {
                ServerSetBlock(
                        x,
                        y,
                        z,
                        blockAtPos.id,
                        this.supports("ExtendedBlocks"),
                    )
                    .send(this)
                return
            }
        }

        currentLevel.setBlock(x, y, z, finalBlockType)
        broadcastBlockUpdate(x, y, z, finalBlockType)
    }

    open fun updateBlock(x: Short, y: Short, z: Short, block: UShort) {
        // Default implementation does nothing - override in Player class
    }

    protected fun broadcastBlockUpdate(
        x: Short,
        y: Short,
        z: Short,
        block: UShort,
    ) {
        level?.getPlayers()?.forEach { player ->
            player.updateBlock(x, y, z, block)
        }
    }

    // endregion

    // region Utility Methods
    protected fun getOtherPlayersInLevel(): List<Player> {
        return level?.getPlayers()?.filter { it.entityId != entityId }
            ?: emptyList()
    }

    protected fun isWithinInteractionRange(
        x: Float,
        y: Float,
        z: Float,
        range: Float = 6.0f,
    ): Boolean {
        val dx = position.x - x
        val dy = position.y - y
        val dz = position.z - z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)
        return distance <= distance
    }

    // endregion

    // region Helper Methods
    protected open fun refreshEntityForPlayers() {
        globalDespawn()
        globalSpawn()
    }

    // endregion
}
