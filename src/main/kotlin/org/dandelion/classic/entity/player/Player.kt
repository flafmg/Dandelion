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

/**
 * Represents a connected player in the game world. Extends Entity with
 * player-specific functionality
 */
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

    /**
     * Sends a message to the player
     *
     * @param message The message string to send.
     */
    override fun sendMessage(message: String) {
        sendMessage(message, 0x00)
    }

    /**
     * Sends a message to the player with specified message type
     *
     * @param message The message string to send.
     * @param messageType the [MessageType] of the message. Defaults to
     *   [MessageType.Chat].
     */
    fun sendMessage(
        message: String,
        messageType: MessageType = MessageType.Chat,
    ) {
        sendMessage(message, messageType.code.toByte())
    }

    /**
     * Sends a message to the player with specified message type ID
     *
     * @param message The message string to send.
     * @param messageTypeId An optional byte identifier for the type of message.
     *   Defaults to `0x00`.
     */
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

    /**
     * Splits long messages into chunks while preserving color codes and word
     * boundaries
     *
     * @param message The message string to split.
     * @param maxLength The maximum length for each chunk. Defaults to
     *   [MAX_MESSAGE_LENGTH].
     * @return A list of message chunk strings.
     */
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

    /**
     * Finds the optimal index to split text, preferring word boundaries
     *
     * @param text The text string to find a split index for.
     * @param maxLength The maximum allowed length for the split.
     * @return The optimal split index.
     */
    private fun findOptimalSplitIndex(text: String, maxLength: Int): Int {
        val lastSpaceIndex = text.substring(0, maxLength).lastIndexOf(' ')
        return if (lastSpaceIndex > 0) lastSpaceIndex else maxLength
    }

    /**
     * Extracts the last color code from a text chunk
     *
     * @param text The text string to extract the color code from.
     * @return The last color code string found, or an empty string if none.
     */
    private fun extractLastColorCode(text: String): String {
        return COLOR_CODE_REGEX.toRegex().findAll(text).lastOrNull()?.value
            ?: ""
    }

    /**
     * Prepares the continuation text for the next chunk with proper color code
     * handling
     *
     * @param text The original text string.
     * @param splitIndex The index where the text was split.
     * @param lastColorCode The last color code found in the previous chunk.
     * @return The prepared continuation text string.
     */
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
    /**
     * Kicks the player from the server with a specified reason
     *
     * @param reason The reason for kicking the player. Defaults to "You have
     *   been kicked".
     */
    fun kick(reason: String = "You have been kicked") {
        ServerDisconnectPlayer(reason).send(channel)
        Players.handleDisconnection(channel)
    }

    /**
     * Bans the player and kicks them from the server
     *
     * @param reason The reason for banning the player. Defaults to "No reason
     *   provided".
     */
    fun ban(reason: String = "No reason provided") {
        info.setBanned(reason)
        kick("You are banned: $reason")
    }

    // endregion
    // region Position Management

    /**
     * Updates player position and sends the update to the player's client
     *
     * @param x The new X coordinate (Float).
     * @param y The new Y coordinate (Float).
     * @param z The new Z coordinate (Float).
     * @param yaw The new yaw rotation (Float).
     * @param pitch The new pitch rotation (Float).
     * @param moveMode the [MoveMode] to the teleport.
     * @param interpolateOrientation if orientation should be interpolated.
     */
    override fun teleportTo(
        x: Float,
        y: Float,
        z: Float,
        yaw: Float,
        pitch: Float,
        moveMode: MoveMode,
        interpolateOrientation: Boolean,
    ) {
        super.teleportTo(x, y, z, yaw, pitch, moveMode, interpolateOrientation)

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
            }
        }
    }

    /**
     * Handles player movement with event system integration and cancellation
     * support
     *
     * @param newX The new X coordinate (Float).
     * @param newY The new Y coordinate (Float).
     * @param newZ The new Z coordinate (Float).
     * @param newYaw The new yaw rotation (Float).
     * @param newPitch The new pitch rotation (Float).
     * @param forceAbsolute Whether to force an absolute position update.
     *   Defaults to `false`.
     */
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

    /**
     * Rejects player movement by sending them back to the original position
     *
     * @param originalPosition The [Position] to send the player back to.
     */
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

    /**
     * Transfers the player to a new level with full level data transmission
     *
     * @param level The [Level] to transfer the player to.
     * @param notifyJoin Whether to notify other players about the level join.
     *   Defaults to `false`.
     */
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
    /**
     * Spawns this player for another entity and vice versa
     *
     * @param other The other [Entity] to spawn mutually with this player.
     */
    override fun mutualSpawn(other: Entity) {
        this.spawnFor(other)
        other.spawnFor(this)
    }

    /**
     * Despawns this player for another entity and vice versa
     *
     * @param other The other [Entity] to despawn mutually with this player.
     */
    override fun mutualDespawn(other: Entity) {
        this.despawnFor(other)
        other.despawnFor(this)
    }

    // endregion
    // region Communication

    /** longer message packet */
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

    /**
     * Handles player chat messages and commands
     *
     * @param message The message string sent by the player.
     */
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
    /**
     * Sends block update to this specific player
     *
     * @param x The X coordinate (Short) of the block to update.
     * @param y The Y coordinate (Short) of the block to update.
     * @param z The Z coordinate (Short) of the block to update.
     * @param block The new [Byte] block type ID.
     */
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

    /**
     * Sets the held block for this player and optionally prevents changes
     *
     * @param block The block type ID (Byte) to set as the held block.
     * @param preventChange Whether to prevent changes to the held block.
     *   Defaults to `false`
     */
    fun setHeldBlock(block: UShort, preventChange: Boolean = false) {
        if (supports("HeldBlock")) {
            this.heldBlock = block
            val preventChange: Byte =
                if (preventChange) 1.toByte() else 0.toByte()
            ServerHoldThis(block, preventChange).send(channel)
        }
    }

    /**
     * Sets a block in the player's hotbar at the specified index
     *
     * @param block The block type ID (Byte) to set in the hotbar.
     * @param index The index in the hotbar (0-8) where the block should be set.
     */
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

    /** Indicates whether the player can fly */
    var canFly: Boolean = true
        /**
         * Sets whether the player can fly
         *
         * @param value true to allow flying, false to disable
         */
        set(value) {
            field = value
            updateHackControl()
        }

    /** Indicates whether the player can use noclip */
    var canNoClip: Boolean = true
        /**
         * Sets whether the player can use noclip
         *
         * @param value true to allow noclip, false to disable
         */
        set(value) {
            field = value
            updateHackControl()
        }

    /** Indicates whether the player can use speed */
    var canSpeed: Boolean = true
        /**
         * Sets whether the player can use speed
         *
         * @param value true to allow speed, false to disable
         */
        set(value) {
            field = value
            updateHackControl()
        }

    /** Indicates whether the player can use spawn control */
    var canSpawnControl: Boolean = true
        /**
         * Sets whether the player can use spawn control
         *
         * @param value true to allow spawn control, false to disable
         */
        set(value) {
            field = value
            updateHackControl()
        }

    /** Indicates whether the player can use third person view */
    var canThirdPerson: Boolean = true
        /**
         * Sets whether the player can use third person view
         *
         * @param value true to allow third person view, false to disable
         */
        set(value) {
            field = value
            updateHackControl()
        }

    /** The jump height value for the player */
    var jumpHeight: Short = -1
        /**
         * Sets the jump height value for the player
         *
         * @param value The jump height value
         */
        set(value) {
            field = value
            updateHackControl()
        }

    /** The maximum click distance for the player */
    var clickDistance: Short = 160
        /**
         * Sets the maximum click distance for the player
         *
         * @param value The maximum click distance
         */
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
        /**
         * Sets the spawn point for this player at the specified position.
         *
         * @param position The [Position] to set as the spawn point.
         */
        set(value) {
            setSpawnPoint(value.x, value.y, value.z, value.yaw, value.pitch)
            field = value
        }

    /**
     * Sets the spawn point for this player at the specified position.
     *
     * @param position The [Position] to set as the spawn point.
     */
    fun setSpawnPoint(position: Position) {
        spawnPosition = position
    }

    /**
     * Sets the spawn point for this player at the specified position.
     *
     * @param x The X coordinate (Short) of the spawn point.
     * @param y The Y coordinate (Short) of the spawn point.
     * @param z The Z coordinate (Short) of the spawn point.
     * @param yaw The yaw rotation (Byte) for the spawn point.
     * @param pitch The pitch rotation (Byte) for the spawn point.
     */
    fun setSpawnPoint(x: Float, y: Float, z: Float, yaw: Float, pitch: Float) {
        if (!supports("SetSpawnpoint")) return
        ServerSetSpawnpoint(x, y, z, yaw, pitch).send(channel)
    }

    /**
     * The message of the day (MOTD) for this player. It is showed when the
     * player joins a level
     */
    var motd: String = ServerConfig.motd
        /**
         * Sets the message of the day (MOTD) for this player
         *
         * @param value The MOTD string to set
         */
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

    /**
     * Sets a permission for this player.
     *
     * @param permission the permission string
     * @param value true to grant, false to deny
     * @return true if the permission was set
     */
    fun setPermission(permission: String, value: Boolean): Boolean =
        Players.setPermission(this.name, permission, value)

    /**
     * Checks if this player has a specific permission.
     *
     * @param permission the permission string
     * @return true if the player has the permission
     */
    override fun hasPermission(permission: String): Boolean {
        return super.hasPermission(permission)
    }
    /**
     * Checks if this player has a specific permission.
     *
     * @param permission the permission string
     * @param default the default permission if its not explicitly set
     * @return true if the player has the permission
     */
    fun hasPermission(permission: String, default: Boolean = false): Boolean {
        return Players.hasPermission(name, permission, default)
    }


    /**
     * Adds a group to this player.
     *
     * @param group the group name to add
     * @return true if the group was added
     */
    fun addGroup(group: String): Boolean = Players.addGroup(this.name, group)

    /**
     * Removes a group from this player.
     *
     * @param group the group name to remove
     * @return true if the group was removed
     */
    fun removeGroup(group: String): Boolean =
        Players.removeGroup(this.name, group)

    // endregion

    // region Velocity Management

    /**
     * Sets the velocity of this player using individual components
     *
     * @param velocityX The X velocity component. Scaled such that 10000 = 1.0
     *   velocity.
     * @param velocityY The Y velocity component. Scaled such that 10000 = 1.0
     *   velocity.
     * @param velocityZ The Z velocity component. Scaled such that 10000 = 1.0
     *   velocity.
     */
    fun setVelocity(velocityX: Int, velocityY: Int, velocityZ: Int) {
        if (supports("VelocityControl")) {
            ServerVelocityControl(velocityX, velocityY, velocityZ, 1, 1, 1)
                .send(channel)
        }
    }

    /**
     * Sets the velocity of this player using Float values
     *
     * @param velocityX The X velocity component as Float (1.0 = normal
     *   velocity).
     * @param velocityY The Y velocity component as Float (1.0 = normal
     *   velocity).
     * @param velocityZ The Z velocity component as Float (1.0 = normal
     *   velocity).
     */
    fun setVelocity(velocityX: Float, velocityY: Float, velocityZ: Float) {
        setVelocity(
            (velocityX * 10000).toInt(),
            (velocityY * 10000).toInt(),
            (velocityZ * 10000).toInt(),
        )
    }

    /**
     * Adds to the velocity of this player using individual components
     *
     * @param velocityX The X velocity component to add. Scaled such that 10000
     *   = 1.0 velocity.
     * @param velocityY The Y velocity component to add. Scaled such that 10000
     *   = 1.0 velocity.
     * @param velocityZ The Z velocity component to add. Scaled such that 10000
     *   = 1.0 velocity.
     */
    fun addVelocity(velocityX: Int, velocityY: Int, velocityZ: Int) {
        if (supports("VelocityControl")) {
            ServerVelocityControl(velocityX, velocityY, velocityZ, 0, 0, 0)
                .send(channel)
        }
    }

    /**
     * Adds to the velocity of this player using Float values
     *
     * @param velocityX The X velocity component to add as Float (1.0 = normal
     *   velocity).
     * @param velocityY The Y velocity component to add as Float (1.0 = normal
     *   velocity).
     * @param velocityZ The Z velocity component to add as Float (1.0 = normal
     *   velocity).
     */
    fun addVelocity(velocityX: Float, velocityY: Float, velocityZ: Float) {
        addVelocity(
            (velocityX * 10000).toInt(),
            (velocityY * 10000).toInt(),
            (velocityZ * 10000).toInt(),
        )
    }

    /**
     * Makes this player jump by setting Y velocity while preserving horizontal
     * velocity
     *
     * @param jumpHeight The jump height as Float (1.0 = normal jump height).
     */
    fun jump(jumpHeight: Float = 1.0f) {
        if (supports("VelocityControl")) {
            ServerVelocityControl(0, (jumpHeight * 10000).toInt(), 0, 0, 1, 0)
                .send(channel)
        }
    }

    /**
     * Applies knockback to this player in a specific direction
     *
     * @param velocityX The X velocity component for knockback.
     * @param velocityZ The Z velocity component for knockback.
     * @param upwardVelocity The Y velocity component for upward knockback.
     *   Defaults to 0.5f.
     */
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

    /**
     * Adds a selection to this player
     *
     * @param selection The [SelectionCuboid] to add
     * @return The assigned selection ID, or null if no ID is available
     *   (overflow)
     */
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

    /**
     * Removes a selection from this player
     *
     * @param selection The [SelectionCuboid] to remove
     */
    fun removeSelection(selection: SelectionCuboid) {
        removeSelection(selection.id)
    }

    /**
     * Removes a selection by ID from this player
     *
     * @param selectionId The ID of the selection to remove
     */
    fun removeSelection(selectionId: Byte) {
        if (
            supports("SelectionCuboid") &&
                activeSelections.containsKey(selectionId)
        ) {
            activeSelections.remove(selectionId)
            ServerRemoveSelection(selectionId).send(channel)
        }
    }

    /** Clears all selections for this player */
    fun clearSelections() {
        if (supports("SelectionCuboid")) {
            activeSelections.keys.forEach { selectionId ->
                ServerRemoveSelection(selectionId).send(channel)
            }
            activeSelections.clear()
            nextSelectionId = 0
        }
    }

    /**
     * Gets all active selections for this player
     *
     * @return A map of selection IDs to [SelectionCuboid] objects
     */
    fun getActiveSelections(): Map<Byte, SelectionCuboid> =
        activeSelections.toMap()

    // endregion
    // region Click Event Handling

    private val pressedButtons = mutableSetOf<MouseButton>()

    /**
     * Checks if a specific mouse button is currently pressed
     *
     * @param button The mouse button to check
     * @return true if the button is currently pressed
     */
    fun isPressingMouse(button: MouseButton): Boolean {
        return pressedButtons.contains(button)
    }

    /** Checks if the left mouse button is currently pressed */
    fun isPressingLeftMouse(): Boolean = isPressingMouse(MouseButton.LEFT)

    /** Checks if the right mouse button is currently pressed */
    fun isPressingRightMouse(): Boolean = isPressingMouse(MouseButton.RIGHT)

    /** Checks if the middle mouse button is currently pressed */
    fun isPressingMiddleMouse(): Boolean = isPressingMouse(MouseButton.MIDDLE)

    /**
     * Handles player click events received from the client
     *
     * @param packet The [ClientPlayerClick] packet containing click information
     */
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

    /**
     * Handles notify action events received from the client
     *
     * @param packet The [ClientNotifyAction] packet containing action
     *   information
     */
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

    /**
     * Handles notify position action events received from the client
     *
     * @param packet The [ClientNotifyPositionAction] packet containing position
     *   action information
     */
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

    /**
     * Sets the cinematic GUI properties for this player
     *
     * @param hideCrosshair Whether to hide the crosshair
     * @param hideHotbar Whether to hide the hotbar
     * @param hideHand Whether to hide the player's hand
     * @param red Red component of the cinematic bars (0-255)
     * @param green Green component of the cinematic bars (0-255)
     * @param blue Blue component of the cinematic bars (0-255)
     * @param opacity Alpha component of the cinematic bars (0-255)
     * @param apertureSize Aperture size for cinematic bars (0-65535)
     */
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

    /**
     * Sets the cinematic GUI properties for this player
     *
     * @param hideCrosshair Whether to hide the crosshair
     * @param hideHotbar Whether to hide the hotbar
     * @param hideHand Whether to hide the player's hand
     * @param color The color for the cinematic bars
     * @param opacity Alpha component of the cinematic bars (0-255)
     * @param apertureSize Aperture size for cinematic bars (0-65535)
     */
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

    /**
     * Resets the cinematic GUI to default values for this player Default
     * values: all UI elements visible, black bars with no opacity and no
     * aperture size
     */
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

    /** sets a hotkey for a player */
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
        /**
         * Finds a player by name (case-insensitive).
         *
         * @param name The name of the player to find.
         * @return The [Player] instance if found, or null if not found.
         */
        fun find(name: String): Player? = Players.find(name)

        /**
         * Gets all connected players.
         *
         * @return A list of all [Player] instances currently connected.
         */
        fun getAllPlayers(): List<Player> = Players.getAllPlayers()

        /**
         * Gets the number of connected players.
         *
         * @return The count of currently connected players.
         */
        fun getPlayerCount(): Int = Players.count()

        /**
         * Gets the permissions of a player
         *
         * @param name the requested player name
         * @return a list containing all player permissions
         */
        fun getPermissions(name: String): List<String> =
            Players.getPermissions(name)

        /**
         * Sets a permission for a player by name.
         *
         * @param name the player name
         * @param permission the permission string
         * @param value true to grant, false to deny
         * @return true if the permission was set
         */
        fun setPermission(
            name: String,
            permission: String,
            value: Boolean,
        ): Boolean = Players.setPermission(name, permission, value)

        /**
         * Checks if a player has a specific permission by name.
         *
         * @param name the player name
         * @param permission the permission string
         * @return true if the player has the permission
         */
        fun hasPermission(name: String, permission: String): Boolean =
            Players.hasPermission(name, permission)

        /**
         * Checks if a player has a specific permission by name.
         *
         * @param name the player name
         * @param permission the permission string
         * @param default the default to send if not explicitly set
         * @return true if the player has the permission
         */
        fun hasPermission(name: String, permission: String, default: Boolean = false): Boolean =
            Players.hasPermission(name, permission, default)

        /**
         * Adds a group to a player by name.
         *
         * @param name the player name
         * @param group the group name to add
         * @return true if the group was added
         */
        fun addGroup(name: String, group: String): Boolean =
            Players.addGroup(name, group)

        /**
         * Removes a group from a player by name.
         *
         * @param name the player name
         * @param group the group name to remove
         * @return true if the group was removed
         */
        fun removeGroup(name: String, group: String): Boolean =
            Players.removeGroup(name, group)
    }
}
