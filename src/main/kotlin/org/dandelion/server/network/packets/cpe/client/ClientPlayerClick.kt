package org.dandelion.server.network.packets.cpe.client

import io.netty.channel.Channel
import org.dandelion.server.entity.player.PlayerRegistry
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketReader

class ClientPlayerClick : Packet() {
    override val id: Byte = 0x22
    override val size: Int = 15
    override val isCpe: Boolean = true

    var button: Byte = 0
    var action: Byte = 0
    var yaw: Short = 0
    var pitch: Short = 0
    var targetEntityId: Byte = 0
    var targetBlockX: Short = 0
    var targetBlockY: Short = 0
    var targetBlockZ: Short = 0
    var targetBlockFace: Byte = 0

    override fun decode(data: ByteArray, channel: Channel) {
        val reader = PacketReader(data)

        button = reader.readByte()
        action = reader.readByte()
        yaw = reader.readShort()
        pitch = reader.readShort()
        targetEntityId = reader.readByte()
        targetBlockX = reader.readShort()
        targetBlockY = reader.readShort()
        targetBlockZ = reader.readShort()
        targetBlockFace = reader.readByte()
    }

    override fun resolve(channel: Channel) {
        val player = PlayerRegistry.find(channel) ?: return
        player.handleClickEvent(this)
    }
}
