package org.dandelion.classic.server.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Logger {
    private val RESET = "\u001B[0m"
    private val RED = "\u001B[31m"
    private val YELLOW = "\u001B[33m"
    private val GRAY = "\u001B[90m"
    private val WHITE = "\u001B[37m"
    private val BLUE = "\u001B[34m"

    private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss:SS")

    var showTimestamp: Boolean = true
    var debugMode: Boolean = false

    private fun timestamp(): String = if (showTimestamp) {
        "[${LocalDateTime.now().format(TIMESTAMP_FORMAT)}] "
    } else ""

    fun log(message: String) {
        println("${timestamp()}$message$RESET")
    }

    fun debugLog(message: String) {
        if (debugMode) {
            println("${timestamp()}${GRAY}[DEBUG] $message$RESET")
        }
    }

    fun infoLog(message: String) {
        println("${timestamp()}${BLUE}[INFO] $message$RESET")
    }

    fun warnLog(message: String) {
        println("${timestamp()}${YELLOW}[WARN] $message$RESET")
    }

    fun errLog(message: String) {
        System.err.println("${timestamp()}${RED}[ERROR] $message$RESET")
    }
}

