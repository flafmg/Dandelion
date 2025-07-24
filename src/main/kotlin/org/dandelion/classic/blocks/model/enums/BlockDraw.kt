package org.dandelion.classic.blocks.model.enums


enum class BlockDraw(val value: Byte) {
    OPAQUE(0),
    TRANSPARENT(1),
    TRANSPARENT_NO_CULLING(2),
    TRANSLUCENT(3),
    GAS(4);

    companion object {
        fun from(value: Byte): BlockDraw {
            return values().find { it.value == value } ?: OPAQUE
        }
    }
}