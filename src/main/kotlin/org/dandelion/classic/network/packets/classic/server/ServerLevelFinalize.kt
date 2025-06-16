package org.dandelion.classic.network.packets.classic.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerLevelFinalize(
    val x: Short,
    val y: Short,
    val z: Short
) : Packet() {

    override val id: Byte = 0x04

    override fun encode(): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeShort(x)
        writer.writeShort(y)
        writer.writeShort(z)
        return writer.toByteArray()
    }

}