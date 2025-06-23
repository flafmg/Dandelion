package org.dandelion.classic.entity

import org.dandelion.classic.level.Level
import org.dandelion.classic.network.packets.classic.server.*
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.Block
import kotlin.math.sqrt

open class Entity(
    val name: String,
    var levelId: String = "",
    var entityId: Byte = -1,
    
    val position: Position = Position(0f,0f,0f,0f,0f),
) {
    var level: Level? = null

    open fun setPosition(position: Position){
        setPosition(position.x, position.y, position.z, position.yaw, position.pitch)
    }
    open fun setPosition(x: Int, y: Int, z: Int){
        setPosition(x.toFloat(), y.toFloat(), z.toFloat(), position.yaw, position.pitch)
    }
    open fun setPosition(x: Float, y: Float, z: Float){
        setPosition(x, y, z, position.yaw, position.pitch)
    }
    open fun setPosition(x: Float, y: Float, z: Float, yaw: Float, pitch: Float){
        position.set(x, y, z, yaw, pitch)
        sendSetPositionAndOrientation(x, y, z, yaw.toInt().toByte(), pitch.toInt().toByte())
    }
    open fun setRotation(yaw: Float, pitch: Float){
        setPosition(position.x, position.y, position.z, yaw, pitch)
    }

    open fun sendSetPositionAndOrientation(x: Float, y: Float, z: Float, yaw: Byte, pitch: Byte) {
        level?.getPlayers()?.filter { it.entityId != entityId }?.forEach { player ->
            ServerSetPositionAndOrientation(entityId, x, y, z, yaw, pitch).send(player.channel)
        }
    }

    open fun sendPositionAndOrientationUpdate(dx: Float, dy: Float, dz: Float, yaw: Byte, pitch: Byte) {
        level?.getPlayers()?.filter { it.entityId != entityId }?.forEach { player ->
            ServerPositionAndOrientationUpdate(entityId, dx, dy, dz, yaw, pitch).send(player.channel)
        }
    }
    open fun sendPositionUpdate(dx: Float, dy: Float, dz: Float) {
        level?.getPlayers()?.filter { it.entityId != entityId }?.forEach { player ->
            ServerPositionUpdate(entityId, dx, dy, dz).send(player.channel)
        }
    }
    open fun sendOrientationUpdate(yaw: Byte, pitch: Byte) {
        level?.getPlayers()?.filter { it.entityId != entityId }?.forEach { player ->
            ServerOrientationUpdate(entityId, yaw, pitch).send(player.channel)

        }
    }

    open fun updateEntityPositionAndOrientation(newX: Float, newY: Float, newZ: Float, newYaw: Float, newPitch: Float, absolute: Boolean = false) {
        val dx = newX - position.x
        val dy = newY - position.y
        val dz = newZ - position.z
        val dyaw = newYaw - position.yaw
        val dpitch = newPitch - position.pitch
        val hasMoved = dx != 0f || dy != 0f || dz != 0f
        val hasRotated = dyaw != 0f || dpitch != 0f
        val maxRel = 3.96875f
        val minRel = -4.0f
        val useAbsolute = absolute || dx < minRel || dx > maxRel || dy < minRel || dy > maxRel || dz < minRel || dz > maxRel
        if (useAbsolute && (hasMoved || hasRotated)) {
            sendSetPositionAndOrientation(newX, newY, newZ, newYaw.toInt().toByte(), newPitch.toInt().toByte())
        } else if (hasMoved && hasRotated) {
            sendPositionAndOrientationUpdate(dx, dy, dz, newYaw.toInt().toByte(), newPitch.toInt().toByte())
        } else if (hasMoved) {
            sendPositionUpdate(dx, dy, dz)
        } else if (hasRotated) {
            sendOrientationUpdate(newYaw.toInt().toByte(), newPitch.toInt().toByte())
        }
        position.set(newX, newY, newZ, newYaw, newPitch)
    }

    open fun sendMessageAsEntity(message: String) {
        level?.broadcast("$name: &7$message")
    }

    open fun setBlockAsEntity(x: Short, y: Short, z: Short, block: Byte, mode: Byte){
        if(level == null) return

        val dx = position.x - x
        val dy = position.y - y
        val dz = position.z - z
        val distance = sqrt((dx * dx + dy* dy + dz * dz).toDouble())

        val maxDist = 6.0

        val finalBlockType = if (mode == 0x00.toByte()) Block.Air.id else block

        if(distance <= maxDist){
            level!!.setBlock(x, y, z, finalBlockType)
        }

        val trueBlock = level!!.getBlock(x, y, z)

        level!!.getPlayers().forEach { player ->
            player.updateBlock(x, y, z, trueBlock)
        }
    }
    
    open fun updateBlock(x: Short, y: Short, z: Short, block: Byte){
       // ServerSetBlock(x, y, z, block).send(player)
    }
    
    open fun spawnEntityFor(target: Entity) {
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
    open fun spawnEntityMutual(other: Entity) {
        this.spawnEntityFor(other)
        other.spawnEntityFor(this)
    }

    open fun despawnEntityFor(target: Entity) {
        if (target is Player) {
            ServerDespawnPlayer(entityId).send(target.channel)
        }
    }

    open fun despawnEntityMutual(other: Entity) {
        this.despawnEntityFor(other)
        other.despawnEntityFor(this)
    }

    open fun sendToLevel(level: Level) {
        if (!level.trySetId(this)) {
            return
        }
        this.level = level
        setPosition(level.spawn)
        level.spawnEntityInLevel(this)
    }
}