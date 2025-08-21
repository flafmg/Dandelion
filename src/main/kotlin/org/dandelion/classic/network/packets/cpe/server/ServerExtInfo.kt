package org.dandelion.classic.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerExtInfo(val appName: String, val extensionCount: Short) : Packet() {
    override val id: Byte = 0x10
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeString(appName)
        writer.writeShort(extensionCount)
        return writer.toByteArray()
    }
}
