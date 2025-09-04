package org.dandelion.server.network.packets.classic.client

import io.netty.channel.Channel
import org.dandelion.server.entity.player.Players
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketReader

class ClientPositionAndOrientation : Packet() {
    override val id: Byte = 0x08
    override val size: Int = 10
    override val sizeOverrides: MutableMap<String, Int> =
        mutableMapOf("ExtendedBlocks" to 1, "ExtEntityPositions" to 6)

    var heldBlock: UShort = 0x00u
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f
    var yaw: Byte = 0x0
    var pitch: Byte = 0x0

    override fun decode(data: ByteArray, channel: Channel) {
        val reader = PacketReader(data)

        if (Players.supports(channel, "ExtendedBlocks")) {
            heldBlock = reader.readUShort()
        } else {
            heldBlock = reader.readByte().toUShort()
        }
        if (Players.supports(channel, "ExtEntityPositions")) {
            x = reader.readFloat()
            y = reader.readFloat()
            z = reader.readFloat()
        } else {
            x = reader.readFShort()
            y = reader.readFShort()
            z = reader.readFShort()
        }
        yaw = reader.readByte()
        pitch = reader.readByte()
    }

    override fun resolve(channel: Channel) {
        val player = Players.find(channel)
        player?.updatePositionAndOrientation(
            x,
            y,
            z,
            yaw.toFloat(),
            pitch.toFloat(),
        )
        player?.updateHeldBlock(heldBlock)
    }
}
