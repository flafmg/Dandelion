package org.dandelion.classic.level.io

import org.dandelion.classic.level.Level
import org.dandelion.classic.types.Color
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
private const val VERSION_V1: Byte = 1 //i know this is redundant but looks better this way
private const val VERSION_V2: Byte = 2

/**
 * Dandelion Level File Format v2
 * ==============================
 *
 * The file is structured as follows:
 *
 * 1. HEADER
 *    - Magic (String, 4 bytes, unprefixed)
 *    - Version (Byte) (currently 2)
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
 * 4. ENVIRONMENT SECTION (v2 only)
 *   - 4.1 SetMapEnvUrl Properties:
 *        - Texture Pack URL (String)
 *   - 4.2 SetMapEnvProperty Properties:
 *        - Side Block (Byte)
 *        - Edge Block (Byte)
 *        - Edge Height (Int)
 *        - Clouds Height (Int)
 *        - Max Fog Distance (Int)
 *        - Clouds Speed (Int)
 *        - Weather Speed (Int)
 *        - Weather Fade (Int)
 *        - Exponential Fog (Boolean)
 *        - Sides Offset (Int)
 *   - 4.3 EnvSetWeatherType Properties:
 *        - Weather Type (Byte)
 *   - 4.4 EnvSetColor Properties (6 colors):
 *        For each color (Sky, Cloud, Fog, Ambient Light, Diffuse Light, Skybox):
 *        - Has Color (Boolean)
 *        - Red (Short) - only if Has Color = true
 *        - Green (Short) - only if Has Color = true
 *        - Blue (Short) - only if Has Color = true
 *
 * notes:
 * - All strings are length-prefixed unless otherwise specified.
 * - The block array is compressed using GZIP before being written.
 * - The MD5 hash is calculated from the compressed block array for integrity validation.
 * - v1 files do not contain the Environment Section.
 */
class DandelionLevelSerializer : LevelSerializer {
    override fun serialize(level: Level, file: File) {
        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
        if (!file.exists()) file.createNewFile()
        val writer = BinaryWriter(file)
        writeHeader(writer)
        writeInfo(writer, level)
        writeData(writer, level)
        writeEnvironment(writer, level)
    }

    private fun writeHeader(writer: BinaryWriter) {
        writer.writeString(MAGIC, false)
        writer.writeByte(VERSION_V2)
    }

    private fun writeInfo(writer: BinaryWriter, level: Level) {
        writer.writeString(level.id)
        writer.writeString(level.author)
        writer.writeString(level.description)
        writer.writeLong(level.timeCreated)
    }

    private fun writeData(writer: BinaryWriter, level: Level) {
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

    private fun writeEnvironment(writer: BinaryWriter, level: Level) {
        writer.writeString(level.texturePackUrl)

        writer.writeByte(level.sideBlock)
        writer.writeByte(level.edgeBlock)
        writer.writeInt(level.edgeHeight)
        writer.writeInt(level.cloudsHeight)
        writer.writeInt(level.maxFogDistance)
        writer.writeInt(level.cloudsSpeed)
        writer.writeInt(level.weatherSpeed)
        writer.writeInt(level.weatherFade)
        writer.writeBoolean(level.exponentialFog)
        writer.writeInt(level.sidesOffset)

        writer.writeByte(level.weatherType)

        writeColor(writer, level.skyColor)
        writeColor(writer, level.cloudColor)
        writeColor(writer, level.fogColor)
        writeColor(writer, level.ambientLightColor)
        writeColor(writer, level.diffuseLightColor)
        writeColor(writer, level.skyboxColor)
    }

    private fun writeColor(writer: BinaryWriter, color: Color?) {
        val hasColor = color != null
        writer.writeBoolean(hasColor)
        if (hasColor) {
            writer.writeShort(color!!.red)
            writer.writeShort(color.green)
            writer.writeShort(color.blue)
        }
    }

    private fun getCompressedData(level: Level): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        GZIPOutputStream(byteArrayOutputStream).use {
            it.write(level.blocks)
        }
        return byteArrayOutputStream.toByteArray()
    }

    private fun generateMd5(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(data)
    }
}

class DandelionLevelDeserializer : LevelDeserializer {
    override fun deserialize(file: File): Level? {
        if (!file.exists() || !file.isFile) return null
        return try {
            val reader = BinaryReader(file)
            val version = readHeader(reader)
            val info = readInfo(reader)
            val level = readData(reader, info)
            if (version >= VERSION_V2) {
                readEnvironment(reader, level)
            }
            level
        } catch (ex: Exception) {
            null
        }
    }

    private fun readHeader(reader: BinaryReader): Byte {
        val magic = reader.readString(4)
        if (magic != MAGIC) throw IllegalArgumentException("Invalid magic: $magic")
        val version = reader.readByte()
        if (version != VERSION_V1 && version != VERSION_V2) {
            throw IllegalArgumentException("Unsupported version: $version")
        }
        return version
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

    private fun readEnvironment(reader: BinaryReader, level: Level) {
        level.texturePackUrl = reader.readString()

        level.sideBlock = reader.readByte()
        level.edgeBlock = reader.readByte()
        level.edgeHeight = reader.readInt()
        level.cloudsHeight = reader.readInt()
        level.maxFogDistance = reader.readInt()
        level.cloudsSpeed = reader.readInt()
        level.weatherSpeed = reader.readInt()
        level.weatherFade = reader.readInt()
        level.exponentialFog = reader.readBoolean()
        level.sidesOffset = reader.readInt()

        level.weatherType = reader.readByte()

        level.skyColor = readColor(reader)
        level.cloudColor = readColor(reader)
        level.fogColor = readColor(reader)
        level.ambientLightColor = readColor(reader)
        level.diffuseLightColor = readColor(reader)
        level.skyboxColor = readColor(reader)
    }

    private fun readColor(reader: BinaryReader): Color? {
        val hasColor = reader.readBoolean()
        return if (hasColor) {
            val red = reader.readShort()
            val green = reader.readShort()
            val blue = reader.readShort()
            Color(red, green, blue)
        } else {
            null
        }
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