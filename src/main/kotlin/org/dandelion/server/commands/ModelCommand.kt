package org.dandelion.server.commands

import org.dandelion.server.commands.annotations.ArgRange
import org.dandelion.server.commands.annotations.CommandDef
import org.dandelion.server.commands.annotations.OnExecute
import org.dandelion.server.commands.annotations.RequirePermission
import org.dandelion.server.commands.model.Command
import org.dandelion.server.commands.model.CommandExecutor
import org.dandelion.server.entity.player.Player

@CommandDef(
    name = "model",
    description = "Change your current model",
    usage = "/model <model_name>",
)
class ModelCommand : Command {
    @OnExecute
    @RequirePermission("dandelion.commands.model")
    @ArgRange(min = 1)
    fun execute(executor: CommandExecutor, args: Array<String>) {
        if (executor !is Player) {
            executor.sendMessage("This command can only be used by players")
            return
        }

        val modelName = args[0]
        executor.model = modelName
        executor.sendMessage("Model changed to: $modelName")
    }
}
