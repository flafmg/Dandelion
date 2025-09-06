package org.dandelion.classic.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerPositionAndOrientationUpdate(
    val playerId: Byte,
    val changeInX: Float,
    val changeInY: Float,
    val changeInZ: Float,
    val yaw: Float,
    val pitch: Float,
) : Packet() {
    override val id: Byte = 0x09

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(playerId)
        writer.writeFByte(changeInX)
        writer.writeFByte(changeInY)
        writer.writeFByte(changeInZ)
        writer.writeAngleByte(yaw)
        writer.writePitchByte(pitch)
        return writer.toByteArray()
    }
}
