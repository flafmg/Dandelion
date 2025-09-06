package org.dandelion.server.commands

import org.dandelion.server.commands.annotations.ArgRange
import org.dandelion.server.commands.annotations.CommandDef
import org.dandelion.server.commands.annotations.OnExecute
import org.dandelion.server.commands.annotations.RequirePermission
import org.dandelion.server.commands.model.Command
import org.dandelion.server.commands.model.CommandExecutor
import org.dandelion.server.entity.player.Players
import org.dandelion.server.server.data.MessageRegistry

@CommandDef(
    name = "say",
    description = "Broadcast a message to all players",
    usage = "/say <message>",
)
class SayCommand : Command {
    @OnExecute
    @RequirePermission("dandelion.command.say")
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
