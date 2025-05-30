package org.dandelion.classic.packets.server

import org.dandelion.classic.packets.model.Packet
import org.dandelion.classic.packets.stream.PacketWriter
import io.netty.channel.Channel

class LevelDataChunk(
    var chunkLength: Short,
    var chunkData: ByteArray,
    var percentComplete: Byte
) : Packet() {
    override val id: Byte = 0x03

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeShort(chunkLength)
        val dataToWrite = if (chunkData.size > chunkLength) {
            chunkData.copyOf(chunkLength.toInt())
        } else {
            chunkData
        }
        writer.writeLevelData(dataToWrite, 1024)
        writer.writeByte(percentComplete)
        return writer.toByteArray()
    }

    override fun resolve(channel: Channel) {
       
        sendNetty(channel)
    }
}