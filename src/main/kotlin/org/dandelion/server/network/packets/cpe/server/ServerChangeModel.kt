package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerChangeModel(val entityId: Byte, val modelName: String) : Packet() {
    override val id: Byte = 0x1D
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(entityId)
        writer.writeString(modelName)
        return writer.toByteArray()
    }
}
