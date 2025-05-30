package org.dandelion.classic

import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.commands.manager.CommandRegistry
import java.io.Console as JConsole
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import kotlinx.coroutines.*
import org.dandelion.classic.util.Logger

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

    fun startInputLoop() {
        inputJob = scope.launch {
            val terminal = TerminalBuilder.builder().system(true).build()
            val reader = LineReaderBuilder.builder().terminal(terminal).build()
            lineReader = reader
            Logger.consoleLineReader = reader

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
            Logger.consoleLineReader = null
        }
    }

    fun stopInputLoop() {
        inputJob?.cancel()
        inputJob = null
    }
}
