package org.dandelion.classic.level.io

import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.dandelion.classic.level.Level
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.enums.LightingMode
import org.dandelion.classic.types.extensions.Color
import org.dandelion.classic.types.vec.SVec
import org.dandelion.classic.util.BinaryReader
import org.dandelion.classic.util.BinaryWriter

private const val MAGIC: String = "DLVL"

/**
 * Dandelion Level File Format v4
 * ==============================
 *
 * The file is structured as follows:
 * 1. HEADER
 *         - Magic (String, 4 bytes, unprefixed)
 *         - Version (Byte) (currently 4)
 * 2. INFO SECTION
 *         - Level ID (String)
 *         - Author (String)
 *         - Description (String)
 *         - Creation Timestamp (Long)
 * 3. LEVEL DATA
 *         - Size X (Short)
 *         - Size Y (Short)
 *         - Size Z (Short)
 *         - Spawn X (Float)
 *         - Spawn Y (Float)
 *         - Spawn Z (Float)
 *         - Spawn Yaw (Float)
 *         - Spawn Pitch (Float)
 *         - Extra Data (String, JSON)
 *         - Main Block Array Data (ByteArray, compressed with GZIP)
 *         - Has Secondary Blocks (Boolean)
 *         - Secondary Block Array Data (ByteArray, compressed with GZIP) - only if Has Secondary Blocks = true
 *         - MD5 Validation Hash (16 bytes, unprefixed)
 * 4. ENVIRONMENT SECTION (v2+)
 *     - 4.1 SetMapEnvUrl Properties:
 *             - Texture Pack URL (String)
 *     - 4.2 SetMapEnvProperty Properties:
 *             - Side Block (Short)
 *             - Edge Block (Short)
 *             - Edge Height (Int)
 *             - Clouds Height (Int)
 *             - Max Fog Distance (Int)
 *             - Clouds Speed (Int)
 *             - Weather Speed (Int)
 *             - Weather Fade (Int)
 *             - Exponential Fog (Boolean)
 *             - Sides Offset (Int)
 *     - 4.3 EnvSetWeatherType Properties:
 *             - Weather Type (Byte)
 *     - 4.4 EnvSetColor Properties (6 colors): For each color (Sky, Cloud, Fog,
 *       Ambient Light, Diffuse Light, Skybox):
 *             - Has Color (Boolean)
 *             - Red (Short) - only if Has Color = true
 *             - Green (Short) - only if Has Color = true
 *             - Blue (Short) - only if Has Color = true
 * 5. LIGHTING SECTION (v3+)
 *     - 5.1 LightingMode Properties:
 *             - Lighting Mode (Byte)
 *             - Lighting Mode Locked (Boolean)
 *
 * notes:
 * - All strings are length-prefixed unless otherwise specified.
 * - Block arrays are compressed using GZIP before being written.
 * - The MD5 hash is calculated from the concatenation of both compressed block arrays for integrity validation.
 * - v1 files do not contain the Environment Section.
 * - v2 files do not contain the Lighting Section.
 * - v3 files use single byte blocks and single block array.
 * - v4+ files support up to 768 different block types using dual block arrays.
 *
 * Block Storage Format (v4+):
 * - Main blocks array contains lower 8 bits of block IDs (0-255)
 * - Secondary blocks array contains upper 8 bits of block IDs (256-767 range)
 * - Final block ID = main[index] | (secondary[index] << 8)
 * - Secondary array is only saved if any block ID > 255 exists
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
        writeLighting(writer, level)
    }

    private fun writeHeader(writer: BinaryWriter) {
        writer.writeString(MAGIC, false)
        writer.writeByte(4)
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

        val (mainCompressed, secondaryCompressed) =
            getCompressedBlockData(level)
        val hash = generateMd5(mainCompressed, secondaryCompressed)

        writer.writeByteArray(mainCompressed)
        val hasSecondary = secondaryCompressed != null
        writer.writeBoolean(hasSecondary)
        if (hasSecondary) {
            writer.writeByteArray(secondaryCompressed!!)
        }
        writer.writeByteArray(hash, false)
    }

    private fun writeEnvironment(writer: BinaryWriter, level: Level) {
        writer.writeString(level.texturePackUrl)

        writer.writeShort(level.sideBlock)
        writer.writeShort(level.edgeBlock)
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

    private fun writeLighting(writer: BinaryWriter, level: Level) {
        writer.writeByte(level.lightingMode.id)
        writer.writeBoolean(level.lightingModeLocked)
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

    private fun getCompressedBlockData(
        level: Level
    ): Pair<ByteArray, ByteArray?> {
        val mainCompressed = compressData(level.blockData)
        val secondaryCompressed = level.blockData2?.let { compressData(it) }
        return Pair(mainCompressed, secondaryCompressed)
    }

    private fun compressData(data: ByteArray): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        GZIPOutputStream(byteArrayOutputStream).use { it.write(data) }
        return byteArrayOutputStream.toByteArray()
    }

    private fun generateMd5(
        mainData: ByteArray,
        secondaryData: ByteArray?,
    ): ByteArray {
        val md = MessageDigest.getInstance("MD5")
        md.update(mainData)
        secondaryData?.let { md.update(it) }
        return md.digest()
    }
}

class DandelionLevelDeserializer : LevelDeserializer {
    override fun deserialize(file: File): Level? {
        if (!file.exists() || !file.isFile) return null
        return try {
            val reader = BinaryReader(file)
            val version = readHeader(reader)
            val info = readInfo(reader)
            val level = readData(reader, info, version)
            if (version >= 2) {
                readEnvironment(reader, level, version)
            }
            if (version >= 3) {
                readLighting(reader, level)
            }
            level
        } catch (ex: Exception) {
            null
        }
    }

    private fun readHeader(reader: BinaryReader): Byte {
        val magic = reader.readString(4)
        if (magic != MAGIC)
            throw IllegalArgumentException("Invalid magic: $magic")
        val version = reader.readByte()
        if (version !in 1..4) {
            throw IllegalArgumentException("Unsupported version: $version")
        }
        return version
    }

    private data class Info(
        val id: String,
        val author: String,
        val description: String,
        val timeCreated: Long,
    )

    private fun readInfo(reader: BinaryReader): Info {
        val id = reader.readString()
        val author = reader.readString()
        val description = reader.readString()
        val timeCreated = reader.readLong()
        return Info(id, author, description, timeCreated)
    }

    private fun readData(
        reader: BinaryReader,
        info: Info,
        version: Byte,
    ): Level {
        val sizeX = reader.readShort()
        val sizeY = reader.readShort()
        val sizeZ = reader.readShort()
        val spawnX = reader.readFloat()
        val spawnY = reader.readFloat()
        val spawnZ = reader.readFloat()
        val spawnYaw = reader.readFloat()
        val spawnPitch = reader.readFloat()
        val extraData = reader.readString()

        val (blockData, blockData2) =
            if (version >= 4) {
                val mainCompressed = reader.readByteArray()
                val hasSecondary = reader.readBoolean()
                val secondaryCompressed =
                    if (hasSecondary) reader.readByteArray() else null
                val hash = reader.readByteArray(16)

                val computedHash =
                    generateMd5(mainCompressed, secondaryCompressed)
                if (!computedHash.contentEquals(hash)) {
                    throw IllegalArgumentException(
                        "Level is corrupted! MD5 hash mismatch"
                    )
                }

                decompressBlockData(
                    mainCompressed,
                    secondaryCompressed,
                    sizeX * sizeY * sizeZ,
                )
            } else {
                val compressedBlocks = reader.readByteArray()
                val hash = reader.readByteArray(16)

                val md = MessageDigest.getInstance("MD5")
                val computedHash = md.digest(compressedBlocks)
                if (!computedHash.contentEquals(hash)) {
                    throw IllegalArgumentException(
                        "Level is corrupted! MD5 hash mismatch"
                    )
                }

                decompressLegacyBlocks(compressedBlocks, sizeX * sizeY * sizeZ)
            }

        val level =
            Level(
                id = info.id,
                author = info.author,
                description = info.description,
                size = SVec(sizeX, sizeY, sizeZ),
                blockData = blockData,
                blockData2 = blockData2,
                spawn = Position(spawnX, spawnY, spawnZ, spawnYaw, spawnPitch),
                extraData = extraData,
                timeCreated = info.timeCreated,
            )

        return level
    }

    private fun readEnvironment(
        reader: BinaryReader,
        level: Level,
        version: Byte,
    ) {
        level.texturePackUrl = reader.readString()

        if (version >= 4) {
            level.sideBlock = reader.readShort()
            level.edgeBlock = reader.readShort()
        } else {
            level.sideBlock = reader.readByte().toShort()
            level.edgeBlock = reader.readByte().toShort()
        }

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

    private fun readLighting(reader: BinaryReader, level: Level) {
        val lightingModeId = reader.readByte()
        level.lightingMode =
            LightingMode.fromId(lightingModeId) ?: LightingMode.CLIENT_LOCAL
        level.lightingModeLocked = reader.readBoolean()
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

    private fun decompressBlockData(
        mainCompressed: ByteArray,
        secondaryCompressed: ByteArray?,
        expectedSize: Int,
    ): Pair<ByteArray, ByteArray?> {
        val mainBlocks = decompressData(mainCompressed, expectedSize)
        val secondaryBlocks =
            secondaryCompressed?.let { decompressData(it, expectedSize) }
        return Pair(mainBlocks, secondaryBlocks)
    }

    private fun decompressLegacyBlocks(
        compressed: ByteArray,
        expectedSize: Int,
    ): Pair<ByteArray, ByteArray?> {
        val blocks = decompressData(compressed, expectedSize)
        return Pair(blocks, null)
    }

    private fun decompressData(
        compressed: ByteArray,
        expectedSize: Int,
    ): ByteArray {
        GZIPInputStream(compressed.inputStream()).use { gzip ->
            val out = ByteArray(expectedSize)
            var read = 0
            while (read < expectedSize) {
                val r = gzip.read(out, read, expectedSize - read)
                if (r == -1) break
                read += r
            }
            if (read != expectedSize)
                throw IllegalArgumentException(
                    "Level is corrupted! block array size mismatch"
                )
            return out
        }
    }

    private fun generateMd5(
        mainData: ByteArray,
        secondaryData: ByteArray?,
    ): ByteArray {
        val md = MessageDigest.getInstance("MD5")
        md.update(mainData)
        secondaryData?.let { md.update(it) }
        return md.digest()
    }
}
