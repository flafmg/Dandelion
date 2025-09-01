package org.dandelion.classic.entity.player.data

import org.dandelion.classic.entity.player.Players
import java.util.Date

class PlayerInfo(
    val id: Int = 0,
    val name: String,
    val firstJoin: Date = Date(),
    var lastJoin: Date = Date(),
    var lastSeen: Date = Date(),
    var isBanned: Boolean = false,
    var banReason: String = "",
    var totalPlaytime: Long = 0,
    var joinCount: Int = 0,
    val ips: MutableSet<String> = mutableSetOf()
) {
    fun setBanned(reason: String = "No reason provided") {
        isBanned = true
        banReason = reason
        save()
    }

    fun removeBan() {
        isBanned = false
        banReason = ""
        save()
    }

    internal fun recordJoin() {
        lastJoin = Date()
        joinCount++
        save()
    }

    internal fun recordDisconnect() {
        val sessionDuration = Date().time - lastJoin.time
        totalPlaytime += sessionDuration
        lastSeen = Date()
        save()
    }

    fun addIp(ip: String) {
        if (!ips.contains(ip)) {
            ips.add(ip)
            save()
        }
    }

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

    fun save() {
        PlayerInfoRegistry.save(this)
    }

    private fun isOnline(): Boolean {
        return Players.find(name) != null
    }

    fun getSummary(): String = buildString {
        appendLine("Player: $name")
        appendLine("First joined: $firstJoin")
        appendLine("Last seen: ${if (isOnline()) Date() else lastSeen}")
        appendLine("Join count: $joinCount")
        appendLine("Total playtime: ${getPlaytime()}")
        appendLine("IPs: ${ips.joinToString(", ")}")

        when {
            isBanned -> {
                appendLine("Status: BANNED")
                appendLine("Ban reason: $banReason")
            }
            else ->
                appendLine("Status: ${if (isOnline()) "Online" else "Offline"}")
        }
    }

    companion object {
        fun load(name: String): PlayerInfo? {
            return PlayerInfoRegistry.load(name)
        }

        fun getOrCreate(name: String): PlayerInfo {
            return load(name) ?: PlayerInfoRegistry.create(name)
        }

        fun existsFor(name: String): Boolean {
            return load(name) != null
        }
    }
}