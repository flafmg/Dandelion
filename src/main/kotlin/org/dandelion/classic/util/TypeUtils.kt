package org.dandelion.classic.util
fun toFShort(value: Int): Float {
    return value * (1.0f / 32.0f)
}

fun fromFShort(value: Float): Int {
    return (value * 32.0f).toInt()
}
