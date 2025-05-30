package org.dandelion.classic.data.config.model

import org.dandelion.classic.data.config.stream.YamlStream

class PermissionsConfig(private val configFileName: String = "permissions.yml") {
    private val yaml = YamlStream(configFileName)
    private var permissions: MutableMap<String, MutableSet<String>> = mutableMapOf()
    private var defaults: MutableSet<String> = mutableSetOf()

    fun load() {
        yaml.load()
        val permsMap = yaml.getMap("permissions")
        permissions = permsMap.mapValues { (it.value as? List<*>)?.mapNotNull { v -> v?.toString() }?.toMutableSet() ?: mutableSetOf() }
            .mapKeys { it.key }
            .toMutableMap()
        defaults = yaml.getList("defaults").mapNotNull { it?.toString() }.toMutableSet()
        if (defaults.isEmpty()) defaults = mutableSetOf("level.list", "level.go", "online")
    }

    fun reload() = load()

    fun save(): Boolean {
        return try {
            yaml.set("permissions", permissions.mapValues { it.value.sorted() })
            yaml.set("defaults", defaults.sorted())
            yaml.save()
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

