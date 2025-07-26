package org.dandelion.classic.network.packets.cpe.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerEnvWeatherType(
    val weatherType: Byte
) : Packet() {
    override val id: Byte = 0x1F
    override val isCpe: Boolean = true

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(weatherType)
        return writer.toByteArray()
    }
}