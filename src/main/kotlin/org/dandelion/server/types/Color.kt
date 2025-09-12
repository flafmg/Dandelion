package org.dandelion.server.types
data class Color(val red: Short, val green: Short, val blue: Short) {
    // why this stupid shit uses a short instead of a byte?
    init {
        require(red in 0..255) {
            "Red component must be between 0 and 255, got $red"
        }
        require(green in 0..255) {
            "Green component must be between 0 and 255, got $green"
        }
        require(blue in 0..255) {
            "Blue component must be between 0 and 255, got $blue"
        }
    }

    // int constructor
    constructor(
        red: Int,
        green: Int,
        blue: Int,
    ) : this(red.toShort(), green.toShort(), blue.toShort())

    // byte constructor
    constructor(
        red: Byte,
        green: Byte,
        blue: Byte,
    ) : this(
        (red.toInt() and 0xFF).toShort(),
        (green.toInt() and 0xFF).toShort(),
        (blue.toInt() and 0xFF).toShort(),
    )

    // hex constructor
    // todo: Change the hex convertion in the level command to use this instead
    constructor(
        hex: String
    ) : this(
        parseHexComponent(hex, 0),
        parseHexComponent(hex, 1),
        parseHexComponent(hex, 2),
    ) {
        require(isValidHex(hex)) {
            "Invalid hex color format: $hex. Expected format: #RRGGBB or RRGGBB"
        }
    }

    override fun toString(): String {
        return "Color(r=$red, g=$green, b=$blue, hex=#%02X%02X%02X)"
            .format(red.toInt(), green.toInt(), blue.toInt())
    }

    companion object {
        private fun isValidHex(hex: String): Boolean {
            val cleanHex = hex.removePrefix("#")
            return cleanHex.length == 6 &&
                cleanHex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
        }

        private fun parseHexComponent(hex: String, componentIndex: Int): Short {
            val cleanHex = hex.removePrefix("#")
            val startIndex = componentIndex * 2
            val hexComponent = cleanHex.substring(startIndex, startIndex + 2)
            return hexComponent.toInt(16).toShort()
        }
    }
}
