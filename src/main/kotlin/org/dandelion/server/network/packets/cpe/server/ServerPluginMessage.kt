package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import jdk.internal.joptsimple.internal.Messages.message
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketReader
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerPluginMessage(val channel: Byte, val message: String) : Packet() {
    override val id: Byte = 0x35
    override val size = 66
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(this.channel)
        writer.writeString(this.message)
        return writer.toByteArray()
    }
} 