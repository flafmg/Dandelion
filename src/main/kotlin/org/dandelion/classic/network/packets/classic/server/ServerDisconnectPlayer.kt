package org.dandelion.classic.network.packets.classic.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerDisconnectPlayer(val reason: String) : Packet() {
    override val id: Byte = 0x0E

    override fun encode(): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeString(reason)
        return writer.toByteArray()
    }
}
