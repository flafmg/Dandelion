package org.dandelion.classic.server.data.level.io.impl

import org.dandelion.classic.server.data.level.io.model.LevelSerializer
import org.dandelion.classic.server.data.level.io.model.LevelDeserializer
import org.dandelion.classic.server.data.level.model.Level
import org.yaml.snakeyaml.Yaml
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.Base64

class DandelionLevelSerializer : LevelSerializer {
    override fun serialize(level: Level): ByteArray {
        val yaml = Yaml()
        val out = ByteArrayOutputStream()
        val blocksCompressed = ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { gzip ->
                gzip.write(level.blocks)
            }
            baos.toByteArray()
        }
        val blocksBase64 = Base64.getEncoder().encodeToString(blocksCompressed)
        val map = mapOf(
            "id" to level.id,
            "sizeX" to level.sizeX,
            "sizeY" to level.sizeY,
            "sizeZ" to level.sizeZ,
            "spawnX" to level.spawnX,
            "spawnY" to level.spawnY,
            "spawnZ" to level.spawnZ,
            "seed" to level.seed,
            "blocks" to blocksBase64,
            "autoSaveInterval" to level.autoSaveInterval
        )
        yaml.dump(map, out.writer())
        return out.toByteArray()
    }
}

class DandelionLevelDeserializer : LevelDeserializer {
    override fun deserialize(data: ByteArray, id: String): Level {
        val yaml = Yaml()
        val map = yaml.load<Map<String, Any>>(data.inputStream().reader())
        val levelId = (map["id"] as? String) ?: id
        val sizeX = (map["sizeX"] as Int).toShort()
        val sizeY = (map["sizeY"] as Int).toShort()
        val sizeZ = (map["sizeZ"] as Int).toShort()
        val spawnX = (map["spawnX"] as Double).toFloat()
        val spawnY = (map["spawnY"] as Double).toFloat()
        val spawnZ = (map["spawnZ"] as Double).toFloat()
        val seed = (map["seed"] as Number).toLong()
        val blocksBase64 = when (val blocksField = map["blocks"]) {
            is String -> blocksField
            is ByteArray -> Base64.getEncoder().encodeToString(blocksField)
            else -> throw IllegalArgumentException("Invalid blocks field in YAML: $blocksField")
        }
        val blocksCompressed = Base64.getDecoder().decode(blocksBase64)
        val blocks = GZIPInputStream(ByteArrayInputStream(blocksCompressed)).readBytes()
        return Level(
            id = levelId,
            sizeX = sizeX,
            sizeY = sizeY,
            sizeZ = sizeZ,
            spawnX = spawnX,
            spawnY = spawnY,
            spawnZ = spawnZ,
            seed = seed,
            blocks = blocks,
            autoSaveInterval = (map["autoSaveInterval"] as? Int) ?: 90
        )
    }
}

