package org.dandelion.classic.network.packets.classic.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerSpawnPlayer(
    val playerId: Byte,
    val playerName: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val yaw: Byte,
    val pitch: Byte,
) : Packet() {
    override val id: Byte = 0x07

    override fun encode(): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(playerId)
        writer.writeString(playerName)
        writer.writeFShort(x)
        writer.writeFShort(y)
        writer.writeFShort(z)
        writer.writeByte(yaw)
        writer.writeByte(pitch)
        return writer.toByteArray()
    }

}