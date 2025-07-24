package org.dandelion.classic.player

import io.netty.channel.Channel
import org.dandelion.classic.events.PlayerConnectEvent
import org.dandelion.classic.events.PlayerPreConnectEvent
import org.dandelion.classic.events.manager.EventDispatcher
import org.dandelion.classic.level.Level
import org.dandelion.classic.level.Levels
import org.dandelion.classic.network.packets.classic.client.ClientIdentification
import org.dandelion.classic.network.packets.classic.server.ServerDisconnectPlayer
import org.dandelion.classic.network.packets.classic.server.ServerIdentification
import org.dandelion.classic.server.Console
import org.dandelion.classic.server.ServerInfo
import java.security.MessageDigest

/**
 * object responsible for managing all connected players.
 * Handles player connections, disconnections, authentication, and global player operations.
 */
object Players {
    private const val EXPECTED_PROTOCOL_VERSION: Byte = 0x07
    private const val MD5_ALGORITHM = "MD5"
    private const val HEX_FORMAT = "%02x"

    //region Connection Management

    /**
     * Handles the pre-connection phase including protocol validation and authentication
     *
     * @param clientInfo The [ClientIdentification] packet containing client information.
     * @param channel The Netty [Channel] for the connecting client.
     */
    internal fun handlePreConnection(clientInfo: ClientIdentification, channel: Channel) {
        ServerIdentification().send(channel)

        if (!isValidProtocolVersion(clientInfo.protocolVersion)) {
            disconnectWithInvalidProtocol(clientInfo.protocolVersion, channel)
            return
        }

        if (!authenticateUser(clientInfo, channel)) {
            return
        }

        val event = PlayerPreConnectEvent(
            clientInfo.protocolVersion,
            clientInfo.userName,
            clientInfo.verificationKey,
            clientInfo.unused,
        )
        EventDispatcher.dispatch(event)
        if(event.isCancelled){
            ServerDisconnectPlayer("Disconnected").send(channel)
            return;
        }
        val player = createPlayerFromClientInfo(clientInfo, channel)
        player.levelId = Levels.getDefaultLevelId()
        attemptPlayerConnection(player)
    }

    /**
     * Attempts to connect a player after all validations
     *
     * @param player The [Player] attempting to connect.
     */
    private fun attemptPlayerConnection(player: Player) {
        when (val connectionResult = validatePlayerConnection(player)) {
            is ConnectionResult.Success -> {
                finalizePlayerConnection(player)
            }
            is ConnectionResult.Failure -> {
                disconnectPlayerWithReason(player.channel, connectionResult.reason)
            }
        }
        val event = PlayerConnectEvent(player)
        EventDispatcher.dispatch(event)
    }

    /**
     * Finalizes successful player connection
     *
     * @param player The [Player] to finalize the connection for.
     */
    private fun finalizePlayerConnection(player: Player) {
        val joinLevel = Levels.getDefaultLevel()
        if (joinLevel == null) {
            disconnectPlayerWithReason(player.channel, "Default level not available")
            return
        }

        player.info.recordJoin()
        player.joinLevel(joinLevel)
        notifyPlayerJoined(player)
    }

    /**
     * Handles player disconnection and cleanup
     *
     * @param channel The Netty [Channel] of the disconnecting player.
     */
    internal fun handlePlayerDisconnection(channel: Channel) {
        val player = findPlayerByChannel(channel) ?: return
        val level = player.level ?: return

        level.removeEntity(player)
        player.info.recordDisconnect()
        notifyPlayerLeft(player)
    }

    //endregion

    //region Authentication & Validation

    /**
     * Validates the client's protocol version
     *
     * @param version The protocol version byte sent by the client.
     * @return `true` if the version is valid, `false` otherwise.
     */
    private fun isValidProtocolVersion(version: Byte): Boolean {
        return version == EXPECTED_PROTOCOL_VERSION
    }

    /**
     * Authenticates user if server verification is enabled
     *
     * @param clientInfo The [ClientIdentification] packet containing client information.
     * @param channel The Netty [Channel] for the connecting client.
     * @return `true` if authentication succeeds or is disabled, `false` otherwise.
     */
    private fun authenticateUser(clientInfo: ClientIdentification, channel: Channel): Boolean {
        if (!ServerInfo.verifyUsers) return true

        val expectedHash = generateUserVerificationHash(clientInfo.userName)
        if (clientInfo.verificationKey != expectedHash) {
            disconnectPlayerWithReason(channel, "Authentication failed - please log in properly")
            return false
        }

        return true
    }

    /**
     * Generates MD5 hash for user verification
     *
     * @param userName The username to generate the hash for.
     * @return The MD5 hash string.
     */
    private fun generateUserVerificationHash(userName: String): String {
        val messageDigest = MessageDigest.getInstance(MD5_ALGORITHM)
        val hashBytes = messageDigest.digest("${ServerInfo.salt}$userName".toByteArray())
        return hashBytes.joinToString("") { HEX_FORMAT.format(it) }
    }

    /**
     * Validates if a player can connect to the server
     *
     * @param player The [Player] to validate.
     * @return A [ConnectionResult] indicating success or failure with reason.
     */
    private fun validatePlayerConnection(player: Player): ConnectionResult {
        return when {
            player.info.isBanned -> {
                ConnectionResult.Failure("You are banned: ${player.info.banReason}")
            }
            isPlayerAlreadyConnected(player) -> {
                ConnectionResult.Failure("You are already connected to this server")
            }
            isServerFull() -> {
                ConnectionResult.Failure("The server is full")
            }
            else -> ConnectionResult.Success
        }
    }

    /**
     * Checks if player is already connected by channel or name
     *
     * @param player The [Player] to check for existing connections.
     * @return `true` if the player is already connected, `false` otherwise.
     */
    private fun isPlayerAlreadyConnected(player: Player): Boolean {
        return findPlayerByChannel(player.channel) != null ||
                getAllPlayers().any { it.name.equals(player.name, ignoreCase = true) }
    }

    /**
     * Checks if the server has reached maximum capacity
     *
     * @return `true` if the server is full, `false` otherwise.
     */
    private fun isServerFull(): Boolean {
        return count() >= ServerInfo.maxPlayers
    }

    /**
     * Creates a Player instance from client identification data
     *
     * @param clientInfo The [ClientIdentification] packet containing client information.
     * @param channel The Netty [Channel] for the connecting client.
     * @return A new [Player] instance.
     */
    private fun createPlayerFromClientInfo(clientInfo: ClientIdentification, channel: Channel): Player {
        return Player(channel, "Unknown", clientInfo.userName)
    }


    //endregion

    //region broadcasts

    /**
     * Notifies all players and console when a player joins
     *
     * @param player The [Player] who joined.
     */
    internal fun notifyPlayerJoined(player: Player) {
        Console.log("${player.name} joined the server")
        broadcastMessage("${player.name} joined the server")
    }

    /**
     * Notifies all players and console when a player leaves
     *
     * @param player The [Player] who left.
     */
    internal fun notifyPlayerLeft(player: Player) {
        Console.log("${player.name} left the server")
        broadcastMessage("${player.name} left the server")
    }

    /**
     * Notifies all players and console when a player joins a level
     *
     * @param player The [Player] who joined the level.
     * @param level The [Level] the player joined.
     */
    internal fun notifyPlayerJoinedLevel(player: Player, level: Level) {
        Console.log("${player.name} joined level ${level.id}")
        broadcastMessage("${player.name} joined level ${level.id}")
    }

    //endregion

    //region Player Lookup

    /**
     * Finds a player by their network channel
     *
     * @param channel The Netty [Channel] to search for.
     * @return The [Player] associated with the channel, or `null` if not found.
     */
    fun findPlayerByChannel(channel: Channel): Player? {
        return Levels.getAllPlayers().find { it.channel == channel }
    }

    /**
     * Finds a player by their username (case-insensitive)
     *
     * @param name The username string to search for.
     * @return The [Player] with the matching name, or `null` if not found.
     */
    fun findPlayerByName(name: String): Player? {
        return Levels.getAllPlayers().find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Gets all currently connected players
     *
     * @return A list of all connected [Player] instances.
     */
    fun getAllPlayers(): List<Player> {
        return Levels.getAllPlayers()
    }

    /**
     * Gets the current number of connected players
     *
     * @return The total count of connected players.
     */
    fun count(): Int {
        return Levels.getTotalPlayerCount()
    }

    //endregion

    //region Global Player Operations

    /**
     * Broadcasts a message to all connected players
     *
     * @param message The message string to broadcast.
     * @param messageTypeId An optional byte identifier for the type of message. Defaults to `0x00`.
     */
    fun broadcastMessage(message: String, messageTypeId: Byte = 0x00) {
        getAllPlayers().forEach { player ->
            player.sendMessage(message, messageTypeId)
        }
    }

    /**
     * Kicks all players from the server
     *
     * @param reason The reason for kicking all players. Defaults to "Server maintenance".
     */
    fun kickAllPlayers(reason: String = "Server maintenance") {
        getAllPlayers().forEach { player ->
            player.kick(reason)
        }
    }

    /**
     * Kicks a specific player by name
     *
     * @param name The name of the player to kick.
     * @param reason The reason for kicking the player. Defaults to "You have been kicked".
     */
    fun kickPlayerByName(name: String, reason: String = "You have been kicked") {
        findPlayerByName(name)?.kick(reason)
    }

    /**
     * Kicks a specific player instance
     *
     * @param player The [Player] instance to kick.
     * @param reason The reason for kicking the player. Defaults to "You have been kicked".
     */
    fun kickPlayer(player: Player, reason: String = "You have been kicked") {
        player.kick(reason)
    }

    /**
     * Bans a player by name
     *
     * @param name The name of the player to ban.
     * @param reason The reason for banning the player. Defaults to "You have been banned".
     */
    fun banPlayerByName(name: String, reason: String = "You have been banned") {
        findPlayerByName(name)?.ban(reason)
    }

    /**
     * Bans a specific player instance
     *
     * @param player The [Player] instance to ban.
     * @param reason The reason for banning the player. Defaults to "You have been banned".
     */
    fun banPlayer(player: Player, reason: String = "You have been banned") {
        player.ban(reason)
    }

    //endregion

    //region Utility Methods

    /**
     * Disconnects a player with invalid protocol version
     *
     * @param version The invalid protocol version byte.
     * @param channel The Netty [Channel] to disconnect.
     */
    private fun disconnectWithInvalidProtocol(version: Byte, channel: Channel) {
        val message = "Invalid protocol version ($version, expected $EXPECTED_PROTOCOL_VERSION)"
        ServerDisconnectPlayer(message).send(channel)
    }

    /**
     * Disconnects a player with a specific reason
     *
     * @param channel The Netty [Channel] to disconnect.
     * @param reason The reason for disconnection.
     */
    private fun disconnectPlayerWithReason(channel: Channel, reason: String) {
        ServerDisconnectPlayer(reason).send(channel)
    }

    //endregion

    /**
     * Sealed class representing connection attempt results
     */
    private sealed class ConnectionResult {
        object Success : ConnectionResult()
        data class Failure(val reason: String) : ConnectionResult()
    }
}