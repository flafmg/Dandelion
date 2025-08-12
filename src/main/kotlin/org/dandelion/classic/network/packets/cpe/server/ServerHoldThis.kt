package org.dandelion.classic.network.packets.cpe.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerHoldThis(
    val blockToHold: Byte,
    val preventChange: Byte
) : Packet() {
    override val id: Byte = 0x14
    override val isCpe: Boolean = true

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(blockToHold)
        writer.writeByte(preventChange)
        return writer.toByteArray()
    }
}