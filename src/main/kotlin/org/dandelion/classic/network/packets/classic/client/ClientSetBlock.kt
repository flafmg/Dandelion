package org.dandelion.classic.network.packets.classic.client

import io.netty.channel.Channel
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketReader

class ClientSetBlock : Packet() {
    override val id: Byte = 0x05
    override val size: Int = 9
    override val sizeOverrides: MutableMap<String, Int> =
        mutableMapOf("ExtendedBlocks" to 1)

    var x: Short = 0
    var y: Short = 0
    var z: Short = 0
    var mode: Byte = 0x0
    var blockType: UShort = 0x0u

    override fun decode(data: ByteArray, channel: Channel) {
        val reader = PacketReader(data)

        x = reader.readShort()
        y = reader.readShort()
        z = reader.readShort()
        mode = reader.readByte()
        if (Players.supports(channel, "ExtendedBlocks")) {
            blockType = reader.readUShort()
        } else {
            blockType = reader.readByte().toUShort()
        }
    }

    override fun resolve(channel: Channel) {
        val player = Players.find(channel) ?: return
        val destroying = mode != 1.toByte()
        player.interactWithBlock(x, y, z, blockType, destroying)
    }
}
