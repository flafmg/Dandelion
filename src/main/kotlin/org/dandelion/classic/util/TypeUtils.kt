package org.dandelion.classic.util

/**
 * Converts an FShort value (fixed-point, where each unit is 1/32) to a
 * conventional Float.
 *
 * @param value The FShort value (substeps, integer).
 * @return The equivalent value in Float.
 */
fun toFShort(value: Int): Float {
    return value * (1.0f / 32.0f)
}

/**
 * Converts a conventional Float to FShort (fixed-point, where each unit is
 * 1/32).
 *
 * @param value The value in Float.
 * @return The equivalent FShort value (substeps, integer).
 */
fun fromFShort(value: Float): Int {
    return (value * 32.0f).toInt()
}
