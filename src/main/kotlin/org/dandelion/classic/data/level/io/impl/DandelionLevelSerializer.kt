package org.dandelion.classic.data.level.io.impl

import org.dandelion.classic.data.level.io.model.LevelSerializer
import org.dandelion.classic.data.level.io.model.LevelDeserializer
import org.dandelion.classic.data.level.model.Level
import org.dandelion.classic.data.config.stream.YamlStream
import org.yaml.snakeyaml.Yaml
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.Base64

class DandelionLevelSerializer : LevelSerializer {
    override fun serialize(level: Level, path: String) {
        val blocksCompressed = ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { gzip ->
                gzip.write(level.blocks)
            }
            baos.toByteArray()
        }
        val blocksBase64 = Base64.getEncoder().encodeToString(blocksCompressed)
        val yamlStream = YamlStream(path)
        yamlStream.set("id", level.id)
        yamlStream.set("sizeX", level.sizeX)
        yamlStream.set("sizeY", level.sizeY)
        yamlStream.set("sizeZ", level.sizeZ)
        yamlStream.set("spawnX", level.spawnX)
        yamlStream.set("spawnY", level.spawnY)
        yamlStream.set("spawnZ", level.spawnZ)
        yamlStream.set("seed", level.seed)
        yamlStream.set("autoSaveInterval", level.autoSaveInterval)
        yamlStream.set("blocks", blocksBase64)
        yamlStream.save() // Salva o arquivo diretamente
    }
}

class DandelionLevelDeserializer : LevelDeserializer {
    override fun deserialize(path: String): Level {
        val yamlStream = YamlStream(path)
        val levelId = yamlStream.getString("id") ?: "none"
        val sizeX = yamlStream.getInt("sizeX")?.toShort() ?: 0
        val sizeY = yamlStream.getInt("sizeY")?.toShort() ?: 0
        val sizeZ = yamlStream.getInt("sizeZ")?.toShort() ?: 0
        val spawnX = yamlStream.get("spawnX")?.toString()?.toFloatOrNull() ?: 0f
        val spawnY = yamlStream.get("spawnY")?.toString()?.toFloatOrNull() ?: 0f
        val spawnZ = yamlStream.get("spawnZ")?.toString()?.toFloatOrNull() ?: 0f
        val seed = yamlStream.get("seed")?.toString()?.toLongOrNull() ?: 0L
        val autoSaveInterval = yamlStream.getInt("autoSaveInterval") ?: 90
        val blocksBase64 = yamlStream.getString("blocks") ?: ""
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
            autoSaveInterval = autoSaveInterval
        )
    }
}




