package org.dandelion.classic.server

import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import kotlinx.coroutines.*
import org.dandelion.classic.commands.CommandExecutor
import org.dandelion.classic.commands.CommandRegistry
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Console: CommandExecutor{
    override val name: String = "Console"
    override val permissions: List<String> = listOf("*")
    override fun sendMessage(message: String) = log(message)


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

    private fun timestamp(): String = if (showTimestamp) {
        "[${LocalDateTime.now().format(TIMESTAMP_FORMAT)}] "
    } else ""

    fun log(message: String) {
        val formattedMessage = "${timestamp()}$message$RESET"
        lineReader?.printAbove(formattedMessage) ?: println(formattedMessage)
    }

    fun debugLog(message: String) {
        if (ServerInfo.debugMode) {
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
                    sendCommand(input)
                } catch (e: org.jline.reader.UserInterruptException) {
                    Server.shutdown()
                }
            }
            lineReader = null
        }
    }

    internal fun shutdown() {
        inputJob?.cancel()
        inputJob = null
    }


}