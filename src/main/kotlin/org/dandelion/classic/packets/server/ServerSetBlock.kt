package org.dandelion.classic.packets.server

import io.netty.channel.Channel
import org.dandelion.classic.packets.model.Packet
import org.dandelion.classic.packets.stream.PacketWriter

class ServerSetBlock(
    val x: Short,
    val y: Short,
    val z: Short,
    val blockType: Byte
) : Packet() {
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
    override fun resolve(channel: Channel) {
       
        sendNetty(channel)
    }
}

