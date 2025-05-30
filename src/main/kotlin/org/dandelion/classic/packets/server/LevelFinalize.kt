package org.dandelion.classic.packets.server

import org.dandelion.classic.packets.model.Packet
import org.dandelion.classic.packets.stream.PacketWriter
import io.netty.channel.Channel

class LevelFinalize(
    private val xSize: Short,
    private val ySize: Short,
    private val zSize: Short
) : Packet() {
    override val id: Byte = 0x04

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeShort(xSize)
        writer.writeShort(ySize)
        writer.writeShort(zSize)
        return writer.toByteArray()
    }
    override fun resolve(channel: Channel) {
       
        sendNetty(channel)
    }
}

