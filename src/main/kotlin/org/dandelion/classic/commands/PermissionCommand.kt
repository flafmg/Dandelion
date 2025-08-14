package org.dandelion.classic.commands

import kotlin.math.ceil
import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.OnSubCommand
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.permission.PermissionRepository
import org.dandelion.classic.server.MessageRegistry

@CommandDef(
    name = "permission",
    description = "Manage server permissions and groups",
    usage = "/permission <subcommand>",
    aliases = ["perm", "p"],
)
class PermissionCommand : Command {

    @OnSubCommand(
        name = "group",
        description = "Manage permission groups",
        usage = "/perm group <subcommand>",
    )
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
            "setpriority" ->
                handleGroupSetPriority(executor, args.drop(1).toTypedArray())
            "setdisplay" ->
                handleGroupSetDisplay(executor, args.drop(1).toTypedArray())
            "grant" ->
                handleGroupGrantPermission(
                    executor,
                    args.drop(1).toTypedArray(),
                )
            "revoke" ->
                handleGroupRevokePermission(
                    executor,
                    args.drop(1).toTypedArray(),
                )
            "removepermission" ->
                handleGroupRemovePermissionEntry(
                    executor,
                    args.drop(1).toTypedArray(),
                )
            "listperms" ->
                handleGroupListPermissions(
                    executor,
                    args.drop(1).toTypedArray(),
                )
            "hasperm" ->
                handleGroupHasPermission(executor, args.drop(1).toTypedArray())
            else ->
                executor.sendMessage(
                    "&cUnknown group subcommand. Use &7/perm group &cfor help."
                )
        }
    }

    @OnSubCommand(
        name = "player",
        description = "Manage player permissions",
        usage = "/perm player <subcommand>",
    )
    @RequirePermission("dandelion.permission.view")
    fun playerCommand(executor: CommandExecutor, args: Array<String>) {
        if (args.isEmpty()) {
            showPlayerHelp(executor)
            return
        }
        when (args[0].lowercase()) {
            "info" -> handlePlayerInfo(executor, args.drop(1).toTypedArray())
            "addgroup" ->
                handlePlayerAddGroup(executor, args.drop(1).toTypedArray())
            "removegroup",
            "remgroup" ->
                handlePlayerRemoveGroup(executor, args.drop(1).toTypedArray())
            "setgroups" ->
                handlePlayerSetGroups(executor, args.drop(1).toTypedArray())
            "listgroups" ->
                handlePlayerListGroups(executor, args.drop(1).toTypedArray())
            "grant" ->
                handlePlayerGrantPermission(
                    executor,
                    args.drop(1).toTypedArray(),
                )
            "revoke" ->
                handlePlayerRevokePermission(
                    executor,
                    args.drop(1).toTypedArray(),
                )
            "removepermission" ->
                handlePlayerRemovePermissionEntry(
                    executor,
                    args.drop(1).toTypedArray(),
                )
            "listperms" ->
                handlePlayerListPermissions(
                    executor,
                    args.drop(1).toTypedArray(),
                )
            "listownperms" ->
                handlePlayerListOwnPermissions(
                    executor,
                    args.drop(1).toTypedArray(),
                )
            "hasperm" ->
                handlePlayerHasPermission(executor, args.drop(1).toTypedArray())
            "check" ->
                handlePlayerCheckPermission(
                    executor,
                    args.drop(1).toTypedArray(),
                )
            else ->
                executor.sendMessage(
                    "&cUnknown player subcommand. Use &7/perm player &cfor help."
                )
        }
    }

    @OnSubCommand(
        name = "reload",
        description = "Reload permissions from files",
        usage = "/perm reload",
    )
    @RequirePermission("dandelion.permission.reload")
    fun reloadPermissions(executor: CommandExecutor, args: Array<String>) {
        try {
            PermissionRepository.reload()
            MessageRegistry.Commands.Permission.sendReloadSuccess(executor)
        } catch (e: Exception) {
            MessageRegistry.Commands.Permission.sendReloadFailed(
                executor,
                e.message ?: "Unknown error",
            )
        }
    }

    @OnSubCommand(
        name = "who",
        description = "List who has a specific permission",
        usage = "/perm who <permission>",
    )
    @RequirePermission("dandelion.permission.view")
    @ArgRange(min = 1, max = 1)
    fun whoHasPermission(executor: CommandExecutor, args: Array<String>) {
        val permission = args[0]
        val playersWithPerm = mutableListOf<String>()
        val groupsWithPerm = mutableListOf<String>()

        PermissionRepository.getAllGroups().forEach { group ->
            val permValue = group.permissions[permission]
            if (permValue != null) {
                val statusText = if (permValue) "GRANTED" else "REVOKED"
                groupsWithPerm.add(
                    MessageRegistry.Commands.Permission.Who.formatGroup(
                        group.name,
                        group.displayName,
                        statusText,
                    )
                )
            }
        }

        PermissionRepository.getAllPlayers().forEach { player ->
            val individualPerm = player.permissions[permission]
            if (individualPerm != null) {
                val statusText = if (individualPerm) "GRANTED" else "REVOKED"
                playersWithPerm.add(
                    MessageRegistry.Commands.Permission.Who.formatPlayer(
                        player.name,
                        statusText,
                    )
                )
            } else if (
                PermissionRepository.hasPermission(player.name, permission)
            ) {
                playersWithPerm.add(
                    MessageRegistry.Commands.Permission.Who
                        .formatPlayerViaGroup(player.name)
                )
            }
        }

        MessageRegistry.Commands.Permission.Who.sendHeader(executor, permission)
        if (groupsWithPerm.isNotEmpty()) {
            MessageRegistry.Commands.Permission.Who.sendGroups(
                executor,
                groupsWithPerm.joinToString(", "),
            )
        }
        if (playersWithPerm.isNotEmpty()) {
            MessageRegistry.Commands.Permission.Who.sendPlayers(
                executor,
                playersWithPerm.joinToString(", "),
            )
        }
        if (groupsWithPerm.isEmpty() && playersWithPerm.isEmpty()) {
            MessageRegistry.Commands.Permission.Who.sendNoOne(executor)
        }
    }

    @OnExecute
    fun showAvailableSubCommands(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        val commandInfo =
            org.dandelion.classic.commands.manager.CommandRegistry.getCommands()
                .find { it.name.equals("permission", ignoreCase = true) }
                ?: run {
                    MessageRegistry.Commands.sendCommandError(executor)
                    return
                }

        val available =
            commandInfo.subCommands.values
                .filter {
                    it.permission.isEmpty() ||
                        executor.hasPermission(it.permission)
                }
                .map { "${it.name}" }

        if (available.isEmpty()) {
            MessageRegistry.Commands.Permission.sendNoSubcommandsAvailable(
                executor
            )
        } else {
            MessageRegistry.Commands.Permission.sendSubcommandsAvailable(
                executor,
                available.joinToString(", "),
            )
        }
    }

    private fun showGroupHelp(executor: CommandExecutor) {
        MessageRegistry.Commands.Permission.Help.sendGroupHeader(executor)
        if (executor.hasPermission("dandelion.permission.group.create")) {
            MessageRegistry.Commands.Permission.Help.sendGroupCreate(executor)
        }
        if (executor.hasPermission("dandelion.permission.group.delete")) {
            MessageRegistry.Commands.Permission.Help.sendGroupDelete(executor)
        }
        MessageRegistry.Commands.Permission.Help.sendGroupList(executor)
        MessageRegistry.Commands.Permission.Help.sendGroupInfo(executor)
        if (executor.hasPermission("dandelion.permission.group.edit")) {
            MessageRegistry.Commands.Permission.Help.sendGroupSetPriority(
                executor
            )
            MessageRegistry.Commands.Permission.Help.sendGroupSetDisplay(
                executor
            )
            MessageRegistry.Commands.Permission.Help.sendGroupGrant(executor)
            MessageRegistry.Commands.Permission.Help.sendGroupRevoke(executor)
            MessageRegistry.Commands.Permission.Help.sendGroupRemove(executor)
        }
        MessageRegistry.Commands.Permission.Help.sendGroupListPerms(executor)
        MessageRegistry.Commands.Permission.Help.sendGroupHasPerm(executor)
    }

    private fun showPlayerHelp(executor: CommandExecutor) {
        MessageRegistry.Commands.Permission.Help.sendPlayerHeader(executor)
        MessageRegistry.Commands.Permission.Help.sendPlayerInfo(executor)
        if (executor.hasPermission("dandelion.permission.player.groups")) {
            MessageRegistry.Commands.Permission.Help.sendPlayerAddGroup(
                executor
            )
            MessageRegistry.Commands.Permission.Help.sendPlayerRemoveGroup(
                executor
            )
            MessageRegistry.Commands.Permission.Help.sendPlayerSetGroups(
                executor
            )
        }
        MessageRegistry.Commands.Permission.Help.sendPlayerListGroups(executor)
        if (executor.hasPermission("dandelion.permission.player.perms")) {
            MessageRegistry.Commands.Permission.Help.sendPlayerGrant(executor)
            MessageRegistry.Commands.Permission.Help.sendPlayerRevoke(executor)
            MessageRegistry.Commands.Permission.Help.sendPlayerRemove(executor)
        }
        MessageRegistry.Commands.Permission.Help.sendPlayerListPerms(executor)
        MessageRegistry.Commands.Permission.Help.sendPlayerListOwnPerms(
            executor
        )
        MessageRegistry.Commands.Permission.Help.sendPlayerHasPerm(executor)
        MessageRegistry.Commands.Permission.Help.sendPlayerCheck(executor)
    }

    private fun handleGroupCreate(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (!executor.hasPermission("dandelion.permission.group.create")) {
            MessageRegistry.Commands.Permission.sendNoPermissionManageGroups(
                executor
            )
            return
        }
        if (args.size < 3) {
            MessageRegistry.Commands.Permission.Group.Create.sendUsage(executor)
            return
        }
        val name = args[0].lowercase()
        val displayName = args[1]
        val priority = args[2].toIntOrNull()
        if (priority == null) {
            MessageRegistry.Commands.Permission.Group.Create
                .sendInvalidPriority(executor)
            return
        }
        if (PermissionRepository.hasGroup(name)) {
            MessageRegistry.Commands.Permission.Group.Create.sendAlreadyExists(
                executor,
                name,
            )
            return
        }
        PermissionRepository.createGroup(name, displayName, priority)
        MessageRegistry.Commands.Permission.Group.Create.sendSuccess(
            executor,
            name,
            displayName,
            priority,
        )
    }

    private fun handleGroupDelete(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (!executor.hasPermission("dandelion.permission.group.delete")) {
            MessageRegistry.Commands.Permission.sendNoPermissionDeleteGroups(
                executor
            )
            return
        }
        if (args.isEmpty()) {
            MessageRegistry.Commands.Permission.Group.Delete.sendUsage(executor)
            return
        }
        val name = args[0].lowercase()
        if (name == "default") {
            MessageRegistry.Commands.Permission.Group.Delete
                .sendCannotDeleteDefault(executor)
            return
        }
        if (args.size == 1) {
            MessageRegistry.Commands.Permission.Group.Delete.sendConfirmMessage(
                executor,
                name,
            )
            MessageRegistry.Commands.Permission.Group.Delete
                .sendConfirmInstruction(executor, name)
            return
        }
        if (args[1].lowercase() != "confirm") {
            MessageRegistry.Commands.Permission.Group.Delete
                .sendInvalidConfirmation(executor)
            return
        }
        if (!PermissionRepository.hasGroup(name)) {
            MessageRegistry.Commands.Permission.Group.Delete.sendNotFound(
                executor,
                name,
            )
            return
        }
        PermissionRepository.getAllPlayers().forEach { player ->
            player.removeGroup(name)
        }
        if (PermissionRepository.deleteGroup(name)) {
            MessageRegistry.Commands.Permission.Group.Delete.sendSuccess(
                executor,
                name,
            )
        } else {
            MessageRegistry.Commands.Permission.Group.Delete.sendFailed(
                executor,
                name,
            )
        }
    }

    private fun handleGroupList(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        val groups =
            PermissionRepository.getAllGroups().sortedByDescending {
                it.priority
            }
        if (groups.isEmpty()) {
            MessageRegistry.Commands.Permission.Group.List.sendNoGroups(
                executor
            )
            return
        }
        val page = if (args.isNotEmpty()) args[0].toIntOrNull() ?: 1 else 1
        val groupsPerPage = 10
        val totalPages = ceil(groups.size.toDouble() / groupsPerPage).toInt()
        if (page < 1 || page > totalPages) {
            MessageRegistry.Commands.Permission.Group.List.sendInvalidPage(
                executor,
                totalPages,
            )
            return
        }
        val startIndex = (page - 1) * groupsPerPage
        val endIndex = minOf(startIndex + groupsPerPage, groups.size)
        MessageRegistry.Commands.Permission.Group.List.sendHeader(
            executor,
            page,
            totalPages,
        )
        for (i in startIndex until endIndex) {
            val group = groups[i]
            val playerCount =
                PermissionRepository.getAllPlayers().count {
                    it.groups.contains(group.name)
                }
            MessageRegistry.Commands.Permission.Group.List.sendGroup(
                executor,
                group.name,
                group.displayName,
                group.priority,
                playerCount,
            )
        }
    }

    private fun handleGroupInfo(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Permission.Group.Info.sendUsage(executor)
            return
        }
        val name = args[0].lowercase()
        val group = PermissionRepository.getGroup(name)
        if (group == null) {
            MessageRegistry.Commands.Permission.Group.Delete.sendNotFound(
                executor,
                name,
            )
            return
        }
        val playerCount =
            PermissionRepository.getAllPlayers().count {
                it.groups.contains(group.name)
            }
        MessageRegistry.Commands.Permission.Group.Info.sendHeader(
            executor,
            group.name,
        )
        MessageRegistry.Commands.Permission.Group.Info.sendDisplayName(
            executor,
            group.displayName,
        )
        MessageRegistry.Commands.Permission.Group.Info.sendPriority(
            executor,
            group.priority,
        )
        MessageRegistry.Commands.Permission.Group.Info.sendPlayers(
            executor,
            playerCount,
        )
        MessageRegistry.Commands.Permission.Group.Info.sendPermissions(
            executor,
            group.permissions.size,
        )
        if (group.permissions.isNotEmpty()) {
            val permissions = group.permissions.entries.sortedBy { it.key }
            MessageRegistry.Commands.Permission.Group.Info.sendPermissionList(
                executor
            )
            permissions.forEach { (perm, value) ->
                val statusText =
                    if (value)
                        MessageRegistry.Commands.Permission.Group.Info
                            .getGranted()
                    else
                        MessageRegistry.Commands.Permission.Group.Info
                            .getRevoked()
                MessageRegistry.Commands.Permission.Group.Info
                    .sendPermissionEntry(executor, perm, statusText)
            }
        }
    }

    private fun handleGroupSetPriority(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (!executor.hasPermission("dandelion.permission.group.edit")) {
            MessageRegistry.Commands.Permission.sendNoPermissionEditGroups(
                executor
            )
            return
        }
        if (args.size != 2) {
            MessageRegistry.Commands.Permission.Group.Edit.sendPriorityUsage(
                executor
            )
            return
        }
        val name = args[0].lowercase()
        val priority = args[1].toIntOrNull()
        if (priority == null) {
            MessageRegistry.Commands.Permission.Group.Create
                .sendInvalidPriority(executor)
            return
        }
        val group = PermissionRepository.getGroup(name)
        if (group == null) {
            MessageRegistry.Commands.Permission.Group.Delete.sendNotFound(
                executor,
                name,
            )
            return
        }
        group.priority = priority
        MessageRegistry.Commands.Permission.Group.Edit.sendPrioritySuccess(
            executor,
            name,
            priority,
        )
    }

    private fun handleGroupSetDisplay(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (!executor.hasPermission("dandelion.permission.group.edit")) {
            MessageRegistry.Commands.Permission.sendNoPermissionEditGroups(
                executor
            )
            return
        }
        if (args.size < 2) {
            MessageRegistry.Commands.Permission.Group.Edit.sendDisplayUsage(
                executor
            )
            return
        }
        val name = args[0].lowercase()
        val displayName = args.drop(1).joinToString(" ")
        val group = PermissionRepository.getGroup(name)
        if (group == null) {
            MessageRegistry.Commands.Permission.Group.Delete.sendNotFound(
                executor,
                name,
            )
            return
        }
        group.displayName = displayName
        MessageRegistry.Commands.Permission.Group.Edit.sendDisplaySuccess(
            executor,
            name,
            displayName,
        )
    }

    private fun handleGroupGrantPermission(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (!executor.hasPermission("dandelion.permission.group.edit")) {
            MessageRegistry.Commands.Permission.sendNoPermissionEditGroups(
                executor
            )
            return
        }
        if (args.size != 2) {
            MessageRegistry.Commands.Permission.Group.Permissions
                .sendGrantUsage(executor)
            return
        }
        val name = args[0].lowercase()
        val permission = args[1]
        if (PermissionRepository.setGroupPermission(name, permission, true)) {
            MessageRegistry.Commands.Permission.Group.Permissions
                .sendGrantSuccess(executor, permission, name)
        } else {
            MessageRegistry.Commands.Permission.Group.Permissions
                .sendGroupNotFound(executor, name)
        }
    }

    private fun handleGroupRevokePermission(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (!executor.hasPermission("dandelion.permission.group.edit")) {
            MessageRegistry.Commands.Permission.sendNoPermissionEditGroups(
                executor
            )
            return
        }
        if (args.size != 2) {
            MessageRegistry.Commands.Permission.Group.Permissions
                .sendRevokeUsage(executor)
            return
        }
        val name = args[0].lowercase()
        val permission = args[1]
        if (PermissionRepository.setGroupPermission(name, permission, false)) {
            MessageRegistry.Commands.Permission.Group.Permissions
                .sendRevokeSuccess(executor, permission, name)
        } else {
            MessageRegistry.Commands.Permission.Group.Permissions
                .sendGroupNotFound(executor, name)
        }
    }

    private fun handleGroupRemovePermissionEntry(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (!executor.hasPermission("dandelion.permission.group.edit")) {
            MessageRegistry.Commands.Permission.sendNoPermissionEditGroups(
                executor
            )
            return
        }
        if (args.size != 2) {
            MessageRegistry.Commands.Permission.Group.Permissions
                .sendRemoveUsage(executor)
            return
        }
        val name = args[0].lowercase()
        val permission = args[1]
        if (PermissionRepository.removeGroupPermission(name, permission)) {
            MessageRegistry.Commands.Permission.Group.Permissions
                .sendRemoveSuccess(executor, permission, name)
        } else {
            MessageRegistry.Commands.Permission.Group.Permissions
                .sendRemoveFailed(executor, name)
        }
    }

    private fun handleGroupListPermissions(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Permission.Group.Permissions.sendListUsage(
                executor
            )
            return
        }
        val name = args[0].lowercase()
        val group = PermissionRepository.getGroup(name)
        if (group == null) {
            MessageRegistry.Commands.Permission.Group.Permissions
                .sendGroupNotFound(executor, name)
            return
        }
        if (group.permissions.isEmpty()) {
            MessageRegistry.Commands.Permission.Group.Permissions.sendListEmpty(
                executor,
                name,
            )
            return
        }
        MessageRegistry.Commands.Permission.Group.Permissions.sendListHeader(
            executor,
            name,
        )
        group.permissions.entries
            .sortedBy { it.key }
            .forEach { (perm, value) ->
                val statusText =
                    if (value)
                        MessageRegistry.Commands.Permission.Group.Info
                            .getGranted()
                    else
                        MessageRegistry.Commands.Permission.Group.Info
                            .getRevoked()
                MessageRegistry.Commands.Permission.Group.Info
                    .sendPermissionEntry(executor, perm, statusText)
            }
    }

    private fun handleGroupHasPermission(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (args.size != 2) {
            MessageRegistry.Commands.Permission.Group.Permissions
                .sendCheckUsage(executor)
            return
        }
        val name = args[0].lowercase()
        val permission = args[1]
        val group = PermissionRepository.getGroup(name)
        if (group == null) {
            MessageRegistry.Commands.Permission.Group.Permissions
                .sendGroupNotFound(executor, name)
            return
        }
        val permValue = group.permissions[permission]
        if (permValue == null) {
            MessageRegistry.Commands.Permission.Group.Permissions
                .sendCheckNotSet(executor, name, permission)
        } else {
            val statusText =
                if (permValue)
                    MessageRegistry.Commands.Permission.Group.Info.getGranted()
                else MessageRegistry.Commands.Permission.Group.Info.getRevoked()
            MessageRegistry.Commands.Permission.Group.Permissions
                .sendCheckResult(executor, name, permission, statusText)
        }
    }

    // Player management methods
    private fun handlePlayerInfo(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Permission.Player.Info.sendUsage(executor)
            return
        }
        val playerName = args[0]
        val player = PermissionRepository.getPlayer(playerName)
        if (player == null) {
            MessageRegistry.Commands.sendPlayerNotFound(executor, playerName)
            return
        }
        val groups = PermissionRepository.getPlayerGroups(playerName)
        val highestGroup = PermissionRepository.getHighestGroup(playerName)
        val totalPermissions =
            PermissionRepository.getPermissionList(playerName).size
        MessageRegistry.Commands.Permission.Player.Info.sendHeader(
            executor,
            playerName,
        )
        MessageRegistry.Commands.Permission.Player.Info.sendHighestGroup(
            executor,
            highestGroup,
        )
        MessageRegistry.Commands.Permission.Player.Info.sendTotalGroups(
            executor,
            groups.size,
        )
        MessageRegistry.Commands.Permission.Player.Info.sendTotalPermissions(
            executor,
            totalPermissions,
        )
        MessageRegistry.Commands.Permission.Player.Info
            .sendIndividualPermissions(executor, player.permissions.size)
        if (groups.isNotEmpty()) {
            val groupNames =
                groups.sortedByDescending { it.priority }.map { "&7${it.name}" }
            MessageRegistry.Commands.Permission.Player.Info.sendGroups(
                executor,
                groupNames.joinToString("&f, "),
            )
        }
    }

    private fun handlePlayerAddGroup(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (!executor.hasPermission("dandelion.permission.player.groups")) {
            MessageRegistry.Commands.Permission
                .sendNoPermissionManagePlayerGroups(executor)
            return
        }
        if (args.size != 2) {
            MessageRegistry.Commands.Permission.Player.Groups.sendAddUsage(
                executor
            )
            return
        }
        val playerName = args[0]
        val groupName = args[1].lowercase()
        if (PermissionRepository.addGroupToPlayer(playerName, groupName)) {
            MessageRegistry.Commands.Permission.Player.Groups.sendAddSuccess(
                executor,
                groupName,
                playerName,
            )
        } else {
            MessageRegistry.Commands.Permission.Player.Groups.sendAddFailed(
                executor,
                groupName,
            )
        }
    }

    private fun handlePlayerRemoveGroup(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (!executor.hasPermission("dandelion.permission.player.groups")) {
            MessageRegistry.Commands.Permission
                .sendNoPermissionManagePlayerGroups(executor)
            return
        }
        if (args.size != 2) {
            MessageRegistry.Commands.Permission.Player.Groups.sendRemoveUsage(
                executor
            )
            return
        }
        val playerName = args[0]
        val groupName = args[1].lowercase()
        if (PermissionRepository.removeGroupFromPlayer(playerName, groupName)) {
            MessageRegistry.Commands.Permission.Player.Groups.sendRemoveSuccess(
                executor,
                groupName,
                playerName,
            )
        } else {
            MessageRegistry.Commands.Permission.Player.Groups.sendRemoveFailed(
                executor,
                groupName,
            )
        }
    }

    private fun handlePlayerSetGroups(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (!executor.hasPermission("dandelion.permission.player.groups")) {
            MessageRegistry.Commands.Permission
                .sendNoPermissionManagePlayerGroups(executor)
            return
        }
        if (args.size != 2) {
            MessageRegistry.Commands.Permission.Player.Groups.sendSetUsage(
                executor
            )
            return
        }
        val playerName = args[0]
        val groupNames = args[1].split(",").map { it.trim().lowercase() }
        val player = PermissionRepository.getPlayer(playerName)
        val validGroups =
            groupNames.filter { PermissionRepository.hasGroup(it) }
        val invalidGroups = groupNames - validGroups.toSet()
        if (invalidGroups.isNotEmpty()) {
            MessageRegistry.Commands.Permission.Player.Groups.sendSetInvalid(
                executor,
                invalidGroups.joinToString(", "),
            )
            return
        }
        player.groups.clear()
        validGroups.forEach { player.addGroup(it) }
        if (!player.groups.contains("default")) {
            player.addGroup("default")
        }
        MessageRegistry.Commands.Permission.Player.Groups.sendSetSuccess(
            executor,
            playerName,
            validGroups.joinToString(", "),
        )
    }

    private fun handlePlayerListGroups(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Permission.Player.Groups.sendListUsage(
                executor
            )
            return
        }
        val playerName = args[0]
        val groups = PermissionRepository.getPlayerGroups(playerName)
        if (groups.isEmpty()) {
            MessageRegistry.Commands.Permission.Player.Groups.sendListEmpty(
                executor,
                playerName,
            )
            return
        }
        MessageRegistry.Commands.Permission.Player.Groups.sendListHeader(
            executor,
            playerName,
        )
        groups
            .sortedByDescending { it.priority }
            .forEach { group ->
                MessageRegistry.Commands.Permission.Player.Groups
                    .sendListFormat(
                        executor,
                        group.name,
                        group.displayName,
                        group.priority,
                    )
            }
    }

    private fun handlePlayerGrantPermission(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (!executor.hasPermission("dandelion.permission.player.perms")) {
            MessageRegistry.Commands.Permission
                .sendNoPermissionManagePlayerPerms(executor)
            return
        }
        if (args.size != 2) {
            MessageRegistry.Commands.Permission.Player.Permissions
                .sendGrantUsage(executor)
            return
        }
        val playerName = args[0]
        val permission = args[1]
        PermissionRepository.setPlayerPermission(playerName, permission, true)
        MessageRegistry.Commands.Permission.Player.Permissions.sendGrantSuccess(
            executor,
            permission,
            playerName,
        )
    }

    private fun handlePlayerRevokePermission(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (!executor.hasPermission("dandelion.permission.player.perms")) {
            MessageRegistry.Commands.Permission
                .sendNoPermissionManagePlayerPerms(executor)
            return
        }
        if (args.size != 2) {
            MessageRegistry.Commands.Permission.Player.Permissions
                .sendRevokeUsage(executor)
            return
        }
        val playerName = args[0]
        val permission = args[1]
        PermissionRepository.setPlayerPermission(playerName, permission, false)
        MessageRegistry.Commands.Permission.Player.Permissions
            .sendRevokeSuccess(executor, permission, playerName)
    }

    private fun handlePlayerRemovePermissionEntry(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (!executor.hasPermission("dandelion.permission.player.perms")) {
            MessageRegistry.Commands.Permission
                .sendNoPermissionManagePlayerPerms(executor)
            return
        }
        if (args.size != 2) {
            MessageRegistry.Commands.Permission.Player.Permissions
                .sendRemoveUsage(executor)
            return
        }
        val playerName = args[0]
        val permission = args[1]
        if (
            PermissionRepository.removePlayerPermission(playerName, permission)
        ) {
            MessageRegistry.Commands.Permission.Player.Permissions
                .sendRemoveSuccess(executor, permission, playerName)
        } else {
            MessageRegistry.Commands.Permission.Player.Permissions
                .sendRemoveFailed(executor, playerName, permission)
        }
    }

    private fun handlePlayerListPermissions(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Permission.Player.Permissions
                .sendListUsage(executor)
            return
        }
        val playerName = args[0]
        val permissions = PermissionRepository.getPermissionList(playerName)
        if (permissions.isEmpty()) {
            MessageRegistry.Commands.Permission.Player.Permissions
                .sendListEmpty(executor, playerName)
            return
        }
        MessageRegistry.Commands.Permission.Player.Permissions.sendListHeader(
            executor,
            playerName,
        )
        permissions.sorted().forEach { perm ->
            val player = PermissionRepository.getPlayer(playerName)
            val individualValue = player.permissions[perm]
            if (individualValue != null) {
                val statusText =
                    if (individualValue)
                        MessageRegistry.Commands.Permission.Group.Info
                            .getGranted()
                    else
                        MessageRegistry.Commands.Permission.Group.Info
                            .getRevoked()
                MessageRegistry.Commands.Permission.Player.Permissions
                    .sendListIndividual(executor, perm, statusText)
            } else {
                MessageRegistry.Commands.Permission.Player.Permissions
                    .sendListGroup(executor, perm)
            }
        }
    }

    private fun handlePlayerListOwnPermissions(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (args.isEmpty()) {
            MessageRegistry.Commands.Permission.Player.Permissions.sendOwnUsage(
                executor
            )
            return
        }
        val playerName = args[0]
        val player = PermissionRepository.getPlayer(playerName)
        val ownPermissions = player.permissions
        if (ownPermissions.isEmpty()) {
            MessageRegistry.Commands.Permission.Player.Permissions.sendOwnEmpty(
                executor,
                playerName,
            )
            return
        }
        MessageRegistry.Commands.Permission.Player.Permissions.sendOwnHeader(
            executor,
            playerName,
        )
        ownPermissions.entries
            .sortedBy { it.key }
            .forEach { (perm, value) ->
                val statusText =
                    if (value)
                        MessageRegistry.Commands.Permission.Group.Info
                            .getGranted()
                    else
                        MessageRegistry.Commands.Permission.Group.Info
                            .getRevoked()
                MessageRegistry.Commands.Permission.Player.Permissions
                    .sendOwnFormat(executor, perm, statusText)
            }
    }

    private fun handlePlayerHasPermission(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (args.size != 2) {
            MessageRegistry.Commands.Permission.Player.Permissions
                .sendHaspermUsage(executor)
            return
        }
        val playerName = args[0]
        val permission = args[1]
        val hasPermission =
            PermissionRepository.hasPermission(playerName, permission)
        val status =
            if (hasPermission)
                MessageRegistry.Commands.Permission.Player.Permissions
                    .getCheckYes()
            else
                MessageRegistry.Commands.Permission.Player.Permissions
                    .getCheckNo()
        MessageRegistry.Commands.Permission.Player.Permissions.sendCheckResult(
            executor,
            playerName,
            permission,
            status,
        )
    }

    private fun handlePlayerCheckPermission(
        executor: CommandExecutor,
        args: Array<String>,
    ) {
        if (args.size != 2) {
            MessageRegistry.Commands.Permission.Player.Permissions
                .sendCheckUsage(executor)
            return
        }
        val playerName = args[0]
        val permission = args[1]

        val player = PermissionRepository.getPlayer(playerName)
        val groups = PermissionRepository.getPlayerGroups(playerName)
        val individualValue = player.permissions[permission]
        if (individualValue != null) {
            val statusText =
                if (individualValue)
                    MessageRegistry.Commands.Permission.Player.Permissions
                        .getCheckYes()
                else
                    MessageRegistry.Commands.Permission.Player.Permissions
                        .getCheckNo()
            MessageRegistry.Commands.Permission.Player.Permissions
                .sendCheckIndividual(
                    executor,
                    playerName,
                    permission,
                    statusText,
                )
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
            val statusText =
                if (isGrantedInGroup)
                    MessageRegistry.Commands.Permission.Player.Permissions
                        .getCheckYes()
                else
                    MessageRegistry.Commands.Permission.Player.Permissions
                        .getCheckNo()
            MessageRegistry.Commands.Permission.Player.Permissions
                .sendCheckGroup(
                    executor,
                    playerName,
                    permission,
                    foundInGroup,
                    statusText,
                )
        } else {
            MessageRegistry.Commands.Permission.Player.Permissions
                .sendCheckNotFound(executor, playerName, permission)
        }
    }
}
