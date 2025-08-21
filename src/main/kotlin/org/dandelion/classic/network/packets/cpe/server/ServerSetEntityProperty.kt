package org.dandelion.classic.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerSetEntityProperty(
    val entityId: Byte,
    val propertyType: Byte,
    val propertyValue: Int,
) : Packet() {
    override val id: Byte = 0x2A
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(entityId)
        writer.writeByte(propertyType)
        writer.writeInt(propertyValue)
        return writer.toByteArray()
    }
}
