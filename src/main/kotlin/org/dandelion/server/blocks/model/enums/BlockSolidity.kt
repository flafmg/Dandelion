package org.dandelion.server.blocks.model.enums

enum class BlockSolidity(val value: Byte) {
    WALK_THROUGH(0),
    SWIM_THROUGH(1),
    SOLID(2),
    PARTIALLY_SLIPPERY(3),
    FULLY_SLIPPERY(4),
    WATER(5),
    LAVA(6),
    ROPE(7);

    companion object {
        fun from(value: Byte): BlockSolidity {
            return values().find { it.value == value } ?: SOLID
        }
    }
}
