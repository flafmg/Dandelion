package org.dandelion.classic.network.packets.classic.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerDespawnPlayer(
    val playerId: Byte,
) : Packet() {
    override val id: Byte = 0x0C

    override fun encode(): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(playerId)
        return writer.toByteArray()
    }

}