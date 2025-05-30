package org.dandelion.classic.server.config.model

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter

class OpsConfig(private val configFileName: String = "ops.yaml") {
    private var ops: MutableSet<String> = mutableSetOf()

    fun load() {
        val file = File(configFileName)
        if (!file.exists()) {
            save()
        }
        ops = try {
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
                yaml.dump(ops.toList(), writer)
            }
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
