package org.dandelion.classic.entity.player

import io.netty.channel.Channel
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dandelion.classic.blocks.manager.BlockRegistry
import org.dandelion.classic.blocks.model.Block
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.entity.Entity
import org.dandelion.classic.entity.player.data.PlayerInfo
import org.dandelion.classic.events.BlockFace
import org.dandelion.classic.events.ClickAction
import org.dandelion.classic.events.MouseButton
import org.dandelion.classic.events.PlayerBlockClickEvent
import org.dandelion.classic.events.PlayerBlockInteractionEvent
import org.dandelion.classic.events.PlayerBlockListSelectedEvent
import org.dandelion.classic.events.PlayerBlockListToggledEvent
import org.dandelion.classic.events.PlayerChangeLevel
import org.dandelion.classic.events.PlayerEntityClickEvent
import org.dandelion.classic.events.PlayerLevelSavedEvent
import org.dandelion.classic.events.PlayerMoveEvent
import org.dandelion.classic.events.PlayerPressEvent
import org.dandelion.classic.events.PlayerReleaseEvent
import org.dandelion.classic.events.PlayerRespawnedEvent
import org.dandelion.classic.events.PlayerSendMessageEvent
import org.dandelion.classic.events.PlayerSpawnUpdatedEvent
import org.dandelion.classic.events.PlayerTexturePackChangedEvent
import org.dandelion.classic.events.PlayerTexturePromptRespondedEvent
import org.dandelion.classic.events.PlayerThirdPersonChangedEvent
import org.dandelion.classic.events.manager.EventDispatcher
import org.dandelion.classic.level.Level
import org.dandelion.classic.level.Levels
import org.dandelion.classic.network.packets.classic.client.ClientMessage
import org.dandelion.classic.network.packets.classic.server.*
import org.dandelion.classic.network.packets.cpe.client.ClientNotifyAction
import org.dandelion.classic.network.packets.cpe.client.ClientNotifyPositionAction
import org.dandelion.classic.network.packets.cpe.client.ClientPlayerClick
import org.dandelion.classic.network.packets.cpe.server.ServerCinematicGui
import org.dandelion.classic.network.packets.cpe.server.ServerClickDistance
import org.dandelion.classic.network.packets.cpe.server.ServerExtEntityTeleport
import org.dandelion.classic.network.packets.cpe.server.ServerHackControl
import org.dandelion.classic.network.packets.cpe.server.ServerHoldThis
import org.dandelion.classic.network.packets.cpe.server.ServerMakeSelection
import org.dandelion.classic.network.packets.cpe.server.ServerRemoveSelection
import org.dandelion.classic.network.packets.cpe.server.ServerSetBlockPermission
import org.dandelion.classic.network.packets.cpe.server.ServerSetHotbar
import org.dandelion.classic.network.packets.cpe.server.ServerSetSpawnpoint
import org.dandelion.classic.network.packets.cpe.server.ServerSetTextHotKey
import org.dandelion.classic.network.packets.cpe.server.ServerToggleBlockList
import org.dandelion.classic.network.packets.cpe.server.ServerVelocityControl
import org.dandelion.classic.permission.PermissionRepository
import org.dandelion.classic.server.Console
import org.dandelion.classic.server.MessageRegistry
import org.dandelion.classic.server.ServerConfig
import org.dandelion.classic.tablist.TabList
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.enums.MessageType
import org.dandelion.classic.types.enums.MoveMode
import org.dandelion.classic.types.extensions.Color
import org.dandelion.classic.types.extensions.SelectionCuboid
import org.dandelion.classic.util.toFShort

class Player(
    val channel: Channel,
    var client: String,
    name: String,
    levelId: String = "",
    entityId: Byte = -1,
    position: Position = Position(0f, 0f, 0f, 0f, 0f),
    val info: PlayerInfo = PlayerInfo.getOrCreate(name),
) : Entity(name, levelId, entityId, position), CommandExecutor {
    var supportsCpe: Boolean = false
    private val supportedCPE = mutableListOf<Pair<String, Int>>()
    internal var supportedCpeCount: Short = 0

    var heldBlock: UShort = 0x00.toUShort()

    private var messageBlocks: List<String> = listOf()

    override val permissions: List<String>
        get() = PermissionRepository.getPermissionList(name)

    private val MAX_MESSAGE_LENGTH = 64
    private val LEVEL_DATA_CHUNK_SIZE = 1024
    private val COLOR_CODE_REGEX = "&[0-9a-fA-F]"

    // region cpe support
    fun addCPE(name: String, version: Int = 1) {
        if (!supportedCPE.any { it.first == name && it.second == version }) {
            supportedCPE.add(name to version)
        }
    }

    fun supports(name: String, version: Int? = null): Boolean {
        return when (version) {
            null -> supportedCPE.any { it.first == name }
            else ->
                supportedCPE.any { it.first == name && it.second == version }
        }
    }

    fun removeCPE(name: String, version: Int) {
        supportedCPE.removeIf { it.first == name && it.second == version }
    }

    fun getCPE(): List<Pair<String, Int>> = supportedCPE.toList()

    // endregion

    // region message system
    override fun sendMessage(message: String) {
        sendMessage(message, 0x00)
    }

    fun sendMessage(
        message: String,
        messageType: MessageType = MessageType.Chat,
    ) {
        sendMessage(message, messageType.code.toByte())
    }

    fun sendMessage(message: String, messageTypeId: Byte = 0x00) {
        when (messageTypeId) {
            0x00.toByte() -> {
                splitMessageIntoChunks(message).forEach { chunk ->
                    ServerMessage(messageTypeId, chunk).send(channel)
                }
            }
            else -> ServerMessage(messageTypeId, message).send(channel)
        }
    }

    private fun splitMessageIntoChunks(
        message: String,
        maxLength: Int = MAX_MESSAGE_LENGTH,
    ): List<String> {
        if (message.length <= maxLength) return listOf(message)

        val chunks = mutableListOf<String>()
        var remainingText = message
        var lastColorCode = ""

        while (remainingText.length > maxLength) {
            val splitIndex = findOptimalSplitIndex(remainingText, maxLength)
            val currentChunk = remainingText.substring(0, splitIndex)

            lastColorCode = extractLastColorCode(currentChunk)
            chunks.add(currentChunk)
            remainingText =
                prepareContinuationText(
                    remainingText,
                    splitIndex,
                    lastColorCode,
                )
        }

        if (remainingText.isNotEmpty()) {
            chunks.add(remainingText)
        }

        return chunks
    }

    private fun findOptimalSplitIndex(text: String, maxLength: Int): Int {
        val lastSpaceIndex = text.substring(0, maxLength).lastIndexOf(' ')
        return if (lastSpaceIndex > 0) lastSpaceIndex else maxLength
    }

    private fun extractLastColorCode(text: String): String {
        return COLOR_CODE_REGEX.toRegex().findAll(text).lastOrNull()?.value
            ?: ""
    }

    private fun prepareContinuationText(
        text: String,
        splitIndex: Int,
        lastColorCode: String,
    ): String {
        val hasSpaceSplit = text.substring(0, splitIndex).contains(' ')
        val continuationStart =
            if (hasSpaceSplit) splitIndex + 1 else splitIndex

        val continuation =
            if (continuationStart < text.length) {
                text.substring(continuationStart)
            } else {
                ""
            }

        return when {
            lastColorCode.isNotEmpty() &&
                continuation.isNotEmpty() &&
                !continuation.startsWith("&") -> lastColorCode + continuation
            else -> continuation
        }
    }

    // endregion

    // region Player Management
    fun kick(reason: String = "You have been kicked") {
        ServerDisconnectPlayer(reason).send(channel)
        Players.handleDisconnection(channel)
    }

    fun ban(reason: String = "No reason provided") {
        info.setBanned(reason)
        kick("You are banned: $reason")
    }

    // endregion
    // region Position Management
    override fun teleportTo(
        x: Float,
        y: Float,
        z: Float,
        yaw: Float,
        pitch: Float,
        moveMode: MoveMode,
        interpolateOrientation: Boolean,
    ) {

        val actualUsePosition =
            x != this.position.x || y != this.position.y || z != this.position.z
        val actualUseOrientation =
            yaw != this.position.yaw || pitch != this.position.pitch

        if (supports("ExtEntityTeleport")) {
            ServerExtEntityTeleport(
                    entityId = -1,
                    usePosition = actualUsePosition,
                    moveMode = moveMode,
                    useOrientation = actualUseOrientation,
                    interpolateOrientation = interpolateOrientation,
                    x = x,
                    y = y,
                    z = z,
                    yaw = yaw,
                    pitch = pitch,
                )
                .send(channel)
            Players.getAllPlayers()
                .filter { it != this }
                .forEach {
                    ServerExtEntityTeleport(
                            entityId = entityId,
                            usePosition = actualUsePosition,
                            moveMode = moveMode,
                            useOrientation = actualUseOrientation,
                            interpolateOrientation = interpolateOrientation,
                            x = x,
                            y = y,
                            z = z,
                            yaw = yaw,
                            pitch = pitch,
                        )
                        .send(it)
                }
        } else {
            if (
                moveMode == MoveMode.RELATIVE_SMOOTH ||
                    moveMode == MoveMode.RELATIVE_SEAMLESS
            ) {
                ServerSetPositionAndOrientation(
                        -1,
                        if (actualUsePosition) this.position.x + x
                        else this.position.x,
                        if (actualUsePosition) this.position.y + y
                        else this.position.y,
                        if (actualUsePosition) this.position.z + z
                        else this.position.z,
                        if (actualUseOrientation)
                            (this.position.yaw + yaw).toInt().toByte()
                        else this.position.yaw.toInt().toByte(),
                        if (actualUseOrientation)
                            (this.position.pitch + pitch).toInt().toByte()
                        else this.position.pitch.toInt().toByte(),
                    )
                    .send(channel)
                Players.getAllPlayers()
                    .filter { it != this }
                    .forEach {
                        ServerSetPositionAndOrientation(
                                entityId,
                                if (actualUsePosition) this.position.x + x
                                else this.position.x,
                                if (actualUsePosition) this.position.y + y
                                else this.position.y,
                                if (actualUsePosition) this.position.z + z
                                else this.position.z,
                                if (actualUseOrientation)
                                    (this.position.yaw + yaw).toInt().toByte()
                                else this.position.yaw.toInt().toByte(),
                                if (actualUseOrientation)
                                    (this.position.pitch + pitch)
                                        .toInt()
                                        .toByte()
                                else this.position.pitch.toInt().toByte(),
                            )
                            .send(it)
                    }
            } else {
                ServerSetPositionAndOrientation(
                        -1,
                        if (actualUsePosition) x else this.position.x,
                        if (actualUsePosition) y else this.position.y,
                        if (actualUsePosition) z else this.position.z,
                        if (actualUseOrientation) yaw.toInt().toByte()
                        else this.position.yaw.toInt().toByte(),
                        if (actualUseOrientation) pitch.toInt().toByte()
                        else this.position.pitch.toInt().toByte(),
                    )
                    .send(channel)
                Players.getAllPlayers()
                    .filter { it != this }
                    .forEach {
                        ServerSetPositionAndOrientation(
                                entityId,
                                if (actualUsePosition) x else this.position.x,
                                if (actualUsePosition) y else this.position.y,
                                if (actualUsePosition) z else this.position.z,
                                if (actualUseOrientation) yaw.toInt().toByte()
                                else this.position.yaw.toInt().toByte(),
                                if (actualUseOrientation) pitch.toInt().toByte()
                                else this.position.pitch.toInt().toByte(),
                            )
                            .send(it)
                    }
            }
        }
    }

    override fun updatePositionAndOrientation(
        newX: Float,
        newY: Float,
        newZ: Float,
        newYaw: Float,
        newPitch: Float,
        forceAbsolute: Boolean,
    ) {
        val newPosition = Position(newX, newY, newZ, newYaw, newPitch)

        if (this.position == newPosition) return

        val moveEvent = PlayerMoveEvent(this, this.position, newPosition)
        EventDispatcher.dispatch(moveEvent)

        when {
            moveEvent.isCancelled -> rejectMovement(moveEvent.from)
            else ->
                super.updatePositionAndOrientation(
                    newX,
                    newY,
                    newZ,
                    newYaw,
                    newPitch,
                    forceAbsolute,
                )
        }
    }

    private fun rejectMovement(originalPosition: Position) {
        ServerSetPositionAndOrientation(
                -1,
                originalPosition.x,
                originalPosition.y - toFShort(22),
                originalPosition.z,
                originalPosition.yaw.toInt().toByte(),
                originalPosition.pitch.toInt().toByte(),
            )
            .send(channel)
        position.set(
            originalPosition.x,
            originalPosition.y,
            originalPosition.z,
            originalPosition.yaw,
            originalPosition.pitch,
        )
    }

    // endregion

    // region Level Management
    @OptIn(DelicateCoroutinesApi::class)
    override fun joinLevel(level: Level, notifyJoin: Boolean) {
        if (!level.tryAddEntity(this)) {
            sendMessage(MessageRegistry.Server.Level.getFull())
            return
        }

        this.level?.let { currentLevel ->
            val event = PlayerChangeLevel(this, currentLevel, level)
            EventDispatcher.dispatch(event)
            if (event.isCancelled) return
        }

        this.level = level
        if (notifyJoin) Players.notifyJoinedLevel(this, level)

        GlobalScope.launch {
            transmitLevelData(level)
            updateTabList()
        }
    }

    private suspend fun transmitLevelData(level: Level) {
        when {
            supports("ExtendedBlocks") && supports("FastMap") -> {
                sendLevelDataFastMapExtendedBlocks(level)
            }
            supports("ExtendedBlocks") -> {
                sendLevelDataVanillaExtendedBlocks(level)
            }
            supports("FastMap") -> {
                val blocksWithFallback =
                    substituteBlocksForFallback(level.blockData)
                sendLevelDataFastMap(blocksWithFallback)
            }
            else -> {
                val blocksWithFallback =
                    substituteBlocksForFallback(level.blockData)
                sendLevelDataVanilla(blocksWithFallback)
            }
        }

        finalizeLevelTransfer(level)
    }

    private suspend fun sendLevelDataVanilla(blockData: ByteArray) {
        ServerLevelInitialize().send(channel)

        val totalLength = blockData.size
        val prefixedData = ByteArray(4 + totalLength)
        prefixedData[0] = (totalLength shr 24).toByte()
        prefixedData[1] = (totalLength shr 16).toByte()
        prefixedData[2] = (totalLength shr 8).toByte()
        prefixedData[3] = totalLength.toByte()
        System.arraycopy(blockData, 0, prefixedData, 4, totalLength)

        val compressedData =
            ByteArrayOutputStream().use { outputStream ->
                GZIPOutputStream(outputStream).use { gzipStream ->
                    gzipStream.write(prefixedData)
                }
                outputStream.toByteArray()
            }

        compressedData.indices.step(LEVEL_DATA_CHUNK_SIZE).forEach { chunkStart
            ->
            val remainingBytes = compressedData.size - chunkStart
            val chunkSize = remainingBytes.coerceAtMost(LEVEL_DATA_CHUNK_SIZE)
            val chunk = ByteArray(chunkSize)

            System.arraycopy(compressedData, chunkStart, chunk, 0, chunkSize)

            val progressPercent =
                ((chunkStart + chunkSize).toFloat() / compressedData.size *
                        100f)
                    .toInt()
                    .coerceAtMost(100)
                    .toByte()
            if (!channel.isOpen || !channel.isActive) {
                return
            }
            ServerLevelDataChunk(chunkSize.toShort(), chunk, progressPercent)
                .send(channel)
        }
    }

    private suspend fun sendLevelDataFastMap(blockData: ByteArray) {
        ServerLevelInitialize(blockData.size).send(channel)

        val compressedData =
            ByteArrayOutputStream().use { outputStream ->
                val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
                try {
                    deflater.setInput(blockData)
                    deflater.finish()

                    val buffer = ByteArray(1024)
                    while (!deflater.finished()) {
                        val compressedDataLength = deflater.deflate(buffer)
                        outputStream.write(buffer, 0, compressedDataLength)
                    }
                } finally {
                    deflater.end()
                }
                outputStream.toByteArray()
            }

        compressedData.indices.step(LEVEL_DATA_CHUNK_SIZE).forEach { chunkStart
            ->
            val remainingBytes = compressedData.size - chunkStart
            val chunkSize = remainingBytes.coerceAtMost(LEVEL_DATA_CHUNK_SIZE)
            val chunk = ByteArray(chunkSize)

            System.arraycopy(compressedData, chunkStart, chunk, 0, chunkSize)

            val progressPercent =
                ((chunkStart + chunkSize).toFloat() / compressedData.size *
                        100f)
                    .toInt()
                    .coerceAtMost(100)
                    .toByte()
            if (!channel.isOpen || !channel.isActive) {
                return
            }
            ServerLevelDataChunk(chunkSize.toShort(), chunk, progressPercent)
                .send(channel)
        }
    }

    private suspend fun sendLevelDataVanillaExtendedBlocks(level: Level) {
        ServerLevelInitialize().send(channel)

        val mainBlockData = level.blockData
        val totalLength = mainBlockData.size
        val prefixedData = ByteArray(4 + totalLength)
        prefixedData[0] = (totalLength shr 24).toByte()
        prefixedData[1] = (totalLength shr 16).toByte()
        prefixedData[2] = (totalLength shr 8).toByte()
        prefixedData[3] = totalLength.toByte()
        System.arraycopy(mainBlockData, 0, prefixedData, 4, totalLength)

        val compressedMainData =
            ByteArrayOutputStream().use { outputStream ->
                GZIPOutputStream(outputStream).use { gzipStream ->
                    gzipStream.write(prefixedData)
                }
                outputStream.toByteArray()
            }

        compressedMainData.indices.step(LEVEL_DATA_CHUNK_SIZE).forEach {
            chunkStart ->
            val remainingBytes = compressedMainData.size - chunkStart
            val chunkSize = remainingBytes.coerceAtMost(LEVEL_DATA_CHUNK_SIZE)
            val chunk = ByteArray(chunkSize)

            System.arraycopy(
                compressedMainData,
                chunkStart,
                chunk,
                0,
                chunkSize,
            )

            if (!channel.isOpen || !channel.isActive) {
                return
            }
            ServerLevelDataChunk(chunkSize.toShort(), chunk, 0).send(channel)
        }

        level.blockData2?.let { secondaryBlockData ->
            val prefixedSecondaryData = ByteArray(4 + secondaryBlockData.size)
            prefixedSecondaryData[0] = (secondaryBlockData.size shr 24).toByte()
            prefixedSecondaryData[1] = (secondaryBlockData.size shr 16).toByte()
            prefixedSecondaryData[2] = (secondaryBlockData.size shr 8).toByte()
            prefixedSecondaryData[3] = secondaryBlockData.size.toByte()
            System.arraycopy(
                secondaryBlockData,
                0,
                prefixedSecondaryData,
                4,
                secondaryBlockData.size,
            )

            val compressedSecondaryData =
                ByteArrayOutputStream().use { outputStream ->
                    GZIPOutputStream(outputStream).use { gzipStream ->
                        gzipStream.write(prefixedSecondaryData)
                    }
                    outputStream.toByteArray()
                }

            compressedSecondaryData.indices
                .step(LEVEL_DATA_CHUNK_SIZE)
                .forEach { chunkStart ->
                    val remainingBytes =
                        compressedSecondaryData.size - chunkStart
                    val chunkSize =
                        remainingBytes.coerceAtMost(LEVEL_DATA_CHUNK_SIZE)
                    val chunk = ByteArray(chunkSize)

                    System.arraycopy(
                        compressedSecondaryData,
                        chunkStart,
                        chunk,
                        0,
                        chunkSize,
                    )

                    if (!channel.isOpen || !channel.isActive) {
                        return
                    }
                    ServerLevelDataChunk(chunkSize.toShort(), chunk, 1)
                        .send(channel)
                }
        }
    }

    private suspend fun sendLevelDataFastMapExtendedBlocks(level: Level) {
        ServerLevelInitialize(level.blockData.size).send(channel)

        val mainBlockData = level.blockData
        val compressedMainData =
            ByteArrayOutputStream().use { outputStream ->
                val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
                try {
                    deflater.setInput(mainBlockData)
                    deflater.finish()

                    val buffer = ByteArray(1024)
                    while (!deflater.finished()) {
                        val compressedDataLength = deflater.deflate(buffer)
                        outputStream.write(buffer, 0, compressedDataLength)
                    }
                } finally {
                    deflater.end()
                }
                outputStream.toByteArray()
            }

        compressedMainData.indices.step(LEVEL_DATA_CHUNK_SIZE).forEach {
            chunkStart ->
            val remainingBytes = compressedMainData.size - chunkStart
            val chunkSize = remainingBytes.coerceAtMost(LEVEL_DATA_CHUNK_SIZE)
            val chunk = ByteArray(chunkSize)

            System.arraycopy(
                compressedMainData,
                chunkStart,
                chunk,
                0,
                chunkSize,
            )

            if (!channel.isOpen || !channel.isActive) {
                return
            }
            ServerLevelDataChunk(chunkSize.toShort(), chunk, 0).send(channel)
        }

        level.blockData2?.let { secondaryBlockData ->
            val compressedSecondaryData =
                ByteArrayOutputStream().use { outputStream ->
                    val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
                    try {
                        deflater.setInput(secondaryBlockData)
                        deflater.finish()

                        val buffer = ByteArray(1024)
                        while (!deflater.finished()) {
                            val compressedDataLength = deflater.deflate(buffer)
                            outputStream.write(buffer, 0, compressedDataLength)
                        }
                    } finally {
                        deflater.end()
                    }
                    outputStream.toByteArray()
                }

            compressedSecondaryData.indices
                .step(LEVEL_DATA_CHUNK_SIZE)
                .forEach { chunkStart ->
                    val remainingBytes =
                        compressedSecondaryData.size - chunkStart
                    val chunkSize =
                        remainingBytes.coerceAtMost(LEVEL_DATA_CHUNK_SIZE)
                    val chunk = ByteArray(chunkSize)

                    System.arraycopy(
                        compressedSecondaryData,
                        chunkStart,
                        chunk,
                        0,
                        chunkSize,
                    )

                    if (!channel.isOpen || !channel.isActive) {
                        return
                    }
                    ServerLevelDataChunk(chunkSize.toShort(), chunk, 1)
                        .send(channel)
                }
        }
    }

    private fun substituteBlocksForFallback(
        originalBlockData: ByteArray
    ): ByteArray {
        val processedData = ByteArray(originalBlockData.size)
        for (i in originalBlockData.indices) {
            val blockId = originalBlockData[i].toInt() and 0xFF

            processedData[i] =
                when {
                    blockId in 50..65 -> {
                        if (!supports("CustomBlocks")) {
                            Block.get(blockId.toUShort())?.fallback?.toByte()
                                ?: blockId.toByte()
                        } else {
                            blockId.toByte()
                        }
                    }
                    blockId > 65 -> {
                        if (!supports("BlockDefinitions")) {
                            Block.get(blockId.toUShort())?.fallback?.toByte()
                                ?: blockId.toByte()
                        } else {
                            blockId.toByte()
                        }
                    }
                    else -> blockId.toByte()
                }
        }
        return processedData
    }

    private fun finalizeLevelTransfer(level: Level) {
        BlockRegistry.sendBlockDefinitions(this)
        level.spawnPlayerInLevel(this)
        updateTabList()
        teleportTo(level.spawn)
        setSpawnPoint(level.spawn)
        ServerLevelFinalize(level.size.x, level.size.y, level.size.z)
            .send(channel)
        ServerSetPositionAndOrientation(
                -1,
                spawnPosition.x,
                spawnPosition.y,
                spawnPosition.z,
                spawnPosition.yaw.toInt().toByte(),
                spawnPosition.pitch.toInt().toByte(),
            )
            .send(channel)

        level.sendAllCustomData(this)
    }

    // endregion

    // region Entity Spawning Overrides
    override fun mutualSpawn(other: Entity) {
        this.spawnFor(other)
        other.spawnFor(this)
    }

    override fun mutualDespawn(other: Entity) {
        this.despawnFor(other)
        other.despawnFor(this)
    }

    // endregion
    // region Communication
    internal fun handleSendMessageAs(packet: ClientMessage) {
        if (supports("LongerMessages")) {
            val waitNext = packet.messageType != 0.toByte()
            messageBlocks = messageBlocks + packet.message
            if (!waitNext) {
                val completeMessage = messageBlocks.joinToString("")
                messageBlocks = emptyList()
                sendMessageAs(completeMessage)
            }
        } else {
            sendMessageAs(packet.message)
        }
    }

    override fun sendMessageAs(message: String) {
        val processedMessage = message.replace("%", "&")
        val event = PlayerSendMessageEvent(this, processedMessage)
        EventDispatcher.dispatch(event)

        if (event.isCancelled) return

        val messageFormat =
            MessageRegistry.Server.Chat.getPlayerFormat(this, processedMessage)
        val consoleFormat =
            MessageRegistry.Server.Chat.getConsoleFormat(this, processedMessage)

        Console.log(consoleFormat)

        when {
            processedMessage.startsWith("/") -> sendCommand(processedMessage)
            else -> Levels.broadcast(messageFormat)
        }
    }

    // endregion

    // region Block Updates
    override fun interactWithBlock(
        x: Short,
        y: Short,
        z: Short,
        blockType: UShort,
        isDestroying: Boolean,
    ) {
        val currentLevel = level ?: return
        if (
            !isWithinInteractionRange(
                x.toFloat(),
                y.toFloat(),
                z.toFloat(),
                clickDistance / 32.0f,
            )
        ) {
            return
        }

        val finalBlockType =
            if (isDestroying) Block.get(0.toUShort())?.id ?: 0.toUShort()
            else blockType
        val blockAtPos = Block.get(currentLevel.getBlock(x, y, z))

        if (Block.get(finalBlockType) == null) {
            return
        }

        val event =
            PlayerBlockInteractionEvent(
                this,
                blockAtPos!!,
                Block.get(finalBlockType)!!,
                Position(x.toFloat(), y.toFloat(), z.toFloat()),
                level!!,
            )
        EventDispatcher.dispatch(event)
        if (event.isCancelled) {
            ServerSetBlock(
                    x,
                    y,
                    z,
                    blockAtPos.id,
                    this.supports("ExtendedBlocks"),
                )
                .send(this)
            return
        }

        currentLevel.setBlock(x, y, z, finalBlockType)
        broadcastBlockUpdate(x, y, z, finalBlockType)
    }

    override fun updateBlock(x: Short, y: Short, z: Short, block: UShort) {
        ServerSetBlock(x, y, z, block, this.supports("ExtendedBlocks"))
            .send(this)
    }

    fun setHeldBlock(block: UShort, preventChange: Boolean = false) {
        if (supports("HeldBlock")) {
            this.heldBlock = block
            val preventChange: Byte =
                if (preventChange) 1.toByte() else 0.toByte()
            ServerHoldThis(block, preventChange).send(channel)
        }
    }

    fun setHotbarBlock(block: UShort, index: Byte) {
        if (supports("SetHotbar")) {
            ServerSetHotbar(block, index).send(channel)
        }
    }

    internal fun updateHeldBlock(block: UShort) {
        if (supports("HeldBlock")) this.heldBlock = block
    }

    // endregion

    // region hack control
    var canFly: Boolean = true
        set(value) {
            field = value
            updateHackControl()
        }

    var canNoClip: Boolean = true
        set(value) {
            field = value
            updateHackControl()
        }

    var canSpeed: Boolean = true
        set(value) {
            field = value
            updateHackControl()
        }

    var canSpawnControl: Boolean = true
        set(value) {
            field = value
            updateHackControl()
        }

    var canThirdPerson: Boolean = true
        set(value) {
            field = value
            updateHackControl()
        }

    var jumpHeight: Short = -1
        set(value) {
            field = value
            updateHackControl()
        }

    var clickDistance: Short = 160
        set(value) {
            field = value
            if (supports("ClickDistance")) {
                ServerClickDistance(value).send(channel)
            }
        }

    private fun updateHackControl() {
        if (supports("HackControl")) {
            ServerHackControl(
                    canFly,
                    canNoClip,
                    canSpeed,
                    canSpawnControl,
                    canThirdPerson,
                    jumpHeight,
                )
                .send(channel)
        }
    }

    // endregion

    var spawnPosition: Position = Position(0, 0, 0)
        set(value) {
            setSpawnPoint(value.x, value.y, value.z, value.yaw, value.pitch)
            field = value
        }

    fun setSpawnPoint(position: Position) {
        spawnPosition = position
    }

    fun setSpawnPoint(x: Float, y: Float, z: Float, yaw: Float, pitch: Float) {
        if (!supports("SetSpawnpoint")) return
        ServerSetSpawnpoint(x, y, z, yaw, pitch).send(channel)
    }

    var motd: String = ServerConfig.motd
        set(value) {
            field = value
            when {
                supports("InstantMOTD") ->
                    ServerIdentification(serverMotd = value).send(channel)
                level != null -> joinLevel(level!!, false)
            }
        }

    // region Tab List Management

    fun addToTabList() {
        TabList.addPlayer(this)
    }

    fun removeFromTabList() {
        TabList.removePlayer(this)
    }

    fun updateTabList() {
        TabList.updatePlayer(this)
        TabList.sendFullTabListTo(this)
    }

    // endregion

    // region permissions and Groups

    fun setBlockPermission(
        blockType: UShort,
        allowPlacement: Boolean,
        allowDeletion: Boolean,
    ) {
        ServerSetBlockPermission(blockType, allowPlacement, allowDeletion)
            .send(channel)
    }

    fun setPermission(permission: String, value: Boolean): Boolean =
        Players.setPermission(this.name, permission, value)

    override fun hasPermission(permission: String): Boolean {
        return super.hasPermission(permission)
    }

    fun hasPermission(permission: String, default: Boolean = false): Boolean {
        return Players.hasPermission(name, permission, default)
    }

    fun addGroup(group: String): Boolean = Players.addGroup(this.name, group)

    fun removeGroup(group: String): Boolean =
        Players.removeGroup(this.name, group)

    // endregion

    // region Velocity Management
    fun setVelocity(velocityX: Int, velocityY: Int, velocityZ: Int) {
        if (supports("VelocityControl")) {
            ServerVelocityControl(velocityX, velocityY, velocityZ, 1, 1, 1)
                .send(channel)
        }
    }

    fun setVelocity(velocityX: Float, velocityY: Float, velocityZ: Float) {
        setVelocity(
            (velocityX * 10000).toInt(),
            (velocityY * 10000).toInt(),
            (velocityZ * 10000).toInt(),
        )
    }

    fun addVelocity(velocityX: Int, velocityY: Int, velocityZ: Int) {
        if (supports("VelocityControl")) {
            ServerVelocityControl(velocityX, velocityY, velocityZ, 0, 0, 0)
                .send(channel)
        }
    }

    fun addVelocity(velocityX: Float, velocityY: Float, velocityZ: Float) {
        addVelocity(
            (velocityX * 10000).toInt(),
            (velocityY * 10000).toInt(),
            (velocityZ * 10000).toInt(),
        )
    }

    fun jump(jumpHeight: Float = 1.0f) {
        if (supports("VelocityControl")) {
            ServerVelocityControl(0, (jumpHeight * 10000).toInt(), 0, 0, 1, 0)
                .send(channel)
        }
    }

    fun knockback(
        velocityX: Float,
        velocityZ: Float,
        upwardVelocity: Float = 0.5f,
    ) {
        addVelocity(velocityX, upwardVelocity, velocityZ)
    }

    // endregion
    // region Selection Management

    private val activeSelections = mutableMapOf<Byte, SelectionCuboid>()
    private var nextSelectionId: Byte = 0

    fun addSelection(selection: SelectionCuboid): Byte? {
        if (!supports("SelectionCuboid")) return null

        if (activeSelections.size >= 256) {
            Console.errLog(
                "Selection overflow for player $name: Maximum of 256 selections reached"
            )
            return null
        }

        var attempts = 0
        while (
            activeSelections.containsKey(nextSelectionId) && attempts < 256
        ) {
            nextSelectionId = ((nextSelectionId.toInt() + 1) and 0xFF).toByte()
            attempts++
        }

        if (attempts >= 256) {
            Console.errLog(
                "Selection ID overflow for player $name: All 256 selection IDs are in use"
            )
            return null
        }

        selection.id = nextSelectionId
        activeSelections[nextSelectionId] = selection
        ServerMakeSelection(selection).send(channel)

        nextSelectionId = ((nextSelectionId.toInt() + 1) and 0xFF).toByte()

        return selection.id
    }

    fun removeSelection(selection: SelectionCuboid) {
        removeSelection(selection.id)
    }

    fun removeSelection(selectionId: Byte) {
        if (
            supports("SelectionCuboid") &&
                activeSelections.containsKey(selectionId)
        ) {
            activeSelections.remove(selectionId)
            ServerRemoveSelection(selectionId).send(channel)
        }
    }

    fun clearSelections() {
        if (supports("SelectionCuboid")) {
            activeSelections.keys.forEach { selectionId ->
                ServerRemoveSelection(selectionId).send(channel)
            }
            activeSelections.clear()
            nextSelectionId = 0
        }
    }

    fun getActiveSelections(): Map<Byte, SelectionCuboid> =
        activeSelections.toMap()

    // endregion
    // region Click Event Handling

    private val pressedButtons = mutableSetOf<MouseButton>()

    fun isPressingMouse(button: MouseButton): Boolean {
        return pressedButtons.contains(button)
    }

    fun isPressingLeftMouse(): Boolean = isPressingMouse(MouseButton.LEFT)

    fun isPressingRightMouse(): Boolean = isPressingMouse(MouseButton.RIGHT)

    fun isPressingMiddleMouse(): Boolean = isPressingMouse(MouseButton.MIDDLE)

    internal fun handleClickEvent(packet: ClientPlayerClick) {
        val highPrecisionYaw = packet.yaw / 32.0f
        val highPrecisionPitch = packet.pitch / 32.0f

        position.yaw = highPrecisionYaw
        position.pitch = highPrecisionPitch

        val button =
            when (packet.button) {
                0.toByte() -> MouseButton.LEFT
                1.toByte() -> MouseButton.RIGHT
                2.toByte() -> MouseButton.MIDDLE
                else -> MouseButton.UNKNOWN
            }

        val action =
            when (packet.action) {
                0.toByte() -> ClickAction.PRESS
                1.toByte() -> ClickAction.RELEASE
                else -> return
            }

        when (action) {
            ClickAction.PRESS -> pressedButtons.add(button)
            ClickAction.RELEASE -> pressedButtons.remove(button)
        }

        val currentLevel = level ?: return

        val hasBlockTarget = packet.targetBlockFace in 0..5
        val hasEntityTarget = packet.targetEntityId.toInt() and 0xFF < 256

        when {
            hasBlockTarget -> {
                val blockFace =
                    when (packet.targetBlockFace) {
                        0.toByte() -> BlockFace.AWAY_FROM_X
                        1.toByte() -> BlockFace.TOWARDS_X
                        2.toByte() -> BlockFace.UP
                        3.toByte() -> BlockFace.DOWN
                        4.toByte() -> BlockFace.AWAY_FROM_Z
                        5.toByte() -> BlockFace.TOWARDS_Z
                        else -> BlockFace.INVALID
                    }

                val blockClickEvent =
                    PlayerBlockClickEvent(
                        this,
                        button,
                        action,
                        currentLevel,
                        highPrecisionYaw,
                        highPrecisionPitch,
                        packet.targetBlockX,
                        packet.targetBlockY,
                        packet.targetBlockZ,
                        blockFace,
                    )
                EventDispatcher.dispatch(blockClickEvent)
            }

            hasEntityTarget -> {
                val targetEntity = currentLevel.getEntity(packet.targetEntityId)

                val entityClickEvent =
                    PlayerEntityClickEvent(
                        this,
                        button,
                        action,
                        currentLevel,
                        highPrecisionYaw,
                        highPrecisionPitch,
                        packet.targetEntityId,
                        targetEntity,
                    )
                EventDispatcher.dispatch(entityClickEvent)
            }
        }

        when (action) {
            ClickAction.PRESS -> {
                val pressEvent =
                    PlayerPressEvent(
                        this,
                        button,
                        action,
                        currentLevel,
                        highPrecisionYaw,
                        highPrecisionPitch,
                    )
                EventDispatcher.dispatch(pressEvent)
            }

            ClickAction.RELEASE -> {
                val releaseEvent =
                    PlayerReleaseEvent(
                        this,
                        button,
                        currentLevel,
                        highPrecisionYaw,
                        highPrecisionPitch,
                    )
                EventDispatcher.dispatch(releaseEvent)
            }
        }
    }

    // endregion
    internal fun handleNotifyAction(packet: ClientNotifyAction) {
        val currentLevel = level ?: return

        when (packet.action) {
            0.toShort() -> {
                val blockSelectedEvent =
                    PlayerBlockListSelectedEvent(
                        this,
                        currentLevel,
                        packet.value,
                    )
                EventDispatcher.dispatch(blockSelectedEvent)
            }

            1.toShort() -> {
                val isOpened = packet.value == 1.toShort()
                val blockToggleEvent =
                    PlayerBlockListToggledEvent(this, currentLevel, isOpened)
                EventDispatcher.dispatch(blockToggleEvent)

                if (
                    blockToggleEvent.isCancelled && supports("ToggleBlockList")
                ) {
                    val oppositeToggle =
                        if (isOpened) 0.toByte() else 1.toByte()
                    ServerToggleBlockList(oppositeToggle).send(channel)
                }
            }

            2.toShort() -> {
                val levelSavedEvent = PlayerLevelSavedEvent(this, currentLevel)
                EventDispatcher.dispatch(levelSavedEvent)
            }

            5.toShort() -> {
                val texturePackEvent =
                    PlayerTexturePackChangedEvent(
                        this,
                        currentLevel,
                        packet.value,
                    )
                EventDispatcher.dispatch(texturePackEvent)
            }

            6.toShort() -> {
                val texturePromptEvent =
                    PlayerTexturePromptRespondedEvent(
                        this,
                        currentLevel,
                        packet.value,
                    )
                EventDispatcher.dispatch(texturePromptEvent)
            }

            7.toShort() -> {
                val enabled = packet.value == 1.toShort()
                val thirdPersonEvent =
                    PlayerThirdPersonChangedEvent(this, currentLevel, enabled)
                EventDispatcher.dispatch(thirdPersonEvent)
            }
        }
    }

    internal fun handleNotifyPositionAction(
        packet: ClientNotifyPositionAction
    ) {
        val currentLevel = level ?: return

        when (packet.action) {
            3.toShort() -> {
                val respawnEvent =
                    PlayerRespawnedEvent(
                        this,
                        currentLevel,
                        packet.x,
                        packet.y,
                        packet.z,
                    )
                EventDispatcher.dispatch(respawnEvent)
            }

            4.toShort() -> {
                val oldSpawn = spawnPosition
                spawnPosition =
                    Position(
                        packet.x.toFloat(),
                        packet.y.toFloat(),
                        packet.z.toFloat(),
                    )

                val spawnUpdateEvent =
                    PlayerSpawnUpdatedEvent(
                        this,
                        currentLevel,
                        packet.x,
                        packet.y,
                        packet.z,
                        oldSpawn.x.toInt().toShort(),
                        oldSpawn.y.toInt().toShort(),
                        oldSpawn.z.toInt().toShort(),
                    )
                EventDispatcher.dispatch(spawnUpdateEvent)

                if (spawnUpdateEvent.isCancelled) {
                    setSpawnPoint(oldSpawn)
                    spawnPosition = oldSpawn
                } else {
                    setSpawnPoint(spawnPosition)
                }
            }
        }
    }

    // endregion
    // region Cinematic GUI Management
    fun setCinematicGui(
        hideCrosshair: Boolean = false,
        hideHotbar: Boolean = false,
        hideHand: Boolean = false,
        red: Int,
        green: Int,
        blue: Int,
        opacity: Int = 255,
        apertureSize: Int = 0,
    ) {
        if (supports("CinematicGui")) {
            ServerCinematicGui(
                    hideCrosshair,
                    hideHotbar,
                    hideHand,
                    red.toByte(),
                    green.toByte(),
                    blue.toByte(),
                    opacity.toByte(),
                    apertureSize.toShort(),
                )
                .send(channel)
        }
    }

    fun setCinematicGui(
        hideCrosshair: Boolean = false,
        hideHotbar: Boolean = false,
        hideHand: Boolean = false,
        color: Color,
        opacity: Int = 255,
        apertureSize: Int = 0,
    ) {
        setCinematicGui(
            hideCrosshair,
            hideHotbar,
            hideHand,
            color.red.toInt(),
            color.green.toInt(),
            color.blue.toInt(),
            opacity,
            apertureSize,
        )
    }

    fun resetCinematicGui() {
        setCinematicGui(
            hideCrosshair = false,
            hideHotbar = false,
            hideHand = false,
            red = 0,
            green = 0,
            blue = 0,
            opacity = 0,
            apertureSize = 0,
        )
    }

    // endregion

    // region misc
    fun setHotKey(
        label: String,
        action: String,
        keyCode: Int,
        keyAddCtrl: Boolean,
        keyAddShift: Boolean,
        keyAddAlt: Boolean,
    ) {
        if (!supports("TextHotKey")) return

        ServerSetTextHotKey(
                label,
                action,
                keyCode,
                keyAddCtrl,
                keyAddShift,
                keyAddAlt,
            )
            .send(channel)
    }

    // endRegion

    companion object {
        fun find(name: String): Player? = Players.find(name)

        fun getAllPlayers(): List<Player> = Players.getAllPlayers()

        fun getPlayerCount(): Int = Players.count()

        fun getPermissions(name: String): List<String> =
            Players.getPermissions(name)

        fun setPermission(
            name: String,
            permission: String,
            value: Boolean,
        ): Boolean = Players.setPermission(name, permission, value)

        fun hasPermission(name: String, permission: String): Boolean =
            Players.hasPermission(name, permission)

        fun hasPermission(
            name: String,
            permission: String,
            default: Boolean = false,
        ): Boolean = Players.hasPermission(name, permission, default)

        fun addGroup(name: String, group: String): Boolean =
            Players.addGroup(name, group)

        fun removeGroup(name: String, group: String): Boolean =
            Players.removeGroup(name, group)
    }
}
