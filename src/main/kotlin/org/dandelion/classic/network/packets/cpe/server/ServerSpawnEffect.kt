package org.dandelion.classic.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerSpawnEffect(
    val effectId: Byte,
    val positionX: Int,
    val positionY: Int,
    val positionZ: Int,
    val originX: Int,
    val originY: Int,
    val originZ: Int
) : Packet() {
    override val id: Byte = 0x31
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(effectId)
        writer.writeInt(positionX)
        writer.writeInt(positionY)
        writer.writeInt(positionZ)
        writer.writeInt(originX)
        writer.writeInt(originY)
        writer.writeInt(originZ)
        return writer.toByteArray()
    }
}

