package org.dandelion.classic.network.packets.classic.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerPing : Packet() {
    override val id: Byte = 0x01

    override fun encode(): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        return writer.toByteArray()
    }
}