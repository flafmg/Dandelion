package org.dandelion.classic.blocks.model

import org.dandelion.classic.blocks.model.enums.BlockDraw
import org.dandelion.classic.blocks.model.enums.BlockSolidity
import org.dandelion.classic.blocks.model.enums.WalkSound

/**
 * Dynamic block implementation loaded from JSON configuration files. This class
 * allows creating custom blocks without hardcoding them.
 */
class JsonBlock(
    override val id: UShort,
    override val name: String,
    override val fallback: Byte,
    override val solidity: BlockSolidity,
    override val movementSpeed: Byte,
    override val topTextureId: UShort,
    override val sideTextureId: UShort,
    override val bottomTextureId: UShort,
    override val transmitsLight: Boolean,
    override val walkSound: WalkSound,
    override val fullBright: Boolean,
    override val shape: Byte,
    override val blockDraw: BlockDraw,
    override val fogDensity: Byte,
    override val fogR: Byte,
    override val fogG: Byte,
    override val fogB: Byte,
    override val extendedBlock: Boolean,
    override val leftTextureId: UShort,
    override val rightTextureId: UShort,
    override val frontTextureId: UShort,
    override val backTextureId: UShort,
    override val minWidth: Byte,
    override val minHeight: Byte,
    override val minDepth: Byte,
    override val maxWidth: Byte,
    override val maxHeight: Byte,
    override val maxDepth: Byte,
    override val slot: UShort = id,
) : Block() {

    companion object {
        /**
         * Creates a JsonBlock from JSON configuration data.
         *
         * @param blockData Map containing block configuration from JSON
         * @return JsonBlock instance created from the configuration
         */
        fun fromJson(blockData: Map<String, Any?>): JsonBlock? {
            val blockId =
                (blockData["BlockID"] as? Number)?.toInt()?.toUShort()?.takeIf {
                    ((blockData["BlockID"] as? Number)?.toInt() ?: 1) <= 768
                } ?: return null
            val name = blockData["Name"] as? String ?: "Custom Block"
            val speed =
                (blockData["Speed"] as? Number)?.toByte() ?: 128.toByte()
            val collideType =
                (blockData["CollideType"] as? Number)?.toByte() ?: 2
            val topTex =
                (blockData["TopTex"] as? Number)?.toInt()?.coerceIn(0, 65535)
                    ?: 0
            val bottomTex =
                (blockData["BottomTex"] as? Number)?.toInt()?.coerceIn(0, 65535)
                    ?: 0
            val blocksLight = blockData["BlocksLight"] as? Boolean ?: false
            val walkSound = (blockData["WalkSound"] as? Number)?.toByte() ?: 4
            val fullBright = blockData["FullBright"] as? Boolean ?: false
            val shape = (blockData["Shape"] as? Number)?.toByte() ?: 16
            val blockDraw = (blockData["BlockDraw"] as? Number)?.toByte() ?: 0
            val fallBack = (blockData["FallBack"] as? Number)?.toByte() ?: 1
            val fogDensity = (blockData["FogDensity"] as? Number)?.toByte() ?: 0

            val fogR = (blockData["FogR"] as? Number)?.toByte() ?: 0
            val fogG = (blockData["FogG"] as? Number)?.toByte() ?: 0
            val fogB = (blockData["FogB"] as? Number)?.toByte() ?: 0

            val minX = (blockData["MinX"] as? Number)?.toByte() ?: 0
            val minY = (blockData["MinZ"] as? Number)?.toByte() ?: 0
            val minZ = (blockData["MinY"] as? Number)?.toByte() ?: 0
            val maxX = (blockData["MaxX"] as? Number)?.toByte() ?: 16
            val maxY = (blockData["MaxZ"] as? Number)?.toByte() ?: 16
            val maxZ = (blockData["MaxY"] as? Number)?.toByte() ?: 16

            val leftTex =
                (blockData["LeftTex"] as? Number)?.toInt()?.coerceIn(0, 65535)
                    ?: topTex
            val rightTex =
                (blockData["RightTex"] as? Number)?.toInt()?.coerceIn(0, 65535)
                    ?: topTex
            val frontTex =
                (blockData["FrontTex"] as? Number)?.toInt()?.coerceIn(0, 65535)
                    ?: topTex
            val backTex =
                (blockData["BackTex"] as? Number)?.toInt()?.coerceIn(0, 65535)
                    ?: topTex

            val slot =
                (blockData["InventoryOrder"] as? Number)?.toInt()?.toUShort()
                    ?: blockId

            val hasCustomBounds =
                minX != 0.toByte() ||
                    minY != 0.toByte() ||
                    minZ != 0.toByte() ||
                    maxX != 16.toByte() ||
                    maxY != 16.toByte() ||
                    maxZ != 16.toByte()
            val hasDifferentSideTextures =
                leftTex != topTex ||
                    rightTex != topTex ||
                    frontTex != topTex ||
                    backTex != topTex
            val isExtended = hasCustomBounds || hasDifferentSideTextures

            return JsonBlock(
                id = blockId,
                name = name,
                fallback = fallBack,
                solidity = BlockSolidity.from(collideType),
                movementSpeed = speed,
                topTextureId = topTex.toUShort(),
                sideTextureId = topTex.toUShort(),
                bottomTextureId = bottomTex.toUShort(),
                transmitsLight = !blocksLight,
                walkSound = WalkSound.from(walkSound),
                fullBright = fullBright,
                shape = shape,
                blockDraw = BlockDraw.from(blockDraw),
                fogDensity = fogDensity,
                fogR = fogR,
                fogG = fogG,
                fogB = fogB,
                extendedBlock = isExtended,
                leftTextureId = leftTex.toUShort(),
                rightTextureId = rightTex.toUShort(),
                frontTextureId = frontTex.toUShort(),
                backTextureId = backTex.toUShort(),
                minWidth = minX,
                minHeight = minY,
                minDepth = minZ,
                maxWidth = maxX,
                maxHeight = maxY,
                maxDepth = maxZ,
                slot = slot,
            )
        }
    }
}
