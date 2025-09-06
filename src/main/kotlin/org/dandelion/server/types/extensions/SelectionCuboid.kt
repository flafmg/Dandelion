package org.dandelion.server.types.extensions

import org.dandelion.server.types.Position
import org.dandelion.server.types.vec.IVec

data class SelectionCuboid(
    var id: Byte = 0,
    val label: String,
    val startX: Short,
    val startY: Short,
    val startZ: Short,
    val endX: Short,
    val endY: Short,
    val endZ: Short,
    val color: Color,
    val opacity: Short = 255,
) {
    constructor(
        label: String,
        startX: Int,
        startY: Int,
        startZ: Int,
        endX: Int,
        endY: Int,
        endZ: Int,
        color: Color,
        opacity: Short = 255,
    ) : this(
        0,
        label,
        startX.toShort(),
        startY.toShort(),
        startZ.toShort(),
        endX.toShort(),
        endY.toShort(),
        endZ.toShort(),
        color,
        opacity,
    )

    constructor(
        label: String,
        startX: Int,
        startY: Int,
        startZ: Int,
        endX: Int,
        endY: Int,
        endZ: Int,
        red: Int,
        green: Int,
        blue: Int,
        opacity: Short = 255,
    ) : this(
        0,
        label,
        startX.toShort(),
        startY.toShort(),
        startZ.toShort(),
        endX.toShort(),
        endY.toShort(),
        endZ.toShort(),
        Color(red, green, blue),
        opacity,
    )

    constructor(
        label: String,
        start: Position,
        end: Position,
        color: Color,
        opacity: Short = 255,
    ) : this(
        0,
        label,
        start.x.toInt().toShort(),
        start.y.toInt().toShort(),
        start.z.toInt().toShort(),
        end.x.toInt().toShort(),
        end.y.toInt().toShort(),
        end.z.toInt().toShort(),
        color,
        opacity,
    )

    constructor(
        label: String,
        start: Position,
        end: Position,
        red: Int,
        green: Int,
        blue: Int,
        opacity: Short = 255,
    ) : this(
        0,
        label,
        start.x.toInt().toShort(),
        start.y.toInt().toShort(),
        start.z.toInt().toShort(),
        end.x.toInt().toShort(),
        end.y.toInt().toShort(),
        end.z.toInt().toShort(),
        Color(red, green, blue),
        opacity,
    )

    constructor(
        label: String,
        start: IVec,
        end: IVec,
        color: Color,
        opacity: Short = 255,
    ) : this(
        0,
        label,
        start.x.toShort(),
        start.y.toShort(),
        start.z.toShort(),
        end.x.toShort(),
        end.y.toShort(),
        end.z.toShort(),
        color,
        opacity,
    )

    constructor(
        label: String,
        start: IVec,
        end: IVec,
        red: Int,
        green: Int,
        blue: Int,
        opacity: Short = 255,
    ) : this(
        0,
        label,
        start.x.toShort(),
        start.y.toShort(),
        start.z.toShort(),
        end.x.toShort(),
        end.y.toShort(),
        end.z.toShort(),
        Color(red, green, blue),
        opacity,
    )

    init {
        require(opacity in 0..255) { "Opacity must be between 0 and 255" }
    }
}
