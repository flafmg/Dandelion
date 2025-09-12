package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.entity.player.PlayerRegistry
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter
import org.dandelion.server.types.enums.MoveMode

class ServerExtEntityTeleport(
    val entityId: Byte,
    val usePosition: Boolean,
    val moveMode: MoveMode,
    val useOrientation: Boolean,
    val interpolateOrientation: Boolean,
    val x: Float,
    val y: Float,
    val z: Float,
    val yaw: Float,
    val pitch: Float,
) : Packet() {
    override val id: Byte = 0x36
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(entityId)
        writer.writeByte(encodeTeleportBehavior())
        if (PlayerRegistry.supports(channel, "ExtEntityPositions")) {
            writer.writeFloat(x)
            writer.writeFloat(y)
            writer.writeFloat(z)
        } else {
            writer.writeFShort(x)
            writer.writeFShort(y)
            writer.writeFShort(z)
        }
        writer.writeAngleByte(yaw)
        writer.writePitchByte(pitch)
        return writer.toByteArray()
    }

    private fun encodeTeleportBehavior(): Byte {
        var b = 0
        if (usePosition) b = b or 0b00000001
        b = b or ((moveMode.value and 0b11) shl 1)
        if (useOrientation) b = b or 0b00010000
        if (interpolateOrientation) b = b or 0b00100000
        return b.toByte()
    }
}
