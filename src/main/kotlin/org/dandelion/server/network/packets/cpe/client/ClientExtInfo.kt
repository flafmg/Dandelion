package org.dandelion.server.network.packets.cpe.client

import io.netty.channel.Channel
import org.dandelion.server.network.PacketRegistry
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketReader

class ClientExtInfo : Packet() {
    override val id: Byte = 0x10
    override val size = 67
    override val isCpe: Boolean = true

    var appName: String = ""
    var extensionCount: Short = 0

    override fun decode(data: ByteArray, channel: Channel) {
        val reader = PacketReader(data)

        appName = reader.readString()
        extensionCount = reader.readShort()
    }

    override fun resolve(channel: Channel) {
        PacketRegistry.handleCPEHandshake(this, channel)
    }
}
