package org.dandelion.classic.player

import org.dandelion.classic.util.YamlConfig
import java.util.Date

class PlayerInfo(
    val name: String,
    val firstJoin: Date = Date(),
    var lastJoin: Date = Date()
) {
    var isOp: Boolean = false
        internal set
    var banned: Boolean = false
        internal set
    var banReason: String = ""
        internal set
    var lastSeen: Date = Date()
        internal set
    var totalPlaytime: Long = 0
        internal set
    var joinCount: Int = 0
        internal set

    fun save(){
        save(this)
    }

    companion object{
        private val infoYaml: YamlConfig = YamlConfig.load("player-info.yml")

        fun get(name: String): PlayerInfo?{
            val section = infoYaml.getSection(name) ?: return null

            val firstJoin = Date(section.getLong("firstJoin", Date().time))
            val lastJoin = Date(section.getLong("lastJoin", Date().time))
            return PlayerInfo(name, firstJoin, lastJoin).apply {
                isOp = section.getBoolean("isOp", false)
                banned = section.getBoolean("banned", false)
                banReason = section.getString("banReason", "")
                lastSeen = Date(section.getLong("lastSeen", Date().time))
                totalPlaytime = section.getLong("totalPlaytime", 0L)
                joinCount = section.getInt("joinCount", 0)
            }
        }

        fun save(playerInfo: PlayerInfo) {
            val section = infoYaml.getOrCreateSection(playerInfo.name)
            section.set("firstJoin", playerInfo.firstJoin.time)
            section.set("lastJoin", playerInfo.lastJoin.time)
            section.set("isOp", playerInfo.isOp)
            section.set("banned", playerInfo.banned)
            section.set("banReason", playerInfo.banReason)
            section.set("lastSeen", playerInfo.lastSeen.time)
            section.set("totalPlaytime", playerInfo.totalPlaytime)
            section.set("joinCount", playerInfo.joinCount)
            infoYaml.save()
        }

        fun getOrCreate(name: String): PlayerInfo {
            return get(name) ?: PlayerInfo(name).also { save(it) }
        }
    }
}
