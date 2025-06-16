package org.dandelion.classic.network.packets.classic.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerSetBlock(
    val x: Short,
    val y: Short,
    val z: Short,
    val blockType: Byte
): Packet() {
    override val id: Byte = 0x06

    override fun encode(): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeShort(x)
        writer.writeShort(y)
        writer.writeShort(z)
        writer.writeByte(blockType)
        return writer.toByteArray()
    }

}