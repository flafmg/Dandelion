package org.dandelion.classic.events

import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.events.model.Cancellable
import org.dandelion.classic.events.model.Event
import org.dandelion.classic.level.Level

enum class NotifyActionType {
    BLOCK_LIST_SELECTED,
    BLOCK_LIST_TOGGLED,
    LEVEL_SAVED,
    RESPAWNED,
    SPAWN_UPDATED,
    TEXTURE_PACK_CHANGED,
    TEXTURE_PROMPT_RESPONDED,
    THIRD_PERSON_CHANGED,
}

abstract class PlayerNotifyEvent(
    val player: Player,
    val actionType: NotifyActionType,
    val level: Level,
) : Event

class PlayerBlockListSelectedEvent(
    player: Player,
    level: Level,
    val selectedBlockId: Short,
) : PlayerNotifyEvent(player, NotifyActionType.BLOCK_LIST_SELECTED, level)

class PlayerBlockListToggledEvent(
    player: Player,
    level: Level,
    val isOpened: Boolean,
    override var isCancelled: Boolean = false,
) :
    PlayerNotifyEvent(player, NotifyActionType.BLOCK_LIST_TOGGLED, level),
    Cancellable

class PlayerLevelSavedEvent(player: Player, level: Level) :
    PlayerNotifyEvent(player, NotifyActionType.LEVEL_SAVED, level)

class PlayerRespawnedEvent(
    player: Player,
    level: Level,
    val x: Short,
    val y: Short,
    val z: Short,
) : PlayerNotifyEvent(player, NotifyActionType.RESPAWNED, level)

class PlayerSpawnUpdatedEvent(
    player: Player,
    level: Level,
    val newX: Short,
    val newY: Short,
    val newZ: Short,
    val oldX: Short,
    val oldY: Short,
    val oldZ: Short,
    override var isCancelled: Boolean = false,
) :
    PlayerNotifyEvent(player, NotifyActionType.SPAWN_UPDATED, level),
    Cancellable

class PlayerTexturePackChangedEvent(
    player: Player,
    level: Level,
    val texturePackId: Short,
) : PlayerNotifyEvent(player, NotifyActionType.TEXTURE_PACK_CHANGED, level)

class PlayerTexturePromptRespondedEvent(
    player: Player,
    level: Level,
    val response: Short,
) : PlayerNotifyEvent(player, NotifyActionType.TEXTURE_PROMPT_RESPONDED, level)

class PlayerThirdPersonChangedEvent(
    player: Player,
    level: Level,
    val enabled: Boolean,
) : PlayerNotifyEvent(player, NotifyActionType.THIRD_PERSON_CHANGED, level)
