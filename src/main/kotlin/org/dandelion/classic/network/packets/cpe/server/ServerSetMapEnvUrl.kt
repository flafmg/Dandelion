package org.dandelion.classic.network.packets.cpe.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerSetMapEnvUrl(val texturePackUrl: String) : Packet() {
    override val id: Byte = 0x28
    override val isCpe: Boolean = true

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeString(texturePackUrl)
        return writer.toByteArray()
    }
}
