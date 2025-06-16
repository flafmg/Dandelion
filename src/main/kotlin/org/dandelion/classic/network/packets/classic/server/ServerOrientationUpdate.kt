package org.dandelion.classic.network.packets.classic.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerOrientationUpdate(
    val playerId: Byte,
    val yaw: Byte,
    val pitch: Byte
) : Packet() {
    override val id: Byte = 0x0B

    override fun encode(): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(playerId)
        writer.writeByte(yaw)
        writer.writeByte(pitch)
        return writer.toByteArray()
    }

}