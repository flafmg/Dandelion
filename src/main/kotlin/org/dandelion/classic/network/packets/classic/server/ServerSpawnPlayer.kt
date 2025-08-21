package org.dandelion.classic.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.classic.entity.player.Players
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

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(playerId)
        writer.writeString(playerName)
        if (Players.supports(channel, "ExtEntityPositions")) {
            writer.writeFloat(x)
            writer.writeFloat(y)
            writer.writeFloat(z)
        } else {
            writer.writeFShort(x)
            writer.writeFShort(y)
            writer.writeFShort(z)
        }
        writer.writeByte(yaw)
        writer.writeByte(pitch)
        return writer.toByteArray()
    }
}
