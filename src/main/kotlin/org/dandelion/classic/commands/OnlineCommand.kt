package org.dandelion.classic.commands


import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.entity.player.Player

@CommandDef(name = "online", description = "Lists all online players.")
class OnlineCommand : Command {
    @OnExecute
    fun execute(executor: CommandExecutor,  args: Array<String>) {
        val players = Player.getAllPlayers()
        if (players.isEmpty()) {
            executor.sendMessage("&cNo players online.")
            return
        }
        val names = players.joinToString(", ") { "&7${it.name}" }
        executor.sendMessage("&eOnline players (&f${players.size}&e): $names")
    }
}

