package org.dandelion.classic.packets.server

import io.netty.channel.Channel
import org.dandelion.classic.packets.model.Packet
import org.dandelion.classic.packets.stream.PacketWriter

class SpawnPlayer(
    var playerId: Byte = 0,
    var playerName: String = "NULL404", //default name in case it explodes we can do the funsies
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var yaw: Byte = 0,
    var pitch: Byte = 0
) : Packet() {
    override val id: Byte = 0x07

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeSByte(playerId)
        writer.writeString(playerName)
        writer.writeFShort(x)
        writer.writeFShort(y)
        writer.writeFShort(z)
        writer.writeByte(yaw)
        writer.writeByte(pitch)
        return writer.toByteArray()
    }

    override fun resolve(channel: Channel) {
       
        sendNetty(channel)
    }
}

