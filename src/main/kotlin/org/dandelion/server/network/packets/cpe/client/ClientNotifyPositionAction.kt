package org.dandelion.server.network.packets.cpe.client

import io.netty.channel.Channel
import org.dandelion.server.entity.player.PlayerRegistry
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketReader

class ClientNotifyPositionAction : Packet() {
    override val id: Byte = 0x3A
    override val size: Int = 9
    override val isCpe: Boolean = true

    var action: Short = 0
    var x: Short = 0
    var y: Short = 0
    var z: Short = 0

    override fun decode(data: ByteArray, channel: Channel) {
        val reader = PacketReader(data)

        action = reader.readShort()
        x = reader.readShort()
        y = reader.readShort()
        z = reader.readShort()
    }

    override fun resolve(channel: Channel) {
        val player = PlayerRegistry.find(channel) ?: return
        player.handleNotifyPositionAction(this)
    }
}
