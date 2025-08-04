package org.dandelion.classic.network.packets.cpe.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerSetBlockPermission(
    val blockType: Byte,
    val allowPlacement: Boolean,
    val allowDeletion: Boolean,
) : Packet() {
    override val id: Byte = 0x0C
    override val isCpe: Boolean = true

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(blockType)
        writer.writeBoolean(allowPlacement)
        writer.writeBoolean(allowDeletion)
        return writer.toByteArray()
    }
}