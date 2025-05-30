package org.dandelion.classic.server.config.model

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.InputStream

class PermissionsConfig(private val configFileName: String = "permissions.yml") {
    private var permissions: MutableMap<String, MutableSet<String>> = mutableMapOf()
    private var defaults: MutableSet<String> = mutableSetOf()

    fun load() {
        val file = File(configFileName)
        if (!file.exists()) {
            val resource: InputStream? = javaClass.classLoader.getResourceAsStream(configFileName)
            if (resource != null) {
                resource.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        try {
            FileInputStream(file).use { inputStream ->
                val yaml = Yaml()
                val loaded = yaml.load<Map<String, Any>>(inputStream)
                val perms = (loaded?.get("permissions") as? Map<*, *>)?.mapValues { (it.value as? List<*>)?.mapNotNull { v -> v?.toString() }?.toMutableSet() ?: mutableSetOf() }?.mapKeys { it.key.toString() }?.toMutableMap() ?: mutableMapOf()
                val defs = (loaded?.get("defaults") as? List<*>)?.mapNotNull { it?.toString() }?.toMutableSet() ?: mutableSetOf()
                permissions = perms
                defaults = defs
            }
        } catch (_: Exception) {
            permissions = mutableMapOf()
            defaults = mutableSetOf("level.list", "level.go", "online")
        }
    }

    fun reload() = load()

    fun save(): Boolean {
        return try {
            val option = DumperOptions().apply {
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                isPrettyFlow = true
                defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
                indent = 2
                indicatorIndent = 2
                width = 80
            }
            val yaml = Yaml(option)
            val toSave = mapOf(
                "permissions" to permissions.mapValues { it.value.sorted() },
                "defaults" to defaults.sorted()
            )
            FileWriter(configFileName).use { writer ->
                yaml.dump(toSave, writer)
            }
            true
        } catch (_: Exception) { false }
    }

    fun get(): Map<String, Set<String>> {
        if (permissions.isEmpty()) load()
        return permissions
    }

    fun getPermissions(player: String): Set<String> = permissions[player]?.toSet() ?: emptySet()

    fun getDefaultPermissions(): Set<String> = defaults

    fun applyDefaultPermissions(player: String) {
        if (defaults.isNotEmpty()) {
            val perms = permissions.getOrPut(player) { mutableSetOf() }
            perms.addAll(defaults)
        }
    }

    fun addPermission(player: String, perm: String): Boolean {
        val perms = permissions.getOrPut(player) { mutableSetOf() }
        val added = perms.add(perm)
        if (added) save()
        return added
    }

    fun removePermission(player: String, perm: String): Boolean {
        val perms = permissions[player] ?: return false
        val removed = perms.remove(perm)
        if (removed) save()
        return removed
    }

    fun hasPermission(player: String, perm: String): Boolean = permissions[player]?.contains(perm) == true
}

