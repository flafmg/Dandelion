package org.dandelion.classic.stream

import java.io.*

class BinaryReader(file: File) {
    private val input = FileInputStream(file)
    private val data = DataInputStream(input)
    private val totalSize = file.length()
    private var bytesRead = 0L
    fun readByte(): Byte{
        bytesRead++
        return data.readByte()
    }
    fun readShort(): Short{
        bytesRead += 2
        return data.readShort()
    }
    fun readInt(): Int{
        bytesRead += 4
        return data.readInt()
    }
    fun readLong(): Long{
        bytesRead += 8
        return data.readLong()
    }
    fun readFloat(): Float{
        bytesRead += 4
        return data.readFloat()
    }
    fun readDouble(): Double{
        bytesRead += 8
        return data.readDouble()
    }
    fun readChar(): Char{
        bytesRead ++
        return data.readChar()
    }
    fun readBool(): Boolean{
        bytesRead++
        return data.readBoolean()
    }
    fun readString(): String{
        val size = readInt()
        bytesRead += size
        val bytes = ByteArray(size)
        data.readFully(bytes)
        return String(bytes)
    }
    fun readString(size: Int): String{
        bytesRead += size
        val bytes = ByteArray(size)
        data.readFully(bytes)
        return String(bytes)
    }
    fun readByteArray(): ByteArray {
        val size = readInt()
        bytesRead += size
        val bytes = ByteArray(size)
        data.readFully(bytes)
        return bytes
    }
    fun readByteArray(size: Int): ByteArray {
        bytesRead += size
        val bytes = ByteArray(size)
        data.readFully(bytes)
        return bytes
    }
    fun remainingSize(): Long {
        return totalSize - bytesRead
    }
    fun readUshort(): UShort {
        return readShort().toUShort()
    }
    fun readUint(): UInt {
        return readInt().toUInt()
    }
    fun readUlong(): ULong {
        return readLong().toULong()
    }
}
class BinaryWriter(private val file: File){
    private val output = FileOutputStream(file)
    private val data = DataOutputStream(output)
    private var bytesWrite = 0L

    fun writeByte(value: Byte){
        data.writeByte(value.toInt())
        bytesWrite ++
    }
    fun writeShort(value: Short){
        data.writeShort(value.toInt())
        bytesWrite += 2
    }
    fun writeInt(value: Int){
        data.writeInt(value)
        bytesWrite += 4
    }
    fun writeLong(value: Long){
        data.writeLong(value)
        bytesWrite += 8
    }
    fun writeFloat(value: Float){
        data.writeFloat(value)
        bytesWrite += 4
    }
    fun writeDouble(value: Double){
        data.writeDouble(value)
        bytesWrite += 8
    }
    fun writeChar(value: Char){
        data.writeChar(value.code)
        bytesWrite ++
    }
    fun writeBool(value: Boolean){
        data.writeBoolean(value)
        bytesWrite ++
    }

    fun writeString(value: String, sizePrefix: Boolean = true){
        val bytes = value.toByteArray()
        if (sizePrefix) {
            writeInt(bytes.size)
        }
        data.write(bytes)
        bytesWrite += bytes.size
    }
    fun writeByteArray(bytes: ByteArray, sizePrefix: Boolean = true){
        if (sizePrefix) {
            writeInt(bytes.size)
        }
        data.write(bytes)
        bytesWrite += bytes.size
    }
    fun writeUshort(value: UShort){
        writeShort(value.toShort())
    }
    fun writeUint(value: UInt){
        writeInt(value.toInt())
    }
    fun writeUlong(value: ULong){
        writeLong(value.toLong())
    }
    fun getBytesWrite(): Long{
        return bytesWrite
    }

}