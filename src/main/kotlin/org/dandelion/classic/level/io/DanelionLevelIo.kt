package org.dandelion.classic.level.io

import org.dandelion.classic.level.Level
import org.dandelion.classic.util.BinaryReader
import org.dandelion.classic.util.BinaryWriter
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.SVec
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private const val MAGIC: String = "DLVL"
private const val VERSION: Byte = 1

/**
 * Dandelion Level Fle Format v1
 * ===========================
 * 
 * The file is structured as follows:
 * 
 * 1. HEADER
 *    - Magic (String, 4 bytes, unprefixed) (for v1 this is "DLVL")
 *    - Version (Byte) (currently 1)
 * 
 * 2. INFO SECTION
 *    - Level ID (String)
 *    - Author (String)
 *    - Description (String)
 *    - Creation Timestamp (Long)
 * 
 * 3. LEVEL DATA
 *    - Size X (Short)
 *    - Size Y (Short)
 *    - Size Z (Short)
 *    - Spawn X (Float)
 *    - Spawn Y (Float)
 *    - Spawn Z (Float)
 *    - Spawn Yaw (Float)
 *    - Spawn Pitch (Float)
 *    - Extra Data (String, JSON)
 *    - Block Array Data (ByteArray, compressed with GZIP)
 *    - MD5 Validation Hash (16 bytes, unprefixed)
 * 
 * notes:
 * - All strings are length-prefixed unless otherwise specified.
 * - The block array is compressed using GZIP before being written.
 * - The MD5 hash is calculated from the compressed block array for integrity validation.
 */
class DandelionLevelSerializer : LevelSerializer {
    override fun serialize(level: Level, file: File) {
        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
        if (!file.exists()) file.createNewFile()
        val writer = BinaryWriter(file)
        writeHeader(writer)
        writeInfo(writer, level)
        writeData(writer, level)
    }

    private fun writeHeader(writer: BinaryWriter){
        writer.writeString(MAGIC, false)
        writer.writeByte(VERSION)
    }
    private fun writeInfo(writer: BinaryWriter, level: Level){
        writer.writeString(level.id)
        writer.writeString(level.author)
        writer.writeString(level.description)
        writer.writeLong(level.timeCreated)
    }
    private fun writeData(writer: BinaryWriter, level: Level){
        writer.writeShort(level.size.x)
        writer.writeShort(level.size.y)
        writer.writeShort(level.size.z)
        writer.writeFloat(level.spawn.x)
        writer.writeFloat(level.spawn.y)
        writer.writeFloat(level.spawn.z)
        writer.writeFloat(level.spawn.yaw)
        writer.writeFloat(level.spawn.pitch)
        writer.writeString(level.extraData)

        val compressedData = getCompressedData(level)
        val hash = generateMd5(compressedData)
        
        writer.writeByteArray(compressedData)
        writer.writeByteArray(hash, false)
    }
    private fun getCompressedData(level: Level): ByteArray{
        val byteArrayOutputStream = ByteArrayOutputStream()
        GZIPOutputStream(byteArrayOutputStream).use {
            it.write(level.blocks)
        }
        return byteArrayOutputStream.toByteArray()
    }
    private fun generateMd5(data: ByteArray): ByteArray{
        val md = MessageDigest.getInstance("MD5")
        return md.digest(data)
    }
}

class DandelionLevelDeserializer : LevelDeserializer {
    override fun deserialize(file: File): Level? {
        if (!file.exists() || !file.isFile) return null
        return try {
            val reader = BinaryReader(file)
            readHeader(reader)
            val info = readInfo(reader)
            readData(reader, info)
        } catch (ex: Exception) {
            null
        }
    }

    private fun readHeader(reader: BinaryReader) {
        val magic = reader.readString(4)
        if (magic != MAGIC) throw IllegalArgumentException("Invalid magic: $magic")
        val version = reader.readByte()
        if (version != VERSION) throw IllegalArgumentException("Unsupported version: $version")
    }
    private data class Info(
        val id: String,
        val author: String,
        val description: String,
        val timeCreated: Long
    )
    private fun readInfo(reader: BinaryReader): Info {
        val id = reader.readString()
        val author = reader.readString()
        val description = reader.readString()
        val timeCreated = reader.readLong()
        return Info(id, author, description, timeCreated)
    }
    private fun readData(reader: BinaryReader, info: Info): Level {
        val sizeX = reader.readShort()
        val sizeY = reader.readShort()
        val sizeZ = reader.readShort()
        val spawnX = reader.readFloat()
        val spawnY = reader.readFloat()
        val spawnZ = reader.readFloat()
        val spawnYaw = reader.readFloat()
        val spawnPitch = reader.readFloat()
        val extraData = reader.readString()
        val compressedBlocks = reader.readByteArray()
        val hash = reader.readByteArray(16)
        
        val md = MessageDigest.getInstance("MD5")
        val computedHash = md.digest(compressedBlocks)
        if (!computedHash.contentEquals(hash)) {
            throw IllegalArgumentException("Level is corrupted! MD5 hash mismatch")
        }

        val blocks = decompressBlocks(compressedBlocks, sizeX * sizeY * sizeZ)

        val level = Level(
            id = info.id,
            author = info.author,
            description = info.description,
            size = SVec(sizeX, sizeY, sizeZ),
            spawn = Position(spawnX, spawnY, spawnZ, spawnYaw, spawnPitch),
            extraData = extraData,
            timeCreated = info.timeCreated
        )
        level.blocks = blocks
        return level
    }
    private fun decompressBlocks(compressed: ByteArray, expectedSize: Int): ByteArray {
        GZIPInputStream(compressed.inputStream()).use { gzip ->
            val out = ByteArray(expectedSize)
            var read = 0
            while (read < expectedSize) {
                val r = gzip.read(out, read, expectedSize - read)
                if (r == -1) break
                read += r
            }
            if (read != expectedSize) throw IllegalArgumentException("Level is corrupted! block array size mismatch")
            return out
        }
    }
}