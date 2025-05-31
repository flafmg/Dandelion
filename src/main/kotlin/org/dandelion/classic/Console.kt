package org.dandelion.classic

import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.commands.manager.CommandRegistry
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Console : CommandExecutor {
    var lineReader: org.jline.reader.LineReader? = null

    override fun sendMessage(message: String) {
        lineReader?.printAbove(message) ?: println(message)
    }

    override fun getName(): String = "Console"
    override fun hasPermission(permission: String): Boolean = true
    override fun isConsole(): Boolean = true

    private val commandHistory = mutableListOf<String>()
    private var historyIndex = 0
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
        sendMessage("${timestamp()}$message$RESET")
    }

    fun debugLog(message: String) {
        if (debugMode) {
            sendMessage("${timestamp()}$GRAY[DEBUG] $message$RESET")
        }
    }

    fun infoLog(message: String) {
        sendMessage("${timestamp()}$BLUE[INFO] $message$RESET")
    }

    fun warnLog(message: String) {
        sendMessage("${timestamp()}$YELLOW[WARN] $message$RESET")
    }

    fun errLog(message: String) {
        sendMessage("${timestamp()}$RED[ERROR] $message$RESET")
    }

    fun startInputLoop() {
        inputJob = scope.launch {
            val terminal = TerminalBuilder.builder().system(true).build()
            val reader = LineReaderBuilder.builder().terminal(terminal).build()
            lineReader = reader

            while (Server.isRunning() && isActive) {
                try {
                    val input = reader.readLine("> ")
                    if (input.isBlank()) continue

                    val cmd = if (input.startsWith("/")) input else "/$input"

                    commandHistory.add(cmd)
                    historyIndex = commandHistory.size
                    CommandRegistry.execute(cmd, this@Console)
                } catch (e: org.jline.reader.UserInterruptException) {
                    Server.stop()
                }
            }
            lineReader = null
        }
    }

    fun stopInputLoop() {
        inputJob?.cancel()
        inputJob = null
    }
}