package org.dandelion.classic.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter
import org.dandelion.classic.types.enums.LightingMode

class ServerLightingMode(
    private val lightingMode: LightingMode,
    private val locked: Boolean = true,
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
