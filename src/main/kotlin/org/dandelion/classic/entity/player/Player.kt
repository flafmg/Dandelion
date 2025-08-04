package org.dandelion.classic.entity.player
import io.netty.channel.Channel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dandelion.classic.blocks.model.Block
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.entity.Entity
import org.dandelion.classic.events.PlayerBlockInteractionEvent
import org.dandelion.classic.events.PlayerChangeLevel
import org.dandelion.classic.events.PlayerMoveEvent
import org.dandelion.classic.events.PlayerSendMessageEvent
import org.dandelion.classic.events.manager.EventDispatcher
import org.dandelion.classic.level.Level
import org.dandelion.classic.level.Levels
import org.dandelion.classic.network.packets.classic.server.*
import org.dandelion.classic.network.packets.cpe.server.ServerClickDistance
import org.dandelion.classic.network.packets.cpe.server.ServerHackControl
import org.dandelion.classic.network.packets.cpe.server.ServerHoldThis
import org.dandelion.classic.network.packets.cpe.server.ServerSetBlockPermission
import org.dandelion.classic.network.packets.cpe.server.ServerSetHotbar
import org.dandelion.classic.network.packets.cpe.server.ServerSetSpawnpoint
import org.dandelion.classic.permission.PermissionRepository
import org.dandelion.classic.server.Console
import org.dandelion.classic.server.MessageRegistry
import org.dandelion.classic.types.MessageType
import org.dandelion.classic.types.Position
import org.dandelion.classic.util.toFShort
import org.jetbrains.annotations.Blocking
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
    var client: String,
    name: String,
    levelId: String = "",
    entityId: Byte = -1,
    position: Position = Position(0f, 0f, 0f, 0f, 0f),
    val info: PlayerInfo = PlayerInfo.getOrCreate(name),

) : Entity(name, levelId, entityId, position), CommandExecutor {
    var displayName: String = name

    var supportsCpe: Boolean = false
    private val supportedCPE = mutableListOf<Pair<String, Int>>()
    internal var supportedCpeCount: Short = 0

    var heldBlock: Byte = 0x00
    var canFly: Boolean = true
    var canNoClip: Boolean = true
    var canSpeed: Boolean = true
    var canSpawnControl: Boolean = true
    var canThirdPerson: Boolean = true
    var jumpHeight: Short = -1
    var clickDistance: Short = 160

    var motd: String = ""

    override val permissions: List<String>
        get() = PermissionRepository.getPermissionList(name)

    private val MAX_MESSAGE_LENGTH = 64
    private val LEVEL_DATA_CHUNK_SIZE = 1024
    private val COLOR_CODE_REGEX = "&[0-9a-fA-F]"


    //region cpe support
    fun addCPE(name: String, version: Int) {
        if (!supportedCPE.any { it.first == name && it.second == version }) {
            supportedCPE.add(name to version)
        }
    }

    fun addCPE(name: String) {
        addCPE(name, 1)
    }
    fun supports(name: String, version: Int? = null): Boolean {
        return if (version == null) {
            supportedCPE.any { it.first == name }
        } else {
            supportedCPE.any { it.first == name && it.second == version }
        }
    }

    fun removeCPE(name: String, version: Int) {
        supportedCPE.removeIf { it.first == name && it.second == version }
    }

    fun getCPE(): List<Pair<String, Int>> {
        return supportedCPE.toList()
    }
    //endregion

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
     * Sends a message to the player with specified message type
     *
     * @param message The message string to send.
     * @param messageType the [MessageType] of the message. Defaults to [MessageType.Chat].
     */
    fun sendMessage(message: String, messageType: MessageType = MessageType.Chat) {
        sendMessage(message, messageType.code.toByte())
    }
    /**
     * Sends a message to the player with specified message type ID
     *
     * @param message The message string to send.
     * @param messageTypeId An optional byte identifier for the type of message. Defaults to `0x00`.
     */
    fun sendMessage(message: String, messageTypeId: Byte = 0x00) {
        if (messageTypeId == 0x00.toByte()) {
        val messageChunks = splitMessageIntoChunks(message)
        messageChunks.forEach { chunk ->
            ServerMessage(messageTypeId, chunk).send(channel)
        }
        } else {
            ServerMessage(messageTypeId, message).send(channel)
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
        Players.handleDisconnection(channel)
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
            sendMessage(MessageRegistry.Server.Level.getFull())
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
        if(notifyJoin) Players.notifyJoinedLevel(this, level)
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
        level.sendEnv(this)
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

        val message = message.replace("%", "&")
        val event = PlayerSendMessageEvent(this, message)
        EventDispatcher.dispatch(event)
        if(event.isCancelled) return

        val messageFormat = MessageRegistry.Server.Chat.getPlayerFormat(this, message)
        val consoleFormat = MessageRegistry.Server.Chat.getConsoleFormat(this, message)

        Console.log(consoleFormat)
        if (message.startsWith("/")) {
            sendCommand(message)
            return
        }
        Levels.broadcast(messageFormat)
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
    override fun interactWithBlock(x: Short, y: Short, z: Short, blockType: Byte, isDestroying: Boolean) {
        val currentLevel = level ?: return
        if (!isWithinInteractionRange(x.toFloat(), y.toFloat(), z.toFloat(), clickDistance / 32.0f)) {
            return
        }

        val finalBlockType = if (isDestroying) Block.get(0)?.id ?: 0 else blockType
        val blockAtPos = Block.get(currentLevel.getBlock(x, y, z))

        if(Block.get(finalBlockType) == null){
            return
        }

        if (this is Player) {
            val event = PlayerBlockInteractionEvent(
                this,
                blockAtPos!!,
                Block.get( finalBlockType)!!,
                Position(x.toFloat(), y.toFloat(), z.toFloat()),
                level!!
            )
            EventDispatcher.dispatch(event)
            if(event.isCancelled){
                ServerSetBlock(x, y, z, blockAtPos.id).send(this)
                return
            }
        }

        currentLevel.setBlock(x, y, z, finalBlockType)
        broadcastBlockUpdate(x, y, z, finalBlockType)
    }

    override fun updateBlock(x: Short, y: Short, z: Short, block: Byte) {
        ServerSetBlock(x, y, z, block).send(this)
    }

    /**
     * Sets the held block for this player and optionally prevents changes
     *
     * @param block The block type ID (Byte) to set as the held block.
     * @param preventChange Whether to prevent changes to the held block. Defaults to `false`
     */
    fun setHeldBlock(block: Byte, preventChange: Boolean = false) {
        if (supports("HeldBlock")) {
            this.heldBlock = block
            val preventChange: Byte = if (preventChange) 1.toByte() else 0.toByte()
            ServerHoldThis(block, preventChange).send(channel)
        }
    }

    /**
     * Sets a block in the player's hotbar at the specified index
     *
     * @param block The block type ID (Byte) to set in the hotbar.
     * @param index The index in the hotbar (0-8) where the block should be set.
     */
    fun setHotbarBlock(block: Byte, index: Byte){
        if(supports("SetHotbar")){
            ServerSetHotbar(block, index).send(channel)
        }
    }

    internal fun updateHeldBlock(block: Byte) {
        if(supports("HeldBlock"))
            this.heldBlock = block
    }
    //endregion

    //region hack control

    /**
     * Enables or disables flying for the player and updates the hack control state.
     *
     * @param canFly true to allow flying, false to disallow.
     */
    fun setCanFly(canFly: Boolean) {
        this.canFly = canFly
        if(!supports("HackControl")) return
        ServerHackControl(canFly, canNoClip, canSpeed, canSpawnControl, canThirdPerson, jumpHeight).send(channel)
    }

    /**
     * Enables or disables noclip for the player and updates the hack control state.
     *
     * @param canNoClip true to allow noclip, false to disallow.
     */
    fun setCanNoClip(canNoClip: Boolean) {
        this.canNoClip = canNoClip
        if(!supports("HackControl")) return
        ServerHackControl(canFly, canNoClip, canSpeed, canSpawnControl, canThirdPerson, jumpHeight).send(channel)
    }

    /**
     * Enables or disables speed for the player and updates the hack control state.
     *
     * @param canSpeed true to allow speed, false to disallow.
     */
    fun setCanSpeed(canSpeed: Boolean) {
        this.canSpeed = canSpeed
        if(!supports("HackControl")) return
        ServerHackControl(canFly, canNoClip, canSpeed, canSpawnControl, canThirdPerson, jumpHeight).send(channel)
    }

    /**
     * Enables or disables spawn control for the player and updates the hack control state.
     *
     * @param canSpawnControl true to allow spawn control, false to disallow.
     */
    fun setCanSpawnControl(canSpawnControl: Boolean) {
        this.canSpawnControl = canSpawnControl
        if(!supports("HackControl")) return
        ServerHackControl(canFly, canNoClip, canSpeed, canSpawnControl, canThirdPerson, jumpHeight).send(channel)
    }

    /**
     * Enables or disables third person view for the player and updates the hack control state.
     *
     * @param canThirdPerson true to allow third person, false to disallow.
     */
    fun setCanThirdPerson(canThirdPerson: Boolean) {
        this.canThirdPerson = canThirdPerson
        if(!supports("HackControl")) return
        ServerHackControl(canFly, canNoClip, canSpeed, canSpawnControl, canThirdPerson, jumpHeight).send(channel)
    }

    /**
     * Sets the jump height for the player and updates the hack control state.
     *
     * @param jumpHeight the new jump height value.
     */
    fun setJumpHeight(jumpHeight: Short) {
        this.jumpHeight = jumpHeight
        if(!supports("HackControl")) return
        ServerHackControl(canFly, canNoClip, canSpeed, canSpawnControl, canThirdPerson, jumpHeight).send(channel)
    }

    fun setClickDistance(distance: Short) {
        clickDistance = distance
        if(!supports("ClickDistance")) return
        ServerClickDistance(distance).send(channel)
    }

    //endregion

    /**
     * Sets the spawn point for this player at the specified position.
     *
     * @param position The [Position] to set as the spawn point.
     */
    fun setSpawnPoint(position: Position) {
        setSpawnPoint(position.x.toInt().toShort(),
            position.y.toInt().toShort(), position.z.toInt().toShort(),
            position.yaw.toInt().toByte(), position.pitch.toInt().toByte()
        )
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
    fun setSpawnPoint(x: Short, y: Short, z: Short, yaw: Byte, pitch: Byte) {
        if(!supports("SetSpawnpoint")) return
        ServerSetSpawnpoint(x, y, z, yaw, pitch).send(channel)
    }

    /**
     * sets this players motd
     *
     * @param motd the mots to set
     */
    fun setMotd(motd: String){
        this.motd = motd
        ServerIdentification(serverMotd = motd).send(channel)
        if(!supports("InstantMOTD")){
            if(level == null) return
            joinLevel(level!!, false)
        }

    }

    //region permissions and Groups

    fun setBlockPermission(blockType: Byte, allowPlacement: Boolean, allowDeletion: Boolean) {
        ServerSetBlockPermission(blockType, allowPlacement, allowDeletion).send(channel)
    }

    /**
     * Sets a permission for this player.
     *
     * @param permission the permission string
     * @param value true to grant, false to deny
     * @return true if the permission was set
     */
    fun setPermission(permission: String, value: Boolean): Boolean = Players.setPermission(this.name, permission, value)

    /**
     * Checks if this player has a specific permission.
     *
     * @param permission the permission string
     * @return true if the player has the permission
     */
    override fun hasPermission(permission: String): Boolean {
        return if(info.isOperator) true else super.hasPermission(permission)
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
    fun removeGroup(group: String): Boolean = Players.removeGroup(this.name, group)
    //endregion
    companion object {
        /**
         * Finds a player by name (case-insensitive).
         * @param name The name of the player to find.
         * @return The [Player] instance if found, or null if not found.
         */
        fun find(name: String): Player? = Players.find(name)

        /**
         * Gets all connected players.
         * @return A list of all [Player] instances currently connected.
         */
        fun getAllPlayers(): List<Player> = Players.getAllPlayers()

        /**
         * Gets the number of connected players.
         * @return The count of currently connected players.
         */
        fun getPlayerCount(): Int = Players.count()

        /**
         * Gets the permissions of a player
         *
         * @param name the requested player name
         * @return a list containing all player permissions
         */
        fun getPermissions(name: String): List<String> = Players.getPermissions(name)

        /**
         * Sets a permission for a player by name.
         *
         * @param name the player name
         * @param permission the permission string
         * @param value true to grant, false to deny
         * @return true if the permission was set
         */
        fun setPermission(name: String, permission: String, value: Boolean): Boolean =
            Players.setPermission(name, permission, value)

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
         * Adds a group to a player by name.
         *
         * @param name the player name
         * @param group the group name to add
         * @return true if the group was added
         */
        fun addGroup(name: String, group: String): Boolean = Players.addGroup(name, group)

        /**
         * Removes a group from a player by name.
         *
         * @param name the player name
         * @param group the group name to remove
         * @return true if the group was removed
         */
        fun removeGroup(name: String, group: String): Boolean = Players.removeGroup(name, group)
    }
}