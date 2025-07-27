package org.dandelion.classic.commands

import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.OnSubCommand
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.permission.PermissionRepository
import org.dandelion.classic.server.Console
import kotlin.math.ceil

@CommandDef(
    name = "permission",
    description = "Manage server permissions and groups",
    usage = "/permission <subcommand>",
    aliases = ["perm", "p"]
)
class PermissionCommand : Command {

    @OnSubCommand(name = "group", description = "Manage permission groups", usage = "/perm group <subcommand>")
    @RequirePermission("dandelion.permission.view")
    fun groupCommand(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            showGroupHelp(executor)
            return
        }
        when (args[0].lowercase()) {
            "create" -> handleGroupCreate(executor, args.drop(1).toTypedArray())
            "delete" -> handleGroupDelete(executor, args.drop(1).toTypedArray())
            "list" -> handleGroupList(executor, args.drop(1).toTypedArray())
            "info" -> handleGroupInfo(executor, args.drop(1).toTypedArray())
            "setpriority" -> handleGroupSetPriority(executor, args.drop(1).toTypedArray())
            "setdisplay" -> handleGroupSetDisplay(executor, args.drop(1).toTypedArray())
            "grant" -> handleGroupGrantPermission(executor, args.drop(1).toTypedArray())
            "revoke" -> handleGroupRevokePermission(executor, args.drop(1).toTypedArray())
            "removepermission" -> handleGroupRemovePermissionEntry(executor, args.drop(1).toTypedArray()) // Novo
            "listperms" -> handleGroupListPermissions(executor, args.drop(1).toTypedArray())
            "hasperm" -> handleGroupHasPermission(executor, args.drop(1).toTypedArray())
            else -> executor.sendMessage("&cUnknown group subcommand. Use &7/perm group &cfor help.")
        }
    }

    @OnSubCommand(name = "player", description = "Manage player permissions", usage = "/perm player <subcommand>")
    @RequirePermission("dandelion.permission.view")
    fun playerCommand(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            showPlayerHelp(executor)
            return
        }
        when (args[0].lowercase()) {
            "info" -> handlePlayerInfo(executor, args.drop(1).toTypedArray())
            "addgroup" -> handlePlayerAddGroup(executor, args.drop(1).toTypedArray())
            "removegroup", "remgroup" -> handlePlayerRemoveGroup(executor, args.drop(1).toTypedArray())
            "setgroups" -> handlePlayerSetGroups(executor, args.drop(1).toTypedArray())
            "listgroups" -> handlePlayerListGroups(executor, args.drop(1).toTypedArray())
            "grant" -> handlePlayerGrantPermission(executor, args.drop(1).toTypedArray()) // Atualizado
            "revoke" -> handlePlayerRevokePermission(executor, args.drop(1).toTypedArray()) // Atualizado
            "removepermission" -> handlePlayerRemovePermissionEntry(executor, args.drop(1).toTypedArray()) // Novo
            "listperms" -> handlePlayerListPermissions(executor, args.drop(1).toTypedArray())
            "listownperms" -> handlePlayerListOwnPermissions(executor, args.drop(1).toTypedArray())
            "hasperm" -> handlePlayerHasPermission(executor, args.drop(1).toTypedArray())
            "check" -> handlePlayerCheckPermission(executor, args.drop(1).toTypedArray())
            else -> executor.sendMessage("&cUnknown player subcommand. Use &7/perm player &cfor help.")
        }
    }

    @OnSubCommand(name = "reload", description = "Reload permissions from files", usage = "/perm reload")
    @RequirePermission("dandelion.permission.reload")
    fun reloadPermissions(executor: CommandExecutor, args: Array<String>) {
        try {
            PermissionRepository.reload()
            executor.sendMessage("&aPermissions reloaded successfully.")
        } catch (e: Exception) {
            executor.sendMessage("&cFailed to reload permissions: ${e.message}")
        }
    }

    @OnSubCommand(name = "who", description = "List who has a specific permission", usage = "/perm who <permission>")
    @RequirePermission("dandelion.permission.view")
    @ArgRange(min = 1, max = 1)
    fun whoHasPermission(executor: CommandExecutor, args: Array<String>) {
        val permission = args[0]
        val playersWithPerm = mutableListOf<String>()
        val groupsWithPerm = mutableListOf<String>()

        PermissionRepository.getAllGroups().forEach { group ->
            val permValue = group.permissions[permission]
            if (permValue != null) { // Mostra GRANTED ou REVOKED
                val statusColor = if (permValue) "&a" else "&c"
                val statusText = if (permValue) "GRANTED" else "REVOKED"
                groupsWithPerm.add("&7${group.name} &f(${group.displayName}&f) [&b$statusText&f]")
            }
        }

        PermissionRepository.getAllPlayers().forEach { player ->
            val individualPerm = player.permissions[permission]
            if (individualPerm != null) {
                val statusColor = if (individualPerm) "&a" else "&c"
                val statusText = if (individualPerm) "GRANTED" else "REVOKED"
                playersWithPerm.add("&7${player.name} [&b$statusText&f]")
            } else if (PermissionRepository.hasPermission(player.name, permission)) {
                playersWithPerm.add("&7${player.name} [&aGRANTED&f via group]")
            }
        }

        executor.sendMessage("&eWho has permission '&7$permission&e':")
        if (groupsWithPerm.isNotEmpty()) {
            executor.sendMessage("&fGroups: ${groupsWithPerm.joinToString(", ")}")
        }
        if (playersWithPerm.isNotEmpty()) {
            executor.sendMessage("&fPlayers: ${playersWithPerm.joinToString(", ")}")
        }
        if (groupsWithPerm.isEmpty() && playersWithPerm.isEmpty()) {
            executor.sendMessage("&cNo one has this permission explicitly set.")
        }
    }

    @OnExecute
    fun showAvailableSubCommands(executor: CommandExecutor, args: Array<String>) {
        val commandInfo = org.dandelion.classic.commands.manager.CommandRegistry.getCommands().find {
            it.name.equals("permission", ignoreCase = true)
        } ?: run {
            executor.sendMessage("&cCommand info not found.")
            return
        }

        val available = commandInfo.subCommands.values.filter {
            it.permission.isEmpty() || executor.hasPermission(it.permission)
        }.map {
            "&7${it.name}&a"
        }

        if (available.isEmpty()) {
            executor.sendMessage("&cNo subcommands available.")
        } else {
            executor.sendMessage("&eAvailable SubCommands: ${available.joinToString(", ")}")
        }
    }

    private fun showGroupHelp(executor: CommandExecutor) {
        executor.sendMessage("&eGroup Management Commands:")
        if (executor.hasPermission("dandelion.permission.group.create")) {
            executor.sendMessage("&7- create <name> <displayName> <priority> &f- Create new group")
        }
        if (executor.hasPermission("dandelion.permission.group.delete")) {
            executor.sendMessage("&7- delete <name> [confirm] &f- Delete group")
        }
        executor.sendMessage("&7- list [page] &f- List all groups")
        executor.sendMessage("&7- info <name> &f- Show group information")
        if (executor.hasPermission("dandelion.permission.group.edit")) {
            executor.sendMessage("&7- setpriority <name> <priority> &f- Set group priority")
            executor.sendMessage("&7- setdisplay <name> <displayName> &f- Set display name")
            executor.sendMessage("&7- grant <name> <permission> &f- Grant permission")
            executor.sendMessage("&7- revoke <name> <permission> &f- Revoke permission")
            executor.sendMessage("&7- removepermission <name> <permission> &f- Remove permission entry")
        }
        executor.sendMessage("&7- listperms <name> &f- List group permissions")
        executor.sendMessage("&7- hasperm <name> <permission> &f- Check permission")
    }

    private fun showPlayerHelp(executor: CommandExecutor) {
        executor.sendMessage("&ePlayer Management Commands:")
        executor.sendMessage("&7- info <player> &f- Show player information")
        if (executor.hasPermission("dandelion.permission.player.groups")) {
            executor.sendMessage("&7- addgroup <player> <group> &f- Add group to player")
            executor.sendMessage("&7- removegroup <player> <group> &f- Remove group from player")
            executor.sendMessage("&7- setgroups <player> <group1,group2,...> &f- Set player groups")
        }
        executor.sendMessage("&7- listgroups <player> &f- List player groups")
        if (executor.hasPermission("dandelion.permission.player.perms")) {
            executor.sendMessage("&7- grant <player> <permission> &f- Grant individual permission")
            executor.sendMessage("&7- revoke <player> <permission> &f- Revoke individual permission")
            executor.sendMessage("&7- removepermission <player> <permission> &f- Remove individual permission entry")
        }
        executor.sendMessage("&7- listperms <player> &f- List all effective permissions")
        executor.sendMessage("&7- listownperms <player> &f- List individual permissions only")
        executor.sendMessage("&7- hasperm <player> <permission> &f- Check if has permission")
        executor.sendMessage("&7- check <player> <permission> &f- Check permission with source")
    }
    private fun handleGroupCreate(executor: CommandExecutor, args: Array<String>) {
        if (!executor.hasPermission("dandelion.permission.group.create")) {
            executor.sendMessage("&cYou don't have permission to create groups.")
            return
        }
        if (args.size < 3) {
            executor.sendMessage("&cUsage: /perm group create <name> <displayName> <priority>")
            return
        }
        val name = args[0].lowercase()
        val displayName = args[1]
        val priority = args[2].toIntOrNull()
        if (priority == null) {
            executor.sendMessage("&cInvalid priority. Must be a number.")
            return
        }
        if (PermissionRepository.hasGroup(name)) {
            executor.sendMessage("&cGroup '&7$name&c' already exists.")
            return
        }
        PermissionRepository.createGroup(name, displayName, priority)
        executor.sendMessage("&aGroup '&7$name&a' created with display name '&7$displayName&a' and priority &7$priority&a.")
    }

    private fun handleGroupDelete(executor: CommandExecutor, args: Array<String>) {
        if (!executor.hasPermission("dandelion.permission.group.delete")) {
            executor.sendMessage("&cYou don't have permission to delete groups.")
            return
        }
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /perm group delete <name> [confirm]")
            return
        }
        val name = args[0].lowercase()
        if (name == "default") {
            executor.sendMessage("&cCannot delete the default group.")
            return
        }
        if (args.size == 1) {
            executor.sendMessage("This will permanently delete group '&7$name&c' and remove it from all players.")
            executor.sendMessage("Execute &7'/perm group delete $name confirm' &cto confirm.")
            return
        }
        if (args[1].lowercase() != "confirm") {
            executor.sendMessage("&cInvalid confirmation. Use 'confirm' to proceed.")
            return
        }
        if (!PermissionRepository.hasGroup(name)) {
            executor.sendMessage("&cGroup '&7$name&c' not found.")
            return
        }
        PermissionRepository.getAllPlayers().forEach { player ->
            player.removeGroup(name)
        }
        if (PermissionRepository.deleteGroup(name)) {
            executor.sendMessage("&aGroup '&7$name&a' deleted successfully.")
        } else {
            executor.sendMessage("&cFailed to delete group '&7$name&c'.")
        }
    }

    private fun handleGroupList(executor: CommandExecutor, args: Array<String>) {
        val groups = PermissionRepository.getAllGroups().sortedByDescending { it.priority }
        if (groups.isEmpty()) {
            executor.sendMessage("&cNo groups found.")
            return
        }
        val page = if (args.isNotEmpty()) args[0].toIntOrNull() ?: 1 else 1
        val groupsPerPage = 10
        val totalPages = ceil(groups.size.toDouble() / groupsPerPage).toInt()
        if (page < 1 || page > totalPages) {
            executor.sendMessage("&cInvalid page number. Valid range: 1-$totalPages")
            return
        }
        val startIndex = (page - 1) * groupsPerPage
        val endIndex = minOf(startIndex + groupsPerPage, groups.size)
        executor.sendMessage("&ePermission Groups (Page $page/$totalPages):")
        for (i in startIndex until endIndex) {
            val group = groups[i]
            val playerCount = PermissionRepository.getAllPlayers().count { it.groups.contains(group.name) }
            executor.sendMessage("&f- &7${group.name} &f(${group.displayName}&f) &7- Priority: &b${group.priority}&7, Players: &b$playerCount")
        }
    }

    private fun handleGroupInfo(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /perm group info <name>")
            return
        }
        val name = args[0].lowercase()
        val group = PermissionRepository.getGroup(name)
        if (group == null) {
            executor.sendMessage("&cGroup '&7$name&c' not found.")
            return
        }
        val playerCount = PermissionRepository.getAllPlayers().count { it.groups.contains(group.name) }
        executor.sendMessage("&eGroup Information: &7${group.name}")
        executor.sendMessage("&7Display Name: &b${group.displayName}")
        executor.sendMessage("&7Priority: &b${group.priority}")
        executor.sendMessage("&7Players: &b$playerCount")
        executor.sendMessage("&7Permissions: &b${group.permissions.size}")
        if (group.permissions.isNotEmpty()) {
            val permissions = group.permissions.entries.sortedBy { it.key }
            executor.sendMessage("&7Permission List:")
            permissions.forEach { (perm, value) ->
                val statusColor = if (value) "&a" else "&c"
                val statusText = if (value) "GRANTED" else "REVOKED"
                executor.sendMessage("  &7- $perm: $statusColor$statusText")
            }
        }
    }

    private fun handleGroupSetPriority(executor: CommandExecutor, args: Array<String>) {
        if (!executor.hasPermission("dandelion.permission.group.edit")) {
            executor.sendMessage("&cYou don't have permission to edit groups.")
            return
        }
        if (args.size != 2) {
            executor.sendMessage("&cUsage: /perm group setpriority <name> <priority>")
            return
        }
        val name = args[0].lowercase()
        val priority = args[1].toIntOrNull()
        if (priority == null) {
            executor.sendMessage("&cInvalid priority. Must be a number.")
            return
        }
        val group = PermissionRepository.getGroup(name)
        if (group == null) {
            executor.sendMessage("&cGroup '&7$name&c' not found.")
            return
        }
        group.priority = priority
        executor.sendMessage("&aGroup '&7$name&a' priority set to &7$priority&a.")
    }

    private fun handleGroupSetDisplay(executor: CommandExecutor, args: Array<String>) {
        if (!executor.hasPermission("dandelion.permission.group.edit")) {
            executor.sendMessage("&cYou don't have permission to edit groups.")
            return
        }
        if (args.size < 2) {
            executor.sendMessage("&cUsage: /perm group setdisplay <name> <displayName>")
            return
        }
        val name = args[0].lowercase()
        val displayName = args.drop(1).joinToString(" ")
        val group = PermissionRepository.getGroup(name)
        if (group == null) {
            executor.sendMessage("&cGroup '&7$name&c' not found.")
            return
        }
        group.displayName = displayName
        executor.sendMessage("&aGroup '&7$name&a' display name set to '&7$displayName&a'.")
    }

    private fun handleGroupGrantPermission(executor: CommandExecutor, args: Array<String>) {
        if (!executor.hasPermission("dandelion.permission.group.edit")) {
            executor.sendMessage("&cYou don't have permission to edit group permissions.")
            return
        }
        if (args.size != 2) {
            executor.sendMessage("&cUsage: /perm group grant <name> <permission>")
            return
        }
        val name = args[0].lowercase()
        val permission = args[1]
        if (PermissionRepository.setGroupPermission(name, permission, true)) { // true = GRANT
            executor.sendMessage("&aPermission '&7$permission&a' granted to group '&7$name&a'.")
        } else {
            executor.sendMessage("&cGroup '&7$name&c' not found.")
        }
    }

    private fun handleGroupRevokePermission(executor: CommandExecutor, args: Array<String>) {
        if (!executor.hasPermission("dandelion.permission.group.edit")) {
            executor.sendMessage("&cYou don't have permission to edit group permissions.")
            return
        }
        if (args.size != 2) {
            executor.sendMessage("&cUsage: /perm group revoke <name> <permission>")
            return
        }
        val name = args[0].lowercase()
        val permission = args[1]
        if (PermissionRepository.setGroupPermission(name, permission, false)) { // false = REVOKE
            executor.sendMessage("&aPermission '&7$permission&a' revoked from group '&7$name&a'.")
        } else {
            executor.sendMessage("&cGroup '&7$name&c' not found.")
        }
    }

    private fun handleGroupRemovePermissionEntry(executor: CommandExecutor, args: Array<String>) {
        if (!executor.hasPermission("dandelion.permission.group.edit")) {
            executor.sendMessage("&cYou don't have permission to edit group permissions.")
            return
        }
        if (args.size != 2) {
            executor.sendMessage("&cUsage: /perm group removepermission <name> <permission>")
            return
        }
        val name = args[0].lowercase()
        val permission = args[1]
        if (PermissionRepository.removeGroupPermission(name, permission)) {
            executor.sendMessage("&aPermission '&7$permission&a' entry removed from group '&7$name&a'.")
        } else {
            executor.sendMessage("&cGroup '&7$name&c' not found or permission entry doesn't exist.")
        }
    }


    private fun handleGroupListPermissions(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /perm group listperms <name>")
            return
        }
        val name = args[0].lowercase()
        val group = PermissionRepository.getGroup(name)
        if (group == null) {
            executor.sendMessage("&cGroup '&7$name&c' not found.")
            return
        }
        if (group.permissions.isEmpty()) {
            executor.sendMessage("&eGroup '&7$name&e' has no permissions.")
            return
        }
        executor.sendMessage("&ePermissions for group '&7$name&e':")
        group.permissions.entries.sortedBy { it.key }.forEach { (perm, value) ->
            val statusColor = if (value) "&a" else "&c"
            val statusText = if (value) "GRANTED" else "REVOKED"
            executor.sendMessage("&7- $perm: $statusColor$statusText")
        }
    }

    private fun handleGroupHasPermission(executor: CommandExecutor, args: Array<String>) {
        if (args.size != 2) {
            executor.sendMessage("&cUsage: /perm group hasperm <name> <permission>")
            return
        }
        val name = args[0].lowercase()
        val permission = args[1]
        val group = PermissionRepository.getGroup(name)
        if (group == null) {
            executor.sendMessage("&cGroup '&7$name&c' not found.")
            return
        }
        val permValue = group.permissions[permission]
        if (permValue == null) {
            executor.sendMessage("&eGroup '&7$name&e' does not have permission '&7$permission&e' set.")
        } else {
            val statusColor = if (permValue) "&a" else "&c"
            val statusText = if (permValue) "GRANTED" else "REVOKED"
            executor.sendMessage("&eGroup '&7$name&e' has permission '&7$permission&e': $statusColor$statusText")
        }
    }
    private fun handlePlayerInfo(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /perm player info <player>")
            return
        }
        val playerName = args[0]
        val player = PermissionRepository.getPlayer(playerName)
        if (player == null) {
            executor.sendMessage("&cPlayer '&7$playerName&c' not found.")
            return
        }
        val groups = PermissionRepository.getPlayerGroups(playerName)
        val highestGroup = PermissionRepository.getHighestGroup(playerName)
        val totalPermissions = PermissionRepository.getPermissionList(playerName).size
        executor.sendMessage("&ePlayer Information: &7$playerName")
        executor.sendMessage("&7Highest Group: &b$highestGroup")
        executor.sendMessage("&7Total Groups: &b${groups.size}")
        executor.sendMessage("&7Total Permissions: &b$totalPermissions")
        executor.sendMessage("&7Individual Permissions: &b${player.permissions.size}")
        if (groups.isNotEmpty()) {
            val groupNames = groups.sortedByDescending { it.priority }.map { "&7${it.name}" }
            executor.sendMessage("&7Groups: ${groupNames.joinToString("&f, ")}")
        }
    }

    private fun handlePlayerAddGroup(executor: CommandExecutor, args: Array<String>) {
        if (!executor.hasPermission("dandelion.permission.player.groups")) {
            executor.sendMessage("&cYou don't have permission to manage player groups.")
            return
        }
        if (args.size != 2) {
            executor.sendMessage("&cUsage: /perm player addgroup <player> <group>")
            return
        }
        val playerName = args[0]
        val groupName = args[1].lowercase()
        if (PermissionRepository.addGroupToPlayer(playerName, groupName)) {
            executor.sendMessage("&aGroup '&7$groupName&a' added to player '&7$playerName&a'.")
        } else {
            executor.sendMessage("&cGroup '&7$groupName&c' not found.")
        }
    }

    private fun handlePlayerRemoveGroup(executor: CommandExecutor, args: Array<String>) {
        if (!executor.hasPermission("dandelion.permission.player.groups")) {
            executor.sendMessage("&cYou don't have permission to manage player groups.")
            return
        }
        if (args.size != 2) {
            executor.sendMessage("&cUsage: /perm player removegroup <player> <group>")
            return
        }
        val playerName = args[0]
        val groupName = args[1].lowercase()
        if (PermissionRepository.removeGroupFromPlayer(playerName, groupName)) {
            executor.sendMessage("&aGroup '&7$groupName&a' removed from player '&7$playerName&a'.")
        } else {
            executor.sendMessage("&cCannot remove group '&7$groupName&c' (may be default group).")
        }
    }

    private fun handlePlayerSetGroups(executor: CommandExecutor, args: Array<String>) {
        if (!executor.hasPermission("dandelion.permission.player.groups")) {
            executor.sendMessage("&cYou don't have permission to manage player groups.")
            return
        }
        if (args.size != 2) {
            executor.sendMessage("&cUsage: /perm player setgroups <player> <group1,group2,...>")
            return
        }
        val playerName = args[0]
        val groupNames = args[1].split(",").map { it.trim().lowercase() }
        val player = PermissionRepository.getPlayer(playerName)
        val validGroups = groupNames.filter { PermissionRepository.hasGroup(it) }
        val invalidGroups = groupNames - validGroups.toSet()
        if (invalidGroups.isNotEmpty()) {
            executor.sendMessage("&cInvalid groups: ${invalidGroups.joinToString(", ")}")
            return
        }
        player.groups.clear()
        validGroups.forEach { player.addGroup(it) }
        if (!player.groups.contains("default")) {
            player.addGroup("default")
        }
        executor.sendMessage("&aPlayer '&7$playerName&a' groups set to: &7${validGroups.joinToString(", ")}")
    }

    private fun handlePlayerListGroups(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /perm player listgroups <player>")
            return
        }
        val playerName = args[0]
        val groups = PermissionRepository.getPlayerGroups(playerName)
        if (groups.isEmpty()) {
            executor.sendMessage("&ePlayer '&7$playerName&e' has no groups.")
            return
        }
        executor.sendMessage("&eGroups for player '&7$playerName&e':")
        groups.sortedByDescending { it.priority }.forEach { group ->
            executor.sendMessage("  &7${group.name} &f(${group.displayName}&f) &7- Priority: &b${group.priority}")
        }
    }
    private fun handlePlayerGrantPermission(executor: CommandExecutor, args: Array<String>) {
        if (!executor.hasPermission("dandelion.permission.player.perms")) {
            executor.sendMessage("&cYou don't have permission to manage player permissions.")
            return
        }
        if (args.size != 2) {
            executor.sendMessage("&cUsage: /perm player grant <player> <permission>")
            return
        }
        val playerName = args[0]
        val permission = args[1]
        PermissionRepository.setPlayerPermission(playerName, permission, true) // true = GRANT
        executor.sendMessage("&aPermission '&7$permission&a' granted to player '&7$playerName&a'.")
    }

    private fun handlePlayerRevokePermission(executor: CommandExecutor, args: Array<String>) {
        if (!executor.hasPermission("dandelion.permission.player.perms")) {
            executor.sendMessage("&cYou don't have permission to manage player permissions.")
            return
        }
        if (args.size != 2) {
            executor.sendMessage("&cUsage: /perm player revoke <player> <permission>")
            return
        }
        val playerName = args[0]
        val permission = args[1]
        PermissionRepository.setPlayerPermission(playerName, permission, false) // false = REVOKE
        executor.sendMessage("&aPermission '&7$permission&a' revoked from player '&7$playerName&a'.")
    }
    private fun handlePlayerRemovePermissionEntry(executor: CommandExecutor, args: Array<String>) {
        if (!executor.hasPermission("dandelion.permission.player.perms")) {
            executor.sendMessage("&cYou don't have permission to manage player permissions.")
            return
        }
        if (args.size != 2) {
            executor.sendMessage("&cUsage: /perm player removepermission <player> <permission>")
            return
        }
        val playerName = args[0]
        val permission = args[1]
        if (PermissionRepository.removePlayerPermission(playerName, permission)) {
            executor.sendMessage("&aPermission '&7$permission&a' entry removed from player '&7$playerName&a'.")
        } else {
            executor.sendMessage("&cPlayer '&7$playerName&c' does not have permission entry '&7$permission&c' set.")
        }
    }
    private fun handlePlayerListPermissions(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /perm player listperms <player>")
            return
        }
        val playerName = args[0]
        val permissions = PermissionRepository.getPermissionList(playerName)
        if (permissions.isEmpty()) {
            executor.sendMessage("&ePlayer '&7$playerName&e' has no effective permissions.")
            return
        }
        executor.sendMessage("&eEffective permissions for player '&7$playerName&e':")
        permissions.sorted().forEach { perm ->
            val player = PermissionRepository.getPlayer(playerName)
            val individualValue = player.permissions[perm]
            if (individualValue != null) {
                val statusColor = if (individualValue) "&a" else "&c"
                val statusText = if (individualValue) "GRANTED" else "REVOKED"
                executor.sendMessage("&7- $perm: $statusColor$statusText &7(Individual)")
            } else {
                executor.sendMessage("&7- $perm: &aGRANTED &7(via group)")
            }
        }
    }

    private fun handlePlayerListOwnPermissions(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("&cUsage: /perm player listownperms <player>")
            return
        }
        val playerName = args[0]
        val player = PermissionRepository.getPlayer(playerName)
        val ownPermissions = player.permissions
        if (ownPermissions.isEmpty()) {
            executor.sendMessage("&ePlayer '&7$playerName&e' has no individual permissions.")
            return
        }
        executor.sendMessage("&eIndividual permissions for player '&7$playerName&e':")
        ownPermissions.entries.sortedBy { it.key }.forEach { (perm, value) ->
            val statusColor = if (value) "&a" else "&c"
            val statusText = if (value) "GRANTED" else "REVOKED"
            executor.sendMessage("&7- $perm: $statusColor$statusText")
        }
    }

    private fun handlePlayerHasPermission(executor: CommandExecutor, args: Array<String>) {
        if (args.size != 2) {
            executor.sendMessage("&cUsage: /perm player hasperm <player> <permission>")
            return
        }
        val playerName = args[0]
        val permission = args[1]
        val hasPermission = PermissionRepository.hasPermission(playerName, permission)
        val status = if (hasPermission) "&aYES" else "&cNO"
        executor.sendMessage("&ePlayer '&7$playerName&e' has permission '&7$permission&e': $status")
    }

    private fun handlePlayerCheckPermission(executor: CommandExecutor, args: Array<String>) {
        if (args.size != 2) {
            executor.sendMessage("&cUsage: /perm player check <player> <permission>")
            return
        }
        val playerName = args[0]
        val permission = args[1]

        val player = PermissionRepository.getPlayer(playerName)
        val groups = PermissionRepository.getPlayerGroups(playerName)
        val individualValue = player.permissions[permission]
        if (individualValue != null) {
            val statusColor = if (individualValue) "&a" else "&c"
            val statusText = if (individualValue) "GRANTED" else "REVOKED"
            executor.sendMessage("&ePlayer '&7$playerName&e' has permission '&7$permission&e' individually: $statusColor$statusText")
            return
        }

        var foundInGroup: String? = null
        var isGrantedInGroup = false
        for (group in groups) {
            val groupValue = group.permissions[permission]
            if (groupValue != null) {
                foundInGroup = group.name
                isGrantedInGroup = groupValue
                break
            }
        }

        if (foundInGroup != null) {
            val statusColor = if (isGrantedInGroup) "&a" else "&c"
            val statusText = if (isGrantedInGroup) "GRANTED" else "REVOKED"
            executor.sendMessage("&ePlayer '&7$playerName&e' has permission '&7$permission&e' via group '&7$foundInGroup&e': $statusColor$statusText")
        } else {
            executor.sendMessage("&ePlayer '&7$playerName&e' does not have permission '&7$permission&e' (not found in individual or group permissions).")
        }
    }
}

