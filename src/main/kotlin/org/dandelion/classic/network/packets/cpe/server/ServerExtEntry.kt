package org.dandelion.classic.network.packets.cpe.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerExtEntry(
    val extName: String,
    val version: Int,
) : Packet() {
    override val id: Byte = 0x11
    override val isCpe: Boolean = true

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeString(extName)
        writer.writeInt(version)
        return writer.toByteArray()
    }
}