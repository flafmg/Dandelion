package org.dandelion.server.network.packets.cpe.client

import io.netty.channel.Channel
import org.dandelion.server.network.PacketRegistry
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketReader

class ClientExtEntry : Packet() {
    override val id: Byte = 0x11
    override val size = 69
    override val isCpe: Boolean = true

    var extName: String = ""
    var version: Int = 0

    override fun decode(data: ByteArray, channel: Channel) {
        val reader = PacketReader(data)
        extName = reader.readString()
        version = reader.readInt()
    }

    override fun resolve(channel: Channel) {
        PacketRegistry.handleCPEEntry(this, channel)
    }
}
