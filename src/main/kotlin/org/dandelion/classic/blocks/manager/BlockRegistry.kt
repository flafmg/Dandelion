package org.dandelion.classic.blocks.manager

import org.dandelion.classic.blocks.*
import org.dandelion.classic.blocks.model.Block
import org.dandelion.classic.server.Console

/**
 * Central registry for managing block definitions in the Minecraft Classic server.
 * Handles both vanilla blocks and custom block definitions.
 */
object BlockRegistry {
    private val blocks = mutableMapOf<Byte, Block>()

    /**
     * Initializes the registry with all standard Minecraft blocks.
     */
    fun init() {
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
    }

    private fun internalRegister(block: Block) {
        blocks[block.id] = block
    }
    /**
     * Registers a block in the registry.
     *
     * @param block The block instance to register
     */
    fun register(block: Block) {
        if (block.id == 0x00.toByte()) {
            Console.errLog("Cannot replace reserved id 0 (AIR)")
            return
        }
        if(blocks.containsKey(block.id)){
            Console.warnLog("Block ${block.id} ${block.name.uppercase()} replaces ${blocks[block.id]!!.name.uppercase()}")
        }

        blocks[block.id] = block
    }

    /**
     * Removes a block from the registry by its ID.
     *
     * @param id The ID of the block to remove
     */
    fun unregister(id: Byte): Boolean {
        if (id == 0x00.toByte()) {
            Console.errLog("Cannot remove reserved id 0 (AIR)")
            return false
        }
        return blocks.remove(id) != null
    }

    /**
     * Removes a block from the registry by its name.
     *
     * @param name The name of the block to remove (case-insensitive)
     * @return The removed block, or null if not found
     */
    fun unregister(name: String): Block? {
        val block = blocks.values.find { it.name.equals(name, ignoreCase = true) }
        if (block?.id == 0x00.toByte()) {
            Console.errLog("Cannot remove reserved id 0 (AIR)")
            return null
        }
        return block?.let { blocks.remove(it.id) }
    }

    /**
     * Retrieves a block by its ID.
     *
     * @param id The ID of the block to retrieve
     * @return The block instance, or null if not found
     */
    fun get(id: Byte): Block? = blocks[id]

    /**
     * Retrieves a block by its name.
     *
     * @param name The name of the block to retrieve
     * @return The block instance, or null if not found
     */
    fun get(name: String): Block? = blocks.values.find { it.name.equals(name, ignoreCase = true) }

    /**
     * Checks if a block with the given ID is registered.
     *
     * @param id The ID to check
     * @return true if the block exists, false otherwise
     */
    fun has(id: Byte): Boolean = blocks.containsKey(id)

    /**
     * Checks if a block with the given name is registered.
     *
     * @param name The name to check
     * @return true if the block exists, false otherwise
     */
    fun has(name: String): Boolean = blocks.values.any { it.name.equals(name, ignoreCase = true) }

    /**
     * Gets all registered blocks.
     *
     * @return Collection of all registered blocks
     */
    fun getAll(): Collection<Block> = blocks.values

    /**
     * Gets all registered block IDs.
     *
     * @return Set of all registered block IDs
     */
    fun getAllIds(): Set<Byte> = blocks.keys

    /**
     * Gets all registered block names.
     *
     * @return Set of all registered block names
     */
    fun getAllNames(): Set<String> = blocks.values.mapTo(mutableSetOf()) { it.name }

    /**
     * Gets the total number of registered blocks.
     *
     * @return Number of registered blocks
     */
    fun size(): Int = blocks.size

    /**
     * Clears all registered blocks.
     * Warning: This will remove all blocks including vanilla ones (except AIR).
     */
    fun clear() {
        blocks.keys
            .filter { it != 0x00.toByte() }
            .forEach { blocks.remove(it) }
    }

}