package org.dandelion.classic.network.packets.cpe.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerSetHotbar(
    val blockId: Byte,
    val hotbarIndex: Byte
) : Packet() {
    override val id: Byte = 0x2D
    override val isCpe: Boolean = true

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(blockId)
        writer.writeByte(hotbarIndex)
        return writer.toByteArray()
    }
}