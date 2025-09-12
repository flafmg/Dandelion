package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter
import org.dandelion.server.types.AnimData
import org.dandelion.server.types.UVCoords
import org.dandelion.server.types.vec.FVec

class ServerDefineModelPart(
    val modelId: UByte,
    val minimumCoords: FVec,
    val maximumCoords: FVec,
    val topFaceUV: UVCoords,
    val bottomFaceUV: UVCoords,
    val frontFaceUV: UVCoords,
    val backFaceUV: UVCoords,
    val leftFaceUV: UVCoords,
    val rightFaceUV: UVCoords,
    val rotationOrigin: FVec,
    val rotationAngles: FVec,
    val animation1: AnimData,
    val animation2: AnimData,
    val animation3: AnimData,
    val animation4: AnimData,
    val flags: UByte
) : Packet() {
    override val id: Byte = 0x33
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(modelId.toByte())
        
        writer.writeIEEEFloat(minimumCoords.x)
        writer.writeIEEEFloat(minimumCoords.y)
        writer.writeIEEEFloat(minimumCoords.z)

        writer.writeIEEEFloat(maximumCoords.x)
        writer.writeIEEEFloat(maximumCoords.y)
        writer.writeIEEEFloat(maximumCoords.z)

        val uvFaces = listOf(topFaceUV, bottomFaceUV, frontFaceUV, backFaceUV, leftFaceUV, rightFaceUV)
        uvFaces.forEach { uv ->
            writer.writeUShort(uv.u1)
            writer.writeUShort(uv.v1)
            writer.writeUShort(uv.u2)
            writer.writeUShort(uv.v2)
        }

        writer.writeIEEEFloat(rotationOrigin.x)
        writer.writeIEEEFloat(rotationOrigin.y)
        writer.writeIEEEFloat(rotationOrigin.z)

        writer.writeIEEEFloat(rotationAngles.x)
        writer.writeIEEEFloat(rotationAngles.y)
        writer.writeIEEEFloat(rotationAngles.z)

        writeAnimData(writer, animation1)
        writeAnimData(writer, animation2)
        writeAnimData(writer, animation3)
        writeAnimData(writer, animation4)
        
        writer.writeByte(flags.toByte())
        
        return writer.toByteArray()
    }
    
    private fun writeAnimData(writer: PacketWriter, anim: AnimData) {
        writer.writeByte(anim.flags)
        writer.writeIEEEFloat(anim.a)
        writer.writeIEEEFloat(anim.b)
        writer.writeIEEEFloat(anim.c)
        writer.writeIEEEFloat(anim.d)
    }
}