package org.dandelion.classic.util

import org.jline.reader.LineReader
import org.dandelion.classic.Console
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
    var consoleLineReader: LineReader? = Console.lineReader

    private fun timestamp(): String = if (showTimestamp) {
        "[${LocalDateTime.now().format(TIMESTAMP_FORMAT)}] "
    } else ""

    private fun print(message: String) {
        consoleLineReader?.printAbove(message) ?: println(message)
    }

    fun log(message: String) {
        print("${timestamp()}$message$RESET")
    }

    fun debugLog(message: String) {
        if (debugMode) {
            print("${timestamp()}$GRAY[DEBUG] $message$RESET")
        }
    }

    fun infoLog(message: String) {
        print("${timestamp()}$BLUE[INFO] $message$RESET")
    }

    fun warnLog(message: String) {
        print("${timestamp()}$YELLOW[WARN] $message$RESET")
    }

    fun errLog(message: String) {
        print("${timestamp()}$RED[ERROR] $message$RESET")
    }
}

