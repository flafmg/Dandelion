package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.entity.player.Players
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerDefineBlockExt(
    val blockId: UShort,
    val name: String,
    val solidity: Byte,
    val movementSpeed: Byte,
    val topTextureId: UShort,
    val leftTextureId: UShort,
    val rightTextureId: UShort,
    val frontTextureId: UShort,
    val backTextureId: UShort,
    val bottomTextureId: UShort,
    val transmitsLight: Boolean,
    val walkSound: Byte,
    val fullBright: Boolean,
    val minX: Byte,
    val minY: Byte,
    val minZ: Byte,
    val maxX: Byte,
    val maxY: Byte,
    val maxZ: Byte,
    val blockDraw: Byte,
    val fogDensity: Byte,
    val fogR: Byte,
    val fogG: Byte,
    val fogB: Byte,
) : Packet() {
    override val id: Byte = 0x25
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        if (Players.supports(channel, "ExtendedBlocks")) {
            writer.writeUShort(blockId)
        } else {
            writer.writeByte((blockId and 0xFFu).toByte())
        }
        writer.writeString(name)

        writer.writeByte(solidity)

        writer.writeByte(movementSpeed)

        if (Players.supports(channel, "ExtendedTextures")) {
            writer.writeUShort(topTextureId)
            writer.writeUShort(leftTextureId)
            writer.writeUShort(rightTextureId)
            writer.writeUShort(frontTextureId)
            writer.writeUShort(backTextureId)
            writer.writeUShort(bottomTextureId)
        } else {
            writer.writeByte((topTextureId and 0xFFu).toByte())
            writer.writeByte((leftTextureId and 0xFFu).toByte())
            writer.writeByte((rightTextureId and 0xFFu).toByte())
            writer.writeByte((frontTextureId and 0xFFu).toByte())
            writer.writeByte((backTextureId and 0xFFu).toByte())
            writer.writeByte((bottomTextureId and 0xFFu).toByte())
        }

        writer.writeBoolean(transmitsLight)
        writer.writeByte(walkSound)
        writer.writeBoolean(fullBright)

        writer.writeByte(minX)
        writer.writeByte(minY)
        writer.writeByte(minZ)
        writer.writeByte(maxX)
        writer.writeByte(maxY)
        writer.writeByte(maxZ)

        writer.writeByte(blockDraw)
        writer.writeByte(fogDensity)

        writer.writeByte(fogR)
        writer.writeByte(fogG)
        writer.writeByte(fogB)
        return writer.toByteArray()
    }
}
