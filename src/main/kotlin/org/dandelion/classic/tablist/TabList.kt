package org.dandelion.classic.tablist

import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.network.packets.cpe.server.ServerExtAddPlayerName
import org.dandelion.classic.network.packets.cpe.server.ServerExtRemovePlayerName
import org.dandelion.classic.permission.PermissionRepository
import org.dandelion.classic.server.Console
import org.dandelion.classic.server.MessageRegistry

object TabList {
    private const val MAX_NAME_IDS = 256

    private val availableNameIds = ArrayDeque<Short>(MAX_NAME_IDS)
    private val nameIdEntries = HashMap<Short, TabListEntry>()
    private val playerNameIds = HashMap<String, Short>()

    init {
        initializeNameIdPool()
    }

    private fun initializeNameIdPool() {
        for (id in 0 until MAX_NAME_IDS) {
            availableNameIds.addFirst(id.toShort())
        }
    }

    private fun getNextAvailableNameId(): Short? {
        return availableNameIds.removeFirstOrNull()
    }

    private fun freeNameId(nameId: Short) {
        availableNameIds.addFirst(nameId)
    }

    @JvmStatic
    fun addPlayer(player: Player) {
        if (playerNameIds.containsKey(player.name)) {
            updatePlayer(player)
            return
        }

        val nameId = getNextAvailableNameId() ?: return

        val highestGroup = PermissionRepository.getHighestGroup(player.name)
        val group = PermissionRepository.getGroup(highestGroup)
        val levelName = player.level?.id ?: ""

        val groupName = MessageRegistry.Server.TabList.getGroupName(levelName)
        val listName = MessageRegistry.Server.TabList.getListName(
            group?.displayName ?: highestGroup,
            player.displayName,
            player.name
        )

        val groupRank = group?.priority ?: 0

        val entry = TabListEntry(
            nameId = nameId,
            playerName = player.name,
            listName = listName,
            groupName = groupName,
            groupRank = groupRank
        )

        nameIdEntries[nameId] = entry
        playerNameIds[player.name] = nameId

        broadcastAddEntry(entry)
    }

    @JvmStatic
    fun removePlayer(player: Player) {
        removeEntry(player.name)
    }

    @JvmStatic
    fun removeEntry(playerName: String) {
        val nameId = playerNameIds[playerName] ?: return

        nameIdEntries.remove(nameId)
        playerNameIds.remove(playerName)
        freeNameId(nameId)

        broadcastRemoveEntry(nameId)
    }

    @JvmStatic
    fun updatePlayer(player: Player) {
        val nameId = playerNameIds[player.name] ?: return
        val oldEntry = nameIdEntries[nameId] ?: return

        val highestGroup = PermissionRepository.getHighestGroup(player.name)
        val group = PermissionRepository.getGroup(highestGroup)
        val levelName = player.level?.id ?: ""

        val groupName = MessageRegistry.Server.TabList.getGroupName(levelName)
        val listName = MessageRegistry.Server.TabList.getListName(
            group?.displayName ?: highestGroup,
            player.displayName,
            player.name
        )

        val groupRank = group?.priority ?: 0

        val newEntry = oldEntry.copy(
            listName = listName,
            groupName = groupName,
            groupRank = groupRank
        )

        nameIdEntries[nameId] = newEntry
        broadcastAddEntry(newEntry)
    }

    @JvmStatic
    fun sendFullTabListTo(player: Player) {
        if (!player.supportsCpe || !player.supports("ExtPlayerList")) return

        nameIdEntries.values.forEach { entry ->
            val packet = ServerExtAddPlayerName(
                entry.nameId,
                entry.playerName,
                entry.listName,
                entry.groupName,
                0
            )
            packet.send(player.channel)
        }
    }

    private fun broadcastAddEntry(entry: TabListEntry) {
        val packet = ServerExtAddPlayerName(
            entry.nameId,
            entry.playerName,
            entry.listName,
            entry.groupName,
            0
        )

        Players.getAllPlayers().forEach { player ->
            if (player.supportsCpe && player.supports("ExtPlayerList")) {
                packet.send(player.channel)
            }
        }
    }

    private fun broadcastRemoveEntry(nameId: Short) {
        val packet = ServerExtRemovePlayerName(nameId)

        Players.getAllPlayers().forEach { player ->
            if (player.supportsCpe && player.supports("ExtPlayerList")) {
                packet.send(player.channel)
            }
        }
    }

    @JvmStatic
    fun getAllEntries(): List<TabListEntry> = nameIdEntries.values.toList()

    @JvmStatic
    fun getEntry(playerName: String): TabListEntry? {
        val nameId = playerNameIds[playerName] ?: return null
        return nameIdEntries[nameId]
    }

    @JvmStatic
    fun hasEntry(playerName: String): Boolean = playerNameIds.containsKey(playerName)

    @JvmStatic
    fun clear() {
        val entriesToRemove = nameIdEntries.keys.toList()
        entriesToRemove.forEach { nameId ->
            broadcastRemoveEntry(nameId)
        }

        nameIdEntries.clear()
        playerNameIds.clear()
        initializeNameIdPool()
    }

    @JvmStatic
    fun getEntryCount(): Int = nameIdEntries.size

    @JvmStatic
    fun getAvailableSlots(): Int = availableNameIds.size
}
