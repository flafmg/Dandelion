package org.dandelion.server.events

import org.dandelion.server.entity.Entity
import org.dandelion.server.entity.player.Player
import org.dandelion.server.events.model.Event
import org.dandelion.server.level.Level

enum class MouseButton {
    LEFT,
    RIGHT,
    MIDDLE,
    UNKNOWN,
}

enum class ClickAction {
    PRESS,
    RELEASE,
}

enum class BlockFace {
    AWAY_FROM_X,
    TOWARDS_X,
    UP,
    DOWN,
    AWAY_FROM_Z,
    TOWARDS_Z,
    INVALID,
}

abstract class PlayerClickEvent(
    val player: Player,
    val button: MouseButton,
    val action: ClickAction,
    val level: Level,
    val yaw: Float,
    val pitch: Float,
) : Event

class PlayerPressEvent(
    player: Player,
    button: MouseButton,
    action: ClickAction,
    level: Level,
    yaw: Float,
    pitch: Float,
) : PlayerClickEvent(player, button, action, level, yaw, pitch)

class PlayerReleaseEvent(
    player: Player,
    button: MouseButton,
    level: Level,
    yaw: Float,
    pitch: Float,
) : PlayerClickEvent(player, button, ClickAction.RELEASE, level, yaw, pitch)

class PlayerBlockClickEvent(
    player: Player,
    button: MouseButton,
    action: ClickAction,
    level: Level,
    yaw: Float,
    pitch: Float,
    val blockX: Short,
    val blockY: Short,
    val blockZ: Short,
    val blockFace: BlockFace,
) : PlayerClickEvent(player, button, action, level, yaw, pitch)

class PlayerEntityClickEvent(
    player: Player,
    button: MouseButton,
    action: ClickAction,
    level: Level,
    yaw: Float,
    pitch: Float,
    val targetEntityId: Byte,
    val targetEntity: Entity?,
) : PlayerClickEvent(player, button, action, level, yaw, pitch)
