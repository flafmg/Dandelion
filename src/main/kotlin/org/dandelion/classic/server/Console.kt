package org.dandelion.classic.server

import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Console{
    var lineReader: org.jline.reader.LineReader? = null


    private var inputJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

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
        val formattedMessage = "${timestamp()}$message$RESET"
        lineReader?.printAbove(formattedMessage) ?: println(formattedMessage)
    }

    fun debugLog(message: String) {
        if (debugMode) {
            val formattedMessage = "${timestamp()}$GRAY[DEBUG] $message$RESET"
            lineReader?.printAbove(formattedMessage) ?: println(formattedMessage)
        }
    }

    fun infoLog(message: String) {
        val formattedMessage = "${timestamp()}$BLUE[INFO] $message$RESET"
        lineReader?.printAbove(formattedMessage) ?: println(formattedMessage)
    }

    fun warnLog(message: String) {
        val formattedMessage = "${timestamp()}$YELLOW[WARN] $message$RESET"
        lineReader?.printAbove(formattedMessage) ?: println(formattedMessage)
    }

    fun errLog(message: String) {
        val formattedMessage = "${timestamp()}$RED[ERROR] $message$RESET"
        lineReader?.printAbove(formattedMessage) ?: println(formattedMessage)
    }

    internal fun init() {
        inputJob = scope.launch {
            val terminal = TerminalBuilder.builder().system(true).build()
            val reader = LineReaderBuilder.builder().terminal(terminal).build()
            lineReader = reader

            while (Server.isRunning() && isActive) {
                try {
                    val input = reader.readLine("> ")
                    if (input.isBlank()) continue
                } catch (e: org.jline.reader.UserInterruptException) {
                    Server.shutDown()
                }
            }
            lineReader = null
        }
    }

    internal fun shutDown() {
        inputJob?.cancel()
        inputJob = null
    }
}