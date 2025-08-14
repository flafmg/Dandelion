package org.dandelion.classic.commands

import org.dandelion.classic.commands.annotations.ArgRange
import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.annotations.RequirePermission
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.server.MessageRegistry

@CommandDef(
    name = "say",
    description = "Broadcast a message to all players",
    usage = "/say <message>",
)
class SayCommand : Command {
    @OnExecute
    @RequirePermission("dandelion.server.say")
    @ArgRange(min = 1)
    fun execute(executor: CommandExecutor, args: Array<String>) {
        val message = args.joinToString(" ")
        val formatMessage =
            MessageRegistry.Commands.Chat.getSayFormat()
                .replace("{sender}", executor.name)
                .replace("{message}", message)
        Players.broadcastMessage(formatMessage)
    }
}
