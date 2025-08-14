package org.dandelion.classic.blocks.manager

import java.io.File
import kotlin.math.pow
import org.dandelion.classic.blocks.*
import org.dandelion.classic.blocks.model.Block
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.level.Level
import org.dandelion.classic.network.packets.cpe.server.ServerDefineBlock
import org.dandelion.classic.network.packets.cpe.server.ServerDefineBlockExt
import org.dandelion.classic.server.Console
import org.dandelion.classic.util.JsonConfig

/**
 * Central registry for managing block definitions in the Minecraft Classic
 * server. Handles both vanilla blocks and custom block definitions with support
 * for global and per-level block registrations.
 */
object BlockRegistry {
    private val globalBlocks = mutableMapOf<Byte, Block>()
    private val levelBlocks = mutableMapOf<String, MutableMap<Byte, Block>>()

    /** Initializes the registry with all standard Minecraft blocks. */
    internal fun init() {
        // Classic blocks
        internalRegister(Air())
        internalRegister(Stone())
        internalRegister(GrassBlock())
        internalRegister(Dirt())
        internalRegister(Cobblestone())
        internalRegister(Planks())
        internalRegister(Sapling())
        internalRegister(Bedrock())
        internalRegister(FlowingWater())
        internalRegister(StationaryWater())
        internalRegister(FlowingLava())
        internalRegister(StationaryLava())
        internalRegister(Sand())
        internalRegister(Gravel())
        internalRegister(GoldOre())
        internalRegister(IronOre())
        internalRegister(CoalOre())
        internalRegister(Wood())
        internalRegister(Leaves())
        internalRegister(Sponge())
        internalRegister(Glass())
        internalRegister(RedCloth())
        internalRegister(OrangeCloth())
        internalRegister(YellowCloth())
        internalRegister(ChartreuseCloth())
        internalRegister(GreenCloth())
        internalRegister(SpringGreenCloth())
        internalRegister(CyanCloth())
        internalRegister(CapriCloth())
        internalRegister(UltramarineCloth())
        internalRegister(VioletCloth())
        internalRegister(PurpleCloth())
        internalRegister(MagentaCloth())
        internalRegister(RoseCloth())
        internalRegister(DarkGrayCloth())
        internalRegister(LightGrayCloth())
        internalRegister(WhiteCloth())
        internalRegister(Flower())
        internalRegister(Rose())
        internalRegister(BrownMushroom())
        internalRegister(RedMushroom())
        internalRegister(BlockOfGold())
        internalRegister(BlockOfIron())
        internalRegister(DoubleSlab())
        internalRegister(Slab())
        internalRegister(Bricks())
        internalRegister(TNT())
        internalRegister(Bookshelf())
        internalRegister(MossyCobblestone())
        internalRegister(Obsidian())

        // CPE blocks
        internalRegister(CobblestoneSlab())
        internalRegister(Rope())
        internalRegister(Sandstone())
        internalRegister(Snow())
        internalRegister(Fire())
        internalRegister(LightPinkWool())
        internalRegister(ForestGreenWool())
        internalRegister(BrownWool())
        internalRegister(DeepBlue())
        internalRegister(Turquoise())
        internalRegister(Ice())
        internalRegister(CeramicTile())
        internalRegister(Magma())
        internalRegister(Pillar())
        internalRegister(Crate())
        internalRegister(StoneBrick())

        JsonBlockLoader.loadAllBlockDefinitions()
    }

    private fun internalRegister(block: Block) {
        globalBlocks[block.id] = block
    }

    /**
     * Registers a block globally in the registry.
     *
     * @param block The block instance to register globally
     */
    fun register(block: Block) {
        if (block.id == 0x00.toByte()) {
            Console.errLog("Cannot replace reserved id 0 (AIR)")
            return
        }

        globalBlocks[block.id] = block
    }

    /**
     * Registers a block for a specific level by level ID.
     *
     * @param levelId The ID of the level to register the block for
     * @param block The block instance to register
     */
    fun register(levelId: String, block: Block) {
        if (block.id == 0x00.toByte()) {
            Console.errLog("Cannot replace reserved id 0 (AIR)")
            return
        }

        val levelBlocksMap = levelBlocks.getOrPut(levelId) { mutableMapOf() }
        levelBlocksMap[block.id] = block
    }

    /**
     * Registers a block for a specific level by level instance.
     *
     * @param level The level instance to register the block for
     * @param block The block instance to register
     */
    fun register(level: Level, block: Block) {
        register(level.id, block)
    }

    /**
     * Removes a global block from the registry by its ID.
     *
     * @param id The ID of the block to remove
     */
    fun unregister(id: Byte): Boolean {
        if (id == 0x00.toByte()) {
            Console.errLog("Cannot remove reserved id 0 (AIR)")
            return false
        }
        return globalBlocks.remove(id) != null
    }

    /**
     * Removes a global block from the registry by its name.
     *
     * @param name The name of the block to remove (case-insensitive)
     * @return The removed block, or null if not found
     */
    fun unregister(name: String): Block? {
        val block =
            globalBlocks.values.find { it.name.equals(name, ignoreCase = true) }
        if (block?.id == 0x00.toByte()) {
            Console.errLog("Cannot remove reserved id 0 (AIR)")
            return null
        }
        return block?.let { globalBlocks.remove(it.id) }
    }

    /**
     * Removes a block from a specific level by level ID and block ID.
     *
     * @param levelId The ID of the level to remove the block from
     * @param blockId The ID of the block to remove
     * @return true if the block was removed, false otherwise
     */
    fun unregister(levelId: String, blockId: Byte): Boolean {
        if (blockId == 0x00.toByte()) {
            Console.errLog("Cannot remove reserved id 0 (AIR)")
            return false
        }

        val levelBlocksMap = levelBlocks[levelId] ?: return false
        val removed = levelBlocksMap.remove(blockId) != null

        if (levelBlocksMap.isEmpty()) {
            levelBlocks.remove(levelId)
        }

        return removed
    }

    /**
     * Removes a block from a specific level by level instance and block ID.
     *
     * @param level The level instance to remove the block from
     * @param blockId The ID of the block to remove
     * @return true if the block was removed, false otherwise
     */
    fun unregister(level: Level, blockId: Byte): Boolean {
        return unregister(level.id, blockId)
    }

    /**
     * Retrieves a global block by its ID.
     *
     * @param id The ID of the block to retrieve
     * @return The block instance, or null if not found
     */
    fun get(id: Byte): Block? = globalBlocks[id]

    /**
     * Retrieves a block by its ID for a specific level, with level blocks
     * taking priority over global blocks.
     *
     * @param levelId The ID of the level to get the block for
     * @param id The ID of the block to retrieve
     * @return The block instance, or null if not found
     */
    fun get(levelId: String, id: Byte): Block? {
        return levelBlocks[levelId]?.get(id) ?: globalBlocks[id]
    }

    /**
     * Retrieves a block by its ID for a specific level instance, with level
     * blocks taking priority over global blocks.
     *
     * @param level The level instance to get the block for
     * @param id The ID of the block to retrieve
     * @return The block instance, or null if not found
     */
    fun get(level: Level, id: Byte): Block? {
        return get(level.id, id)
    }

    /**
     * Retrieves a global block by its name.
     *
     * @param name The name of the block to retrieve
     * @return The block instance, or null if not found
     */
    fun get(name: String): Block? =
        globalBlocks.values.find { it.name.equals(name, ignoreCase = true) }

    /**
     * Checks if a global block with the given ID is registered.
     *
     * @param id The ID to check
     * @return true if the block exists globally, false otherwise
     */
    fun has(id: Byte): Boolean = globalBlocks.containsKey(id)

    /**
     * Checks if a block with the given ID is registered for a specific level.
     *
     * @param levelId The ID of the level to check
     * @param id The ID of the block to check
     * @return true if the block exists for the level (including global blocks),
     *   false otherwise
     */
    fun has(levelId: String, id: Byte): Boolean {
        return levelBlocks[levelId]?.containsKey(id) == true ||
            globalBlocks.containsKey(id)
    }

    /**
     * Checks if a block with the given ID is registered for a specific level
     * instance.
     *
     * @param level The level instance to check
     * @param id The ID of the block to check
     * @return true if the block exists for the level (including global blocks),
     *   false otherwise
     */
    fun has(level: Level, id: Byte): Boolean {
        return has(level.id, id)
    }

    /**
     * Checks if a global block with the given name is registered.
     *
     * @param name The name to check
     * @return true if the block exists globally, false otherwise
     */
    fun has(name: String): Boolean =
        globalBlocks.values.any { it.name.equals(name, ignoreCase = true) }

    /**
     * Gets all global blocks.
     *
     * @return Collection of all global blocks
     */
    fun getAll(): Collection<Block> = globalBlocks.values

    /**
     * Gets all blocks available for a specific level, with level blocks taking
     * priority over global blocks.
     *
     * @param levelId The ID of the level to get blocks for
     * @return Collection of all blocks available for the level
     */
    fun getAll(levelId: String): Collection<Block> {
        val levelBlocksMap = levelBlocks[levelId] ?: emptyMap()
        val combinedMap = globalBlocks + levelBlocksMap
        return combinedMap.values
    }

    /**
     * Gets all blocks available for a specific level instance, with level
     * blocks taking priority over global blocks.
     *
     * @param level The level instance to get blocks for
     * @return Collection of all blocks available for the level
     */
    fun getAll(level: Level): Collection<Block> {
        return getAll(level.id)
    }

    /**
     * Gets all global block IDs.
     *
     * @return Set of all global block IDs
     */
    fun getAllIds(): Set<Byte> = globalBlocks.keys

    /**
     * Gets all global block names.
     *
     * @return Set of all global block names
     */
    fun getAllNames(): Set<String> =
        globalBlocks.values.mapTo(mutableSetOf()) { it.name }

    /**
     * Gets the total number of global blocks.
     *
     * @return Number of global blocks
     */
    fun size(): Int = globalBlocks.size

    /**
     * Gets the total number of blocks registered for a specific level
     * (including global blocks).
     *
     * @param levelId The ID of the level to count blocks for
     * @return Number of blocks available for the level
     */
    fun sizeForLevel(levelId: String): Int {
        return getAll(levelId).size
    }

    /**
     * Gets the total number of blocks registered for a specific level instance
     * (including global blocks).
     *
     * @param level The level instance to count blocks for
     * @return Number of blocks available for the level
     */
    fun sizeForLevel(level: Level): Int {
        return sizeForLevel(level.id)
    }

    /**
     * Clears all global blocks. Warning: This will remove all global blocks
     * including vanilla ones (except AIR).
     */
    fun clear() {
        globalBlocks.keys
            .filter { it != 0x00.toByte() }
            .forEach { globalBlocks.remove(it) }
    }

    /**
     * Clears all blocks for a specific level.
     *
     * @param levelId The ID of the level to clear blocks for
     */
    fun clearForLevel(levelId: String) {
        levelBlocks.remove(levelId)
    }

    /**
     * Clears all blocks for a specific level instance.
     *
     * @param level The level instance to clear blocks for
     */
    fun clearForLevel(level: Level) {
        clearForLevel(level.id)
    }

    /**
     * Saves block definitions for a specific target (global or level).
     *
     * @param target The target to save blocks for ("global" or levelId)
     */
    fun saveBlockDefinitions(target: String) {
        try {
            val blockDefsDir = File("blockDefs")
            if (!blockDefsDir.exists()) {
                blockDefsDir.mkdirs()
            }

            val fileName =
                if (target == "global") "global.json" else "$target.json"
            val file = File(blockDefsDir, fileName)

            val blocks =
                if (target == "global") {
                    getAll().filter { !it.isDefault }
                } else {
                    val levelBlocksMap = levelBlocks[target]
                    if (levelBlocksMap != null) {
                        levelBlocksMap.values.filter { !it.isDefault }
                    } else {
                        emptyList()
                    }
                }

            if (blocks.isEmpty()) {
                if (file.exists()) {
                    file.delete()
                }
                Console.debugLog(
                    "No custom blocks to save for $target, removed file if it existed"
                )
                return
            }

            val config = JsonConfig()
            blocks.forEachIndexed { index, block ->
                val blockPath = "blocks.$index"
                config.setInt("$blockPath.BlockID", block.id.toInt())
                config.setString("$blockPath.Name", block.name)
                config.setDouble(
                    "$blockPath.Speed",
                    if (block.movementSpeed == 128.toByte()) 1.0
                    else {
                        2.0.pow((block.movementSpeed.toInt() - 128) / 64.0)
                    },
                )
                config.setInt(
                    "$blockPath.CollideType",
                    block.solidity.value.toInt(),
                )
                config.setInt("$blockPath.TopTex", block.topTextureId.toInt())
                config.setInt(
                    "$blockPath.BottomTex",
                    block.bottomTextureId.toInt(),
                )
                config.setBoolean(
                    "$blockPath.BlocksLight",
                    !block.transmitsLight,
                )
                config.setInt(
                    "$blockPath.WalkSound",
                    block.walkSound.value.toInt(),
                )
                config.setBoolean("$blockPath.FullBright", block.fullBright)
                config.setInt("$blockPath.Shape", block.shape.toInt())
                config.setInt(
                    "$blockPath.BlockDraw",
                    block.blockDraw.value.toInt(),
                )
                config.setInt("$blockPath.FallBack", block.fallback.toInt())
                config.setDouble(
                    "$blockPath.FogDensity",
                    if (block.fogDensity == 0.toByte()) 0.0
                    else {
                        (block.fogDensity.toInt() + 1) / 128.0
                    },
                )
                config.setInt("$blockPath.FogR", block.fogR.toInt())
                config.setInt("$blockPath.FogG", block.fogG.toInt())
                config.setInt("$blockPath.FogB", block.fogB.toInt())
                config.setInt("$blockPath.MinX", block.minWidth.toInt())
                config.setInt("$blockPath.MinY", block.minHeight.toInt())
                config.setInt("$blockPath.MinZ", block.minDepth.toInt())
                config.setInt("$blockPath.MaxX", block.maxWidth.toInt())
                config.setInt("$blockPath.MaxY", block.maxHeight.toInt())
                config.setInt("$blockPath.MaxZ", block.maxDepth.toInt())
                config.setInt("$blockPath.LeftTex", block.leftTextureId.toInt())
                config.setInt(
                    "$blockPath.RightTex",
                    block.rightTextureId.toInt(),
                )
                config.setInt(
                    "$blockPath.FrontTex",
                    block.frontTextureId.toInt(),
                )
                config.setInt("$blockPath.BackTex", block.backTextureId.toInt())
            }

            config.save(file)
            Console.debugLog(
                "Saved ${blocks.size} block definitions to ${file.path}"
            )
        } catch (e: Exception) {
            Console.errLog(
                "Failed to save block definitions for $target: ${e.message}"
            )
        }
    }

    /** Saves global block definitions. */
    fun saveGlobalBlockDefinitions() {
        saveBlockDefinitions("global")
    }

    /**
     * Saves block definitions for a specific level.
     *
     * @param levelId The ID of the level to save blocks for
     */
    fun saveLevelBlockDefinitions(levelId: String) {
        saveBlockDefinitions(levelId)
    }

    /**
     * Saves block definitions for a specific level instance.
     *
     * @param level The level instance to save blocks for
     */
    fun saveLevelBlockDefinitions(level: Level) {
        saveBlockDefinitions(level.id)
    }

    /**
     * Sends block definitions to a player based on their current level.
     *
     * @param player The player to send block definitions to
     */
    fun sendBlockDefinitions(player: Player) {
        val level = player.level ?: return

        if (!player.supports("BlockDefinitions")) {
            Console.debugLog(
                "Player ${player.name} does not support BlockDefinitions extension"
            )
            return
        }

        val blocksForLevel = getAll(level)
        val customBlocks = blocksForLevel.filter { !it.isDefault }

        if (customBlocks.isEmpty()) {
            Console.debugLog(
                "No custom blocks to send to player ${player.name}"
            )
            return
        }

        val validCustomBlocks =
            customBlocks.filter { block ->
                val unsignedId = block.id.toInt() and 0xFF
                if (unsignedId < 1 || unsignedId > 255) {
                    false
                } else {
                    true
                }
            }

        if (validCustomBlocks.isEmpty()) {
            Console.debugLog(
                "No valid custom blocks to send to player ${player.name}"
            )
            return
        }

        Console.debugLog(
            "Sending ${validCustomBlocks.size} block definitions to player ${player.name}"
        )

        validCustomBlocks.forEach { block ->
            try {
                when {
                    block.extendedBlock &&
                        player.supports("BlockDefinitionsExt") -> {
                        val packet =
                            ServerDefineBlockExt(
                                blockId = block.id,
                                name = block.name,
                                solidity = block.solidity.value,
                                movementSpeed = block.movementSpeed,
                                topTextureId = block.topTextureId,
                                leftTextureId = block.leftTextureId,
                                rightTextureId = block.rightTextureId,
                                frontTextureId = block.frontTextureId,
                                backTextureId = block.backTextureId,
                                bottomTextureId = block.bottomTextureId,
                                transmitsLight = block.transmitsLight,
                                walkSound = block.walkSound.value,
                                fullBright = block.fullBright,
                                minX = block.minWidth,
                                minY = block.minHeight,
                                minZ = block.minDepth,
                                maxX = block.maxWidth,
                                maxY = block.maxHeight,
                                maxZ = block.maxDepth,
                                blockDraw = block.blockDraw.value,
                                fogDensity = block.fogDensity,
                                fogR = block.fogR,
                                fogG = block.fogG,
                                fogB = block.fogB,
                            )
                        Console.debugLog(
                            "Sending DefineBlockExt for ${block.name} (ID: ${block.id})"
                        )
                        packet.send(player)
                    }
                    else -> {
                        val packet =
                            ServerDefineBlock(
                                blockId = block.id,
                                name = block.name,
                                solidity = block.solidity.value,
                                movementSpeed = block.movementSpeed,
                                topTextureId = block.topTextureId,
                                sideTextureId = block.sideTextureId,
                                bottomTextureId = block.bottomTextureId,
                                transmitsLight = block.transmitsLight,
                                walkSound = block.walkSound.value,
                                fullBright = block.fullBright,
                                shape = block.shape,
                                blockDraw = block.blockDraw.value,
                                fogDensity = block.fogDensity,
                                fogR = block.fogR,
                                fogG = block.fogG,
                                fogB = block.fogB,
                            )
                        Console.debugLog(
                            "Sending DefineBlock for ${block.name} (ID: ${block.id})"
                        )
                        packet.send(player)
                    }
                }
            } catch (e: Exception) {
                Console.errLog(
                    "Failed to send block definition for ${block.name} (ID: ${block.id}): ${e.message}"
                )
            }
        }

        Console.debugLog(
            "Completed sending block definitions to player ${player.name}"
        )
    }
}
