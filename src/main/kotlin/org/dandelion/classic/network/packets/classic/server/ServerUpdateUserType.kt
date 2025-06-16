package org.dandelion.classic.network.packets.classic.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerUpdateUserType(
    val userType: Byte
) : Packet() {
    override val id: Byte = 0x0F

    override fun encode(): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(userType)
        return writer.toByteArray()
    }
}