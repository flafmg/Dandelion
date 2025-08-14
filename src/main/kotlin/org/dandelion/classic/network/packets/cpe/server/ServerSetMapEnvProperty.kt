package org.dandelion.classic.network.packets.cpe.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerSetMapEnvProperty(val propertyType: Byte, val propertyValue: Int) :
    Packet() {
    override val id: Byte = 0x29
    override val isCpe: Boolean = true

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(propertyType)
        writer.writeInt(propertyValue)
        return writer.toByteArray()
    }
}
