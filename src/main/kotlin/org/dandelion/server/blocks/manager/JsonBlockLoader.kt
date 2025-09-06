package org.dandelion.server.blocks.manager

import java.io.File
import kotlin.collections.set
import org.dandelion.server.blocks.model.JsonBlock
import org.dandelion.server.server.Console
import org.dandelion.server.util.JsonConfig

internal object JsonBlockLoader {
    private const val BLOCK_DEFS_DIRECTORY = "blockDefs"
    private const val GLOBAL_BLOCKS_FILE = "global.json"

    fun loadAllBlockDefinitions() {
        val blockDefsDir = File(BLOCK_DEFS_DIRECTORY)
        if (!blockDefsDir.exists()) {
            Console.debugLog(
                "BlockDefs directory not found, creating: $BLOCK_DEFS_DIRECTORY"
            )
            blockDefsDir.mkdirs()
            return
        }
        if (!blockDefsDir.isDirectory) {
            Console.warnLog(
                "BlockDefs path exists but is not a directory: $BLOCK_DEFS_DIRECTORY"
            )
            return
        }

        var loadedCount = 0
        loadedCount += loadGlobalBlocks(blockDefsDir)
        loadedCount += loadLevelBlocks(blockDefsDir)

        if (loadedCount > 0) {
            Console.log(
                "Loaded $loadedCount custom block definitions from JSON files"
            )
        }
    }

    private fun loadGlobalBlocks(blockDefsDir: File): Int {
        val globalFile = File(blockDefsDir, GLOBAL_BLOCKS_FILE)
        return if (globalFile.exists()) {
            loadBlocksFromFile(globalFile, null)
        } else {
            Console.debugLog("Global blocks file not found: ${globalFile.path}")
            0
        }
    }

    private fun loadLevelBlocks(blockDefsDir: File): Int {
        val levelFiles =
            blockDefsDir.listFiles { file ->
                file.isFile &&
                    file.name.endsWith(".json") &&
                    file.name != GLOBAL_BLOCKS_FILE
            } ?: return 0

        var totalLoaded = 0

        levelFiles.forEach { file ->
            val levelId = file.nameWithoutExtension
            val loaded = loadBlocksFromFile(file, levelId)
            if (loaded > 0) {
                Console.debugLog("Loaded $loaded blocks for level '$levelId'")
            }
            totalLoaded += loaded
        }

        return totalLoaded
    }

    private fun loadBlocksFromFile(file: File, levelId: String?): Int {
        return try {
            val blockConfigs = JsonConfig.loadArray(file)
            var loadedCount = 0

            blockConfigs.forEach { config ->
                try {
                    val blockData = mutableMapOf<String, Any?>()
                    extractBlockData(config, blockData)
                    val jsonBlock = JsonBlock.fromJson(blockData)
                    if (jsonBlock == null) {
                        return@forEach
                    }
                    if (levelId != null) {
                        BlockRegistry.register(levelId, jsonBlock)
                        Console.debugLog(
                            "Registered block '${jsonBlock.name}' (ID: ${jsonBlock.id}) for level '$levelId'"
                        )
                    } else {
                        BlockRegistry.register(jsonBlock)
                        Console.debugLog(
                            "Registered global block '${jsonBlock.name}' (ID: ${jsonBlock.id})"
                        )
                    }

                    loadedCount++
                } catch (e: Exception) {
                    Console.warnLog(
                        "Failed to load block from ${file.name}: ${e.message}"
                    )
                }
            }

            loadedCount
        } catch (e: Exception) {
            Console.warnLog(
                "Failed to load blocks from file ${file.path}: ${e.message}"
            )
            0
        }
    }

    private fun extractBlockData(
        config: JsonConfig,
        blockData: MutableMap<String, Any?>,
    ) {
        config.getInt("BlockID")?.let { id -> blockData["BlockID"] = id }
        config.getString("Name")?.let { blockData["Name"] = it }

        val rawSpeed = config.getDouble("Speed", 1.0)
        val normalizedSpeed =
            when {
                rawSpeed <= 0.0 -> 0
                rawSpeed == 1.0 -> 128
                else -> {
                    val speedRatio = rawSpeed.coerceIn(0.25, 3.96)
                    val cpeSpeed =
                        (128 + 64 * kotlin.math.log2(speedRatio))
                            .toInt()
                            .coerceIn(0, 255)
                    cpeSpeed
                }
            }
        blockData["Speed"] = normalizedSpeed

        blockData["CollideType"] = config.getInt("CollideType", 2)
        blockData["TopTex"] = config.getInt("TopTex", 0).coerceIn(0, 65535)
        blockData["BottomTex"] =
            config.getInt("BottomTex", 0).coerceIn(0, 65535)
        blockData["BlocksLight"] = config.getBoolean("BlocksLight", true)
        blockData["WalkSound"] = config.getInt("WalkSound", 4)
        blockData["FullBright"] = config.getBoolean("FullBright", false)
        blockData["Shape"] = config.getInt("Shape", 16)
        blockData["BlockDraw"] = config.getInt("BlockDraw", 0)
        blockData["FallBack"] =
            config.getInt("FallBack") ?: config.getInt("BlockID", 1)

        val rawFogDensity = config.getDouble("FogDensity", 0.0)
        val normalizedFogDensity =
            when {
                rawFogDensity <= 0.0 -> 0
                else -> {
                    val density = rawFogDensity.coerceIn(0.0156, 2.0)
                    val cpeFogDensity =
                        ((density * 128) - 1).toInt().coerceIn(1, 255)
                    cpeFogDensity
                }
            }
        blockData["FogDensity"] = normalizedFogDensity

        blockData["FogR"] = config.getInt("FogR", 0).coerceIn(0, 255)
        blockData["FogG"] = config.getInt("FogG", 0).coerceIn(0, 255)
        blockData["FogB"] = config.getInt("FogB", 0).coerceIn(0, 255)

        blockData["MinX"] = config.getInt("MinX", 0).coerceIn(0, 15)
        blockData["MinY"] = config.getInt("MinY", 0).coerceIn(0, 15)
        blockData["MinZ"] = config.getInt("MinZ", 0).coerceIn(0, 15)
        blockData["MaxX"] = config.getInt("MaxX", 16).coerceIn(1, 16)
        blockData["MaxY"] = config.getInt("MaxY", 16).coerceIn(1, 16)
        blockData["MaxZ"] = config.getInt("MaxZ", 16).coerceIn(1, 16)

        val topTex = config.getInt("TopTex", 0).coerceIn(0, 65535)
        blockData["LeftTex"] =
            config.getInt("LeftTex", topTex).coerceIn(0, 65535)
        blockData["RightTex"] =
            config.getInt("RightTex", topTex).coerceIn(0, 65535)
        blockData["FrontTex"] =
            config.getInt("FrontTex", topTex).coerceIn(0, 65535)
        blockData["BackTex"] =
            config.getInt("BackTex", topTex).coerceIn(0, 65535)
        config.getInt("InventoryOrder")?.let {
            blockData["InventoryOrder"] = it
        }
    }

    fun reloadBlockDefinitions() {
        Console.log("Reloading block definitions from JSON files...")
        loadAllBlockDefinitions()
    }
}
