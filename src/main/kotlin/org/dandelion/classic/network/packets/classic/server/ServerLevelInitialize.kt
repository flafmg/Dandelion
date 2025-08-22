package org.dandelion.classic.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerLevelInitialize(private val mapSize: Int? = null) : Packet() {
    override val id: Byte = 0x02

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        mapSize?.let { size -> writer.writeInt(size) }

        return writer.toByteArray()
    }
}
