package org.dandelion.classic.server.packets.stream

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class PacketReader(private val data: ByteArray) {
    private var index = 1 // drops the packet ID

    fun readByte(): Byte {
        val value = data[index].toInt() and 0xFF
        index++
        return value.toByte()
    }
    fun readSByte(): Byte {
        val value = data[index]
        index++
        return value
    }
    fun readFByte(): Float {
        val value = data[index].toInt()
        index++
        // converte para signed byte
        val signed = if (value >= 128) value - 256 else value
        return signed.toFloat() / 32f
    }
    fun readBoolean(): Boolean {
        return readByte().toInt() != 0
    }
    fun readShort(): Short {
        val high = data[index].toInt() and 0xFF
        val low = data[index + 1].toInt() and 0xFF
        index += 2
        return ((high shl 8) or low).toShort()
    }
    fun readFShort(): Float {
        val high = data[index].toInt() and 0xFF
        val low = data[index + 1].toInt() and 0xFF
        index += 2
        val value = (high shl 8) or low
        val signed = if (value >= 32768) value - 65536 else value
        return signed / 32.0f
    }
    fun readInt(): Int {
        val value = ByteBuffer.wrap(data, index, 4).int
        index += 4
        return value
    }
    fun readLong(): Long {
        val value = ByteBuffer.wrap(data, index, 8).long
        index += 8
        return value
    }
    fun readString(length: Int = 64): String {
        val strBytes = data.copyOfRange(index, index + length)
        index += length
        return strBytes.toString(StandardCharsets.UTF_8).trimEnd(' ')
    }
    fun readLevelData(length: Int = 1024): ByteArray {
        val arr = data.copyOfRange(index, index + length)
        index += length
        return arr
    }
    fun getIndex(): Int {
        return index
    }
}
