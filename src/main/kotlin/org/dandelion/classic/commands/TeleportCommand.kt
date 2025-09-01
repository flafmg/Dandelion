import org.dandelion.classic.commands.annotations.CommandDef
import org.dandelion.classic.commands.annotations.OnExecute
import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.server.MessageRegistry
import org.dandelion.classic.types.Position

@CommandDef(
    name = "teleport",
    description = "teleports to a player or location",
    usage = "/teleport [player]/[location] [location]",
    aliases = ["tp"],
)
class TeleportCommand : Command {
    @OnExecute
    fun execute(executor: CommandExecutor, args: Array<String>) {
        when (args.size) {
            1 -> {
                if (!executor.hasPermission("dandelion.teleport.self.player")) {
                    MessageRegistry.Commands.sendNoPermission(executor)
                    return
                }

                val targetPlayer = args[0]
                teleportExecutorToPlayer(executor, targetPlayer)
            }
            2 -> {
                if (!executor.hasPermission("dandelion.teleport.other.player")) {
                    MessageRegistry.Commands.sendNoPermission(executor)
                    return
                }

                val player1 = args[0]
                val player2 = args[1]
                teleportPlayerToPlayer(executor, player1, player2)
            }
            3 -> {
                if (!executor.hasPermission("dandelion.teleport.self.location")) {
                    MessageRegistry.Commands.sendNoPermission(executor)
                    return
                }

                val x = args[0].toFloatOrNull()
                val y = args[1].toFloatOrNull()
                val z = args[2].toFloatOrNull()

                if (x != null && y != null && z != null) {
                    teleportExecutorToLocation(executor, x, y, z)
                } else {
                    MessageRegistry.Commands.Teleport.sendInvalidPosition(executor)
                }
            }
            4 -> {
                if (!executor.hasPermission("dandelion.teleport.other.location")) {
                    MessageRegistry.Commands.sendNoPermission(executor)
                    return
                }

                val playerName = args[0]
                val x = args[1].toFloatOrNull()
                val y = args[2].toFloatOrNull()
                val z = args[3].toFloatOrNull()

                if (x != null && y != null && z != null) {
                    teleportPlayerToLocation(executor, playerName, x, y, z)
                } else {
                    MessageRegistry.Commands.Teleport.sendInvalidPosition(executor)
                }
            }
            else -> {
                MessageRegistry.Commands.sendInvalidUsage(executor, "/tp <player> | /tp <player1> <player2> | /tp <x> <y> <z> | /tp <player> <x> <y> <z>")
            }
        }
    }

    private fun teleportExecutorToPlayer(
        executor: CommandExecutor,
        targetPlayerName: String,
    ) {
        if (executor !is Player) return

        val target = Players.find(targetPlayerName)
        if (target == null) {
            MessageRegistry.Commands.sendPlayerNotFound(executor, targetPlayerName)
            return
        }

        executor.teleportTo(target.position)
        MessageRegistry.Commands.Teleport.sendSuccessSelfToPlayer(executor, target.name)
        MessageRegistry.Commands.Teleport.sendTargetReceived(executor = target, where = target.name)
    }

    private fun teleportPlayerToPlayer(
        executor: CommandExecutor,
        playerName: String,
        targetPlayerName: String,
    ) {
        val origin = Players.find(playerName)
        val target = Players.find(targetPlayerName)
        if (origin == null) {
            MessageRegistry.Commands.sendPlayerNotFound(executor, playerName)
            return
        }
        if (target == null) {
            MessageRegistry.Commands.sendPlayerNotFound(executor, targetPlayerName)
            return
        }

        origin.teleportTo(target.position)
        MessageRegistry.Commands.Teleport.sendSuccessPlayerToPlayer(executor, playerName, targetPlayerName)
        MessageRegistry.Commands.Teleport.sendTargetReceived(executor = origin, where = targetPlayerName)
    }

    private fun teleportExecutorToLocation(
        executor: CommandExecutor,
        x: Float,
        y: Float,
        z: Float,
    ) {
        if (executor !is Player) return
        val pos = Position(x, y, z)
        executor.teleportTo(pos)
        MessageRegistry.Commands.Teleport.sendSuccessSelfToLocation(executor, x, y, z)
    }

    private fun teleportPlayerToLocation(
        executor: CommandExecutor,
        playerName: String,
        x: Float,
        y: Float,
        z: Float,
    ) {
        val origin = Players.find(playerName)
        if (origin == null) {
            MessageRegistry.Commands.sendPlayerNotFound(executor, playerName)
            return
        }
        val pos = Position(x, y, z)
        origin.teleportTo(pos)
        MessageRegistry.Commands.Teleport.sendSuccessPlayerToLocation(executor, playerName, x, y, z)
        MessageRegistry.Commands.Teleport.sendTargetReceivedLocation(executor = origin, x = x, y = y, z = z)
    }
}
