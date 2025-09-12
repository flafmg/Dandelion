package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter
import org.dandelion.server.types.vec.FVec

class ServerDefineModel(
    val modelId: UByte,
    val name: String,
    val flags: UByte,
    val nameY: Float,
    val eyeY: Float,
    val collisionSize: FVec,
    val pickingBoundsMin: FVec,
    val pickingBoundsMax: FVec,
    val uScale: UShort,
    val vScale: UShort,
    val partsCount: UByte
) : Packet() {
    override val id: Byte = 0x32
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(modelId.toByte())
        writer.writeString(name)
        writer.writeByte(flags.toByte())
        writer.writeIEEEFloat(nameY)
        writer.writeIEEEFloat(eyeY)
        writer.writeIEEEFloat(collisionSize.x)
        writer.writeIEEEFloat(collisionSize.y)
        writer.writeIEEEFloat(collisionSize.z)
        writer.writeIEEEFloat(pickingBoundsMin.x)
        writer.writeIEEEFloat(pickingBoundsMin.y)
        writer.writeIEEEFloat(pickingBoundsMin.z)
        writer.writeIEEEFloat(pickingBoundsMax.x)
        writer.writeIEEEFloat(pickingBoundsMax.y)
        writer.writeIEEEFloat(pickingBoundsMax.z)
        writer.writeUShort(uScale)
        writer.writeUShort(vScale)
        writer.writeByte(partsCount.toByte())
        return writer.toByteArray()
    }
}