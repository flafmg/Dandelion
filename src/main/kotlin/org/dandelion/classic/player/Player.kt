package org.dandelion.classic.player
import io.netty.channel.Channel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.events.PlayerChangeLevel
import org.dandelion.classic.events.PlayerMoveEvent
import org.dandelion.classic.events.PlayerSendMessageEvent
import org.dandelion.classic.events.manager.EventDispatcher
import org.dandelion.classic.level.Level
import org.dandelion.classic.network.packets.classic.server.*
import org.dandelion.classic.server.Console
import org.dandelion.classic.types.Position
import org.dandelion.classic.util.toFShort
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
/**
 * Represents a connected player in the game world.
 * Extends Entity with player-specific functionality
 *
 * @property channel The Netty [Channel] used for network communication with this player.
 * @property client The client software identifier string.
 * @property name The name of the player.
 * @property levelId The ID of the level the player is associated with.
 * @property entityId The unique byte identifier for this player within its level.
 * @property position The current [Position] of the player.
 * @property info The [PlayerInfo] object containing persistent player data.
 * @property permissions The list of permission strings granted to this player.
 */
class Player(
    val channel: Channel,
    val client: String,
    name: String,
    levelId: String = "",
    entityId: Byte = -1,
    position: Position = Position(0f, 0f, 0f, 0f, 0f),
    val info: PlayerInfo = PlayerInfo.getOrCreate(name),
    override val permissions: List<String> = listOf(),
) : Entity(name, levelId, entityId, position), CommandExecutor {
    private val MAX_MESSAGE_LENGTH = 64
    private val LEVEL_DATA_CHUNK_SIZE = 1024
    private val COLOR_CODE_REGEX = "&[0-9a-fA-F]"
    //region message system

    /**
     * Sends a message to the player
     *
     * @param message The message string to send.
     */
    override fun sendMessage(message: String) {
        sendMessage(message, 0x00)
    }
    /**
     * Sends a message to the player with specified message type ID
     *
     * @param message The message string to send.
     * @param messageTypeId An optional byte identifier for the type of message. Defaults to `0x00`.
     */
    fun sendMessage(message: String, messageTypeId: Byte = 0x00) {
        val messageChunks = splitMessageIntoChunks(message)
        messageChunks.forEach { chunk ->
            ServerMessage(messageTypeId, chunk).send(channel)
        }
    }
    /**
     * Splits long messages into chunks while preserving color codes and word boundaries
     *
     * @param message The message string to split.
     * @param maxLength The maximum length for each chunk. Defaults to [MAX_MESSAGE_LENGTH].
     * @return A list of message chunk strings.
     */
    private fun splitMessageIntoChunks(message: String, maxLength: Int = MAX_MESSAGE_LENGTH): List<String> {
        if (message.length <= maxLength) {
            return listOf(message)
        }
        val chunks = mutableListOf<String>()
        var remainingText = message
        var lastColorCode = ""
        while (remainingText.length > maxLength) {
            val splitIndex = findOptimalSplitIndex(remainingText, maxLength)
            val currentChunk = remainingText.substring(0, splitIndex)
            lastColorCode = extractLastColorCode(currentChunk)
            chunks.add(currentChunk)
            remainingText = prepareContinuationText(remainingText, splitIndex, lastColorCode)
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
        val colorCodeRegex = COLOR_CODE_REGEX.toRegex()
        val colorMatches = colorCodeRegex.findAll(text)
        return if (colorMatches.any()) colorMatches.last().value else ""
    }
    /**
     * Prepares the continuation text for the next chunk with proper color code handling
     *
     * @param text The original text string.
     * @param splitIndex The index where the text was split.
     * @param lastColorCode The last color code found in the previous chunk.
     * @return The prepared continuation text string.
     */
    private fun prepareContinuationText(text: String, splitIndex: Int, lastColorCode: String): String {
        val hasSpaceSplit = text.substring(0, splitIndex).contains(' ')
        val continuationStart = if (hasSpaceSplit) splitIndex + 1 else splitIndex
        var continuation = if (continuationStart < text.length) {
            text.substring(continuationStart)
        } else {
            ""
        }
        if (lastColorCode.isNotEmpty() && continuation.isNotEmpty() && !continuation.startsWith("&")) {
            continuation = lastColorCode + continuation
        }
        return continuation
    }
    //endregion
    //region Player Management
    /**
     * Kicks the player from the server with a specified reason
     *
     * @param reason The reason for kicking the player. Defaults to "You have been kicked".
     */
    fun kick(reason: String = "You have been kicked") {
        ServerDisconnectPlayer(reason).send(channel)
        Players.handlePlayerDisconnection(channel)
    }
    /**
     * Bans the player and kicks them from the server
     *
     * @param reason The reason for banning the player. Defaults to "No reason provided".
     */
    fun ban(reason: String = "No reason provided") {
        info.setBanned(reason)
        kick("You are banned: $reason")
    }
    //endregion
    //region Position Management
    /**
     * Updates player position and sends the update to the player's client
     *
     * @param x The new X coordinate (Float).
     * @param y The new Y coordinate (Float).
     * @param z The new Z coordinate (Float).
     * @param yaw The new yaw rotation (Float).
     * @param pitch The new pitch rotation (Float).
     */
    override fun teleportTo(x: Float, y: Float, z: Float, yaw: Float, pitch: Float) {
        super.teleportTo(x, y, z, yaw, pitch)
        ServerSetPositionAndOrientation(-1, x, y, z, yaw.toInt().toByte(), pitch.toInt().toByte()).send(channel)
    }
    /**
     * Handles player movement with event system integration and cancellation support
     *
     * @param newX The new X coordinate (Float).
     * @param newY The new Y coordinate (Float).
     * @param newZ The new Z coordinate (Float).
     * @param newYaw The new yaw rotation (Float).
     * @param newPitch The new pitch rotation (Float).
     * @param forceAbsolute Whether to force an absolute position update. Defaults to `false`.
     */
    override fun updatePositionAndOrientation(
        newX: Float,
        newY: Float,
        newZ: Float,
        newYaw: Float,
        newPitch: Float,
        forceAbsolute: Boolean
    ) {
        val newPosition = Position(newX, newY, newZ, newYaw, newPitch)
        if (this.position == newPosition) {
            return
        }
        val moveEvent = PlayerMoveEvent(this, this.position, newPosition)
        EventDispatcher.dispatch(moveEvent)
        if (moveEvent.isCancelled) {
            rejectMovement(moveEvent.from)
        } else {
            super.updatePositionAndOrientation(newX, newY, newZ, newYaw, newPitch, forceAbsolute)
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
            originalPosition.pitch.toInt().toByte()
        ).send(channel)
        position.set(originalPosition.x, originalPosition.y, originalPosition.z, originalPosition.yaw, originalPosition.pitch)
    }
    //endregion
    //region Level Management
    /**
     * Transfers the player to a new level with full level data transmission
     *
     * @param level The [Level] to transfer the player to.
     * @param notifyJoin Whether to notify other players about the level join. Defaults to `false`.
     */
    @OptIn(DelicateCoroutinesApi::class)
    override fun joinLevel(level: Level, notifyJoin: Boolean) {
        initializeLevelTransfer()
        if (!level.tryAddEntity(this)) {
            sendMessage("Level is full")
            return
        }
        if(this.level != null) {
            val event = PlayerChangeLevel(this, this.level!!, level)
            EventDispatcher.dispatch(event)
            if(event.isCancelled){
                return
            }
        }
        this.level = level
        if(notifyJoin) Players.notifyPlayerJoinedLevel(this, level)
        GlobalScope.launch {
            transmitLevelData(level)
        }
    }
    /**
     * Initializes the level transfer process
     */
    private fun initializeLevelTransfer() {
        ServerLevelInitialize().send(channel)
    }
    /**
     * Transmits complete level data to the player in compressed chunks
     *
     * @param level The [Level] whose data should be transmitted.
     */
    private suspend fun transmitLevelData(level: Level) {
        val compressedLevelData = compressLevelData(level)
        sendLevelDataInChunks(compressedLevelData)
        finalizeLevelTransfer(level)
    }
    /**
     * Compresses level block data with size prefix
     *
     * @param level The [Level] whose block data should be compressed.
     * @return The compressed byte array containing the prefixed block data.
     */
    private fun compressLevelData(level: Level): ByteArray {
        val blockData = level.blocks
        val prefixedData = createPrefixedBlockData(blockData)
        return ByteArrayOutputStream().use { outputStream ->
            GZIPOutputStream(outputStream).use { gzipStream ->
                gzipStream.write(prefixedData)
            }
            outputStream.toByteArray()
        }
    }
    /**
     * Creates block data with 4-byte size prefix
     *
     * @param blockData The raw block data byte array.
     * @return A new byte array with the 4-byte size prefix prepended.
     */
    private fun createPrefixedBlockData(blockData: ByteArray): ByteArray {
        val totalLength = blockData.size
        val prefixedData = ByteArray(4 + totalLength)
        prefixedData[0] = (totalLength shr 24).toByte()
        prefixedData[1] = (totalLength shr 16).toByte()
        prefixedData[2] = (totalLength shr 8).toByte()
        prefixedData[3] = totalLength.toByte()
        System.arraycopy(blockData, 0, prefixedData, 4, totalLength)
        return prefixedData
    }
    /**
     * Sends compressed level data in manageable chunks with progress updates
     *
     * @param compressedData The compressed level data byte array to send.
     */
    private fun sendLevelDataInChunks(compressedData: ByteArray) {
        for (chunkStart in compressedData.indices step LEVEL_DATA_CHUNK_SIZE) {
            val remainingBytes = compressedData.size - chunkStart
            val chunkSize = remainingBytes.coerceAtMost(LEVEL_DATA_CHUNK_SIZE)
            val chunk = ByteArray(chunkSize)
            System.arraycopy(compressedData, chunkStart, chunk, 0, chunkSize)
            val progressPercent = calculateTransferProgress(chunkStart, chunkSize, compressedData.size)
            ServerLevelDataChunk(chunkSize.toShort(), chunk, progressPercent).send(channel)
        }
    }
    /**
     * Calculates the transfer progress percentage
     *
     * @param chunkStart The starting index of the current chunk.
     * @param chunkSize The size of the current chunk.
     * @param totalSize The total size of the compressed data.
     * @return The progress percentage as a byte (0-100).
     */
    private fun calculateTransferProgress(chunkStart: Int, chunkSize: Int, totalSize: Int): Byte {
        return ((chunkStart + chunkSize).toFloat() / totalSize * 100f)
            .toInt()
            .coerceAtMost(100)
            .toByte()
    }
    /**
     * Finalizes the level transfer by positioning the player and completing the handshake
     *
     * @param level The [Level] the player has been transferred to.
     */
    private fun finalizeLevelTransfer(level: Level) {
        teleportTo(level.spawn)
        level.spawnPlayerInLevel(this)
        ServerLevelFinalize(level.size.x, level.size.y, level.size.z).send(channel)
    }

    //endregion

    //region Entity Spawning Overrides
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
    //endregion
    //region Communication
    /**
     * Handles player chat messages and commands
     *
     * @param message The message string sent by the player.
     */
    override fun sendMessageAs(message: String) {
        val event = PlayerSendMessageEvent(this, message)
        EventDispatcher.dispatch(event)
        if(event.isCancelled) return

        Console.log("[$levelId] $name: $message")
        if (message.startsWith("/")) {
            sendCommand(message)
            return
        }
        level?.broadcast("$name: &7$message")
    }
    //endregion
    //region Block Updates
    /**
     * Sends block update to this specific player
     *
     * @param x The X coordinate (Short) of the block to update.
     * @param y The Y coordinate (Short) of the block to update.
     * @param z The Z coordinate (Short) of the block to update.
     * @param block The new [Byte] block type ID.
     */
    override fun updateBlock(x: Short, y: Short, z: Short, block: Byte) {
        ServerSetBlock(x, y, z, block).send(channel)
    }
    //endregion
}