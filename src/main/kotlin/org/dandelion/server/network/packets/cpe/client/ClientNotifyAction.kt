package org.dandelion.server.network.packets.cpe.client

import io.netty.channel.Channel
import org.dandelion.server.entity.player.Players
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketReader

class ClientNotifyAction : Packet() {
    override val id: Byte = 0x39
    override val size: Int = 5
    override val isCpe: Boolean = true

    var action: Short = 0
    var value: Short = 0

    override fun decode(data: ByteArray, channel: Channel) {
        val reader = PacketReader(data)

        action = reader.readShort()
        value = reader.readShort()
    }

    override fun resolve(channel: Channel) {
        val player = Players.find(channel) ?: return
        player.handleNotifyAction(this)
    }
}
