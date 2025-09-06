package org.dandelion.server.entity.player.data

import org.dandelion.server.util.DatabaseManager
import java.util.Date

enum class PlayerFilterType {
    ALPHABETICAL,
    ALPHABETICAL_DESC,
    MOST_PLAYTIME,
    LEAST_PLAYTIME,
    MOST_RECENT_JOIN,
    OLDEST_JOIN,
    MOST_JOINS,
    LEAST_JOINS,
    BANNED_ONLY,
    UNBANNED_ONLY
}

object PlayerInfoRegistry {
    private val db = DatabaseManager("player-data.db")

    fun init() {
        db.executeUpdate("""
            CREATE TABLE IF NOT EXISTS players (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                first_join INTEGER NOT NULL,
                last_join INTEGER NOT NULL,
                last_seen INTEGER NOT NULL,
                is_banned BOOLEAN NOT NULL DEFAULT 0,
                ban_reason TEXT NOT NULL DEFAULT '',
                total_playtime INTEGER NOT NULL DEFAULT 0,
                join_count INTEGER NOT NULL DEFAULT 0
            )
        """)

        db.executeUpdate("""
            CREATE TABLE IF NOT EXISTS player_ips (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_id INTEGER NOT NULL,
                ip_address TEXT NOT NULL,
                FOREIGN KEY (player_id) REFERENCES players (id),
                UNIQUE(player_id, ip_address)
            )
        """)
    }

    fun getPlayerCount(): Int {
        val result = db.executeQuery("SELECT COUNT(*) as count FROM players")
        val count = if (result.next()) result.getInt("count") else 0
        result.close()
        return count
    }

    fun getPlayerInfoPage(size: Int, page: Int, filter: PlayerFilterType = PlayerFilterType.ALPHABETICAL): List<PlayerInfo> {
        val offset = (page - 1) * size
        val orderClause = when (filter) {
            PlayerFilterType.ALPHABETICAL -> "ORDER BY name ASC"
            PlayerFilterType.ALPHABETICAL_DESC -> "ORDER BY name DESC"
            PlayerFilterType.MOST_PLAYTIME -> "ORDER BY total_playtime DESC"
            PlayerFilterType.LEAST_PLAYTIME -> "ORDER BY total_playtime ASC"
            PlayerFilterType.MOST_RECENT_JOIN -> "ORDER BY last_join DESC"
            PlayerFilterType.OLDEST_JOIN -> "ORDER BY first_join ASC"
            PlayerFilterType.MOST_JOINS -> "ORDER BY join_count DESC"
            PlayerFilterType.LEAST_JOINS -> "ORDER BY join_count ASC"
            PlayerFilterType.BANNED_ONLY -> "WHERE is_banned = 1 ORDER BY name ASC"
            PlayerFilterType.UNBANNED_ONLY -> "WHERE is_banned = 0 ORDER BY name ASC"
        }

        val sql = "SELECT * FROM players $orderClause LIMIT $size OFFSET $offset"
        val result = db.executeQuery(sql)
        val players = mutableListOf<PlayerInfo>()

        while (result.next()) {
            val playerId = result.getInt("id")
            val playerInfo = PlayerInfo(
                id = playerId,
                name = result.getString("name"),
                firstJoin = Date(result.getLong("first_join")),
                lastJoin = Date(result.getLong("last_join")),
                lastSeen = Date(result.getLong("last_seen")),
                isBanned = result.getBoolean("is_banned"),
                banReason = result.getString("ban_reason"),
                totalPlaytime = result.getLong("total_playtime"),
                joinCount = result.getInt("join_count")
            )

            loadIps(playerId, playerInfo.ips)
            players.add(playerInfo)
        }

        result.close()
        return players
    }

    fun getPlayersByIp(ip: String): List<PlayerInfo> {
        val sql = """
            SELECT DISTINCT p.* FROM players p
            INNER JOIN player_ips pi ON p.id = pi.player_id
            WHERE pi.ip_address = ?
            ORDER BY p.name ASC
        """

        val stmt = db.prepareStatement(sql)
        stmt.setString(1, ip)
        val result = stmt.executeQuery()
        val players = mutableListOf<PlayerInfo>()

        while (result.next()) {
            val playerId = result.getInt("id")
            val playerInfo = PlayerInfo(
                id = playerId,
                name = result.getString("name"),
                firstJoin = Date(result.getLong("first_join")),
                lastJoin = Date(result.getLong("last_join")),
                lastSeen = Date(result.getLong("last_seen")),
                isBanned = result.getBoolean("is_banned"),
                banReason = result.getString("ban_reason"),
                totalPlaytime = result.getLong("total_playtime"),
                joinCount = result.getInt("join_count")
            )

            loadIps(playerId, playerInfo.ips)
            players.add(playerInfo)
        }

        result.close()
        stmt.close()
        return players
    }

    fun load(name: String): PlayerInfo? {
        val stmt = db.prepareStatement("SELECT * FROM players WHERE name = ?")
        stmt.setString(1, name)
        val result = stmt.executeQuery()

        if (!result.next()) {
            result.close()
            stmt.close()
            return null
        }

        val playerId = result.getInt("id")
        val playerInfo = PlayerInfo(
            id = playerId,
            name = result.getString("name"),
            firstJoin = Date(result.getLong("first_join")),
            lastJoin = Date(result.getLong("last_join")),
            lastSeen = Date(result.getLong("last_seen")),
            isBanned = result.getBoolean("is_banned"),
            banReason = result.getString("ban_reason"),
            totalPlaytime = result.getLong("total_playtime"),
            joinCount = result.getInt("join_count")
        )

        result.close()
        stmt.close()

        loadIps(playerId, playerInfo.ips)
        return playerInfo
    }

    private fun loadIps(playerId: Int, ipList: MutableSet<String>) {
        val stmt = db.prepareStatement("SELECT ip_address FROM player_ips WHERE player_id = ?")
        stmt.setInt(1, playerId)
        val result = stmt.executeQuery()

        while (result.next()) {
            ipList.add(result.getString("ip_address"))
        }

        result.close()
        stmt.close()
    }

    fun save(playerInfo: PlayerInfo) {
        val playerId = if (playerInfo.id == 0) {
            insertPlayer(playerInfo)
        } else {
            updatePlayer(playerInfo)
            playerInfo.id
        }

        saveIps(playerId, playerInfo.ips)
    }

    private fun insertPlayer(playerInfo: PlayerInfo): Int {
        val stmt = db.prepareStatement("""
            INSERT INTO players (name, first_join, last_join, last_seen, is_banned, ban_reason, total_playtime, join_count)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """)

        stmt.setString(1, playerInfo.name)
        stmt.setLong(2, playerInfo.firstJoin.time)
        stmt.setLong(3, playerInfo.lastJoin.time)
        stmt.setLong(4, playerInfo.lastSeen.time)
        stmt.setBoolean(5, playerInfo.isBanned)
        stmt.setString(6, playerInfo.banReason)
        stmt.setLong(7, playerInfo.totalPlaytime)
        stmt.setInt(8, playerInfo.joinCount)

        stmt.executeUpdate()
        stmt.close()

        val idResult = db.executeQuery("SELECT last_insert_rowid()")
        val id = idResult.getInt(1)
        idResult.close()

        return id
    }

    private fun updatePlayer(playerInfo: PlayerInfo) {
        val stmt = db.prepareStatement("""
            UPDATE players SET 
                last_join = ?, last_seen = ?, is_banned = ?, ban_reason = ?, 
                total_playtime = ?, join_count = ?
            WHERE id = ?
        """)

        stmt.setLong(1, playerInfo.lastJoin.time)
        stmt.setLong(2, playerInfo.lastSeen.time)
        stmt.setBoolean(3, playerInfo.isBanned)
        stmt.setString(4, playerInfo.banReason)
        stmt.setLong(5, playerInfo.totalPlaytime)
        stmt.setInt(6, playerInfo.joinCount)
        stmt.setInt(7, playerInfo.id)

        stmt.executeUpdate()
        stmt.close()
    }

    private fun saveIps(playerId: Int, ips: Set<String>) {
        db.executeUpdate("DELETE FROM player_ips WHERE player_id = $playerId")

        if (ips.isNotEmpty()) {
            val stmt = db.prepareStatement("INSERT INTO player_ips (player_id, ip_address) VALUES (?, ?)")

            for (ip in ips) {
                stmt.setInt(1, playerId)
                stmt.setString(2, ip)
                stmt.addBatch()
            }

            stmt.executeBatch()
            stmt.close()
        }
    }

    fun create(name: String): PlayerInfo {
        val newPlayerInfo = PlayerInfo(name = name)
        save(newPlayerInfo)
        return load(name)!!
    }

    fun deletePlayer(name: String) {
        val playerInfo = load(name) ?: return

        db.executeUpdate("DELETE FROM player_ips WHERE player_id = ${playerInfo.id}")
        db.executeUpdate("DELETE FROM players WHERE id = ${playerInfo.id}")
    }
}