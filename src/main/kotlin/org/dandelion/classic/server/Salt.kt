package org.dandelion.classic.server

import java.security.SecureRandom

object Salt {
    private val base62 =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private val saltLength = 16
    private var salt: String = generate()

    fun get(): String = salt

    internal fun regenerate() {
        Console.log("Generating salt...")
        salt = generate()
    }

    private fun generate(): String {
        val random = SecureRandom()
        return (1..saltLength)
            .map { base62[random.nextInt(base62.length)] }
            .joinToString("")
    }
}
