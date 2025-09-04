package org.dandelion.server.permission

data class Group(
    val name: String, // in the future make name mutable?
    private var _displayName: String,
    private var _priority: Int,
    val permissions: MutableMap<String, Boolean> = mutableMapOf(),
) {
    // this is weird but will work
    var displayName: String
        get() = _displayName
        set(value) {
            _displayName = value
            PermissionRepository.save()
        }

    var priority: Int
        get() = _priority
        set(value) {
            _priority = value
            PermissionRepository.save()
        }

    fun setPermission(permission: String, value: Boolean) {
        permissions[permission] = value
    }

    fun removePermission(permission: String) {
        permissions.remove(permission)
    }

    fun hasPermission(permission: String): Boolean =
        permissions[permission] ?: false
}

data class PlayerPermission(
    val name: String,
    val groups: MutableList<String> = mutableListOf("default"),
    val permissions: MutableMap<String, Boolean> = mutableMapOf(),
) {
    fun setPermission(permission: String, value: Boolean) {
        permissions[permission] = value
    }

    fun removePermission(permission: String) {
        permissions.remove(permission)
    }

    fun hasPermission(permission: String): Boolean =
        permissions[permission] ?: false

    fun addGroup(groupName: String) {
        if (groupName != "default" && !groups.contains(groupName)) {
            groups.add(groupName)
        }
    }

    fun removeGroup(groupName: String) {
        if (groupName != "default") {
            groups.remove(groupName)
        }
    }

    fun getGroupsExcludingDefault(): List<String> =
        groups.filter { it != "default" }
}
