package org.dandelion.classic.data.config.model

import org.dandelion.classic.data.config.stream.YamlStream

class OpsConfig(private val configFileName: String = "ops.yaml") {
    private val yaml = YamlStream(configFileName)
    private var ops: MutableSet<String> = mutableSetOf()

    fun load() {
        yaml.load()
        ops = yaml.getList("").mapNotNull { it?.toString() }.toMutableSet()
    }

    fun reload() = load()

    fun save(): Boolean {
        return try {
            yaml.set("", ops.toList())
            yaml.save()
            true
        } catch (_: Exception) { false }
    }

    fun get(): Set<String> {
        if (ops.isEmpty()) load()
        return ops
    }

    fun addOp(player: String): Boolean {
        val added = ops.add(player)
        if (added) save()
        return added
    }
    fun removeOp(player: String): Boolean {
        val removed = ops.remove(player)
        if (removed) save()
        return removed
    }
    fun isOp(player: String): Boolean = ops.contains(player)
}
