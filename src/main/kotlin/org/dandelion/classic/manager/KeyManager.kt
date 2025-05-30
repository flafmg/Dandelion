package org.dandelion.classic.server.manager

import java.security.SecureRandom

class KeyManager {
    private val base62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private val saltLength = 16
    private var salt: String = generate()
    fun getSalt(): String = salt
    fun regenerate() {
        salt = generate()
    }
    private fun generate(): String {
        val random = SecureRandom()
        return (1..saltLength).map { base62[random.nextInt(base62.length)] }.joinToString("")
    }
}

