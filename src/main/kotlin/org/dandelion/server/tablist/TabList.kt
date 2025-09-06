package org.dandelion.server.tablist

import org.dandelion.server.entity.player.Player
import org.dandelion.server.entity.player.Players
import org.dandelion.server.level.Levels
import org.dandelion.server.network.packets.cpe.server.ServerExtAddPlayerName
import org.dandelion.server.network.packets.cpe.server.ServerExtRemovePlayerName
import org.dandelion.server.permission.PermissionRepository
import org.dandelion.server.server.data.MessageRegistry

object TabList {
    private const val MAX_NAME_IDS = 255
    private val availableNameIds = ArrayDeque<Short>(MAX_NAME_IDS)
    private val playerNameIds = HashMap<String, Short>()

    init {
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
        val listName =
            MessageRegistry.Server.TabList.getListName(
                group?.displayName ?: highestGroup,
                player.displayName,
                player.name,
            )

        val groupRank =
            if (levelName == Levels.getDefaultLevel()?.id) {
                255
            } else {
                0
            }

        playerNameIds[player.name] = nameId

        Players.getAll().forEach { recipient ->
            if (recipient.supportsCpe && recipient.supports("ExtPlayerList")) {
                val sendNameId =
                    if (recipient.name == player.name) 255 else nameId
                val packet =
                    ServerExtAddPlayerName(
                        sendNameId,
                        player.name,
                        listName,
                        groupName,
                        groupRank.toByte(),
                    )
                packet.send(recipient.channel)
            }
        }
    }

    @JvmStatic
    fun removePlayer(player: Player) {
        val nameId = playerNameIds[player.name] ?: return

        playerNameIds.remove(player.name)
        freeNameId(nameId)

        Players.getAll()
            .filter { it != player }
            .forEach { recipient ->
                if (
                    recipient.supportsCpe && recipient.supports("ExtPlayerList")
                ) {
                    val packet = ServerExtRemovePlayerName(nameId)
                    packet.send(recipient.channel)
                }
            }
    }

    @JvmStatic
    fun updatePlayer(player: Player) {
        val nameId = playerNameIds[player.name] ?: return

        val highestGroup = PermissionRepository.getHighestGroup(player.name)
        val group = PermissionRepository.getGroup(highestGroup)
        val levelName = player.level?.id ?: ""

        val groupName = MessageRegistry.Server.TabList.getGroupName(levelName)
        val listName =
            MessageRegistry.Server.TabList.getListName(
                group?.displayName ?: highestGroup,
                player.displayName,
                player.name,
            )

        val groupRank =
            if (levelName == Levels.getDefaultLevel()?.id) {
                255
            } else {
                0
            }

        Players.getAll().forEach { recipient ->
            if (recipient.supportsCpe && recipient.supports("ExtPlayerList")) {
                val sendNameId =
                    if (recipient.name == player.name) 255 else nameId
                val packet =
                    ServerExtAddPlayerName(
                        sendNameId,
                        player.name,
                        listName,
                        groupName,
                        groupRank.toByte(),
                    )
                packet.send(recipient.channel)
            }
        }
    }

    @JvmStatic
    fun sendFullTabListTo(player: Player) {
        if (!player.supportsCpe || !player.supports("ExtPlayerList")) return

        Players.getAll().forEach { otherPlayer ->
            val nameId = playerNameIds[otherPlayer.name] ?: return@forEach

            val highestGroup =
                PermissionRepository.getHighestGroup(otherPlayer.name)
            val group = PermissionRepository.getGroup(highestGroup)
            val levelName = otherPlayer.level?.id ?: ""

            val groupName =
                MessageRegistry.Server.TabList.getGroupName(levelName)
            val listName =
                MessageRegistry.Server.TabList.getListName(
                    group?.displayName ?: highestGroup,
                    otherPlayer.displayName,
                    otherPlayer.name,
                )

            val groupRank =
                if (levelName == Levels.getDefaultLevel()?.id) {
                    255
                } else {
                    0
                }

            val sendNameId =
                if (player.name == otherPlayer.name) 255 else nameId
            val packet =
                ServerExtAddPlayerName(
                    sendNameId,
                    otherPlayer.name,
                    listName,
                    groupName,
                    groupRank.toByte(),
                )
            packet.send(player.channel)
        }
    }

    fun updateTabListToAll() {
        Players.getAll()
            .filter { it.supports("ExtPlayerList") }
            .forEach { player -> sendFullTabListTo(player) }
    }
}
