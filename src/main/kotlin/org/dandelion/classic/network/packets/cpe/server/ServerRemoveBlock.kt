package org.dandelion.classic.network.packets.cpe.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerRemoveBlock(
    val blockId: Byte
) : Packet() {
    override val id: Byte = 0x24
    override val isCpe: Boolean = true

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(blockId)
        return writer.toByteArray()
    }
}