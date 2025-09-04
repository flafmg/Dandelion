package org.dandelion.server.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerLevelDataChunk(
    val chunkLength: Short,
    val chunkData: ByteArray,
    val percentage: Byte,
) : Packet() {
    override val id: Byte = 0x03

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeShort(chunkLength)
        writer.writeByteArray(chunkData)
        writer.writeByte(percentage)
        return writer.toByteArray()
    }
}
