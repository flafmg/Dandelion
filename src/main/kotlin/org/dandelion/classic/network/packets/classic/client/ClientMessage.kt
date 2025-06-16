package org.dandelion.classic.network.packets.classic.client

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketReader

class ClientMessage : Packet() {
    override val id: Byte = 0x0d
    override val size: Int = 65

    var playerId: Byte = 0x00
    var message: String = ""


    override fun decode(data: ByteArray) {
        val reader = PacketReader(data)

        playerId = reader.readByte()
        message = reader.readString()
    }
}