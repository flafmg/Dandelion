package org.dandelion.classic.server.commands.model

interface Command {
    val name: String
    val aliases: List<String> get() = emptyList()
    val permission: String get() = ""
    val description: String get() = ""
    fun onExecute(executor: CommandExecutor, args: List<String>)
}

