package org.dandelion.classic.types.enums

enum class LightingMode(val id: Byte) {
    CLIENT_LOCAL(0),
    CLASSIC(1),
    FANCY(2);

    companion object {
        fun fromId(id: Byte): LightingMode? {
            return values().find { it.id == id }
        }
    }
}
