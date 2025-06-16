package org.dandelion.classic.network.packets.classic.server

import org.dandelion.classic.network.packets.Packet

class LevelInitialize : Packet() {
    override val id: Byte = 0x02

    override fun encode(): ByteArray {
        return byteArrayOf(id)
    }

}