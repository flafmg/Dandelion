package org.dandelion.classic.server.config.model

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter

class BansConfig(private val configFileName: String = "bans.yaml") {
    private var bans: MutableSet<String> = mutableSetOf()

    fun load() {
        val file = File(configFileName)
        if (!file.exists()) {
            save()
        }
        bans = try {
            FileInputStream(file).use { inputStream ->
                val yaml = Yaml()
                val loaded = yaml.load<List<String>>(inputStream)
                loaded?.toMutableSet() ?: mutableSetOf()
            }
        } catch (_: Exception) {
            mutableSetOf()
        }
    }

    fun reload() = load()

    fun save(): Boolean {
        return try {
            val option = DumperOptions().apply {
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                isPrettyFlow = true
            }
            val yaml = Yaml(option)
            FileWriter(configFileName).use { writer ->
                yaml.dump(bans.toList(), writer)
            }
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
