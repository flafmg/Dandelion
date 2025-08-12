package org.dandelion.classic.blocks.model

import org.dandelion.classic.blocks.manager.BlockRegistry
import org.dandelion.classic.blocks.model.enums.BlockDraw
import org.dandelion.classic.blocks.model.enums.BlockSolidity
import org.dandelion.classic.blocks.model.enums.WalkSound
import kotlin.math.pow

abstract class Block {
    abstract val id: Byte
    open val fallback: Byte = 0
    abstract val name: String

    internal open val isDefault: Boolean = false

    open val solidity: BlockSolidity = BlockSolidity.SOLID

    open val movementSpeed: Byte = 128.toByte()

    open val topTextureId: Byte = 0
    open val sideTextureId: Byte = 0
    open val bottomTextureId: Byte = 0

    open val transmitsLight: Boolean = false
    open val fullBright: Boolean = false

    open val walkSound: WalkSound = WalkSound.STONE

    open val shape: Byte = 16
    open val blockDraw: BlockDraw = BlockDraw.OPAQUE

    open val fogDensity: Byte = 0
    open val fogR: Byte = 255.toByte()
    open val fogG: Byte = 255.toByte()
    open val fogB: Byte = 255.toByte()

    open val extendedBlock: Boolean = false

    open val leftTextureId: Byte = 0
    open val rightTextureId: Byte = 0
    open val frontTextureId: Byte = 0
    open val backTextureId: Byte = 0

    open val minWidth: Byte = 0
    open val minHeight: Byte = 0
    open val minDepth: Byte = 0
    open val maxWidth: Byte = 16
    open val maxHeight: Byte = 16
    open val maxDepth: Byte = 16



    fun getSpeedRatio(): Double {
        return 2.0.pow((movementSpeed.toInt() - 128) / 64.0)
    }

    fun getFogDensity(): Float {
        return if (fogDensity == 0.toByte()) 0f else (fogDensity.toInt() + 1) / 128f
    }

    fun isSprite(): Boolean = shape == 0.toByte()

    fun getHeight(): Int = if (isSprite()) 0 else shape.toInt()

    fun isSolid(): Boolean = solidity == BlockSolidity.SOLID ||
            solidity == BlockSolidity.PARTIALLY_SLIPPERY ||
            solidity == BlockSolidity.FULLY_SLIPPERY

    fun isLiquid(): Boolean = solidity == BlockSolidity.WATER || solidity == BlockSolidity.LAVA

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Block(")
        sb.append("id=$id, ")
        sb.append("name='$name', ")
        sb.append("fallback=$fallback, ")
        sb.append("solidity=$solidity, ")
        sb.append("movementSpeed=$movementSpeed, ")
        sb.append("textures=[top=$topTextureId, ")
        if (extendedBlock) {
            sb.append("left=$leftTextureId, right=$rightTextureId, front=$frontTextureId, back=$backTextureId, ")
        } else {
            sb.append("side=$sideTextureId, ")
        }
        sb.append("bottom=$bottomTextureId], ")
        sb.append("transmitsLight=$transmitsLight, ")
        sb.append("walkSound=$walkSound, ")
        sb.append("fullBright=$fullBright, ")
        if (extendedBlock) {
            sb.append("bounds=[min=($minWidth,$minHeight,$minDepth), max=($maxWidth,$maxHeight,$maxDepth)], ")
        } else {
            sb.append("shape=$shape, ")
        }
        sb.append("blockDraw=$blockDraw")
        if (fogDensity > 0) {
            sb.append(", fog=[density=$fogDensity, rgb=($fogR,$fogG,$fogB)]")
        }
        sb.append(")")
        return sb.toString()
    }
    companion object{
        /**
         * Retrieves a block by its id.
         *
         * @param id The id of the block to retrieve.
         * @return The block instance, or null if not found.
         */
        fun get(id: Byte): Block? = BlockRegistry.get(id)

        /**
         * Retrieves a block by its name.
         *
         * @param name The name of the block to retrieve.
         * @return The block instance, or null if not found.
         */
        fun get(name: String): Block? = BlockRegistry.get(name)

        /**
         * Checks if a block with the given id is registered.
         *
         * @param id The id to check.
         * @return true if the block exists, false otherwise.
         */
        fun has(id: Byte): Boolean = BlockRegistry.has(id)

        /**
         * Checks if a block with the given name is registered.
         *
         * @param name The name to check.
         * @return true if the block exists, false otherwise.
         */
        fun has(name: String): Boolean = BlockRegistry.has(name)

        /**
         * Gets all registered blocks.
         * @return Collection of all registered blocks.
         */
        fun getAll(): Collection<Block> = BlockRegistry.getAll()

        /**
         * Gets all registered block IDs.
         *
         * @return Set of all registered block IDs.
         */
        fun getAllIds(): Set<Byte> = BlockRegistry.getAllIds()

        /**
         * Gets all registered block names.
         *
         * @return Set of all registered block names.
         */
        fun getAllNames(): Set<String> = BlockRegistry.getAllNames()

        /**
         * Gets the total number of registered blocks.
         *
         * @return Number of registered blocks.
         */
        fun size(): Int = BlockRegistry.size()

        /**
         * Clears all registered blocks.
         * Warning: This will remove all blocks including vanilla ones (except AIR).
         */
        fun clear() = BlockRegistry.clear()
    }
}
