package org.dandelion.classic.network.packets.classic.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerLevelDataChunk(
    val chunkLength: Short,
    val chunkData: ByteArray,
    val percentage: Byte,
) : Packet() {
    override val id: Byte = 0x03

    override fun encode(): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeShort(chunkLength)
        writer.writeLevelData(chunkData)
        writer.writeByte(percentage)
        return writer.toByteArray()
    }
}
