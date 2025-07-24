package org.dandelion.classic.network.packets.classic.client

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketReader
import org.dandelion.classic.player.Players

class ClientPositionAndOrientation : Packet() {
    override val id: Byte = 0x08
    override val size: Int = 10

    var playerId: Byte = 0x00
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f
    var yaw: Byte = 0x0
    var pitch: Byte = 0x0

    override fun decode(data: ByteArray) {
        val reader = PacketReader(data)

        playerId = reader.readByte()
        x = reader.readFShort()
        y = reader.readFShort()
        z = reader.readFShort()
        yaw = reader.readByte()
        pitch = reader.readByte()
    }

    override fun resolve(channel: Channel) {
        val player = Players.findPlayerByChannel(channel)
        player?.updatePositionAndOrientation(x, y, z, yaw.toFloat(), pitch.toFloat())
    }
}