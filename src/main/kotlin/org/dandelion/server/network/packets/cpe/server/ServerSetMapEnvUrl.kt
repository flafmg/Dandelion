package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerSetMapEnvUrl(val texturePackUrl: String) : Packet() {
    override val id: Byte = 0x28
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeString(texturePackUrl)
        return writer.toByteArray()
    }
}
