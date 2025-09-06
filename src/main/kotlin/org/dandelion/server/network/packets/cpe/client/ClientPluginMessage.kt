package org.dandelion.server.network.packets.cpe.client

import io.netty.channel.Channel
import org.dandelion.server.events.PluginMessageReceive
import org.dandelion.server.events.manager.EventDispatcher
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketReader

class ClientPluginMessage : Packet() {
    override val id: Byte = 0x35
    override val size = 66
    override val isCpe: Boolean = true

    var channel: Byte = 0x0
    var message: String = ""

    override fun decode(data: ByteArray, channel: Channel) {
        val reader = PacketReader(data)
        this.channel = reader.readByte()
        this.message = reader.readString()
    }

    override fun resolve(channel: Channel) {
        val event = PluginMessageReceive(this.channel, this.message)
        EventDispatcher.dispatch(event)
    }
}