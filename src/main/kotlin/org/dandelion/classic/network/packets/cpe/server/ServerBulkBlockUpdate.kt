package org.dandelion.classic.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerBulkBlockUpdate(val indices: IntArray, val blocks: UShortArray) :
    Packet() {
    override val id: Byte = 0x26
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)

        val count = (indices.size - 1).toByte()
        writer.writeByte(count)

        val indicesArray = ByteArray(1024)
        for (i in indices.indices) {
            val index = indices[i]
            val offset = i * 4
            indicesArray[offset] = (index shr 24).toByte()
            indicesArray[offset + 1] = (index shr 16).toByte()
            indicesArray[offset + 2] = (index shr 8).toByte()
            indicesArray[offset + 3] = index.toByte()
        }
        writer.writeByteArray(indicesArray, 1024)

        if (!Players.supports(channel, "ExtendedBlocks")) {
            val blocksArray = ByteArray(256)
            for (i in blocks.indices) {
                blocksArray[i] = (blocks[i] and 0xFFu).toByte()
            }
            writer.writeByteArray(blocksArray, 256)
        } else {
            val blocksArray = ByteArray(320)
            for (i in 0 until 256) {
                blocksArray[i] = (blocks[i] and 0xFFu).toByte()
            }
            for (i in 0 until 64) {
                val base = i * 4
                var flags = 0
                if (base + 0 < blocks.size)
                    flags =
                        flags or
                            (((blocks[base + 0].toInt() shr 8) and 0x03) shl 0)
                if (base + 1 < blocks.size)
                    flags =
                        flags or
                            (((blocks[base + 1].toInt() shr 8) and 0x03) shl 2)
                if (base + 2 < blocks.size)
                    flags =
                        flags or
                            (((blocks[base + 2].toInt() shr 8) and 0x03) shl 4)
                if (base + 3 < blocks.size)
                    flags =
                        flags or
                            (((blocks[base + 3].toInt() shr 8) and 0x03) shl 6)
                blocksArray[256 + i] = flags.toByte()
            }
            writer.writeByteArray(blocksArray, 320)
        }

        return writer.toByteArray()
    }
}
