package org.dandelion.classic.level.io

import com.viaversion.nbt.io.NBTIO
import com.viaversion.nbt.limiter.TagLimiter
import com.viaversion.nbt.tag.*
import java.io.File
import java.security.MessageDigest
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.experimental.and
import org.dandelion.classic.blocks.manager.BlockRegistry
import org.dandelion.classic.blocks.model.Block
import org.dandelion.classic.blocks.model.enums.BlockDraw
import org.dandelion.classic.blocks.model.enums.BlockSolidity
import org.dandelion.classic.blocks.model.enums.WalkSound
import org.dandelion.classic.level.Level
import org.dandelion.classic.server.Console
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.enums.LightingMode
import org.dandelion.classic.types.extensions.Color
import org.dandelion.classic.types.vec.SVec

class ClassicWorldLevelSerializer : LevelSerializer {
    override fun serialize(level: Level, file: File) {
        file.parentFile?.mkdirs()
        file.createNewFile()

        val worldData = CompoundTag()
        writeHeader(worldData, level)
        writeSpawn(worldData, level)
        writeBlockData(worldData, level)
        writeMetadata(worldData, level)

        val root = CompoundTag()
        root.put("", worldData)

        try {
            DataOutputStream(GZIPOutputStream(file.outputStream())).use { out ->
                out.writeByte(10)
                out.writeUTF("ClassicWorld")
                worldData.write(out)
                out.writeByte(0)
            }
        } catch (e: Exception) {
            Console.errLog("Error serializing level ${level.id}: (${e.message}) ${e.stackTraceToString()}")
        }
    }

    private fun writeHeader(root: CompoundTag, level: Level) {
        root.put("FormatVersion", ByteTag(1))
        root.put("Name", StringTag(level.id))
        root.put("UUID", ByteArrayTag(generateUUID(level.id)))
        root.put("X", ShortTag(level.size.x))
        root.put("Y", ShortTag(level.size.y))
        root.put("Z", ShortTag(level.size.z))

        val createdBy = CompoundTag()
        createdBy.put("Service", StringTag("Dandelion"))
        createdBy.put("Username", StringTag(level.author))
        root.put("CreatedBy", createdBy)

        val mapGenerator = CompoundTag()
        mapGenerator.put("Software", StringTag("Dandelion"))
        mapGenerator.put("MapGeneratorName", StringTag("Unknown"))
        root.put("MapGenerator", mapGenerator)

        root.put("TimeCreated", LongTag(level.timeCreated))
        root.put("LastAccessed", LongTag(System.currentTimeMillis()))
        root.put("LastModified", LongTag(System.currentTimeMillis()))
    }

    private fun writeSpawn(root: CompoundTag, level: Level) {
        val spawn = CompoundTag()
        spawn.put("X", ShortTag((level.spawn.x).toInt().toShort()))
        spawn.put("Y", ShortTag((level.spawn.y).toInt().toShort()))
        spawn.put("Z", ShortTag((level.spawn.z).toInt().toShort()))
        spawn.put("H", ByteTag((level.spawn.yaw).toInt().toByte()))
        spawn.put("P", ByteTag((level.spawn.pitch).toInt().toByte()))
        root.put("Spawn", spawn)
    }

    private fun writeBlockData(root: CompoundTag, level: Level) {
        root.put("BlockArray", ByteArrayTag(level.blockData))
        if (level.blockData2 != null) {
            root.put("BlockArray2", ByteArrayTag(level.blockData2))
        }
    }

    private fun writeMetadata(root: CompoundTag, level: Level) {
        val metadata = CompoundTag()
        val cpe = CompoundTag()

        writeEnvironmentMetadata(cpe, level)
        writeLightingMetadata(cpe, level)

        metadata.put("CPE", cpe)
        root.put("Metadata", metadata)
    }

    private fun writeEnvironmentMetadata(cpe: CompoundTag, level: Level) {
        writeEnvMapAppearance(cpe, level)
        writeEnvColors(cpe, level)
        writeEnvWeatherType(cpe, level)
    }

    private fun writeEnvMapAppearance(cpe: CompoundTag, level: Level) {
        val appearance = CompoundTag()
        appearance.put("SideBlock", ByteTag((level.sideBlock and 0xFF).toByte()))
        appearance.put("EdgeBlock", ByteTag((level.edgeBlock and 0xFF).toByte()))
        appearance.put("SideLevel", ShortTag(level.edgeHeight.toShort()))
        appearance.put("CloudsHeight", IntTag(level.cloudsHeight))
        appearance.put("MaxFog", IntTag(level.maxFogDistance))
        appearance.put("CloudsSpeed", IntTag(level.cloudsSpeed))
        appearance.put("WeatherSpeed", IntTag(level.weatherSpeed))
        appearance.put("WeatherFade", IntTag(level.weatherFade))
        appearance.put("ExpFog", ByteTag(if (level.exponentialFog) 1 else 0))
        appearance.put("SidesOffset", IntTag(level.sidesOffset))
        if (level.texturePackUrl.isNotEmpty()) {
            appearance.put("TextureURL", StringTag(level.texturePackUrl))
        }
        cpe.put("EnvMapAppearance", appearance)
    }

    private fun writeEnvColors(cpe: CompoundTag, level: Level) {
        val colors = CompoundTag()
        writeColor(colors, "Sky", level.skyColor)
        writeColor(colors, "Cloud", level.cloudColor)
        writeColor(colors, "Fog", level.fogColor)
        writeColor(colors, "Ambient", level.ambientLightColor)
        writeColor(colors, "Sunlight", level.diffuseLightColor)
        writeColor(colors, "Skybox", level.skyboxColor)
        cpe.put("EnvColors", colors)
    }

    private fun writeColor(parent: CompoundTag, name: String, color: Color?) {
        if (color == null) return
        val colorTag = CompoundTag()
        colorTag.put("R", ShortTag(color.red))
        colorTag.put("G", ShortTag(color.green))
        colorTag.put("B", ShortTag(color.blue))
        parent.put(name, colorTag)
    }

    private fun writeEnvWeatherType(cpe: CompoundTag, level: Level) {
        val weather = CompoundTag()
        weather.put("WeatherType", ByteTag(level.weatherType))
        cpe.put("EnvWeatherType", weather)
    }

    private fun writeLightingMetadata(cpe: CompoundTag, level: Level) {
        val lighting = CompoundTag()
        lighting.put("LightingMode", ByteTag(level.lightingMode.id))
        lighting.put("LightingModeLocked", ByteTag(if (level.lightingModeLocked) 1 else 0))
        cpe.put("EnvMapAspect", lighting)
    }

    private fun generateUUID(levelId: String): ByteArray {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(levelId.toByteArray())
    }
}

class ClassicWorldLevelDeserializer : LevelDeserializer {
    override fun deserialize(file: File): Level? {
        if (!file.exists() || !file.isFile) return null

        return try {
            DataInputStream(GZIPInputStream(file.inputStream())).use { dataInput ->
                val root = NBTIO.readTag(
                    dataInput,
                    TagLimiter.noop(),
                    true,
                    CompoundTag::class.java
                )
                readLevel(root, file)
            }
        } catch (ex: Exception) {
            Console.errLog("Error deserializing level ${file.nameWithoutExtension}: ${ex.message}")
            null
        }
    }

    private fun readLevel(root: CompoundTag, file: File): Level {
        validateFormat(root)

        val info = readInfo(root, file.nameWithoutExtension)
        val spawn = readSpawn(root)
        val (blockData, blockData2) = readBlockData(root)

        val level = Level(
            id = info.name,
            author = info.author,
            description = "",
            size = SVec(info.width, info.height, info.length),
            blockData = blockData,
            blockData2 = blockData2,
            spawn = spawn,
            extraData = "",
            timeCreated = info.timeCreated,
            targetFormat = "cw"
        )

        if (root.contains("Metadata")) {
            readMetadata(root.get("Metadata") as CompoundTag, level)
        }

        return level
    }

    private fun validateFormat(root: CompoundTag) {
        val version = (root.get("FormatVersion") as? ByteTag)?.value ?: 1.toByte()
        if (version > 1) {
            throw IllegalArgumentException("Unsupported ClassicWorld format version: $version")
        }
    }

    private data class LevelInfo(
        val name: String,
        val author: String,
        val width: Short,
        val height: Short,
        val length: Short,
        val timeCreated: Long
    )

    private fun readInfo(root: CompoundTag, defaultName: String): LevelInfo {
        val name = (root.get("Name") as? StringTag)?.value ?: defaultName
        val width = (root.get("X") as ShortTag).value
        val height = (root.get("Y") as ShortTag).value
        val length = (root.get("Z") as ShortTag).value

        val author = if (root.contains("CreatedBy")) {
            val createdBy = root.get("CreatedBy") as CompoundTag
            (createdBy.get("Username") as StringTag).value
        } else "Unknown"

        val timeCreated = if (root.contains("TimeCreated")) {
            (root.get("TimeCreated") as LongTag).value
        } else System.currentTimeMillis()

        return LevelInfo(name, author, width, height, length, timeCreated)
    }

    private fun readSpawn(root: CompoundTag): Position {
        if (!root.contains("Spawn")) {
            return Position(0f, 0f, 0f, 0f, 0f)
        }

        val spawn = root.get("Spawn") as CompoundTag
        val x = (spawn.get("X") as ShortTag).value
        val y = (spawn.get("Y") as ShortTag).value
        val z = (spawn.get("Z") as ShortTag).value
        val yaw = (spawn.get("H") as ByteTag).value
        val pitch = (spawn.get("P") as ByteTag).value

        return Position(x.toFloat(), y.toFloat(), z.toFloat(), yaw.toFloat(), pitch.toFloat())
    }

    private fun readBlockData(root: CompoundTag): Pair<ByteArray, ByteArray?> {
        val blockArray = (root.get("BlockArray") as ByteArrayTag).value
        val blockArray2 = if (root.contains("BlockArray2")) {
            (root.get("BlockArray2") as ByteArrayTag).value
        } else null

        return Pair(blockArray, blockArray2)
    }

    private fun readMetadata(metadata: CompoundTag, level: Level) {
        if (!metadata.contains("CPE")) return
        val cpe = metadata.get("CPE") as CompoundTag

        readEnvironmentMetadata(cpe, level)
        readLightingMetadata(cpe, level)
        readAndRegisterBlockDefinitions(cpe, level)
    }

    private fun readEnvironmentMetadata(cpe: CompoundTag, level: Level) {
        readEnvMapAppearance(cpe, level)
        readEnvColors(cpe, level)
        readEnvWeatherType(cpe, level)
    }

    private fun readEnvMapAppearance(cpe: CompoundTag, level: Level) {
        if (!cpe.contains("EnvMapAppearance")) return
        val appearance = cpe.get("EnvMapAppearance") as CompoundTag

        level.sideBlock = (appearance.get("SideBlock") as ByteTag).value.toShort()
        level.edgeBlock = (appearance.get("EdgeBlock") as ByteTag).value.toShort()
        level.edgeHeight = (appearance.get("SideLevel") as ShortTag).value.toInt()

        appearance.get("CloudsHeight")?.let { level.cloudsHeight = (it as IntTag).value }
        appearance.get("MaxFog")?.let { level.maxFogDistance = (it as IntTag).value }
        appearance.get("CloudsSpeed")?.let { level.cloudsSpeed = (it as IntTag).value }
        appearance.get("WeatherSpeed")?.let { level.weatherSpeed = (it as IntTag).value }
        appearance.get("WeatherFade")?.let { level.weatherFade = (it as IntTag).value }
        appearance.get("ExpFog")?.let { level.exponentialFog = (it as ByteTag).value != 0.toByte() }
        appearance.get("SidesOffset")?.let { level.sidesOffset = (it as IntTag).value }
        appearance.get("TextureURL")?.let { level.texturePackUrl = (it as StringTag).value }
    }

    private fun readEnvColors(cpe: CompoundTag, level: Level) {
        if (!cpe.contains("EnvColors")) return
        val colors = cpe.get("EnvColors") as CompoundTag

        level.skyColor = readColor(colors, "Sky")
        level.cloudColor = readColor(colors, "Cloud")
        level.fogColor = readColor(colors, "Fog")
        level.ambientLightColor = readColor(colors, "Ambient")
        level.diffuseLightColor = readColor(colors, "Sunlight")
        level.skyboxColor = readColor(colors, "Skybox")
    }

    private fun readColor(parent: CompoundTag, name: String): Color? {
        if (!parent.contains(name)) return null
        val colorTag = parent.get(name) as CompoundTag
        val r = (colorTag.get("R") as ShortTag).value
        val g = (colorTag.get("G") as ShortTag).value
        val b = (colorTag.get("B") as ShortTag).value
        return Color(r, g, b)
    }

    private fun readEnvWeatherType(cpe: CompoundTag, level: Level) {
        if (!cpe.contains("EnvWeatherType")) return
        val weather = cpe.get("EnvWeatherType") as CompoundTag
        level.weatherType = (weather.get("WeatherType") as ByteTag).value
    }

    private fun readLightingMetadata(cpe: CompoundTag, level: Level) {
        if (!cpe.contains("EnvMapAspect")) return
        val aspect = cpe.get("EnvMapAspect") as CompoundTag

        aspect.get("LightingMode")?.let {
            val lightingModeId = (it as ByteTag).value
            level.lightingMode = LightingMode.fromId(lightingModeId) ?: LightingMode.CLIENT_LOCAL
        }
        aspect.get("LightingModeLocked")?.let {
            level.lightingModeLocked = (it as ByteTag).value != 0.toByte()
        }
    }

    private fun readAndRegisterBlockDefinitions(cpe: CompoundTag, level: Level) {
        if (!cpe.contains("BlockDefinitions")) return
        val blockDefs = cpe.get("BlockDefinitions") as CompoundTag

        for ((_, tag) in blockDefs) {
            if (tag !is CompoundTag) continue

            try {
                val customBlock = parseBlockDefinition(tag)
                BlockRegistry.register(level.id, customBlock)
            } catch (e: Exception) {
                Console.errLog("Failed to parse block definition: ${e.message}")
            }
        }

        BlockRegistry.saveLevelBlockDefinitions(level.id)
    }

    private fun parseBlockDefinition(blockDef: CompoundTag): Block {
        val id = if (blockDef.contains("ID2")) {
            (blockDef.get("ID2") as ShortTag).value.toUShort()
        } else {
            ((blockDef.get("ID") as ByteTag).value.toInt() and 0xFF).toUShort()
        }

        val name = (blockDef.get("Name") as StringTag).value
        val collideType = (blockDef.get("CollideType") as ByteTag).value
        val speed = (blockDef.get("Speed") as ByteTag).value
        val transmitsLight = (blockDef.get("TransmitsLight") as ByteTag).value == 0.toByte()
        val walkSound = (blockDef.get("WalkSound") as ByteTag).value
        val fullBright = (blockDef.get("FullBright") as ByteTag).value != 0.toByte()
        val shape = (blockDef.get("Shape") as ByteTag).value
        val blockDraw = (blockDef.get("BlockDraw") as ByteTag).value

        val fog = (blockDef.get("Fog") as ByteArrayTag).value
        val textures = (blockDef.get("Textures") as ByteArrayTag).value
        val coords = (blockDef.get("Coords") as ByteArrayTag).value

        return createCustomBlock(
            id, name, collideType, speed, transmitsLight, walkSound, fullBright,
            shape, blockDraw, fog[0], fog[1], fog[2], fog[3], textures, coords
        )
    }

    private fun createCustomBlock(
        id: UShort, name: String, collideType: Byte, speed: Byte, transmitsLight: Boolean,
        walkSound: Byte, fullBright: Boolean, shape: Byte, blockDraw: Byte,
        fogDensity: Byte, fogR: Byte, fogG: Byte, fogB: Byte,
        textures: ByteArray, coords: ByteArray
    ): Block {
        return object : Block() {
            override val id = id
            override val name = name
            override val solidity = getSolidity(collideType)
            override val movementSpeed = speed
            override val transmitsLight = transmitsLight
            override val walkSound = getWalkSound(walkSound)
            override val fullBright = fullBright
            override val shape = shape
            override val blockDraw = getBlockDraw(blockDraw)
            override val fogDensity = fogDensity
            override val fogR = fogR
            override val fogG = fogG
            override val fogB = fogB
            override val extendedBlock = textures.size > 6

            override val topTextureId = if (extendedBlock) {
                ((textures[6].toInt() and 0xFF) shl 8) or (textures[0].toInt() and 0xFF)
            } else {
                (textures[0].toInt() and 0xFF)
            }.toUShort()

            override val bottomTextureId = if (extendedBlock) {
                ((textures[7].toInt() and 0xFF) shl 8) or (textures[1].toInt() and 0xFF)
            } else {
                (textures[1].toInt() and 0xFF)
            }.toUShort()

            override val sideTextureId = if (extendedBlock) {
                ((textures[8].toInt() and 0xFF) shl 8) or (textures[2].toInt() and 0xFF)
            } else {
                (textures[2].toInt() and 0xFF)
            }.toUShort()

            override val leftTextureId = if (extendedBlock) {
                (((textures[8].toInt() and 0xFF) shl 8) or (textures[2].toInt() and 0xFF)).toUShort()
            } else sideTextureId

            override val rightTextureId = if (extendedBlock) {
                (((textures[9].toInt() and 0xFF) shl 8) or (textures[3].toInt() and 0xFF)).toUShort()
            } else sideTextureId

            override val frontTextureId = if (extendedBlock) {
                (((textures[10].toInt() and 0xFF) shl 8) or (textures[4].toInt() and 0xFF)).toUShort()
            } else sideTextureId

            override val backTextureId = if (extendedBlock) {
                (((textures[11].toInt() and 0xFF) shl 8) or (textures[5].toInt() and 0xFF)).toUShort()
            } else sideTextureId

            override val minWidth = coords[0]
            override val minDepth = coords[1]
            override val minHeight = coords[2]
            override val maxWidth = coords[3]
            override val maxDepth = coords[4]
            override val maxHeight = coords[5]
        }
    }

    private fun getSolidity(byte: Byte): BlockSolidity = when (byte) {
        0.toByte() -> BlockSolidity.WALK_THROUGH
        1.toByte() -> BlockSolidity.SWIM_THROUGH
        2.toByte() -> BlockSolidity.SOLID
        3.toByte() -> BlockSolidity.WATER
        4.toByte() -> BlockSolidity.LAVA
        5.toByte() -> BlockSolidity.PARTIALLY_SLIPPERY
        6.toByte() -> BlockSolidity.FULLY_SLIPPERY
        else -> BlockSolidity.SOLID
    }

    private fun getWalkSound(byte: Byte): WalkSound = when (byte) {
        0.toByte() -> WalkSound.NONE
        1.toByte() -> WalkSound.WOOD
        2.toByte() -> WalkSound.GRAVEL
        3.toByte() -> WalkSound.GRASS
        4.toByte() -> WalkSound.STONE
        5.toByte() -> WalkSound.METAL
        6.toByte() -> WalkSound.GLASS
        7.toByte() -> WalkSound.WOOL
        8.toByte() -> WalkSound.SAND
        9.toByte() -> WalkSound.SNOW
        else -> WalkSound.STONE
    }

    private fun getBlockDraw(byte: Byte): BlockDraw = when (byte) {
        0.toByte() -> BlockDraw.OPAQUE
        1.toByte() -> BlockDraw.TRANSPARENT
        2.toByte() -> BlockDraw.TRANSPARENT_NO_CULLING
        3.toByte() -> BlockDraw.TRANSLUCENT
        4.toByte() -> BlockDraw.GAS
        else -> BlockDraw.OPAQUE
    }
}