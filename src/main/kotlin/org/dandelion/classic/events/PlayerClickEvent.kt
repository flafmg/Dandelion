package org.dandelion.classic.events

import org.dandelion.classic.entity.Entity
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.events.model.Event
import org.dandelion.classic.level.Level

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

/** Base event for all player click interactions */
abstract class PlayerClickEvent(
    val player: Player,
    val button: MouseButton,
    val action: ClickAction,
    val level: Level,
    val yaw: Float,
    val pitch: Float,
) : Event

/** Event fired when a player presses or releases a mouse button */
class PlayerPressEvent(
    player: Player,
    button: MouseButton,
    action: ClickAction,
    level: Level,
    yaw: Float,
    pitch: Float,
) : PlayerClickEvent(player, button, action, level, yaw, pitch)

/** Event fired when a player releases a mouse button */
class PlayerReleaseEvent(
    player: Player,
    button: MouseButton,
    level: Level,
    yaw: Float,
    pitch: Float,
) : PlayerClickEvent(player, button, ClickAction.RELEASE, level, yaw, pitch)

/** Event fired when a player clicks on a block */
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

/** Event fired when a player clicks on an entity */
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
