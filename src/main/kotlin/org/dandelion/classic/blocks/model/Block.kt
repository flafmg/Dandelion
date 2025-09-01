package org.dandelion.classic.blocks.model

import kotlin.math.pow
import org.dandelion.classic.blocks.manager.BlockRegistry
import org.dandelion.classic.blocks.model.enums.BlockDraw
import org.dandelion.classic.blocks.model.enums.BlockSolidity
import org.dandelion.classic.blocks.model.enums.WalkSound

abstract class Block {
    abstract val id: UShort
    open val fallback: Byte = 0
    abstract val name: String

    internal open val isDefault: Boolean = false

    open val solidity: BlockSolidity = BlockSolidity.SOLID

    open val movementSpeed: Byte = 128.toByte()

    open val topTextureId: UShort = 0u
    open val sideTextureId: UShort = 0u
    open val bottomTextureId: UShort = 0u

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

    open val leftTextureId: UShort = 0u
    open val rightTextureId: UShort = 0u
    open val frontTextureId: UShort = 0u
    open val backTextureId: UShort = 0u

    open val minWidth: Byte = 0
    open val minHeight: Byte = 0
    open val minDepth: Byte = 0
    open val maxWidth: Byte = 16
    open val maxHeight: Byte = 16
    open val maxDepth: Byte = 16

    open val slot: UShort = UShort.MAX_VALUE

    internal fun hasCustomSlot(): Boolean = slot != UShort.MAX_VALUE

    fun getSpeedRatio(): Double {
        return 2.0.pow((movementSpeed.toInt() - 128) / 64.0)
    }

    fun getFogDensity(): Float {
        return if (fogDensity == 0.toByte()) 0f
        else (fogDensity.toInt() + 1) / 128f
    }

    fun isSprite(): Boolean = shape == 0.toByte()

    fun getHeight(): Int = if (isSprite()) 0 else shape.toInt()

    fun isSolid(): Boolean =
        solidity == BlockSolidity.SOLID ||
            solidity == BlockSolidity.PARTIALLY_SLIPPERY ||
            solidity == BlockSolidity.FULLY_SLIPPERY

    fun isLiquid(): Boolean =
        solidity == BlockSolidity.WATER || solidity == BlockSolidity.LAVA

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
            sb.append(
                "left=$leftTextureId, right=$rightTextureId, front=$frontTextureId, back=$backTextureId, "
            )
        } else {
            sb.append("side=$sideTextureId, ")
        }
        sb.append("bottom=$bottomTextureId], ")
        sb.append("transmitsLight=$transmitsLight, ")
        sb.append("walkSound=$walkSound, ")
        sb.append("fullBright=$fullBright, ")
        if (extendedBlock) {
            sb.append(
                "bounds=[min=($minWidth,$minHeight,$minDepth), max=($maxWidth,$maxHeight,$maxDepth)], "
            )
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

    companion object {
        fun get(id: UShort): Block? = BlockRegistry.get(id)

        fun get(name: String): Block? = BlockRegistry.get(name)

        fun has(id: UShort): Boolean = BlockRegistry.has(id)

        fun has(name: String): Boolean = BlockRegistry.has(name)

        fun getAll(): Collection<Block> = BlockRegistry.getAll()

        fun getAllIds(): Set<UShort> = BlockRegistry.getAllIds()

        fun getAllNames(): Set<String> = BlockRegistry.getAllNames()

        fun size(): Int = BlockRegistry.size()

        fun clear() = BlockRegistry.clear()
    }
}
