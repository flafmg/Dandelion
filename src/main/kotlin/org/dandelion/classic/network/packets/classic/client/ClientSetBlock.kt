package org.dandelion.classic.network.packets.classic.client

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketReader

class ClientSetBlock : Packet() {
    override val id: Byte = 0x05
    override val size: Int = 8

    var x: Short = 0
    var y: Short = 0
    var z: Short = 0
    var mode: Byte = 0x0
    var blockType: Byte = 0x0

    override fun decode(data: ByteArray) {
        val reader = PacketReader(data)

        x = reader.readShort()
        y = reader.readShort()
        z = reader.readShort()
        mode = reader.readByte()
        blockType = reader.readByte()
    }
}