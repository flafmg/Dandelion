package org.dandelion.classic.blocks.model.enums

enum class WalkSound(val value: Byte) {
    NONE(0),
    WOOD(1),
    GRAVEL(2),
    GRASS(3),
    STONE(4),
    METAL(5),
    GLASS(6),
    WOOL(7),
    SAND(8),
    SNOW(9);

    companion object {
        fun from(value: Byte): WalkSound {
            return values().find { it.value == value } ?: NONE
        }
    }
}
