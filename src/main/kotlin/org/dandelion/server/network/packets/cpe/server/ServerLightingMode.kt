package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter
import org.dandelion.server.types.enums.LightingMode

class ServerLightingMode(
    val lightingMode: LightingMode,
    val locked: Boolean = true,
) : Packet() {
    override val id: Byte = 0x37

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(lightingMode.id)
        writer.writeByte(if (locked) 1 else 0)
        return writer.toByteArray()
    }
}
