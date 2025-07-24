package org.dandelion.classic.player

import org.dandelion.classic.util.YamlConfig
import java.util.Date

/**
 * Represents persistent information about a player that is stored between sessions.
 * Handles player data such as permissions, ban status, playtime statistics, and connection history.
 *
 * @property name The name of the player.
 * @property firstJoin The [Date] when the player first joined the server.
 * @property lastJoin The [Date] when the player last joined the server.
 */
class PlayerInfo(
    val name: String,
    val firstJoin: Date = Date(),
    var lastJoin: Date = Date()
) {

    /**
     * Whether the player has operator permissions
     */
    var isOperator: Boolean = false
        internal set

    /**
     * Whether the player is currently banned
     */
    var isBanned: Boolean = false
        internal set

    /**
     * The reason for the player's ban (empty if not banned)
     */
    var banReason: String = ""
        internal set

    /**
     * The last time the player was seen online.
     * Returns current time if player is currently online, otherwise returns stored value.
     */
    var lastSeen: Date = Date()
        get() = if (isOnline()) Date() else field
        internal set

    /**
     * Total playtime in milliseconds across all sessions
     */
    var totalPlaytime: Long = 0
        internal set

    /**
     * Number of times the player has joined the server
     */
    var joinCount: Int = 0
        internal set

    /**
     * Grants operator permissions to the player
     */
    fun grantOperator() {
        isOperator = true
        save()
    }

    /**
     * Revokes operator permissions from the player
     */
    fun revokeOperator() {
        isOperator = false
        save()
    }

    /**
     * Bans the player with the specified reason
     *
     * @param reason The reason for banning the player. Defaults to "No reason provided".
     */
    fun setBanned(reason: String = "No reason provided") {
        isBanned = true
        banReason = reason
        save()
    }

    /**
     * Unbans the player and clears the ban reason
     */
    fun removeBan() {
        isBanned = false
        banReason = ""
        save()
    }

    /**
     * Updates the join statistics when player connects
     */
    internal fun recordJoin() {
        lastJoin = Date()
        joinCount++
        save()
    }

    /**
     * Updates playtime and last seen when player disconnects
     */
    internal fun recordDisconnect() {
        val sessionDuration = Date().time - lastJoin.time
        totalPlaytime += sessionDuration
        lastSeen = Date()
        save()
    }

    /**
     * Gets formatted total playtime as a human-readable string
     *
     * @return A formatted string representing the total playtime (e.g., "2h 30m 15s").
     */
    fun getPlaytime(): String {
        val totalSeconds = totalPlaytime / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Saves this player's information to the persistent storage
     */
    fun save() {
        PlayerInfoRepository.save(this)
    }

    /**
     * Checks if the player is currently online
     *
     * @return `true` if the player is currently online, `false` otherwise.
     */
    private fun isOnline(): Boolean {
        return Players.findPlayerByName(name) != null
    }

    /**
     * Gets a summary of the player's information
     *
     * @return A formatted string containing the player's summary information.
     */
    fun getSummary(): String {
        return buildString {
            appendLine("Player: $name")
            appendLine("First joined: $firstJoin")
            appendLine("Last seen: $lastSeen")
            appendLine("Join count: $joinCount")
            appendLine("Total playtime: ${getPlaytime()}")
            appendLine("Operator: ${if (isOperator) "Yes" else "No"}")
            if (isBanned) {
                appendLine("Status: BANNED")
                appendLine("Ban reason: $banReason")
            } else {
                appendLine("Status: ${if (isOnline()) "Online" else "Offline"}")
            }
        }
    }

    companion object {
        /**
         * Repository for loading and saving player information
         */
        private object PlayerInfoRepository {
            private val playerDataConfig: YamlConfig = YamlConfig.load("player-info.yml")

            /**
             * Loads player information from persistent storage
             *
             * @param name The name of the player to load.
             * @return The loaded [PlayerInfo] instance if found, `null` otherwise.
             */
            fun load(name: String): PlayerInfo? {
                val playerSection = playerDataConfig.getSection(name) ?: return null

                val firstJoinTime = playerSection.getLong("firstJoin", Date().time)
                val lastJoinTime = playerSection.getLong("lastJoin", Date().time)

                return PlayerInfo(name, Date(firstJoinTime), Date(lastJoinTime)).apply {
                    isOperator = playerSection.getBoolean("isOp", false)
                    isBanned = playerSection.getBoolean("banned", false)
                    banReason = playerSection.getString("banReason", "")
                    lastSeen = Date(playerSection.getLong("lastSeen", Date().time))
                    totalPlaytime = playerSection.getLong("totalPlaytime", 0L)
                    joinCount = playerSection.getInt("joinCount", 0)
                }
            }

            /**
             * Saves player information to persistent storage
             *
             * @param playerInfo The [PlayerInfo] instance to save.
             */
            fun save(playerInfo: PlayerInfo) {
                val playerSection = playerDataConfig.getOrCreateSection(playerInfo.name)

                with(playerSection) {
                    set("firstJoin", playerInfo.firstJoin.time)
                    set("lastJoin", playerInfo.lastJoin.time)
                    set("isOp", playerInfo.isOperator)
                    set("banned", playerInfo.isBanned)
                    set("banReason", playerInfo.banReason)
                    set("lastSeen", playerInfo.lastSeen.time)
                    set("totalPlaytime", playerInfo.totalPlaytime)
                    set("joinCount", playerInfo.joinCount)
                }

                playerDataConfig.save()
            }

            /**
             * Creates a new player info entry with default values
             *
             * @param name The name of the player for the new entry.
             * @return The newly created [PlayerInfo] instance.
             */
            fun create(name: String): PlayerInfo {
                val newPlayerInfo = PlayerInfo(name)
                save(newPlayerInfo)
                return newPlayerInfo
            }
        }

        /**
         * Loads existing player information or returns null if not found
         *
         * @param name The name of the player to load.
         * @return The loaded [PlayerInfo] instance if found, `null` otherwise.
         */
        fun load(name: String): PlayerInfo? {
            return PlayerInfoRepository.load(name)
        }

        /**
         * Loads existing player information or creates new entry if not found
         *
         * @param name The name of the player to load or create.
         * @return The loaded or newly created [PlayerInfo] instance.
         */
        fun getOrCreate(name: String): PlayerInfo {
            return load(name) ?: PlayerInfoRepository.create(name)
        }

        /**
         * Checks if player information exists for the given name
         *
         * @param name The name of the player to check.
         * @return `true` if player information exists, `false` otherwise.
         */
        fun existsFor(name: String): Boolean {
            return load(name) != null
        }
    }
}