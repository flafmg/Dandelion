package org.dandelion.classic.network.packets.classic.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerPositionAndOrientationUpdate(
    val playerId: Byte,
    val changeInX: Float,
    val changeInY: Float,
    val changeInZ: Float,
    val yaw: Byte,
    val pitch: Byte,
) : Packet() {
    override val id: Byte = 0x09

    override fun encode(): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(playerId)
        writer.writeFByte(changeInX)
        writer.writeFByte(changeInY)
        writer.writeFByte(changeInZ)
        writer.writeByte(yaw)
        writer.writeByte(pitch)
        return writer.toByteArray()
    }
}
