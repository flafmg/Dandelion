package org.dandelion.classic.entity.player

import io.netty.channel.Channel
import org.dandelion.classic.events.PlayerConnectEvent
import org.dandelion.classic.events.PlayerPreConnectEvent
import org.dandelion.classic.events.manager.EventDispatcher
import org.dandelion.classic.level.Level
import org.dandelion.classic.level.Levels
import org.dandelion.classic.network.PacketRegistry
import org.dandelion.classic.network.packets.classic.client.ClientIdentification
import org.dandelion.classic.network.packets.classic.server.ServerDisconnectPlayer
import org.dandelion.classic.network.packets.classic.server.ServerIdentification
import org.dandelion.classic.permission.Group
import org.dandelion.classic.permission.PermissionRepository
import org.dandelion.classic.server.Console
import org.dandelion.classic.server.ServerInfo
import org.dandelion.classic.server.MessageRegistry
import java.security.MessageDigest

/**
 * object responsible for managing all connected players.
 * Handles player connections, disconnections, authentication, and global player operations.
 */
object Players {
    private const val EXPECTED_PROTOCOL_VERSION: Byte = 0x07
    private const val MD5_ALGORITHM = "MD5"
    private const val HEX_FORMAT = "%02x"

    //hold connecting players while packetregistry resolves it's cpe support
    private val connectingPlayers: MutableMap<Channel, Player> = mutableMapOf()

    internal fun getConnecting(channel: Channel): Player?{
        return connectingPlayers[channel]
    }
    private fun addConnecting(player: Player){
        connectingPlayers[player.channel] = player
    }
    internal fun removeConnecting(player: Player){
        connectingPlayers.remove(player.channel)
    }

    //region Connection Management

    /**
     * Handles the pre-connection phase including protocol validation and authentication
     *
     * @param clientInfo The [ClientIdentification] packet containing client information.
     * @param channel The Netty [Channel] for the connecting client.
     */
    internal fun handlePreConnection(clientInfo: ClientIdentification, channel: Channel) {
        if (!isValidProtocol(clientInfo.protocolVersion)) {
            disconnectWithInvalidProtocol(clientInfo.protocolVersion, channel)
            return
        }

        if (!authenticateUser(clientInfo, channel)) return

        val event = PlayerPreConnectEvent(
            clientInfo.protocolVersion,
            clientInfo.userName,
            clientInfo.verificationKey,
            clientInfo.unused,
        )
        EventDispatcher.dispatch(event)

        if (event.isCancelled) {
            ServerDisconnectPlayer("Disconnected").send(channel)
            return
        }

        val player = createPlayerFromClientInfo(clientInfo, channel).apply {
            levelId = Levels.getDefaultLevelId()
            supportsCpe = clientInfo.unused == 0x42.toByte()
        }

        attemptConnection(player)
    }

    /**
     * Attempts to connect a player after all validations
     *
     * @param player The [Player] attempting to connect.
     */
    private fun attemptConnection(player: Player) {
        when (val connectionResult = validateConnection(player)) {
            is ConnectionResult.Success -> {
                CPEHandshake(player)
                EventDispatcher.dispatch(PlayerConnectEvent(player))
            }
            is ConnectionResult.Failure -> {
                disconnectPlayerWithReason(player.channel, connectionResult.reason)
            }
        }
    }

    /**
     * Sends the CPE handshake to the player
     *
     * @param player The [Player] to send the handshake for.
     */
    private fun CPEHandshake(player: Player) {
        addConnecting(player)

        if (!player.supportsCpe) {
            finalizeHandshake(player)
            return
        }

        PacketRegistry.sendCPEHandshake(player)
        PacketRegistry.sendCPEEntries(player)
    }

    /**
     * Finalizes successful player connection
     *
     * @param player The [Player] to finalize the connection for.
     */
    internal fun finalizeHandshake(player: Player) {
        removeConnecting(player)

        if (!player.channel.isOpen) return

        ServerIdentification().send(player.channel)

        val joinLevel = Levels.getDefaultLevel()
        if (joinLevel == null) {
            disconnectPlayerWithReason(player.channel, MessageRegistry.Server.Level.getNotAvailable())
            return
        }

        player.info.recordJoin()
        player.joinLevel(joinLevel)
        notifyJoined(player)
    }

    /**
     * Handles player disconnection and cleanup
     *
     * @param channel The Netty [Channel] of the disconnecting player.
     */
    internal fun handleDisconnection(channel: Channel) {
        try {
            val player = find(channel)

            if (player != null) {
                handlePlayerDisconnection(player)
                return
            }

            val connectingPlayer = connectingPlayers[channel]
            if (connectingPlayer != null) {
                handleConnectingPlayerDisconnection(connectingPlayer)
                return
            }

            Console.debugLog("Disconnection handled for unregistered channel: ${channel.id().asShortText()}")
        } catch (e: Exception) {
            Console.errLog("Error during player disconnection handling: ${e.message}")
            e.printStackTrace()

            forceDisconnectionCleanup(channel)
        }
    }

    /**
     * Handles disconnection for fully connected players
     */
    private fun handlePlayerDisconnection(player: Player) {
        try {
            Console.debugLog("Handling disconnection for player: ${player.name}")

            removeConnecting(player)

            player.level?.let { level ->
                try {
                    level.removeEntity(player)
                    Console.debugLog("Removed player '${player.name}' from level '${level.id}'")
                } catch (e: Exception) {
                    Console.errLog("Error removing player '${player.name}' from level: ${e.message}")
                }
            }

            player.info.recordDisconnect()
            notifyLeft(player)

        } catch (e: Exception) {
            Console.errLog("Critical error in handlePlayerDisconnection for '${player.name}': ${e.message}")
            throw e
        }
    }

    /**
     * Handles disconnection for players who were still connecting
     */
    private fun handleConnectingPlayerDisconnection(connectingPlayer: Player) {
        try {
            Console.debugLog("Handling disconnection for connecting player: ${connectingPlayer.name}")

            // Remove from connecting players
            removeConnecting(connectingPlayer)

            // Remove from level if they were added
            connectingPlayer.level?.let { level ->
                try {
                    level.removeEntity(connectingPlayer)
                    Console.debugLog("Removed connecting player '${connectingPlayer.name}' from level '${level.id}'")
                } catch (e: Exception) {
                    Console.errLog("Error removing connecting player from level: ${e.message}")
                }
            }

            Console.debugLog("Successfully cleaned up connecting player: ${connectingPlayer.name}")

        } catch (e: Exception) {
            Console.errLog("Error in handleConnectingPlayerDisconnection: ${e.message}")
            throw e
        }
    }

    /**
     * Forces cleanup when normal disconnection handling fails
     */
    private fun forceDisconnectionCleanup(channel: Channel) {
        try {
            Console.warnLog("Performing force cleanup for channel: ${channel.id().asShortText()}")

            // Force remove from connecting players by channel
            val connectingToRemove = connectingPlayers.entries.find { it.key == channel }
            connectingToRemove?.let {
                connectingPlayers.remove(it.key)
                Console.debugLog("Force removed connecting player: ${it.value.name}")
            }

            // Try to find and force remove from levels
            getAllPlayers().find { it.channel == channel }?.let { player ->
                try {
                    player.level?.removeEntity(player)
                    Console.debugLog("Force removed player '${player.name}' from level")
                } catch (e: Exception) {
                    Console.errLog("Error in force removal from level: ${e.message}")
                }
            }

            Console.debugLog("Force cleanup completed for channel: ${channel.id().asShortText()}")

        } catch (e: Exception) {
            Console.errLog("Critical error in force disconnection cleanup: ${e.message}")
            // At this point, we've exhausted all cleanup options
        }
    }

    /**
     * Forces player disconnection without sending any packets to the client.
     * This method should be used when there are connection errors and the channel may be corrupted.
     * Only removes the player instance and notifies server-side that the player left.
     *
     * @param channel The Netty [Channel] of the player to force disconnect.
     */
    internal fun forceDisconnect(channel: Channel) {
        connectingPlayers[channel]?.let { connectingPlayer ->
            removeConnecting(connectingPlayer)
            connectingPlayer.level?.removeEntity(connectingPlayer)
            return
        }

        getAllPlayers().find { it.channel == channel }?.let { connectedPlayer ->
            connectedPlayer.level?.removeEntity(connectedPlayer)
            connectedPlayer.info.recordDisconnect()
            notifyLeft(connectedPlayer)
        }
    }

    //endregion

    //region Authentication & Validation

    /**
     * Validates the client's protocol version
     *
     * @param version The protocol version byte sent by the client.
     * @return `true` if the version is valid, `false` otherwise.
     */
    private fun isValidProtocol(version: Byte): Boolean {
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

        val expectedHash = generateHash(clientInfo.userName)
        if (clientInfo.verificationKey != expectedHash) {
            disconnectPlayerWithReason(channel, MessageRegistry.Server.Connection.getAuthenticationFailed())
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
    private fun generateHash(userName: String): String {
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
    private fun validateConnection(player: Player): ConnectionResult {
        return when {
            player.info.isBanned -> ConnectionResult.Failure("You are banned: ${player.info.banReason}")
            isConnected(player) -> ConnectionResult.Failure(MessageRegistry.Server.Connection.getAlreadyConnected())
            isServerFull() -> ConnectionResult.Failure(MessageRegistry.Server.Connection.getServerFull())
            else -> ConnectionResult.Success
        }
    }

    /**
     * Checks if player is already connected by channel or name
     *
     * @param player The [Player] to check for existing connections.
     * @return `true` if the player is already connected, `false` otherwise.
     */
    private fun isConnected(player: Player): Boolean {
        return find(player.channel) != null ||
                getAllPlayers().any { it.name.equals(player.name, ignoreCase = true) }
    }

    /**
     * Checks if the server has reached maximum capacity
     *
     * @return `true` if the server is full, `false` otherwise.
     */
    private fun isServerFull(): Boolean = count() >= ServerInfo.maxPlayers

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
    internal fun notifyJoined(player: Player) {
        val message = MessageRegistry.Server.Player.getJoined(player.name)
        Console.log(message)
        broadcastMessage(message)
    }

    /**
     * Notifies all players and console when a player leaves
     *
     * @param player The [Player] who left.
     */
    internal fun notifyLeft(player: Player) {
        val message = MessageRegistry.Server.Player.getLeft(player.name)
        Console.log(message)
        broadcastMessage(message)
    }

    /**
     * Notifies all players and console when a player joins a level
     *
     * @param player The [Player] who joined the level.
     * @param level The [Level] the player joined.
     */
    internal fun notifyJoinedLevel(player: Player, level: Level) {
        val message = MessageRegistry.Server.Player.getJoinedLevel(player.name, level.id)
        Console.log(message)
        broadcastMessage(message)
    }

    //endregion

    //region Player Lookup

    /**
     * Finds a player by their network channel
     *
     * @param channel The Netty [Channel] to search for.
     * @return The [Player] associated with the channel, or `null` if not found.
     */
    fun find(channel: Channel): Player? = Levels.getAllPlayers().find { it.channel == channel }

    /**
     * Finds a player by their username (case-insensitive)
     *
     * @param name The username string to search for.
     * @return The [Player] with the matching name, or `null` if not found.
     */
    fun find(name: String): Player? = Levels.getAllPlayers().find { it.name.equals(name, ignoreCase = true) }

    /**
     * Gets all currently connected players
     *
     * @return A list of all connected [Player] instances.
     */
    fun getAllPlayers(): List<Player> = Levels.getAllPlayers()

    /**
     * Gets the current number of connected players
     *
     * @return The total count of connected players.
     */
    fun count(): Int = Levels.getTotalPlayerCount()

    //endregion

    //region Global Player Operations

    /**
     * Broadcasts a message to all connected players
     *
     * @param message The message string to broadcast.
     * @param messageTypeId An optional byte identifier for the type of message. Defaults to `0x00`.
     */
    fun broadcastMessage(message: String, messageTypeId: Byte = 0x00) {
        getAllPlayers().forEach { it.sendMessage(message, messageTypeId) }
    }

    /**
     * Kicks all players from the server
     *
     * @param reason The reason for kicking all players. Defaults to "Server maintenance".
     */
    fun kickAll(reason: String = "Server maintenance") {
        getAllPlayers().forEach { it.kick(reason) }
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

    /**
     * Gets the permissions of a player
     * @param name the requested player name
     * @return a list containing all player permissions
     */
    fun getPermissions(name: String): List<String> = PermissionRepository.getPermissionList(name)

    /**
     * Gets the permission groups of a player
     *
     * @param name the requested player name
     * @return the player's groups
     */
    fun getGroups(name: String): List<Group> = PermissionRepository.getPlayerGroups(name)

    /**
     * Sets a permission for a player.
     *
     * @param name the player name
     * @param permission the permission string
     * @param value true to grant, false to deny
     * @return true if the permission was set
     */
    fun setPermission(name: String, permission: String, value: Boolean): Boolean =
        PermissionRepository.setPlayerPermission(name, permission, value)

    /**
     * Checks if a player has a specific permission.
     *
     * @param name the player name
     * @param permission the permission string
     * @return true if the player has the permission
     */
    fun hasPermission(name: String, permission: String): Boolean =
        PermissionRepository.hasPermission(name, permission)

    /**
     * Adds a group to a player.
     *
     * @param name the player name
     * @param group the group name to add
     * @return true if the group was added
     */
    fun addGroup(name: String, group: String): Boolean =
        PermissionRepository.addGroupToPlayer(name, group)

    /**
     * Removes a group from a player.
     *
     * @param name the player name
     * @param group the group name to remove
     * @return true if the group was removed
     */
    fun removeGroup(name: String, group: String): Boolean =
        PermissionRepository.removeGroupFromPlayer(name, group)

    //region Utility Methods
    /**
     * Disconnects a player with invalid protocol version
     *
     * @param version The invalid protocol version byte.
     * @param channel The Netty [Channel] to disconnect.
     */
    private fun disconnectWithInvalidProtocol(version: Byte, channel: Channel) {
        val message = MessageRegistry.Server.Connection.getInvalidProtocol(version.toInt(), EXPECTED_PROTOCOL_VERSION.toInt())
        ServerDisconnectPlayer(message).send(channel)
    }

    private fun disconnectWithNoCPESupport(channel: Channel){
        val message = MessageRegistry.Server.Connection.getNoCpeSupport()
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

    /**
     * Emergency shutdown method to disconnect all players safely
     */
    fun emergencyDisconnectAll(reason: String = "Server emergency shutdown") {
        Console.warnLog("Emergency disconnect all players initiated: $reason")

        try {
            // Disconnect all fully connected players
            getAllPlayers().forEach { player ->
                try {
                    player.kick(reason)
                } catch (e: Exception) {
                    Console.errLog("Error disconnecting player '${player.name}': ${e.message}")
                }
            }

            // Clean up all connecting players
            connectingPlayers.values.toList().forEach { connectingPlayer ->
                try {
                    handleConnectingPlayerDisconnection(connectingPlayer)
                } catch (e: Exception) {
                    Console.errLog("Error cleaning up connecting player '${connectingPlayer.name}': ${e.message}")
                }
            }

            // Clear all connecting players as final cleanup
            connectingPlayers.clear()

            Console.log("Emergency disconnect completed")

        } catch (e: Exception) {
            Console.errLog("Critical error during emergency disconnect: ${e.message}")
        }
    }
}
