package org.dandelion.classic.server.packets.server

import io.netty.channel.ChannelHandlerContext
import org.dandelion.classic.server.packets.model.Packet
import org.dandelion.classic.server.packets.stream.PacketWriter
import io.netty.channel.Channel
import org.dandelion.classic.server.events.packetEvents.manager.PacketEventManager

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
        if (!PacketEventManager.fireSend(this, channel)) return
        sendNetty(channel)
    }
}