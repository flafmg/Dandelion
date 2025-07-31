package org.dandelion.classic.network.packets.cpe.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerCustomBlockLevel(
    val supportLevel: Byte
) : Packet() {
    override val id: Byte = 0x13
    override val isCpe: Boolean = true

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(supportLevel)
        return writer.toByteArray()
    }
}