package org.dandelion.server.types.enums

enum class MoveMode(val value: Int) {
    INSTANT(0),
    SMOOTH(1),
    RELATIVE_SMOOTH(2),
    RELATIVE_SEAMLESS(3);

    companion object {
        fun fromValue(value: Int): MoveMode =
            values().first { it.value == value }
    }
}
