package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.entity.player.PlayerRegistry
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerDefineBlock(
    val blockId: UShort,
    val name: String,
    val solidity: Byte,
    val movementSpeed: Byte,
    val topTextureId: UShort,
    val sideTextureId: UShort,
    val bottomTextureId: UShort,
    val transmitsLight: Boolean,
    val walkSound: Byte,
    val fullBright: Boolean,
    val shape: Byte,
    val blockDraw: Byte,
    val fogDensity: Byte,
    val fogR: Byte,
    val fogG: Byte,
    val fogB: Byte,
) : Packet() {
    override val id: Byte = 0x23
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        if (PlayerRegistry.supports(channel, "ExtendedBlocks")) {
            writer.writeUShort(blockId)
        } else {
            writer.writeByte((blockId and 0xFFu).toByte())
        }
        writer.writeString(name)

        writer.writeByte(solidity)
        writer.writeByte(movementSpeed)

        if (PlayerRegistry.supports(channel, "ExtendedTextures")) {
            writer.writeUShort(topTextureId)
            writer.writeUShort(sideTextureId)
            writer.writeUShort(bottomTextureId)
        } else {
            writer.writeByte((topTextureId and 0xFFu).toByte())
            writer.writeByte((sideTextureId and 0xFFu).toByte())
            writer.writeByte((bottomTextureId and 0xFFu).toByte())
        }

        writer.writeBoolean(transmitsLight)
        writer.writeByte(walkSound)

        writer.writeBoolean(fullBright)

        writer.writeByte(shape)

        writer.writeByte(blockDraw)
        writer.writeByte(fogDensity)
        writer.writeByte(fogR)
        writer.writeByte(fogG)
        writer.writeByte(fogB)
        return writer.toByteArray()
    }
}
