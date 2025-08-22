package org.dandelion.classic.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter
import org.dandelion.classic.types.extensions.SelectionCuboid

/** Packet to create a selection cuboid on the client */
class ServerMakeSelection(
    private val selectionId: Byte,
    private val label: String,
    private val startX: Short,
    private val startY: Short,
    private val startZ: Short,
    private val endX: Short,
    private val endY: Short,
    private val endZ: Short,
    private val red: Short,
    private val green: Short,
    private val blue: Short,
    private val opacity: Short,
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
