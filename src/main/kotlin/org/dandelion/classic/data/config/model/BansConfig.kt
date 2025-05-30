package org.dandelion.classic.data.config.model

import org.dandelion.classic.data.config.stream.YamlStream

class BansConfig(private val configFileName: String = "bans.yaml") {
    private val yaml = YamlStream(configFileName)
    private var bans: MutableSet<String> = mutableSetOf()

    fun load() {
        yaml.load()
        bans = yaml.getList("").mapNotNull { it?.toString() }.toMutableSet()
    }

    fun reload() = load()

    fun save(): Boolean {
        return try {
            yaml.set("", bans.toList())
            yaml.save()
            true
        } catch (_: Exception) { false }
    }

    fun get(): Set<String> {
        if (bans.isEmpty()) load()
        return bans
    }

    fun ban(player: String): Boolean {
        val added = bans.add(player)
        if (added) save()
        return added
    }
    fun unban(player: String): Boolean {
        val removed = bans.remove(player)
        if (removed) save()
        return removed
    }
    fun isBanned(player: String): Boolean = bans.contains(player)
}
