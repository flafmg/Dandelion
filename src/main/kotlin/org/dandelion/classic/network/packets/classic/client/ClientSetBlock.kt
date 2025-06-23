package org.dandelion.classic.network.packets.classic.client

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketReader
import org.dandelion.classic.entity.PlayerManager

class ClientSetBlock : Packet() {
    override val id: Byte = 0x05
    override val size: Int = 9

    var x: Short = 0
    var y: Short = 0
    var z: Short = 0
    var mode: Byte = 0x0
    var blockType: Byte = 0x0

    override fun decode(data: ByteArray) {
        val reader = PacketReader(data)

        x = reader.readShort()
        y = reader.readShort()
        z = reader.readShort()
        mode = reader.readByte()
        blockType = reader.readByte()
    }

    override fun resolve(channel: Channel) {
        val player = PlayerManager.getPlayerByChannel(channel) ?: return
        player.setBlockAsEntity(x, y, z, blockType, mode)
    }
}
