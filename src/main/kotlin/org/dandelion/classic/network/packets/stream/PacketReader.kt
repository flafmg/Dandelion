package org.dandelion.classic.network.packets.stream

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

internal class PacketReader(private val data: ByteArray) {
    private var index = 1 // the first byte is packet id, we drop it

    fun readByte(): Byte {
        val value = data[index].toByte()
        index++
        return value
    }

    fun readSByte(): Byte {
        val value = data[index].toByte()
        index++
        return value
    }

    fun readFByte(): Float {
        val value = data[index].toInt()
        index++
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

    fun readUShort(): UShort {
        val high = data[index].toInt() and 0xFF
        val low = data[index + 1].toInt() and 0xFF
        index += 2
        return ((high shl 8) or low).toUShort()
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

    fun readByteArray(length: Int = 1024): ByteArray {
        val arr = data.copyOfRange(index, index + length)
        index += length
        return arr
    }

    fun readFloat(): Float {
        val value = ByteBuffer.wrap(data, index, 4).int
        index += 4
        return value / 32.0f
    }

    fun readRangedByte(min: Float, max: Float): Float {
        val byteValue = data[index].toInt()
        index++
        val normalizedByte = (byteValue + 128) / 255.0f // 0..1
        return min + normalizedByte * (max - min)
    }

    fun readAngleByte(): Float {
        return readRangedByte(0f, 360f)
    }

    fun readPitchByte(): Float {
        return readRangedByte(-90f, 90f)
    }

    fun getIndex(): Int {
        return index
    }
}
