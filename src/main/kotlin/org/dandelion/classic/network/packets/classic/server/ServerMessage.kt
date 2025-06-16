package org.dandelion.classic.network.packets.classic.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerMessage(
    val playerId: Byte,
    val message: String,
) : Packet() {
    override val id: Byte = 0x0D

    override fun encode(): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(playerId)
        writer.writeString(message)
        return writer.toByteArray()
    }

}