package org.dandelion.server.commands

import org.dandelion.server.blocks.manager.BlockRegistry
import org.dandelion.server.blocks.manager.JsonBlockLoader
import org.dandelion.server.blocks.model.Block
import org.dandelion.server.blocks.model.JsonBlock
import org.dandelion.server.blocks.model.enums.BlockDraw
import org.dandelion.server.blocks.model.enums.BlockSolidity
import org.dandelion.server.blocks.model.enums.WalkSound
import org.dandelion.server.commands.annotations.ArgRange
import org.dandelion.server.commands.annotations.CommandDef
import org.dandelion.server.commands.annotations.OnExecute
import org.dandelion.server.commands.annotations.OnSubCommand
import org.dandelion.server.commands.annotations.RequirePermission
import org.dandelion.server.commands.model.Command
import org.dandelion.server.commands.model.CommandExecutor
import org.dandelion.server.level.LevelRegistry
import org.dandelion.server.server.Console
import org.dandelion.server.server.data.MessageRegistry

@CommandDef(
    name = "block",
    description = "Manage custom block definitions",
    usage = "/block <global|levelId> <subcommand>",
    aliases = ["b", "blocks"],
)
class BlockCommand : Command {

    @OnExecute
    @RequirePermission("dandelion.command.block.manage")
    fun onExecute(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Block.sendGlobalOrLevelRequired(executor)
            MessageRegistry.Commands.Block.Subcommands.sendAvailable(executor)
            return
        }

        val target = args[0].lowercase()
        if (args.size == 1) {
            when (target) {
                "global",
                "all" -> {
                    MessageRegistry.Commands.Block.sendGlobalOrLevelRequired(
                        executor
                    )
                    MessageRegistry.Commands.Block.Subcommands.sendAvailable(
                        executor
                    )
                }
                else -> {
                    if (LevelRegistry.getLevel(target) == null) {
                        MessageRegistry.Commands.Block.sendLevelNotFound(
                            executor,
                            target,
                        )
                        return
                    }
                    MessageRegistry.Commands.Block.sendGlobalOrLevelRequired(
                        executor
                    )
                    MessageRegistry.Commands.Block.Subcommands.sendAvailable(
                        executor
                    )
                }
            }
        }
    }

    @OnSubCommand(
        name = "info",
        description = "Show detailed information about a block",
        usage = "/block <global|levelId> info <id>",
    )
    @RequirePermission("dandelion.command.block.info")
    @ArgRange(min = 2, max = 2)
    fun blockInfo(executor: CommandExecutor, args: Array<String>) {
        val target = args[0].lowercase()
        val blockId = args[1].toUShortOrNull()

        if (blockId == null) {
            MessageRegistry.Commands.Block.sendInvalidId(executor)
            return
        }

        val block =
            when (target) {
                "global" -> BlockRegistry.get(blockId)
                "all" -> BlockRegistry.get(blockId)
                else -> {
                    val level = LevelRegistry.getLevel(target)
                    if (level == null) {
                        MessageRegistry.Commands.Block.sendLevelNotFound(
                            executor,
                            target,
                        )
                        return
                    }
                    BlockRegistry.get(level, blockId)
                }
            }

        if (block == null) {
            MessageRegistry.Commands.Block.sendBlockNotFound(executor, blockId)
            return
        }

        sendBlockInfo(executor, block)
    }

    @OnSubCommand(
        name = "edit",
        description = "Edit block properties",
        usage = "/block <global|levelId> edit <id> <property> <value>",
    )
    @RequirePermission("dandelion.command.block.edit")
    @ArgRange(min = 4, max = 4)
    fun editBlock(executor: CommandExecutor, args: Array<String>) {
        val target = args[0].lowercase()
        val blockId = args[1].toUShortOrNull()
        val property = args[2].lowercase()
        val value = args[3]

        if (blockId == null) {
            MessageRegistry.Commands.Block.sendInvalidId(executor)
            return
        }

        if (blockId == 0u.toUShort()) {
            MessageRegistry.Commands.Block.sendAirNotEditable(executor)
            return
        }

        val level =
            if (target != "global") {
                val lvl = LevelRegistry.getLevel(target)
                if (lvl == null) {
                    MessageRegistry.Commands.Block.sendLevelNotFound(
                        executor,
                        target,
                    )
                    return
                }
                lvl
            } else null

        val block =
            if (level != null) {
                BlockRegistry.get(level, blockId)
            } else {
                BlockRegistry.get(blockId)
            }

        if (block == null) {
            MessageRegistry.Commands.Block.sendBlockNotFound(executor, blockId)
            return
        }

        try {
            val newBlock = editBlockProperty(block, property, value)
            if (newBlock != null) {
                val originalId = block.id
                val newId = newBlock.id

                if (originalId != newId) {
                    if (level != null) {
                        if (BlockRegistry.has(level, newId)) {
                            MessageRegistry.Commands.Block.sendIdAlreadyExists(
                                executor,
                                newId,
                            )
                            return
                        }
                        BlockRegistry.unregister(level, originalId)
                        BlockRegistry.register(level, newBlock)
                    } else {
                        if (BlockRegistry.has(newId)) {
                            MessageRegistry.Commands.Block.sendIdAlreadyExists(
                                executor,
                                newId,
                            )
                            return
                        }
                        BlockRegistry.unregister(originalId)
                        BlockRegistry.register(newBlock)
                    }
                    MessageRegistry.Commands.Block.Edit.sendSuccessIdChanged(
                        executor,
                        originalId,
                        newId,
                        newBlock.name,
                        property,
                        value,
                    )
                } else {
                    if (level != null) {
                        BlockRegistry.register(level, newBlock)
                    } else {
                        BlockRegistry.register(newBlock)
                    }
                    MessageRegistry.Commands.Block.Edit.sendSuccess(
                        executor,
                        newId,
                        newBlock.name,
                        property,
                        value,
                    )
                }

                BlockRegistry.saveBlockDefinitions(target)
            } else {
                MessageRegistry.Commands.Block.sendInvalidProperty(
                    executor,
                    property,
                )
                MessageRegistry.Commands.Block.Edit.sendProperties(executor)
            }
        } catch (e: Exception) {
            Console.errLog("Error editing block: ${e.message}")
            MessageRegistry.Commands.Block.sendInvalidProperty(
                executor,
                property,
            )
        }
    }

    @OnSubCommand(
        name = "add",
        description = "Add a new custom block",
        usage = "/block <global|levelId> add <id> [name] [textureId]",
    )
    @RequirePermission("dandelion.command.block.add")
    @ArgRange(min = 2, max = 4)
    fun addBlock(executor: CommandExecutor, args: Array<String>) {
        val target = args[0].lowercase()
        val blockId = args[1].toUShortOrNull()
        val name = if (args.size > 2) args[2] else "Custom Block"
        val textureId =
            if (args.size > 3)
                args[3].toIntOrNull()?.coerceIn(0, 65535)?.toUShort() ?: 1u
            else 1u

        if (blockId == null || blockId < 1u) {
            MessageRegistry.Commands.Block.sendInvalidId(executor)
            return
        }

        if (blockId == 0u.toUShort()) {
            MessageRegistry.Commands.Block.sendAirNotEditable(executor)
            return
        }

        val level =
            if (target != "global") {
                val lvl = LevelRegistry.getLevel(target)
                if (lvl == null) {
                    MessageRegistry.Commands.Block.sendLevelNotFound(
                        executor,
                        target,
                    )
                    return
                }
                lvl
            } else null

        val existingBlock =
            if (level != null) {
                BlockRegistry.get(level, blockId)
            } else {
                BlockRegistry.get(blockId)
            }

        if (existingBlock != null) {
            MessageRegistry.Commands.Block.sendIdAlreadyExists(
                executor,
                blockId,
            )
            return
        }

        val newBlock =
            JsonBlock(
                id = blockId,
                name = name,
                fallback = blockId.toByte(),
                solidity = BlockSolidity.SOLID,
                movementSpeed = 128.toByte(),
                topTextureId = textureId,
                sideTextureId = textureId,
                bottomTextureId = textureId,
                transmitsLight = false,
                walkSound = WalkSound.STONE,
                fullBright = false,
                shape = 16,
                blockDraw = BlockDraw.OPAQUE,
                fogDensity = 0,
                fogR = 255.toByte(),
                fogG = 255.toByte(),
                fogB = 255.toByte(),
                extendedBlock = false,
                leftTextureId = textureId,
                rightTextureId = textureId,
                frontTextureId = textureId,
                backTextureId = textureId,
                minWidth = 0,
                minHeight = 0,
                minDepth = 0,
                maxWidth = 16,
                maxHeight = 16,
                maxDepth = 16,
            )

        if (level != null) {
            BlockRegistry.register(level, newBlock)
        } else {
            BlockRegistry.register(newBlock)
        }

        MessageRegistry.Commands.Block.Add.sendSuccess(executor, blockId, name)
        BlockRegistry.saveBlockDefinitions(target)
    }

    @OnSubCommand(
        name = "delete",
        description = "Delete a custom block",
        usage = "/block <global|levelId> delete <id>",
    )
    @RequirePermission("dandelion.command.block.delete")
    @ArgRange(min = 2, max = 2)
    fun deleteBlock(executor: CommandExecutor, args: Array<String>) {
        val target = args[0].lowercase()
        val blockId = args[1].toUShortOrNull()

        if (blockId == null) {
            MessageRegistry.Commands.Block.sendInvalidId(executor)
            return
        }

        if (blockId == 0u.toUShort()) {
            MessageRegistry.Commands.Block.sendAirNotEditable(executor)
            return
        }

        val level =
            if (target != "global") {
                val lvl = LevelRegistry.getLevel(target)
                if (lvl == null) {
                    MessageRegistry.Commands.Block.sendLevelNotFound(
                        executor,
                        target,
                    )
                    return
                }
                lvl
            } else null

        val block =
            if (level != null) {
                BlockRegistry.get(level, blockId)
            } else {
                BlockRegistry.get(blockId)
            }

        if (block == null) {
            MessageRegistry.Commands.Block.sendBlockNotFound(executor, blockId)
            return
        }

        val success =
            if (level != null) {
                BlockRegistry.unregister(level, blockId)
            } else {
                BlockRegistry.unregister(blockId)
            }

        if (success) {
            MessageRegistry.Commands.Block.Delete.sendSuccess(
                executor,
                blockId,
                block.name,
            )
            BlockRegistry.saveBlockDefinitions(target)
        } else {
            Console.errLog("Failed to delete block $blockId")
        }
    }

    @OnSubCommand(
        name = "list",
        description = "List blocks",
        usage = "/block <global|levelId|all> list",
    )
    @RequirePermission("dandelion.command.block.info")
    @ArgRange(min = 1, max = 1)
    fun listBlocks(executor: CommandExecutor, args: Array<String>) {
        val target = args[0].lowercase()
        val blocks =
            when (target) {
                "global" -> {
                    MessageRegistry.Commands.Block.List.sendHeaderGlobal(
                        executor
                    )
                    BlockRegistry.getAll()
                }
                "all" -> {
                    MessageRegistry.Commands.Block.List.sendHeaderAll(executor)
                    BlockRegistry.getAll()
                }
                else -> {
                    val level = LevelRegistry.getLevel(target)
                    if (level == null) {
                        MessageRegistry.Commands.Block.sendLevelNotFound(
                            executor,
                            target,
                        )
                        return
                    }
                    MessageRegistry.Commands.Block.List.sendHeaderLevel(
                        executor,
                        target,
                    )
                    BlockRegistry.getAll(level)
                }
            }

        if (blocks.isEmpty()) {
            MessageRegistry.Commands.Block.List.sendNoBlocks(executor)
            return
        }

        blocks
            .sortedBy { it.id }
            .forEach { block ->
                MessageRegistry.Commands.Block.List.sendFormat(
                    executor,
                    block.id,
                    block.name,
                )
            }
    }

    @OnSubCommand(
        name = "reload",
        description = "Reload all block definitions from disk",
        usage = "/block reload",
    )
    @RequirePermission("dandelion.command.block.reload")
    fun reloadBlocks(executor: CommandExecutor, args: Array<String>) {
        try {
            JsonBlockLoader.loadAllBlockDefinitions()

            LevelRegistry.getAllPlayers().forEach { player ->
                BlockRegistry.sendBlockDefinitions(player)
            }

            MessageRegistry.Commands.Block.Reload.sendSuccess(executor)
        } catch (e: Exception) {
            Console.errLog("Error reloading block definitions: ${e.message}")
            MessageRegistry.Commands.Block.Reload.sendFailed(
                executor,
                e.message ?: "Unknown error",
            )
        }
    }

    private fun sendBlockInfo(executor: CommandExecutor, block: Block) {
        MessageRegistry.Commands.Block.Info.sendHeader(executor, block.id)
        MessageRegistry.Commands.Block.Info.sendName(executor, block.name)
        MessageRegistry.Commands.Block.Info.sendFallback(
            executor,
            block.fallback,
        )
        MessageRegistry.Commands.Block.Info.sendSolidity(
            executor,
            block.solidity.name,
        )
        MessageRegistry.Commands.Block.Info.sendMovementSpeed(
            executor,
            block.movementSpeed,
        )

        if (block.extendedBlock) {
            MessageRegistry.Commands.Block.Info.sendExtendedTextures(
                executor,
                block.topTextureId,
                block.leftTextureId,
                block.rightTextureId,
                block.frontTextureId,
                block.backTextureId,
                block.bottomTextureId,
            )
            MessageRegistry.Commands.Block.Info.sendBounds(
                executor,
                block.minWidth,
                block.minHeight,
                block.minDepth,
                block.maxWidth,
                block.maxHeight,
                block.maxDepth,
            )
        } else {
            MessageRegistry.Commands.Block.Info.sendTextures(
                executor,
                block.topTextureId,
                block.sideTextureId,
                block.bottomTextureId,
            )
            MessageRegistry.Commands.Block.Info.sendShape(executor, block.shape)
        }

        MessageRegistry.Commands.Block.Info.sendTransmitsLight(
            executor,
            block.transmitsLight,
        )
        MessageRegistry.Commands.Block.Info.sendWalkSound(
            executor,
            block.walkSound.name,
        )
        MessageRegistry.Commands.Block.Info.sendFullBright(
            executor,
            block.fullBright,
        )
        MessageRegistry.Commands.Block.Info.sendBlockDraw(
            executor,
            block.blockDraw.name,
        )

        if (block.fogDensity > 0) {
            MessageRegistry.Commands.Block.Info.sendFog(
                executor,
                block.fogDensity,
                block.fogR,
                block.fogG,
                block.fogB,
            )
        }

        MessageRegistry.Commands.Block.Info.sendExtended(
            executor,
            block.extendedBlock,
        )
    }

    private fun editBlockProperty(
        block: Block,
        property: String,
        value: String,
    ): JsonBlock? {
        return try {
            when (property) {
                "id" -> {
                    val newId = value.toUShortOrNull()
                    if (newId == null || newId < 1u) return null
                    copyBlock(block, id = newId)
                }
                "name" -> copyBlock(block, name = value)
                "fallback" -> {
                    val fallback =
                        value.toUByteOrNull()?.toByte() ?: return null
                    copyBlock(block, fallback = fallback)
                }
                "solidity" -> {
                    val solidity =
                        value.toUByteOrNull()?.let {
                            BlockSolidity.from(it.toByte())
                        } ?: return null
                    copyBlock(block, solidity = solidity)
                }
                "speed" -> {
                    val speed = value.toUByteOrNull()?.toByte() ?: return null
                    copyBlock(block, movementSpeed = speed)
                }
                "toptex" -> {
                    val tex =
                        value.toIntOrNull()?.coerceIn(0, 65535)?.toUShort()
                            ?: return null
                    copyBlock(block, topTextureId = tex)
                }
                "sidetex" -> {
                    val tex =
                        value.toIntOrNull()?.coerceIn(0, 65535)?.toUShort()
                            ?: return null
                    copyBlock(block, sideTextureId = tex)
                }
                "bottomtex" -> {
                    val tex =
                        value.toIntOrNull()?.coerceIn(0, 65535)?.toUShort()
                            ?: return null
                    copyBlock(block, bottomTextureId = tex)
                }
                "lefttex" -> {
                    val tex =
                        value.toIntOrNull()?.coerceIn(0, 65535)?.toUShort()
                            ?: return null
                    copyBlock(block, leftTextureId = tex)
                }
                "righttex" -> {
                    val tex =
                        value.toIntOrNull()?.coerceIn(0, 65535)?.toUShort()
                            ?: return null
                    copyBlock(block, rightTextureId = tex)
                }
                "fronttex" -> {
                    val tex =
                        value.toIntOrNull()?.coerceIn(0, 65535)?.toUShort()
                            ?: return null
                    copyBlock(block, frontTextureId = tex)
                }
                "backtex" -> {
                    val tex =
                        value.toIntOrNull()?.coerceIn(0, 65535)?.toUShort()
                            ?: return null
                    copyBlock(block, backTextureId = tex)
                }
                "transmitslight" -> {
                    val transmits = value.toBooleanStrictOrNull() ?: return null
                    copyBlock(block, transmitsLight = transmits)
                }
                "walksound" -> {
                    val sound =
                        value.toUByteOrNull()?.let {
                            WalkSound.from(it.toByte())
                        } ?: return null
                    copyBlock(block, walkSound = sound)
                }
                "fullbright" -> {
                    val bright = value.toBooleanStrictOrNull() ?: return null
                    copyBlock(block, fullBright = bright)
                }
                "shape" -> {
                    val shape = value.toUByteOrNull()?.toByte() ?: return null
                    copyBlock(block, shape = shape)
                }
                "blockdraw" -> {
                    val draw =
                        value.toUByteOrNull()?.let {
                            BlockDraw.from(it.toByte())
                        } ?: return null
                    copyBlock(block, blockDraw = draw)
                }
                "fogdensity" -> {
                    val density = value.toUByteOrNull()?.toByte() ?: return null
                    copyBlock(block, fogDensity = density)
                }
                "fogr" -> {
                    val r = value.toUByteOrNull()?.toByte() ?: return null
                    copyBlock(block, fogR = r)
                }
                "fogg" -> {
                    val g = value.toUByteOrNull()?.toByte() ?: return null
                    copyBlock(block, fogG = g)
                }
                "fogb" -> {
                    val b = value.toUByteOrNull()?.toByte() ?: return null
                    copyBlock(block, fogB = b)
                }
                "minx" -> {
                    val min = value.toUByteOrNull()?.toByte() ?: return null
                    copyBlock(block, minX = min)
                }
                "miny" -> {
                    val min = value.toUByteOrNull()?.toByte() ?: return null
                    copyBlock(block, minY = min)
                }
                "minz" -> {
                    val min = value.toUByteOrNull()?.toByte() ?: return null
                    copyBlock(block, minZ = min)
                }
                "maxx" -> {
                    val max = value.toUByteOrNull()?.toByte() ?: return null
                    copyBlock(block, maxX = max)
                }
                "maxy" -> {
                    val max = value.toUByteOrNull()?.toByte() ?: return null
                    copyBlock(block, maxY = max)
                }
                "maxz" -> {
                    val max = value.toUByteOrNull()?.toByte() ?: return null
                    copyBlock(block, maxZ = max)
                }
                "slot" -> {
                    val slot = value.toUShortOrNull() ?: return null
                    copyBlock(block, slot = slot)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun copyBlock(
        block: Block,
        id: UShort = block.id,
        name: String = block.name,
        fallback: Byte = block.fallback,
        solidity: BlockSolidity = block.solidity,
        movementSpeed: Byte = block.movementSpeed,
        topTextureId: UShort = block.topTextureId,
        sideTextureId: UShort = block.sideTextureId,
        bottomTextureId: UShort = block.bottomTextureId,
        transmitsLight: Boolean = block.transmitsLight,
        walkSound: WalkSound = block.walkSound,
        fullBright: Boolean = block.fullBright,
        shape: Byte = block.shape,
        blockDraw: BlockDraw = block.blockDraw,
        fogDensity: Byte = block.fogDensity,
        fogR: Byte = block.fogR,
        fogG: Byte = block.fogG,
        fogB: Byte = block.fogB,
        leftTextureId: UShort = block.leftTextureId,
        rightTextureId: UShort = block.rightTextureId,
        frontTextureId: UShort = block.frontTextureId,
        backTextureId: UShort = block.backTextureId,
        minX: Byte = block.minWidth,
        minY: Byte = block.minHeight,
        minZ: Byte = block.minDepth,
        maxX: Byte = block.maxWidth,
        maxY: Byte = block.maxHeight,
        maxZ: Byte = block.maxDepth,
        slot: UShort = block.slot,
    ): JsonBlock {
        val hasCustomBounds =
            minX != 0.toByte() ||
                minY != 0.toByte() ||
                minZ != 0.toByte() ||
                maxX != 16.toByte() ||
                maxY != 16.toByte() ||
                maxZ != 16.toByte()
        val hasDifferentSideTextures =
            leftTextureId != topTextureId ||
                rightTextureId != topTextureId ||
                frontTextureId != topTextureId ||
                backTextureId != topTextureId
        val isExtended = hasCustomBounds || hasDifferentSideTextures

        return JsonBlock(
            id = id,
            name = name,
            fallback = fallback,
            solidity = solidity,
            movementSpeed = movementSpeed,
            topTextureId = topTextureId,
            sideTextureId = sideTextureId,
            bottomTextureId = bottomTextureId,
            transmitsLight = transmitsLight,
            walkSound = walkSound,
            fullBright = fullBright,
            shape = shape,
            blockDraw = blockDraw,
            fogDensity = fogDensity,
            fogR = fogR,
            fogG = fogG,
            fogB = fogB,
            extendedBlock = isExtended,
            leftTextureId = leftTextureId,
            rightTextureId = rightTextureId,
            frontTextureId = frontTextureId,
            backTextureId = backTextureId,
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
