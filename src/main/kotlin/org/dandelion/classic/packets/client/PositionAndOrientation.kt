package org.dandelion.classic.packets.client

import org.dandelion.classic.packets.model.Packet
import org.dandelion.classic.packets.stream.PacketReader
import io.netty.channel.Channel
import org.dandelion.classic.data.player.manager.PlayerManager

class PositionAndOrientation : Packet() {
    override val id: Byte = 0x08
    override val size = 10

    var playerId: Byte = 0
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f
    var yaw: Byte = 0
    var pitch: Byte = 0

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

        val player = PlayerManager.getPlayerByChannel(channel) ?: return
        val fyaw = yaw.toInt().toFloat()
        val fpitch = pitch.toInt().toFloat()
        PlayerManager.updatePlayerPositionAndOrientation(player, x, y, z, fyaw, fpitch)
    }
}
