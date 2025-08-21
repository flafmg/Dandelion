package org.dandelion.classic.types.enums

enum class LightingMode(val id: Byte) {
    CLIENT_LOCAL(0),
    CLASSIC(1),
    FANCY(2);

    companion object {
        /**
         * Gets a LightingMode by its byte ID
         *
         * @param id The byte identifier of the lighting mode
         * @return The corresponding LightingMode, or null if not found
         */
        fun fromId(id: Byte): LightingMode? {
            return values().find { it.id == id }
        }
    }
}
