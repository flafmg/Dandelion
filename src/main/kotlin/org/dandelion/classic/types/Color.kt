package org.dandelion.classic.types
/**
 * Represents a RGB color with values from 0 to 255
 *
 * @property red The red component (0-255)
 * @property green The green component (0-255)
 * @property blue The blue component (0-255)
 */
data class Color(
    val red: Short,
    val green: Short,
    val blue: Short
) {
    //why this stupid shit uses a short instead of a byte?
    init {
        require(red in 0..255) { "Red component must be between 0 and 255, got $red" }
        require(green in 0..255) { "Green component must be between 0 and 255, got $green" }
        require(blue in 0..255) { "Blue component must be between 0 and 255, got $blue" }
    }

}