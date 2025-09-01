package org.dandelion.classic.entity.player

import io.netty.channel.Channel
import java.security.MessageDigest
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
import org.dandelion.classic.server.MessageRegistry
import org.dandelion.classic.server.ServerConfig
import org.dandelion.classic.tablist.TabList
import java.net.InetSocketAddress

object Players {
    private const val EXPECTED_PROTOCOL_VERSION: Byte = 0x07
    private const val MD5_ALGORITHM = "MD5"
    private const val HEX_FORMAT = "%02x"

    // hold connecting players while packetregistry resolves it's cpe support
    private val connectingPlayers: MutableMap<Channel, Player> = mutableMapOf()

    internal fun getConnecting(channel: Channel): Player? {
        return connectingPlayers[channel]
    }

    private fun addConnecting(player: Player) {
        connectingPlayers[player.channel] = player
    }

    internal fun removeConnecting(player: Player) {
        connectingPlayers.remove(player.channel)
    }

    // region Connection Management
    internal fun handlePreConnection(
        clientInfo: ClientIdentification,
        channel: Channel,
    ) {
        if (!isValidProtocol(clientInfo.protocolVersion)) {
            disconnectWithInvalidProtocol(clientInfo.protocolVersion, channel)
            return
        }

        if (!authenticateUser(clientInfo, channel)) return

        val event =
            PlayerPreConnectEvent(
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

        val player =
            createPlayerFromClientInfo(clientInfo, channel).apply {
                levelId = Levels.defaultLevelId
                supportsCpe = clientInfo.unused == 0x42.toByte()
            }

        attemptConnection(player)
    }

    private fun attemptConnection(player: Player) {
        when (val connectionResult = validateConnection(player)) {
            is ConnectionResult.Success -> {
                CPEHandshake(player)
                EventDispatcher.dispatch(PlayerConnectEvent(player))
            }
            is ConnectionResult.Failure -> {
                disconnectPlayerWithReason(
                    player.channel,
                    connectionResult.reason,
                )
            }
        }
    }

    private fun CPEHandshake(player: Player) {
        addConnecting(player)

        if (!player.supportsCpe) {
            finalizeHandshake(player)
            return
        }

        PacketRegistry.sendCPEHandshake(player)
        PacketRegistry.sendCPEEntries(player)
    }

    internal fun finalizeHandshake(player: Player) {
        removeConnecting(player)

        if (!player.channel.isOpen) return

        ServerIdentification().send(player.channel)

        val joinLevel = Levels.getDefaultLevel()
        if (joinLevel == null) {
            disconnectPlayerWithReason(
                player.channel,
                MessageRegistry.Server.Level.getNotAvailable(),
            )
            return
        }

        player.info.recordJoin()
        val channel = player.channel
        val remoteAddress = channel.remoteAddress() as InetSocketAddress
        val ip = remoteAddress.address.hostAddress
        player.info.addIp(ip)

        player.joinLevel(joinLevel)
        player.displayName = MessageRegistry.Server.Player.getDisplayName(player)

        TabList.sendFullTabListTo(player)
        addPlayerToTabList(player)

        notifyJoined(player)
    }

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

            Console.debugLog(
                "Disconnection handled for unregistered channel: ${channel.id().asShortText()}"
            )
        } catch (e: Exception) {
            Console.errLog(
                "Error during player disconnection handling: ${e.message}"
            )
            e.printStackTrace()

            forceDisconnectionCleanup(channel)
        }
    }

    private fun handlePlayerDisconnection(player: Player) {
        try {
            Console.debugLog(
                "Handling disconnection for player: ${player.name}"
            )
            removeConnecting(player)
            removePlayerFromTabList(player)
            player.level?.let { level ->
                try {
                    level.removeEntity(player)
                    Console.debugLog(
                        "Removed player '${player.name}' from level '${level.id}'"
                    )
                } catch (e: Exception) {
                    Console.errLog(
                        "Error removing player '${player.name}' from level: ${e.message}"
                    )
                }
            }

            player.info.recordDisconnect()
            removePlayerFromTabList(player)
            notifyLeft(player)
        } catch (e: Exception) {
            Console.errLog(
                "Critical error in handlePlayerDisconnection for '${player.name}': ${e.message}"
            )
            throw e
        }
    }

    private fun handleConnectingPlayerDisconnection(connectingPlayer: Player) {
        try {
            Console.debugLog(
                "Handling disconnection for connecting player: ${connectingPlayer.name}"
            )
            removeConnecting(connectingPlayer)

            connectingPlayer.level?.let { level ->
                try {
                    level.removeEntity(connectingPlayer)
                    Console.debugLog(
                        "Removed connecting player '${connectingPlayer.name}' from level '${level.id}'"
                    )
                } catch (e: Exception) {
                    Console.errLog(
                        "Error removing connecting player from level: ${e.message}"
                    )
                }
            }

            Console.debugLog(
                "Successfully cleaned up connecting player: ${connectingPlayer.name}"
            )
        } catch (e: Exception) {
            Console.errLog(
                "Error in handleConnectingPlayerDisconnection: ${e.message}"
            )
            throw e
        }
    }

    private fun forceDisconnectionCleanup(channel: Channel) {
        try {
            Console.warnLog(
                "Performing force cleanup for channel: ${channel.id().asShortText()}"
            )
            val connectingToRemove =
                connectingPlayers.entries.find { it.key == channel }
            connectingToRemove?.let {
                connectingPlayers.remove(it.key)
                Console.debugLog(
                    "Force removed connecting player: ${it.value.name}"
                )
            }

            getAllPlayers()
                .find { it.channel == channel }
                ?.let { player ->
                    try {
                        player.level?.removeEntity(player)
                        Console.debugLog(
                            "Force removed player '${player.name}' from level"
                        )
                    } catch (e: Exception) {
                        Console.errLog(
                            "Error in force removal from level: ${e.message}"
                        )
                    }
                }

            Console.debugLog(
                "Force cleanup completed for channel: ${channel.id().asShortText()}"
            )
        } catch (e: Exception) {
            Console.errLog(
                "Critical error in force disconnection cleanup: ${e.message}"
            )
        }
    }

    internal fun forceDisconnect(channel: Channel) {
        connectingPlayers[channel]?.let { connectingPlayer ->
            removeConnecting(connectingPlayer)
            connectingPlayer.level?.removeEntity(connectingPlayer)
            return
        }

        find(channel)?.let { connectedPlayer ->
            connectedPlayer.level?.removeEntity(connectedPlayer)
            connectedPlayer.info.recordDisconnect()
            removePlayerFromTabList(connectedPlayer)
            notifyLeft(connectedPlayer)
        }
    }

    // endregion

    // region Authentication & Validation

    private fun isValidProtocol(version: Byte): Boolean {
        return version == EXPECTED_PROTOCOL_VERSION
    }

    private fun authenticateUser(
        clientInfo: ClientIdentification,
        channel: Channel,
    ): Boolean {
        if (!ServerConfig.verifyUsers) return true

        val expectedHash = generateHash(clientInfo.userName)
        if (clientInfo.verificationKey != expectedHash) {
            disconnectPlayerWithReason(
                channel,
                MessageRegistry.Server.Connection.getAuthenticationFailed(),
            )
            return false
        }

        return true
    }

    private fun generateHash(userName: String): String {
        val messageDigest = MessageDigest.getInstance(MD5_ALGORITHM)
        val hashBytes =
            messageDigest.digest("${ServerConfig.salt}$userName".toByteArray())
        return hashBytes.joinToString("") { HEX_FORMAT.format(it) }
    }

    private fun validateConnection(player: Player): ConnectionResult {
        return when {
            player.info.isBanned ->
                ConnectionResult.Failure(
                    "You are banned: ${player.info.banReason}"
                )
            isConnected(player) ->
                ConnectionResult.Failure(
                    MessageRegistry.Server.Connection.getAlreadyConnected()
                )
            isServerFull() ->
                ConnectionResult.Failure(
                    MessageRegistry.Server.Connection.getServerFull()
                )
            else -> ConnectionResult.Success
        }
    }

    private fun isConnected(player: Player): Boolean {
        return find(player.channel) != null ||
            getAllPlayers().any {
                it.name.equals(player.name, ignoreCase = true)
            }
    }

    private fun isServerFull(): Boolean = count() >= ServerConfig.maxPlayers

    private fun createPlayerFromClientInfo(
        clientInfo: ClientIdentification,
        channel: Channel,
    ): Player {
        return Player(channel, "Unknown", clientInfo.userName)
    }

    // endregion

    // region broadcasts

    internal fun notifyJoined(player: Player) {
        val message = MessageRegistry.Server.Player.getJoined(player.name)
        Console.log(message)
        broadcastMessage(message)
    }

    internal fun notifyLeft(player: Player) {
        val message = MessageRegistry.Server.Player.getLeft(player.name)
        Console.log(message)
        broadcastMessage(message)
    }

    internal fun notifyJoinedLevel(player: Player, level: Level) {
        val message =
            MessageRegistry.Server.Player.getJoinedLevel(player.name, level.id)
        Console.log(message)
        broadcastMessage(message)
    }

    // endregion

    // region Player Lookup
    fun find(channel: Channel): Player? =
        Levels.getAllPlayers().find { it.channel == channel }

    fun find(name: String): Player? =
        Levels.getAllPlayers().find { it.name.equals(name, ignoreCase = true) }

    fun getAllPlayers(): List<Player> = Levels.getAllPlayers()

    fun count(): Int = Levels.getTotalPlayerCount()

    // endregion

    // region Tab List Management

    internal fun addPlayerToTabList(player: Player) {
        TabList.addPlayer(player)
    }

    internal fun removePlayerFromTabList(player: Player) {
        TabList.removePlayer(player)
    }

    // endregion

    // region Global Player Operations
    fun broadcastMessage(message: String, messageTypeId: Byte = 0x00) {
        getAllPlayers().forEach { it.sendMessage(message, messageTypeId) }
    }

    fun kickAll(reason: String = "Server maintenance") {
        getAllPlayers().forEach { it.kick(reason) }
    }

    fun banPlayer(player: Player, reason: String = "You have been banned") {
        player.ban(reason)
    }

    // endregion
    fun getPermissions(name: String): List<String> =
        PermissionRepository.getPermissionList(name)

    fun getGroups(name: String): List<Group> =
        PermissionRepository.getPlayerGroups(name)

    fun setPermission(
        name: String,
        permission: String,
        value: Boolean,
    ): Boolean =
        PermissionRepository.setPlayerPermission(name, permission, value)

    fun hasPermission(name: String, permission: String): Boolean =
        PermissionRepository.hasPermission(name, permission)

    fun hasPermission(
        name: String,
        permission: String,
        default: Boolean = false,
    ): Boolean = PermissionRepository.hasPermission(name, permission, default)

    fun addGroup(name: String, group: String): Boolean =
        PermissionRepository.addGroupToPlayer(name, group)

    fun removeGroup(name: String, group: String): Boolean =
        PermissionRepository.removeGroupFromPlayer(name, group)

    // region Utility Methods
    private fun disconnectWithInvalidProtocol(version: Byte, channel: Channel) {
        val message =
            MessageRegistry.Server.Connection.getInvalidProtocol(
                version.toInt(),
                EXPECTED_PROTOCOL_VERSION.toInt(),
            )
        ServerDisconnectPlayer(message).send(channel)
    }

    private fun disconnectWithNoCPESupport(channel: Channel) {
        val message = MessageRegistry.Server.Connection.getNoCpeSupport()
        ServerDisconnectPlayer(message).send(channel)
    }

    private fun disconnectPlayerWithReason(channel: Channel, reason: String) {
        ServerDisconnectPlayer(reason).send(channel)
    }

    // endregion
    private sealed class ConnectionResult {
        object Success : ConnectionResult()

        data class Failure(val reason: String) : ConnectionResult()
    }

    fun emergencyDisconnectAll(reason: String = "Server emergency shutdown") {
        Console.warnLog("Emergency disconnect all players initiated: $reason")

        try {
            // Disconnect all fully connected players
            getAllPlayers().forEach { player ->
                try {
                    player.kick(reason)
                } catch (e: Exception) {
                    Console.errLog(
                        "Error disconnecting player '${player.name}': ${e.message}"
                    )
                }
            }

            connectingPlayers.values.toList().forEach { connectingPlayer ->
                try {
                    handleConnectingPlayerDisconnection(connectingPlayer)
                } catch (e: Exception) {
                    Console.errLog(
                        "Error cleaning up connecting player '${connectingPlayer.name}': ${e.message}"
                    )
                }
            }
            connectingPlayers.clear()

            Console.log("Emergency disconnect completed")
        } catch (e: Exception) {
            Console.errLog(
                "Critical error during emergency disconnect: ${e.message}"
            )
        }
    }

    // endregion

    fun supports(player: Player, extension: String): Boolean {
        return player.supports(extension)
    }

    fun supports(channel: Channel, extension: String): Boolean {
        return find(channel)?.supports(extension) == true
    }
}
