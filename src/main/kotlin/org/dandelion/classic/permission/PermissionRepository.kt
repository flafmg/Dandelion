package org.dandelion.classic.permission

import org.dandelion.classic.util.YamlConfig
import java.io.File

/**
 * Repository for managing permission groups and player permissions.
 * Handles loading, saving, and resolving permissions with in-memory caching.
 */
object PermissionRepository {

    private val groupsCache: MutableMap<String, Group> = mutableMapOf()
    private val playersCache: MutableMap<String, PlayerPermission> = mutableMapOf()

    private lateinit var groupsConfig: YamlConfig
    private lateinit var playersConfig: YamlConfig
    private val permissionsDir = File("permissions")
    private lateinit var groupsFile: File
    private lateinit var playersFile: File

    fun init() {
        setupFiles()
        loadFromFiles()
        ensureDefaultGroupExists()
    }

    private fun setupFiles() {
        if (!permissionsDir.exists()) {
            permissionsDir.mkdirs()
        }

        groupsFile = File(permissionsDir, "groups.yml")
        playersFile = File(permissionsDir, "players.yml")

        if (!groupsFile.exists()) groupsFile.createNewFile()
        if (!playersFile.exists()) playersFile.createNewFile()

        groupsConfig = YamlConfig.load(groupsFile)
        playersConfig = YamlConfig.load(playersFile)
    }

    private fun ensureDefaultGroupExists() {
        if (!groupsCache.containsKey("default")) {
            val defaultGroup = Group("default", "&7Default", 0)
            groupsCache["default"] = defaultGroup
        }

        if (!groupsCache.containsKey("owner")) {
            val ownerGroup = Group("owner", "&4Owner", 1000).apply {
                setPermission("*", true)
            }
            groupsCache["owner"] = ownerGroup
        }
    }

    /**
     * Reload all data from files, discarding any unsaved changes in memory.
     */
    fun reload() {
        groupsCache.clear()
        playersCache.clear()
        loadFromFiles()
        ensureDefaultGroupExists()
    }

    /**
     * Save all in-memory data to files.
     */
    fun save() {
        saveGroupsToFile()
        savePlayersToFile()
        groupsConfig.save()
        playersConfig.save()
    }

    private fun loadFromFiles() {
        loadGroupsFromFile()
        loadPlayersFromFile()
    }

    private fun loadGroupsFromFile() {
        groupsConfig.root.keys.forEach { groupName ->
            val displayName = groupsConfig.getString("$groupName.display-name") ?: "&f$groupName"
            val priority = groupsConfig.getInt("$groupName.priority") ?: 0

            val group = Group(groupName, displayName, priority)

            val permsSection = groupsConfig.getSection("$groupName.permissions")
            permsSection?.root?.forEach { (perm, value) ->
                if (value is Boolean) {
                    group.setPermission(perm, value)
                }
            }

            groupsCache[groupName] = group
        }
    }

    private fun loadPlayersFromFile() {
        playersConfig.root.keys.forEach { playerName ->
            val player = PlayerPermission(playerName)

            val groupList = playersConfig.getStringList("$playerName.groups") ?: listOf("default")
            player.groups.clear()
            player.groups.addAll(groupList.distinct().filter { it != "default" } + "default")

            val permsSection = playersConfig.getSection("$playerName.permissions")
            permsSection?.root?.forEach { (perm, value) ->
                if (value is Boolean) {
                    player.setPermission(perm, value)
                }
            }

            playersCache[playerName] = player
        }
    }

    private fun saveGroupsToFile() {
        groupsCache.forEach { (groupName, group) ->
            groupsConfig.setString("$groupName.display-name", group.displayName)
            groupsConfig.setInt("$groupName.priority", group.priority)

            val permsPath = "$groupName.permissions"
            val permsSection = groupsConfig.getOrCreateSection(permsPath)
            permsSection.root.clear()

            group.permissions.forEach { (perm, value) ->
                permsSection.setLiteralKey("", perm, value)
            }
        }
        groupsConfig.save(groupsFile)
    }

    private fun savePlayersToFile() {
        playersCache.forEach { (playerName, player) ->
            val groupsToSave = player.getGroupsExcludingDefault() + "default"
            playersConfig.setStringList("$playerName.groups", groupsToSave)

            val permsPath = "$playerName.permissions"
            val permsSection = playersConfig.getOrCreateSection(permsPath)
            permsSection.root.clear()

            player.permissions.forEach { (perm, value) ->
                permsSection.setLiteralKey("", perm, value)
            }
        }
        playersConfig.save(playersFile)
    }


    /**
     * Get all groups in memory.
     */
    fun getAllGroups(): Collection<Group> = groupsCache.values

    /**
     * Get a group by name.
     */
    fun getGroup(name: String): Group? = groupsCache[name]

    /**
     * Create a new group.
     */
    fun createGroup(name: String, displayName: String, priority: Int, permissions: Map<String, Boolean> = emptyMap()): Group {
        val group = Group(name, displayName, priority)
        permissions.forEach { (perm, value) -> group.setPermission(perm, value) }
        groupsCache[name] = group
        save()
        return group
    }

    /**
     * Delete a group.
     */
    fun deleteGroup(name: String): Boolean {
        if (name == "default") return false
        val removed = groupsCache.remove(name) != null
        if (removed) save()
        return removed
    }

    /**
     * Check if a group exists.
     */
    fun hasGroup(name: String): Boolean = groupsCache.containsKey(name)

    /**
     * Get all players in memory.
     */
    fun getAllPlayers(): Collection<PlayerPermission> = playersCache.values

    /**
     * Get a player's permissions by name, creating if not exists.
     */
    fun getPlayer(name: String): PlayerPermission {
        return playersCache.getOrPut(name) { PlayerPermission(name) }
    }

    /**
     * Check if a player exists in cache.
     */
    fun hasPlayer(name: String): Boolean = playersCache.containsKey(name)

    /**
     * Resolves and returns all permissions for a player, considering group priorities and individual overrides.
     */
    fun getPlayerPermissions(playerName: String): Map<String, Boolean> {
        val player = getPlayer(playerName)
        val result = mutableMapOf<String, Boolean>()

        val sortedGroups = player.groups
            .mapNotNull { groupsCache[it] }
            .sortedByDescending { it.priority }

        for (group in sortedGroups) {
            group.permissions.forEach { (perm, value) ->
                if (!result.containsKey(perm)) {
                    result[perm] = value
                }
            }
        }

        result.putAll(player.permissions)

        return result
    }

    /**
     * Returns a list of all permission keys granted to the player (excluding those set to false).
     */
    fun getPermissionList(playerName: String): List<String> {
        return getPlayerPermissions(playerName)
            .filter { it.value }
            .map { it.key }
    }

    /**
     * Check if a player has a specific permission.
     */
    fun hasPermission(playerName: String, permission: String): Boolean {
        return getPlayerPermissions(playerName)[permission] ?: false
    }

    /**
     * Get the highest priority group for a player.
     */
    fun getHighestGroup(playerName: String): String {
        val player = getPlayer(playerName)
        val groups = player.getGroupsExcludingDefault()
        if (groups.isEmpty()) return "default"

        val highest = groups
            .mapNotNull { groupsCache[it] }
            .maxByOrNull { it.priority }

        return highest?.name ?: "default"
    }

    /**
     * Add a group to a player.
     */
    fun addGroupToPlayer(playerName: String, groupName: String): Boolean {
        if (!hasGroup(groupName)) return false
        val player = getPlayer(playerName)
        player.addGroup(groupName)
        save()
        return true
    }

    /**
     * Remove a group from a player.
     */
    fun removeGroupFromPlayer(playerName: String, groupName: String): Boolean {
        if (groupName == "default") return false
        val player = getPlayer(playerName)
        player.removeGroup(groupName)
        save()
        return true
    }

    /**
     * Set a permission for a group.
     */
    fun setGroupPermission(groupName: String, permission: String, value: Boolean): Boolean {
        val group = getGroup(groupName) ?: return false
        group.setPermission(permission, value)
        save()
        return true
    }

    /**
     * Set a permission for a player.
     */
    fun setPlayerPermission(playerName: String, permission: String, value: Boolean): Boolean {
        val player = getPlayer(playerName)
        player.setPermission(permission, value)
        save()
        return true
    }

    /**
     * Remove a permission from a group.
     */
    fun removeGroupPermission(groupName: String, permission: String): Boolean {
        val group = getGroup(groupName) ?: return false
        group.removePermission(permission)
        save()
        return true
    }

    /**
     * Remove a permission from a player.
     */
    fun removePlayerPermission(playerName: String, permission: String): Boolean {
        val player = getPlayer(playerName)
        player.removePermission(permission)
        save()
        return true
    }

    /**
     * Returns a list of all group instances the player belongs to, always including the default group.
     *
     * @param playerName The name of the player.
     * @return List of Group instances for the player, including the default group.
     */
    fun getPlayerGroups(playerName: String): List<Group> {
        val player = getPlayer(playerName)
        val groupNames = player.groups.distinct().plus("default").distinct()
        return groupNames.mapNotNull { getGroup(it) }
    }
}