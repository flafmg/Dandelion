package org.dandelion.server.server

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*
import org.dandelion.server.commands.model.CommandExecutor
import org.dandelion.server.server.data.ServerConfig
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder

object Console : CommandExecutor {
    override val name: String = "Console"
    override val permissions: Map<String, Boolean> = mapOf("*" to true)

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

    private val colorMap =
        mapOf(
            '0' to "\u001b[38;2;0;0;0m",
            '1' to "\u001b[38;2;0;0;191m",
            '2' to "\u001b[38;2;0;191;0m",
            '3' to "\u001b[38;2;0;191;191m",
            '4' to "\u001b[38;2;191;0;0m",
            '5' to "\u001b[38;2;191;0;191m",
            '6' to "\u001b[38;2;191;191;0m",
            '7' to "\u001b[38;2;191;191;191m",
            '8' to "\u001b[38;2;64;64;64m",
            '9' to "\u001b[38;2;64;64;255m",
            'a' to "\u001b[38;2;64;255;64m",
            'b' to "\u001b[38;2;64;255;255m",
            'c' to "\u001b[38;2;255;64;64m",
            'd' to "\u001b[38;2;255;64;255m",
            'e' to "\u001b[38;2;255;255;64m",
            'f' to "\u001b[38;2;255;255;255m",
        )

    internal fun init() {
        inputJob =
            scope.launch {
                val terminal = TerminalBuilder.builder().system(true).build()
                val reader =
                    LineReaderBuilder.builder().terminal(terminal).build()
                lineReader = reader

                while (Server.isRunning() && isActive) {
                    try {
                        val input = reader.readLine("> ")
                        if (input.isBlank()) continue
                        sendCommand(input)
                    } catch (e: org.jline.reader.UserInterruptException) {
                        Server.shutdown()
                    } catch (e: Exception) {
                        if (isActive) {
                            errLog("Console input error: ${e.message}")
                        }
                    }
                }
                lineReader = null
            }
    }

    private fun timestamp(): String =
        if (showTimestamp) {
            "[${LocalDateTime.now().format(TIMESTAMP_FORMAT)}] "
        } else ""

    @JvmStatic
    fun log(message: String) {
        val formattedMessage =
            "${timestamp()}${processColorCodes(message)}$RESET"
        lineReader?.printAbove(formattedMessage) ?: println(formattedMessage)
    }

    @JvmStatic
    fun debugLog(message: String) {
        if (ServerConfig.debugMode) {
            val formattedMessage =
                "${timestamp()}$GRAY[DEBUG] ${processColorCodes(message)}$RESET"
            lineReader?.printAbove(formattedMessage)
                ?: println(formattedMessage)
        }
    }

    @JvmStatic
    fun infoLog(message: String) {
        val formattedMessage =
            "${timestamp()}$BLUE[INFO] ${processColorCodes(message)}$RESET"
        lineReader?.printAbove(formattedMessage) ?: println(formattedMessage)
    }

    @JvmStatic
    fun warnLog(message: String) {
        val formattedMessage =
            "${timestamp()}$YELLOW[WARN] ${processColorCodes(message)}$RESET"
        lineReader?.printAbove(formattedMessage) ?: println(formattedMessage)
    }

    @JvmStatic
    fun errLog(message: String) {
        val formattedMessage =
            "${timestamp()}$RED[ERROR] ${processColorCodes(message)}$RESET"
        lineReader?.printAbove(formattedMessage) ?: println(formattedMessage)
    }

    private fun processColorCodes(message: String): String {
        val builder = StringBuilder()
        var i = 0
        while (i < message.length) {
            if (message[i] == '&' && i + 1 < message.length) {
                val colorCode = message[i + 1]
                val ansiCode = colorMap[colorCode]
                if (ansiCode != null) {
                    builder.append(ansiCode)
                    i += 2
                } else {
                    builder.append(message[i])
                    i++
                }
            } else {
                builder.append(message[i])
                i++
            }
        }
        return builder.toString()
    }

    internal fun shutdown() {
        inputJob?.cancel()
        inputJob = null
    }
}
