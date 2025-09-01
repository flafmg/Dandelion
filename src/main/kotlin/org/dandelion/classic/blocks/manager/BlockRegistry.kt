package org.dandelion.classic.blocks.manager

import java.io.File
import kotlin.math.pow
import org.dandelion.classic.blocks.*
import org.dandelion.classic.blocks.model.Block
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.level.Level
import org.dandelion.classic.network.packets.cpe.server.ServerDefineBlock
import org.dandelion.classic.network.packets.cpe.server.ServerDefineBlockExt
import org.dandelion.classic.network.packets.cpe.server.ServerSetBlockPermission
import org.dandelion.classic.network.packets.cpe.server.ServerSetInventoryOrder
import org.dandelion.classic.server.Console
import org.dandelion.classic.util.JsonConfig

object BlockRegistry {
    private val globalBlocks = mutableMapOf<UShort, Block>()
    private val levelBlocks = mutableMapOf<String, MutableMap<UShort, Block>>()

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

    fun register(block: Block) {
        if (block.id == 0.toUShort()) {
            Console.errLog("Cannot replace reserved id 0 (AIR)")
            return
        }
        if (block.id >= 768u) {
            Console.errLog(
                "Maximum block id is 767, you're trying to use ${block.id}"
            )
            return
        }

        globalBlocks[block.id] = block
    }

    fun register(levelId: String, block: Block) {
        if (block.id == 0x00.toUShort()) {
            Console.errLog("Cannot replace reserved id 0 (AIR)")
            return
        }
        if (block.id >= 768u) {
            Console.errLog(
                "Maximum block id is 767, you're trying to use ${block.id}"
            )
            return
        }

        val levelBlocksMap = levelBlocks.getOrPut(levelId) { mutableMapOf() }
        levelBlocksMap[block.id] = block
    }

    fun register(level: Level, block: Block) {
        register(level.id, block)
    }

    fun unregister(id: UShort): Boolean {
        if (id == 0x00.toUShort()) {
            Console.errLog("Cannot remove reserved id 0 (AIR)")
            return false
        }
        if (id >= 768u) {
            Console.errLog("Maximum block id is 767, you're trying to use $id")
            return false
        }
        return globalBlocks.remove(id) != null
    }

    fun unregister(name: String): Block? {
        val block =
            globalBlocks.values.find { it.name.equals(name, ignoreCase = true) }
        if (block?.id == 0x00.toUShort()) {
            Console.errLog("Cannot remove reserved id 0 (AIR)")
            return null
        }
        return block?.let { globalBlocks.remove(it.id) }
    }

    fun unregister(levelId: String, blockId: UShort): Boolean {
        if (blockId == 0x00.toUShort()) {
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

    fun unregister(level: Level, blockId: UShort): Boolean {
        return unregister(level.id, blockId)
    }

    fun get(id: UShort): Block? = globalBlocks[id]

    fun get(levelId: String, id: UShort): Block? {
        return levelBlocks[levelId]?.get(id) ?: globalBlocks[id]
    }

    fun get(level: Level, id: UShort): Block? {
        return get(level.id, id)
    }

    fun get(name: String): Block? =
        globalBlocks.values.find { it.name.equals(name, ignoreCase = true) }

    fun has(id: UShort): Boolean = globalBlocks.containsKey(id)

    fun has(levelId: String, id: UShort): Boolean {
        return levelBlocks[levelId]?.containsKey(id) == true ||
            globalBlocks.containsKey(id)
    }

    fun has(level: Level, id: UShort): Boolean {
        return has(level.id, id)
    }

    fun has(name: String): Boolean =
        globalBlocks.values.any { it.name.equals(name, ignoreCase = true) }

    fun getAll(): Collection<Block> = globalBlocks.values

    fun getAll(levelId: String): Collection<Block> {
        val levelBlocksMap = levelBlocks[levelId] ?: emptyMap()
        val combinedMap = globalBlocks + levelBlocksMap
        return combinedMap.values
    }

    fun getAll(level: Level): Collection<Block> {
        return getAll(level.id)
    }

    fun getAllIds(): Set<UShort> = globalBlocks.keys

    fun getAllNames(): Set<String> =
        globalBlocks.values.mapTo(mutableSetOf()) { it.name }

    fun size(): Int = globalBlocks.size

    fun sizeForLevel(levelId: String): Int {
        return getAll(levelId).size
    }

    fun sizeForLevel(level: Level): Int {
        return sizeForLevel(level.id)
    }

    fun clear() {
        globalBlocks.keys
            .filter { it != 0x00.toUShort() }
            .forEach { globalBlocks.remove(it) }
    }

    fun clearForLevel(levelId: String) {
        levelBlocks.remove(levelId)
    }

    fun clearForLevel(level: Level) {
        clearForLevel(level.id)
    }

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

    fun saveGlobalBlockDefinitions() {
        saveBlockDefinitions("global")
    }

    fun saveLevelBlockDefinitions(levelId: String) {
        saveBlockDefinitions(levelId)
    }

    fun saveLevelBlockDefinitions(level: Level) {
        saveBlockDefinitions(level.id)
    }

    fun sendBlockDefinitions(player: Player) {
        val level = player.level ?: return

        if (!player.supports("BlockDefinitions")) {
            return
        }

        val supportsExtendedTextures = player.supports("ExtendedTextures")
        val supportsExtendedBlocks = player.supports("ExtendedBlocks")
        val blocksForLevel = getAll(level)
        val customBlocks = blocksForLevel.filter { !it.isDefault }

        if (customBlocks.isEmpty()) {
            return
        }

        val validCustomBlocks =
            if (supportsExtendedBlocks) {
                customBlocks
            } else {
                customBlocks.filter { block ->
                    val unsignedId = block.id.toInt() and 0xFFFF
                    unsignedId in 1..255
                }
            }

        if (validCustomBlocks.isEmpty()) {
            return
        }

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
                        packet.send(player)
                    }
                }
                sendBlockPermission(player, block, level)
            } catch (e: Exception) {
                Console.errLog(
                    "Failed to send block definition for ${block.name} (ID: ${block.id}): ${e.message}"
                )
            }
        }

        if (player.supports("InventoryOrder")) {
            sendLevelInventoryOrder(player)
        }
    }

    private fun sendBlockPermission(
        player: Player,
        block: Block,
        level: Level,
    ) {
        val canPlace =
            player.hasPermission(
                "dandelion.blocks.${level.id}.${block.id}.place",
                true,
            )
        val canBreak =
            player.hasPermission(
                "dandelion.blocks.${level.id}.${block.id}.break",
                true,
            )
        ServerSetBlockPermission(block.id, canPlace, canBreak).send(player)
    }

    private fun hasCustomSlot(block: Block): Boolean =
        block.slot != UShort.MAX_VALUE

    private fun sendLevelInventoryOrder(player: Player) {
        val level = player.level ?: return
        val allBlocks = getAll(level)
        val maxRawBlock = if (player.supports("ExtendedBlocks")) 767 else 255
        val count = maxRawBlock + 1
        val orderToBlocks = IntArray(768) { -1 }
        val blockToOrders = IntArray(768) { -1 }

        val defs = arrayOfNulls<Block>(768)
        for (block in allBlocks) {
            val blockId = block.id.toInt()
            if (blockId < 768) {
                defs[blockId] = block
            }
        }

        for (i in defs.indices) {
            val def = defs[i] ?: continue
            if (def.id.toInt() > maxRawBlock || !hasCustomSlot(def)) continue

            val inventoryOrder = def.slot.toInt()
            if (inventoryOrder > maxRawBlock || inventoryOrder < 0) continue

            if (inventoryOrder != 0 && orderToBlocks[inventoryOrder] == -1) {
                orderToBlocks[inventoryOrder] = def.id.toInt()
            }
            blockToOrders[def.id.toInt()] = inventoryOrder
        }

        for (i in 0 until count) {
            val def = if (i < defs.size) defs[i] else null
            val raw = def?.id?.toInt() ?: i
            if (raw > maxRawBlock || (def == null && raw >= 66)) continue

            if (def != null && hasCustomSlot(def)) continue
            if (orderToBlocks[raw] == -1) {
                orderToBlocks[raw] = raw
                blockToOrders[raw] = raw
            }
        }

        for (i in (count - 1) downTo 0) {
            val def = if (i < defs.size) defs[i] else null
            val raw = def?.id?.toInt() ?: i
            if (raw > maxRawBlock || (def == null && raw >= 66)) continue

            if (blockToOrders[raw] != -1) continue
            for (slot in (count - 1) downTo 1) {
                if (orderToBlocks[slot] != -1) continue

                blockToOrders[raw] = slot
                orderToBlocks[slot] = raw
                break
            }
        }

        for (raw in 0 until count) {
            var order = blockToOrders[raw]
            if (order == -1) order = 0

            val def = if (raw < defs.size) defs[raw] else null
            if (def == null && raw >= 66) continue
            if (raw == 255 && (def == null || !hasCustomSlot(def))) continue

            ServerSetInventoryOrder(raw.toUShort(), order.toUShort())
                .send(player)
        }
    }
}
