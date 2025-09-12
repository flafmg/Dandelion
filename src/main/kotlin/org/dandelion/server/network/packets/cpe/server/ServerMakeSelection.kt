package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter
import org.dandelion.server.types.SelectionCuboid

class ServerMakeSelection(
    val selectionId: Byte,
    val label: String,
    val startX: Short,
    val startY: Short,
    val startZ: Short,
    val endX: Short,
    val endY: Short,
    val endZ: Short,
    val red: Short,
    val green: Short,
    val blue: Short,
    val opacity: Short,
) : Packet() {

    constructor(
        selection: SelectionCuboid
    ) : this(
        selection.id,
        selection.label,
        selection.startX,
        selection.startY,
        selection.startZ,
        selection.endX,
        selection.endY,
        selection.endZ,
        selection.color.red,
        selection.color.green,
        selection.color.blue,
        selection.opacity,
    )

    override val id: Byte = 0x1A
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(selectionId)
        writer.writeString(label)
        writer.writeShort(startX)
        writer.writeShort(startY)
        writer.writeShort(startZ)
        writer.writeShort(endX)
        writer.writeShort(endY)
        writer.writeShort(endZ)
        writer.writeShort(red)
        writer.writeShort(green)
        writer.writeShort(blue)
        writer.writeShort(opacity)
        return writer.toByteArray()
    }
}
