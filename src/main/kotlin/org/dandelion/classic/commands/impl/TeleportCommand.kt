package org.dandelion.classic.commands.impl

import org.dandelion.classic.commands.model.Command
import org.dandelion.classic.data.level.manager.LevelManager
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.data.player.manager.PlayerManager
import org.dandelion.classic.data.player.model.Player

class TeleportCommand : Command {
    override val name = "teleport"
    override val permission = "dandelion.server.teleport"
    override val description = "Teleports players or yourself."
    override val aliases: List<String> = listOf("tp")
    override fun onExecute(executor: CommandExecutor, args: List<String>) {
        if (args.isEmpty()) {
            executor.sendMessage("Correct usage: /teleport <player> | <player> <target> | <player> <x> <y> <z> [yaw] [pitch] | <x> <y> <z> [yaw] [pitch]")
            return
        }
        // /teleport <posx> <posy> <posz> [yaw] [pitch] (player only)
        if (args.size in 3..5 && !executor.isConsole()) {
            val player = executor as? Player ?: return executor.sendMessage("Only players can use this teleport format.")
            val x = args[0].toFloatOrNull()
            val y = args[1].toFloatOrNull()
            val z = args[2].toFloatOrNull()
            if (x == null || y == null || z == null) {
                executor.sendMessage("Correct usage: /teleport <x> <y> <z> [yaw] [pitch]")
                return
            }
            val yaw = args.getOrNull(3)?.toFloatOrNull() ?: player.yaw
            val pitch = args.getOrNull(4)?.toFloatOrNull() ?: player.pitch
            player.teleport(x, y, z, yaw, pitch)
            executor.sendMessage("Teleported to $x $y $z.")
            return
        }
        // /teleport <player> (to yourself)
        if (args.size == 1 && !executor.isConsole()) {
            val player = PlayerManager.getAllPlayers().find { it.userName.equals(args[0], ignoreCase = true) }
            val target = executor as? Player ?: return executor.sendMessage("Only players can use this teleport format.")
            if (player == null) {
                executor.sendMessage("Player not found.")
                return
            }
            if (player.levelId != target.levelId) {
                target.changeLevel(LevelManager.getLevel(player.levelId)!!)
            }
            target.teleport(player.posX, player.posY, player.posZ, player.yaw, player.pitch)
            executor.sendMessage("Teleported to ${player.userName}.")
            return
        }
        // /teleport <player> <target>
        if (args.size == 2) {
            val player = PlayerManager.getAllPlayers().find { it.userName.equals(args[0], ignoreCase = true) }
            val target = PlayerManager.getAllPlayers().find { it.userName.equals(args[1], ignoreCase = true) }
            if (player == null || target == null) {
                executor.sendMessage("Player(s) not found.")
                return
            }
            if (player.levelId != target.levelId) {
                player.changeLevel(LevelManager.getLevel(target.levelId)!!)
            }
            player.teleport(target.posX, target.posY, target.posZ, target.yaw, target.pitch)
            executor.sendMessage("Teleported ${player.userName} to ${target.userName}.")
            return
        }
        // /teleport <player> <x> <y> <z> [yaw] [pitch] (console or player)
        if (args.size in 4..6) {
            val player = PlayerManager.getAllPlayers().find { it.userName.equals(args[0], ignoreCase = true) }
            if (player == null) {
                executor.sendMessage("Player not found.")
                return
            }
            val x = args[1].toFloatOrNull()
            val y = args[2].toFloatOrNull()
            val z = args[3].toFloatOrNull()
            if (x == null || y == null || z == null) {
                executor.sendMessage("Correct usage: /teleport <player> <x> <y> <z> [yaw] [pitch]")
                return
            }
            val yaw = args.getOrNull(4)?.toFloatOrNull() ?: player.yaw
            val pitch = args.getOrNull(5)?.toFloatOrNull() ?: player.pitch
            player.teleport(x, y, z, yaw, pitch)
            executor.sendMessage("Teleported ${player.userName} to $x $y $z.")
            return
        }
        executor.sendMessage("Correct usage: /teleport <player> | <player> <target> | <player> <x> <y> <z> [yaw] [pitch] | <x> <y> <z> [yaw] [pitch]")
    }
}

