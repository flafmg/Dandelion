package org.dandelion.server.server.data

import java.io.File
import org.dandelion.server.commands.model.CommandExecutor
import org.dandelion.server.permission.PermissionRepository
import org.dandelion.server.util.YamlConfig
internal object MessageRegistry {
    private lateinit var config: YamlConfig

    fun reload() {
        val messagesFile = File("messages.yml")
        if (!messagesFile.exists()) {
            val resourceStream =
                MessageRegistry::class
                    .java
                    .classLoader
                    .getResourceAsStream("messages.yml")
            if (resourceStream != null) {
                messagesFile.writeBytes(resourceStream.readBytes())
                resourceStream.close()
            }
        }
        config = YamlConfig.Companion.load(messagesFile)
    }

    fun getMessage(path: String): String {
        return config.getString(path) ?: "&cMessage not found: $path"
    }

    fun getMessage(
        path: String,
        vararg placeholders: Pair<String, Any>,
    ): String {
        var message = getMessage(path)
        placeholders.forEach { (key, value) ->
            message = message.replace("{$key}", value.toString())
        }
        return message
    }

    fun sendMessage(executor: CommandExecutor, path: String) {
        executor.sendMessage(getMessage(path))
    }

    fun sendMessage(
        executor: CommandExecutor,
        path: String,
        vararg placeholders: Pair<String, Any>,
    ) {
        executor.sendMessage(getMessage(path, *placeholders))
    }

    object Server {
        object Connection {
            fun getInvalidProtocol(version: Int, expected: Int): String =
                getMessage(
                    "server.connection.invalid_protocol",
                    "version" to version,
                    "expected" to expected,
                )

            fun getNoCpeSupport(): String =
                getMessage("server.connection.no_cpe_support")

            fun getAuthenticationFailed(): String =
                getMessage("server.connection.authentication_failed")

            fun getReconnected(): String =
                getMessage("server.connection.reconnected")

            fun getServerFull(): String =
                getMessage("server.connection.server_full")
        }

        object Player {
            fun getJoined(player: String): String =
                getMessage("server.player.joined", "player" to player)

            fun getLeft(player: String): String =
                getMessage("server.player.left", "player" to player)

            fun getJoinedLevel(player: String, level: String): String =
                getMessage(
                    "server.player.joined_level",
                    "player" to player,
                    "level" to level,
                )
            fun getDisplayName(player: org.dandelion.server.entity.player.Player): String {
                val group = PermissionRepository.getGroup(
                    PermissionRepository.getHighestGroup(
                        player.name
                    )
                )!!.displayName

                return getMessage(
                    "server.player.display_name",
                    "name" to player.name,
                    "group" to  group
                )
            }
        }

        object Level {
            fun getFull(): String = getMessage("server.level.full")

            fun getNotAvailable(): String =
                getMessage("server.level.not_available")

            fun getBeingReloaded(): String =
                getMessage("server.level.being_reloaded")

            fun getBeingUnloaded(): String =
                getMessage("server.level.being_unloaded")

            fun getNoBuildPermission(): String = getMessage("server.level.no_build_permission")
        }

        object Chat {
            fun getPlayerFormat(
                player: org.dandelion.server.entity.player.Player,
                message: String,
            ): String =
                getMessage(
                    "server.chat_format.player",
                    "player" to player.displayName,
                    "level" to player.levelId,
                    "group" to
                        PermissionRepository.getGroup(
                                PermissionRepository.getHighestGroup(
                                    player.name
                                )
                            )!!
                            .displayName,
                    "message" to message,
                ) // WHATAFUK IS THIS LINE

            fun getConsoleFormat(
                player: org.dandelion.server.entity.player.Player,
                message: String,
            ): String =
                getMessage(
                    "server.chat_format.console",
                    "player" to player.displayName,
                    "level" to player.levelId,
                    "group" to
                        PermissionRepository.getGroup(
                                PermissionRepository.getHighestGroup(
                                    player.name
                                )
                            )!!
                            .displayName,
                    "message" to message,
                )

            fun getSayFormat(
                executor: CommandExecutor,
                message: String,
            ): String =
                getMessage(
                    "server.chat_format.say",
                    "sender" to executor.name,
                    "message" to message,
                )
        }

        object TabList {
            fun getGroupName(level: String): String =
                getMessage("server.tab_list.group_name", "level" to level)

            fun getListName(
                groupDisplayName: String,
                playerDisplayName: String,
                playerName: String,
            ): String =
                getMessage(
                    "server.tab_list.list_name",
                    "group_display_name" to groupDisplayName,
                    "player_display_name" to playerDisplayName,
                    "player_name" to playerName,
                )
        }
    }

    object Commands {
        fun sendNoPermission(executor: CommandExecutor) {
            sendMessage(executor, "commands.general.no_permission")
        }

        fun sendPlayerNotFound(executor: CommandExecutor, playerName: String) {
            sendMessage(
                executor,
                "commands.general.player_not_found",
                "player" to playerName,
            )
        }

        fun sendPlayerNotOnline(executor: CommandExecutor, playerName: String) {
            sendMessage(
                executor,
                "commands.general.player_not_online",
                "player" to playerName,
            )
        }

        fun sendInvalidUsage(executor: CommandExecutor, usage: String) {
            sendMessage(
                executor,
                "commands.general.invalid_usage",
                "usage" to usage,
            )
        }

        fun sendCommandError(executor: CommandExecutor) {
            sendMessage(executor, "commands.general.command_error")
        }

        fun sendUnknownCommand(executor: CommandExecutor) {
            sendMessage(executor, "commands.general.unknown_command")
        }

        object Server {
            object Ban {
                fun sendSuccess(
                    executor: CommandExecutor,
                    player: String,
                    reason: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.server.ban.success",
                        "player" to player,
                        "reason" to reason,
                    )
                }

                fun sendAlreadyBanned(
                    executor: CommandExecutor,
                    player: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.server.ban.already_banned",
                        "player" to player,
                    )
                }

                fun getDefaultReason(): String =
                    getMessage("commands.server.ban.default_reason")
            }

            object Unban {
                fun sendSuccess(executor: CommandExecutor, player: String) {
                    sendMessage(
                        executor,
                        "commands.server.unban.success",
                        "player" to player,
                    )
                }

                fun sendNotBanned(executor: CommandExecutor, player: String) {
                    sendMessage(
                        executor,
                        "commands.server.unban.not_banned",
                        "player" to player,
                    )
                }
            }

            object Kick {
                fun sendSuccess(executor: CommandExecutor, player: String) {
                    sendMessage(
                        executor,
                        "commands.server.kick.success",
                        "player" to player,
                    )
                }

                fun getDefaultReason(): String =
                    getMessage("commands.server.kick.default_reason")
            }

            object Stop {
                fun sendShuttingDown(executor: CommandExecutor) {
                    sendMessage(executor, "commands.server.stop.shutting_down")
                }

                fun getKickMessage(): String =
                    getMessage("commands.server.stop.kick_message")
            }

            object Info {
                fun sendHeader(executor: CommandExecutor) {
                    sendMessage(executor, "commands.server.info.header")
                }

                fun sendSoftware(executor: CommandExecutor, software: String) {
                    sendMessage(
                        executor,
                        "commands.server.info.software",
                        "software" to software,
                    )
                }

                fun sendUptime(executor: CommandExecutor, uptime: String) {
                    sendMessage(
                        executor,
                        "commands.server.info.uptime",
                        "uptime" to uptime,
                    )
                }

                fun sendPublic(executor: CommandExecutor, isPublic: Boolean) {
                    val status =
                        if (isPublic)
                            getMessage("commands.server.info.public_yes")
                        else getMessage("commands.server.info.public_no")
                    sendMessage(
                        executor,
                        "commands.server.info.public",
                        "status" to status,
                    )
                }
            }
            fun sendUniquePlayers(executor: CommandExecutor, count: Int) {
                sendMessage(
                    executor,
                    "commands.server.info.unique_players",
                    "count" to count,
                )
            }
        }

        object Chat {
            fun getSayFormat(): String = getMessage("commands.chat.say.format")

            fun sendNoPlayersOnline(executor: CommandExecutor) {
                sendMessage(executor, "commands.chat.no_players_online")
            }
        }

        object Player {
            object Info {
                fun sendHeader(executor: CommandExecutor, player: String) {
                    sendMessage(
                        executor,
                        "commands.player.info.header",
                        "player" to player,
                    )
                }

                fun sendClient(executor: CommandExecutor, client: String) {
                    sendMessage(
                        executor,
                        "commands.player.info.client",
                        "client" to client,
                    )
                }

                fun sendBannedStatus(
                    executor: CommandExecutor,
                    isBanned: Boolean,
                    reason: String = "",
                ) {
                    val status =
                        if (isBanned) {
                            getMessage(
                                "commands.player.info.banned_yes",
                                "reason" to reason,
                            )
                        } else {
                            getMessage("commands.player.info.banned_no")
                        }
                    sendMessage(
                        executor,
                        "commands.player.info.banned",
                        "status" to status,
                    )
                }

                fun sendFirstJoin(executor: CommandExecutor, date: String) {
                    sendMessage(
                        executor,
                        "commands.player.info.first_join",
                        "date" to date,
                    )
                }

                fun sendLastJoin(executor: CommandExecutor, date: String) {
                    sendMessage(
                        executor,
                        "commands.player.info.last_join",
                        "date" to date,
                    )
                }

                fun sendLastSeen(
                    executor: CommandExecutor,
                    time: String,
                    isOnline: Boolean = false,
                ) {
                    val displayTime =
                        if (isOnline)
                            getMessage("commands.player.info.last_seen_now")
                        else time
                    sendMessage(
                        executor,
                        "commands.player.info.last_seen",
                        "time" to displayTime,
                    )
                }

                fun sendPlaytime(executor: CommandExecutor, time: String) {
                    sendMessage(
                        executor,
                        "commands.player.info.playtime",
                        "time" to time,
                    )
                }

                fun sendJoinCount(executor: CommandExecutor, count: Int) {
                    sendMessage(
                        executor,
                        "commands.player.info.join_count",
                        "count" to count,
                    )
                }

                fun getUnknownClient(): String =
                    getMessage("commands.player.info.unknown_client")
            }
        }

        object Online {
            fun sendList(
                executor: CommandExecutor,
                count: Int,
                players: String,
            ) {
                sendMessage(
                    executor,
                    "commands.online.list",
                    "count" to count,
                    "players" to players,
                )
            }

            fun formatPlayer(name: String): String =
                getMessage("commands.online.format_player", "name" to name)
        }

        object Clients {
            fun sendHeader(executor: CommandExecutor) {
                sendMessage(executor, "commands.clients.header")
            }

            fun sendClientList(
                executor: CommandExecutor,
                client: String,
                players: String,
            ) {
                sendMessage(
                    executor,
                    "commands.clients.format",
                    "client" to client,
                    "players" to players,
                )
            }

            fun formatPlayer(name: String): String =
                getMessage("commands.clients.format_player", "name" to name)
        }

        object Help {
            fun sendHeader(executor: CommandExecutor, page: Int, total: Int) {
                sendMessage(
                    executor,
                    "commands.help.header",
                    "page" to page,
                    "total" to total,
                )
            }

            fun sendFooter(executor: CommandExecutor, page: Int, total: Int) {
                sendMessage(
                    executor,
                    "commands.help.footer",
                    "page" to page,
                    "total" to total,
                )
            }

            fun sendCommandNotFound(
                executor: CommandExecutor,
                command: String,
            ) {
                sendMessage(
                    executor,
                    "commands.help.command_not_found",
                    "command" to command,
                )
            }

            fun sendSubcommandNotFound(
                executor: CommandExecutor,
                subcommand: String,
                command: String,
            ) {
                sendMessage(
                    executor,
                    "commands.help.subcommand_not_found",
                    "subcommand" to subcommand,
                    "command" to command,
                )
            }

            fun sendInvalidPage(executor: CommandExecutor) {
                sendMessage(executor, "commands.help.invalid_page")
            }

            fun sendCommandInfoHeader(
                executor: CommandExecutor,
                command: String,
            ) {
                sendMessage(
                    executor,
                    "commands.help.command_info_header",
                    "command" to command,
                )
            }

            fun sendSubcommandInfoHeader(
                executor: CommandExecutor,
                path: String,
            ) {
                sendMessage(
                    executor,
                    "commands.help.subcommand_info_header",
                    "path" to path,
                )
            }

            fun sendSubcommandListHeader(
                executor: CommandExecutor,
                command: String,
                page: Int,
                total: Int,
            ) {
                sendMessage(
                    executor,
                    "commands.help.subcommand_list_header",
                    "command" to command,
                    "page" to page,
                    "total" to total,
                )
            }

            fun sendSeparator(executor: CommandExecutor) {
                sendMessage(executor, "commands.help.separator")
            }

            object Fields {
                fun sendCommand(executor: CommandExecutor, name: String) {
                    sendMessage(
                        executor,
                        "commands.help.fields.command",
                        "name" to name,
                    )
                }

                fun sendAliases(executor: CommandExecutor, aliases: String) {
                    sendMessage(
                        executor,
                        "commands.help.fields.aliases",
                        "aliases" to aliases,
                    )
                }

                fun sendDescription(
                    executor: CommandExecutor,
                    description: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.help.fields.description",
                        "description" to description,
                    )
                }

                fun sendUsage(executor: CommandExecutor, usage: String) {
                    sendMessage(
                        executor,
                        "commands.help.fields.usage",
                        "usage" to usage,
                    )
                }

                fun sendPermission(
                    executor: CommandExecutor,
                    permission: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.help.fields.permission",
                        "permission" to permission,
                    )
                }

                fun sendSubcommand(executor: CommandExecutor, name: String) {
                    sendMessage(
                        executor,
                        "commands.help.fields.subcommand",
                        "name" to name,
                    )
                }

                fun sendSubcommands(executor: CommandExecutor) {
                    sendMessage(executor, "commands.help.fields.subcommands")
                }
            }
        }

        object Level {
            fun sendSubcommandsAvailable(
                executor: CommandExecutor,
                commands: String,
            ) {
                sendMessage(
                    executor,
                    "commands.level.subcommands.available",
                    "commands" to commands,
                )
            }

            fun sendNoSubcommandsAvailable(executor: CommandExecutor) {
                sendMessage(
                    executor,
                    "commands.level.subcommands.none_available",
                )
            }

            object Create {
                fun sendSuccess(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.create.success",
                        "id" to id,
                    )
                }

                fun sendFailed(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.create.failed",
                        "id" to id,
                    )
                }

                fun sendInvalidDimensions(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.level.create.invalid_dimensions",
                    )
                }

                fun sendInvalidGenerator(
                    executor: CommandExecutor,
                    generator: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.create.invalid_generator",
                        "generator" to generator,
                    )
                }

                fun sendAvailableGenerators(
                    executor: CommandExecutor,
                    generators: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.create.available_generators",
                        "generators" to generators,
                    )
                }

                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.create.usage")
                }
            }

            object Load {
                fun sendSuccess(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.load.success",
                        "id" to id,
                    )
                }

                fun sendFailed(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.load.failed",
                        "id" to id,
                    )
                }

                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.load.usage")
                }
            }

            object Unload {
                fun sendSuccess(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.unload.success",
                        "id" to id,
                    )
                }

                fun sendFailed(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.unload.failed",
                        "id" to id,
                    )
                }

                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.unload.usage")
                }
            }

            object Delete {
                fun sendConfirmMessage(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.delete.confirm_message",
                        "id" to id,
                    )
                }

                fun sendConfirmInstruction(
                    executor: CommandExecutor,
                    id: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.delete.confirm_instruction",
                        "id" to id,
                    )
                }

                fun sendSuccess(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.delete.success",
                        "id" to id,
                    )
                }

                fun sendFailed(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.delete.failed",
                        "id" to id,
                    )
                }

                fun sendFileDeleteFailed(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.level.delete.file_delete_failed",
                    )
                }

                fun sendCannotDeleteDefault(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.level.delete.cannot_delete_default",
                    )
                }

                fun sendInvalidConfirmation(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.level.delete.invalid_confirmation",
                    )
                }

                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.delete.usage")
                }
            }

            object List {
                fun sendNoLevels(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.list.no_levels")
                }

                fun sendHeader(
                    executor: CommandExecutor,
                    page: Int,
                    total: Int,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.list.header",
                        "page" to page,
                        "total" to total,
                    )
                }

                fun sendLevel(
                    executor: CommandExecutor,
                    id: String,
                    players: Int,
                    size: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.list.format",
                        "id" to id,
                        "players" to players,
                        "size" to size,
                    )
                }

                fun sendInvalidPage(executor: CommandExecutor, total: Int) {
                    sendMessage(
                        executor,
                        "commands.level.list.invalid_page",
                        "total" to total,
                    )
                }
            }

            object Info {
                fun sendNotFound(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.info.not_found",
                        "id" to id,
                    )
                }

                fun sendHeader(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.info.header",
                        "id" to id,
                    )
                }

                fun sendAuthor(executor: CommandExecutor, author: String) {
                    sendMessage(
                        executor,
                        "commands.level.info.author",
                        "author" to author,
                    )
                }

                fun sendDescription(
                    executor: CommandExecutor,
                    description: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.info.description",
                        "description" to description,
                    )
                }

                fun sendSize(executor: CommandExecutor, size: String) {
                    sendMessage(
                        executor,
                        "commands.level.info.size",
                        "size" to size,
                    )
                }

                fun sendSpawn(executor: CommandExecutor, spawn: String) {
                    sendMessage(
                        executor,
                        "commands.level.info.spawn",
                        "spawn" to spawn,
                    )
                }

                fun sendPlayers(
                    executor: CommandExecutor,
                    players: String,
                    max: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.info.players",
                        "players" to players,
                        "max" to max,
                    )
                }

                fun sendEntities(executor: CommandExecutor, entities: String) {
                    sendMessage(
                        executor,
                        "commands.level.info.entities",
                        "entities" to entities,
                    )
                }

                fun sendAutoSave(executor: CommandExecutor, status: String) {
                    sendMessage(
                        executor,
                        "commands.level.info.auto_save",
                        "status" to status,
                    )
                }

                fun sendWeather(executor: CommandExecutor, weather: String) {
                    sendMessage(
                        executor,
                        "commands.level.info.weather",
                        "weather" to weather,
                    )
                }

                fun sendTexturePack(executor: CommandExecutor, url: String) {
                    sendMessage(
                        executor,
                        "commands.level.info.texture_pack",
                        "url" to url,
                    )
                }

                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.info.usage")
                }
            }

            object Stats {
                fun sendHeader(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.stats.header")
                }

                fun sendTotalLevels(executor: CommandExecutor, count: Any) {
                    sendMessage(
                        executor,
                        "commands.level.stats.total_levels",
                        "count" to count,
                    )
                }

                fun sendTotalPlayers(executor: CommandExecutor, count: Any) {
                    sendMessage(
                        executor,
                        "commands.level.stats.total_players",
                        "count" to count,
                    )
                }

                fun sendTotalEntities(executor: CommandExecutor, count: Any) {
                    sendMessage(
                        executor,
                        "commands.level.stats.total_entities",
                        "count" to count,
                    )
                }

                fun sendDefaultLevel(executor: CommandExecutor, level: Any) {
                    sendMessage(
                        executor,
                        "commands.level.stats.default_level",
                        "level" to level,
                    )
                }

                fun sendAutoSaveInterval(
                    executor: CommandExecutor,
                    interval: Any,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.stats.auto_save_interval",
                        "interval" to interval,
                    )
                }
            }

            object Teleport {
                fun sendSuccessSelf(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.teleport.success_self",
                        "id" to id,
                    )
                }

                fun sendSuccessOther(
                    executor: CommandExecutor,
                    player: String,
                    id: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.teleport.success_other",
                        "player" to player,
                        "id" to id,
                    )
                }

                fun sendSpecifyPlayer(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.level.teleport.specify_player",
                    )
                }

                fun sendNoPermission(executor: CommandExecutor){
                    sendMessage(
                        executor,
                        "commands.level.teleport.no_permission",
                    )
                }

                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.teleport.usage")
                }
            }

            object Kick {
                fun sendSuccess(
                    executor: CommandExecutor,
                    count: Int,
                    id: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.kick.success",
                        "count" to count,
                        "id" to id,
                    )
                }

                fun getDefaultReason(): String =
                    getMessage("commands.level.kick.default_reason")

                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.kick.usage")
                }
            }

            object Redirect {
                fun sendSuccess(
                    executor: CommandExecutor,
                    from: String,
                    to: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.redirect.success",
                        "from" to from,
                        "to" to to,
                    )
                }

                fun sendFailed(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.redirect.failed")
                }

                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.redirect.usage")
                }
            }

            object Set {
                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.set.usage")
                }

                fun sendProperties(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.set.properties")
                }

                fun sendUnknownProperty(
                    executor: CommandExecutor,
                    property: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.set.unknown_property",
                        "property" to property,
                    )
                }

                object Spawn {
                    fun sendSuccessCurrent(
                        executor: CommandExecutor,
                        id: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.level.set.spawn.success_current",
                            "id" to id,
                        )
                    }

                    fun sendSuccessCoords(
                        executor: CommandExecutor,
                        coords: String,
                        id: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.level.set.spawn.success_coords",
                            "coords" to coords,
                            "id" to id,
                        )
                    }

                    fun sendConsoleCoords(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.set.spawn.console_coords",
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(executor, "commands.level.set.spawn.usage")
                    }

                    fun sendInvalidCoords(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.set.spawn.invalid_coords",
                        )
                    }
                }

                object AutoSave {
                    fun sendEnabled(executor: CommandExecutor, id: String) {
                        sendMessage(
                            executor,
                            "commands.level.set.autosave.enabled",
                            "id" to id,
                        )
                    }

                    fun sendDisabled(executor: CommandExecutor, id: String) {
                        sendMessage(
                            executor,
                            "commands.level.set.autosave.disabled",
                            "id" to id,
                        )
                    }

                    fun sendInvalidValue(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.set.autosave.invalid_value",
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.set.autosave.usage",
                        )
                    }
                }

                object Default {
                    fun sendSuccess(executor: CommandExecutor, id: String) {
                        sendMessage(
                            executor,
                            "commands.level.set.default.success",
                            "id" to id,
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.set.default.usage",
                        )
                    }
                }

                object Description {
                    fun sendSuccess(
                        executor: CommandExecutor,
                        id: String,
                        description: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.level.set.description.success",
                            "id" to id,
                            "description" to description,
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.set.description.usage",
                        )
                    }
                }
            }

            object Env {
                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.env.usage")
                }

                fun sendProperties(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.env.properties")
                }

                fun sendUnknownProperty(
                    executor: CommandExecutor,
                    property: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.env.unknown_property",
                        "property" to property,
                    )
                }

                object Texture {
                    fun sendSuccess(executor: CommandExecutor, id: String) {
                        sendMessage(
                            executor,
                            "commands.level.env.texture.success",
                            "id" to id,
                        )
                    }

                    fun sendReset(executor: CommandExecutor, id: String) {
                        sendMessage(
                            executor,
                            "commands.level.env.texture.reset",
                            "id" to id,
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.env.texture.usage",
                        )
                    }
                }

                object Weather {
                    fun sendSuccess(
                        executor: CommandExecutor,
                        weather: String,
                        id: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.level.env.weather.success",
                            "weather" to weather,
                            "id" to id,
                        )
                    }

                    fun sendInvalid(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.env.weather.invalid",
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.env.weather.usage",
                        )
                    }
                }

                object Blocks {
                    fun sendSuccess(
                        executor: CommandExecutor,
                        type: String,
                        blockId: Short,
                        id: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.level.env.blocks.success",
                            "type" to type,
                            "blockId" to blockId,
                            "id" to id,
                        )
                    }

                    fun sendInvalidType(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.env.blocks.invalid_type",
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(executor, "commands.level.env.blocks.usage")
                    }
                }

                object Height {
                    fun sendSuccess(
                        executor: CommandExecutor,
                        type: String,
                        height: Int,
                        id: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.level.env.height.success",
                            "type" to type,
                            "height" to height,
                            "id" to id,
                        )
                    }

                    fun sendInvalidType(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.env.height.invalid_type",
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(executor, "commands.level.env.height.usage")
                    }
                }

                object Fog {
                    fun sendSuccess(
                        executor: CommandExecutor,
                        type: String,
                        value: String,
                        id: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.level.env.fog.success",
                            "type" to type,
                            "value" to value,
                            "id" to id,
                        )
                    }

                    fun sendInvalidType(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.env.fog.invalid_type",
                        )
                    }

                    fun sendInvalidValue(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.env.fog.invalid_value",
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(executor, "commands.level.env.fog.usage")
                    }
                }

                object Speed {
                    fun sendSuccess(
                        executor: CommandExecutor,
                        type: String,
                        speed: Int,
                        id: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.level.env.speed.success",
                            "type" to type,
                            "speed" to speed,
                            "id" to id,
                        )
                    }

                    fun sendInvalidType(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.env.speed.invalid_type",
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(executor, "commands.level.env.speed.usage")
                    }
                }

                object Fade {
                    fun sendSuccess(
                        executor: CommandExecutor,
                        fade: Int,
                        id: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.level.env.fade.success",
                            "fade" to fade,
                            "id" to id,
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(executor, "commands.level.env.fade.usage")
                    }
                }

                object Offset {
                    fun sendSuccess(
                        executor: CommandExecutor,
                        offset: Int,
                        id: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.level.env.offset.success",
                            "offset" to offset,
                            "id" to id,
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(executor, "commands.level.env.offset.usage")
                    }
                }

                object Colors {
                    fun sendSuccess(
                        executor: CommandExecutor,
                        colorType: String,
                        id: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.level.env.colors.success",
                            "colorType" to colorType,
                            "id" to id,
                        )
                    }

                    fun sendReset(
                        executor: CommandExecutor,
                        colorType: String,
                        id: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.level.env.colors.reset",
                            "colorType" to colorType,
                            "id" to id,
                        )
                    }

                    fun sendInvalidType(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.env.colors.invalid_type",
                        )
                    }

                    fun sendInvalidColor(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.env.colors.invalid_color",
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(executor, "commands.level.env.colors.usage")
                    }
                }

                object LightingMode {
                    fun sendSuccess(
                        executor: CommandExecutor,
                        mode: String,
                        id: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.level.env.lightingmode.success",
                            "mode" to mode,
                            "id" to id,
                        )
                    }

                    fun sendLocked(executor: CommandExecutor, id: String) {
                        sendMessage(
                            executor,
                            "commands.level.env.lightingmode.locked",
                            "id" to id,
                        )
                    }

                    fun sendUnlocked(executor: CommandExecutor, id: String) {
                        sendMessage(
                            executor,
                            "commands.level.env.lightingmode.unlocked",
                            "id" to id,
                        )
                    }

                    fun sendInvalidMode(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.env.lightingmode.invalid_mode",
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.env.lightingmode.usage",
                        )
                    }

                    fun sendModesHelp(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.level.env.lightingmode.modes_help",
                        )
                    }
                }
            }

            object Save {
                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.save.usage")
                }

                fun sendSuccessAll(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.save.success_all")
                }

                fun sendSuccessSingle(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.save.success_single",
                        "id" to id,
                    )
                }

                fun sendFailed(
                    executor: CommandExecutor,
                    id: String,
                    error: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.save.failed",
                        "id" to id,
                        "error" to error,
                    )
                }
            }

            object Reload {
                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.level.reload.usage")
                }

                fun sendConfirmMessage(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.reload.confirm_message",
                        "id" to id,
                    )
                }

                fun sendConfirmInstruction(
                    executor: CommandExecutor,
                    id: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.level.reload.confirm_instruction",
                        "id" to id,
                    )
                }

                fun sendSuccess(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.reload.success",
                        "id" to id,
                    )
                }

                fun sendFailed(executor: CommandExecutor, id: String) {
                    sendMessage(
                        executor,
                        "commands.level.reload.failed",
                        "id" to id,
                    )
                }

                fun getKickMessage(): String =
                    getMessage("commands.level.reload.kick_message")
            }
        }

        object Permission {
            fun sendNoPermissionManageGroups(executor: CommandExecutor) {
                sendMessage(
                    executor,
                    "commands.permission.no_permission_manage_groups",
                )
            }

            fun sendNoPermissionDeleteGroups(executor: CommandExecutor) {
                sendMessage(
                    executor,
                    "commands.permission.no_permission_delete_groups",
                )
            }

            fun sendNoPermissionEditGroups(executor: CommandExecutor) {
                sendMessage(
                    executor,
                    "commands.permission.no_permission_edit_groups",
                )
            }

            fun sendNoPermissionManagePlayerGroups(executor: CommandExecutor) {
                sendMessage(
                    executor,
                    "commands.permission.no_permission_manage_player_groups",
                )
            }

            fun sendNoPermissionManagePlayerPerms(executor: CommandExecutor) {
                sendMessage(
                    executor,
                    "commands.permission.no_permission_manage_player_perms",
                )
            }

            fun sendNoPermissionReload(executor: CommandExecutor) {
                sendMessage(
                    executor,
                    "commands.permission.no_permission_reload",
                )
            }

            fun sendSubcommandsAvailable(
                executor: CommandExecutor,
                commands: String,
            ) {
                sendMessage(
                    executor,
                    "commands.permission.subcommands.available",
                    "commands" to commands,
                )
            }

            fun sendNoSubcommandsAvailable(executor: CommandExecutor) {
                sendMessage(
                    executor,
                    "commands.permission.subcommands.none_available",
                )
            }

            fun sendReloadSuccess(executor: CommandExecutor) {
                sendMessage(executor, "commands.permission.reload.success")
            }

            fun sendReloadFailed(executor: CommandExecutor, error: String) {
                sendMessage(
                    executor,
                    "commands.permission.reload.failed",
                    "error" to error,
                )
            }

            object Who {
                fun sendHeader(executor: CommandExecutor, permission: String) {
                    sendMessage(
                        executor,
                        "commands.permission.who.header",
                        "permission" to permission,
                    )
                }

                fun sendGroups(executor: CommandExecutor, groups: String) {
                    sendMessage(
                        executor,
                        "commands.permission.who.groups",
                        "groups" to groups,
                    )
                }

                fun sendPlayers(executor: CommandExecutor, players: String) {
                    sendMessage(
                        executor,
                        "commands.permission.who.players",
                        "players" to players,
                    )
                }

                fun formatGroup(
                    name: String,
                    display: String,
                    status: String,
                ): String {
                    return getMessage(
                        "permission.who.group_format",
                        "name" to name,
                        "display" to display,
                        "status" to status,
                    )
                }

                fun formatPlayer(name: String, status: String): String {
                    return getMessage(
                        "permission.who.player_format",
                        "name" to name,
                        "status" to status,
                    )
                }

                fun formatPlayerViaGroup(name: String): String {
                    return getMessage(
                        "permission.who.player_via_group",
                        "name" to name,
                    )
                }

                fun sendNoOne(executor: CommandExecutor) {
                    sendMessage(executor, "commands.permission.who.no_one")
                }

                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.permission.who.usage")
                }
            }

            object Group {
                object Create {
                    fun sendSuccess(
                        executor: CommandExecutor,
                        name: String,
                        display: String,
                        priority: Int,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.create.success",
                            "name" to name,
                            "display" to display,
                            "priority" to priority,
                        )
                    }

                    fun sendAlreadyExists(
                        executor: CommandExecutor,
                        name: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.create.already_exists",
                            "name" to name,
                        )
                    }

                    fun sendInvalidPriority(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.create.invalid_priority",
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.create.usage",
                        )
                    }
                }

                object Delete {
                    fun sendConfirmMessage(
                        executor: CommandExecutor,
                        name: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.delete.confirm_message",
                            "name" to name,
                        )
                    }

                    fun sendConfirmInstruction(
                        executor: CommandExecutor,
                        name: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.delete.confirm_instruction",
                            "name" to name,
                        )
                    }

                    fun sendSuccess(executor: CommandExecutor, name: String) {
                        sendMessage(
                            executor,
                            "commands.permission.group.delete.success",
                            "name" to name,
                        )
                    }

                    fun sendFailed(executor: CommandExecutor, name: String) {
                        sendMessage(
                            executor,
                            "commands.permission.group.delete.failed",
                            "name" to name,
                        )
                    }

                    fun sendCannotDeleteDefault(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.delete.cannot_delete_default",
                        )
                    }

                    fun sendNotFound(executor: CommandExecutor, name: String) {
                        sendMessage(
                            executor,
                            "commands.permission.group.delete.not_found",
                            "name" to name,
                        )
                    }

                    fun sendInvalidConfirmation(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.delete.invalid_confirmation",
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.delete.usage",
                        )
                    }
                }

                object List {
                    fun sendNoGroups(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.list.no_groups",
                        )
                    }

                    fun sendHeader(
                        executor: CommandExecutor,
                        page: Int,
                        total: Int,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.list.header",
                            "page" to page,
                            "total" to total,
                        )
                    }

                    fun sendGroup(
                        executor: CommandExecutor,
                        name: String,
                        display: String,
                        priority: Int,
                        count: Int,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.list.format",
                            "name" to name,
                            "display" to display,
                            "priority" to priority,
                            "count" to count,
                        )
                    }

                    fun sendInvalidPage(executor: CommandExecutor, total: Int) {
                        sendMessage(
                            executor,
                            "commands.permission.group.list.invalid_page",
                            "total" to total,
                        )
                    }
                }

                object Info {
                    fun sendHeader(executor: CommandExecutor, name: String) {
                        sendMessage(
                            executor,
                            "commands.permission.group.info.header",
                            "name" to name,
                        )
                    }

                    fun sendDisplayName(
                        executor: CommandExecutor,
                        display: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.info.display_name",
                            "display" to display,
                        )
                    }

                    fun sendPriority(executor: CommandExecutor, priority: Int) {
                        sendMessage(
                            executor,
                            "commands.permission.group.info.priority",
                            "priority" to priority,
                        )
                    }

                    fun sendPlayers(executor: CommandExecutor, count: Int) {
                        sendMessage(
                            executor,
                            "commands.permission.group.info.players",
                            "count" to count,
                        )
                    }

                    fun sendPermissions(executor: CommandExecutor, count: Int) {
                        sendMessage(
                            executor,
                            "commands.permission.group.info.permissions",
                            "count" to count,
                        )
                    }

                    fun sendPermissionList(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.info.permission_list",
                        )
                    }

                    fun sendPermissionEntry(
                        executor: CommandExecutor,
                        permission: String,
                        status: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.info.permission_entry",
                            "permission" to permission,
                            "status" to status,
                        )
                    }

                    fun getGranted(): String =
                        getMessage("commands.permission.group.info.granted")

                    fun getRevoked(): String =
                        getMessage("commands.permission.group.info.revoked")

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.info.usage",
                        )
                    }
                }

                object Permissions {
                    fun sendGrantSuccess(
                        executor: CommandExecutor,
                        permission: String,
                        name: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.permissions.grant_success",
                            "permission" to permission,
                            "name" to name,
                        )
                    }

                    fun sendRevokeSuccess(
                        executor: CommandExecutor,
                        permission: String,
                        name: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.permissions.revoke_success",
                            "permission" to permission,
                            "name" to name,
                        )
                    }

                    fun sendRemoveSuccess(
                        executor: CommandExecutor,
                        permission: String,
                        name: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.permissions.remove_success",
                            "permission" to permission,
                            "name" to name,
                        )
                    }

                    fun sendGroupNotFound(
                        executor: CommandExecutor,
                        name: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.permissions.group_not_found",
                            "name" to name,
                        )
                    }

                    fun sendRemoveFailed(
                        executor: CommandExecutor,
                        name: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.permissions.remove_failed",
                            "name" to name,
                        )
                    }

                    fun sendGrantUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.permissions.grant_usage",
                        )
                    }

                    fun sendRevokeUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.permissions.revoke_usage",
                        )
                    }

                    fun sendRemoveUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.permissions.remove_usage",
                        )
                    }

                    fun sendListEmpty(executor: CommandExecutor, name: String) {
                        sendMessage(
                            executor,
                            "commands.permission.group.permissions.list_empty",
                            "name" to name,
                        )
                    }

                    fun sendListHeader(
                        executor: CommandExecutor,
                        name: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.permissions.list_header",
                            "name" to name,
                        )
                    }

                    fun sendListUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.permissions.list_usage",
                        )
                    }

                    fun sendCheckNotSet(
                        executor: CommandExecutor,
                        name: String,
                        permission: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.permissions.check_not_set",
                            "name" to name,
                            "permission" to permission,
                        )
                    }

                    fun sendCheckResult(
                        executor: CommandExecutor,
                        name: String,
                        permission: String,
                        status: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.permissions.check_result",
                            "name" to name,
                            "permission" to permission,
                            "status" to status,
                        )
                    }

                    fun sendCheckUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.permissions.check_usage",
                        )
                    }
                }

                object Edit {
                    fun sendPrioritySuccess(
                        executor: CommandExecutor,
                        name: String,
                        priority: Int,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.edit.priority_success",
                            "name" to name,
                            "priority" to priority,
                        )
                    }

                    fun sendDisplaySuccess(
                        executor: CommandExecutor,
                        name: String,
                        display: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.group.edit.display_success",
                            "name" to name,
                            "display" to display,
                        )
                    }

                    fun sendPriorityUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.edit.priority_usage",
                        )
                    }

                    fun sendDisplayUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.group.edit.display_usage",
                        )
                    }
                }
            }

            object Player {
                object Info {
                    fun sendHeader(executor: CommandExecutor, name: String) {
                        sendMessage(
                            executor,
                            "commands.permission.player.info.header",
                            "name" to name,
                        )
                    }

                    fun sendHighestGroup(
                        executor: CommandExecutor,
                        group: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.info.highest_group",
                            "group" to group,
                        )
                    }

                    fun sendTotalGroups(executor: CommandExecutor, count: Int) {
                        sendMessage(
                            executor,
                            "commands.permission.player.info.total_groups",
                            "count" to count,
                        )
                    }

                    fun sendTotalPermissions(
                        executor: CommandExecutor,
                        count: Int,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.info.total_permissions",
                            "count" to count,
                        )
                    }

                    fun sendIndividualPermissions(
                        executor: CommandExecutor,
                        count: Int,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.info.individual_permissions",
                            "count" to count,
                        )
                    }

                    fun sendGroups(executor: CommandExecutor, groups: String) {
                        sendMessage(
                            executor,
                            "commands.permission.player.info.groups",
                            "groups" to groups,
                        )
                    }

                    fun sendUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.player.info.usage",
                        )
                    }
                }

                object Groups {
                    fun sendAddSuccess(
                        executor: CommandExecutor,
                        group: String,
                        player: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.groups.add_success",
                            "group" to group,
                            "player" to player,
                        )
                    }

                    fun sendAddFailed(
                        executor: CommandExecutor,
                        group: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.groups.add_failed",
                            "group" to group,
                        )
                    }

                    fun sendRemoveSuccess(
                        executor: CommandExecutor,
                        group: String,
                        player: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.groups.remove_success",
                            "group" to group,
                            "player" to player,
                        )
                    }

                    fun sendRemoveFailed(
                        executor: CommandExecutor,
                        group: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.groups.remove_failed",
                            "group" to group,
                        )
                    }

                    fun sendSetSuccess(
                        executor: CommandExecutor,
                        player: String,
                        groups: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.groups.set_success",
                            "player" to player,
                            "groups" to groups,
                        )
                    }

                    fun sendSetInvalid(
                        executor: CommandExecutor,
                        groups: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.groups.set_invalid",
                            "groups" to groups,
                        )
                    }

                    fun sendListEmpty(
                        executor: CommandExecutor,
                        player: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.groups.list_empty",
                            "player" to player,
                        )
                    }

                    fun sendListHeader(
                        executor: CommandExecutor,
                        player: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.groups.list_header",
                            "player" to player,
                        )
                    }

                    fun sendListFormat(
                        executor: CommandExecutor,
                        name: String,
                        display: String,
                        priority: Int,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.groups.list_format",
                            "name" to name,
                            "display" to display,
                            "priority" to priority,
                        )
                    }

                    fun sendAddUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.player.groups.add_usage",
                        )
                    }

                    fun sendRemoveUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.player.groups.remove_usage",
                        )
                    }

                    fun sendSetUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.player.groups.set_usage",
                        )
                    }

                    fun sendListUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.player.groups.list_usage",
                        )
                    }
                }

                object Permissions {
                    fun sendGrantSuccess(
                        executor: CommandExecutor,
                        permission: String,
                        player: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.grant_success",
                            "permission" to permission,
                            "player" to player,
                        )
                    }

                    fun sendRevokeSuccess(
                        executor: CommandExecutor,
                        permission: String,
                        player: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.revoke_success",
                            "permission" to permission,
                            "player" to player,
                        )
                    }

                    fun sendRemoveSuccess(
                        executor: CommandExecutor,
                        permission: String,
                        player: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.remove_success",
                            "permission" to permission,
                            "player" to player,
                        )
                    }

                    fun sendRemoveFailed(
                        executor: CommandExecutor,
                        player: String,
                        permission: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.remove_failed",
                            "player" to player,
                            "permission" to permission,
                        )
                    }

                    fun sendListEmpty(
                        executor: CommandExecutor,
                        player: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.list_empty",
                            "player" to player,
                        )
                    }

                    fun sendListHeader(
                        executor: CommandExecutor,
                        player: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.list_header",
                            "player" to player,
                        )
                    }

                    fun sendListIndividual(
                        executor: CommandExecutor,
                        permission: String,
                        status: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.list_individual",
                            "permission" to permission,
                            "status" to status,
                        )
                    }

                    fun sendListGroup(
                        executor: CommandExecutor,
                        permission: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.list_group",
                            "permission" to permission,
                        )
                    }

                    fun sendOwnEmpty(
                        executor: CommandExecutor,
                        player: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.own_empty",
                            "player" to player,
                        )
                    }

                    fun sendOwnHeader(
                        executor: CommandExecutor,
                        player: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.own_header",
                            "player" to player,
                        )
                    }

                    fun sendOwnFormat(
                        executor: CommandExecutor,
                        permission: String,
                        status: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.own_format",
                            "permission" to permission,
                            "status" to status,
                        )
                    }

                    fun getCheckYes(): String =
                        getMessage(
                            "commands.permission.player.permissions.check_yes"
                        )

                    fun getCheckNo(): String =
                        getMessage(
                            "commands.permission.player.permissions.check_no"
                        )

                    fun sendCheckResult(
                        executor: CommandExecutor,
                        player: String,
                        permission: String,
                        status: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.check_result",
                            "player" to player,
                            "permission" to permission,
                            "status" to status,
                        )
                    }

                    fun sendCheckIndividual(
                        executor: CommandExecutor,
                        player: String,
                        permission: String,
                        status: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.check_individual",
                            "player" to player,
                            "permission" to permission,
                            "status" to status,
                        )
                    }

                    fun sendCheckGroup(
                        executor: CommandExecutor,
                        player: String,
                        permission: String,
                        group: String,
                        status: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.check_group",
                            "player" to player,
                            "permission" to permission,
                            "group" to group,
                            "status" to status,
                        )
                    }

                    fun sendCheckNotFound(
                        executor: CommandExecutor,
                        player: String,
                        permission: String,
                    ) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.check_not_found",
                            "player" to player,
                            "permission" to permission,
                        )
                    }

                    fun sendGrantUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.grant_usage",
                        )
                    }

                    fun sendRevokeUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.revoke_usage",
                        )
                    }

                    fun sendRemoveUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.remove_usage",
                        )
                    }

                    fun sendListUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.list_usage",
                        )
                    }

                    fun sendOwnUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.own_usage",
                        )
                    }

                    fun sendCheckUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.check_usage",
                        )
                    }

                    fun sendHaspermUsage(executor: CommandExecutor) {
                        sendMessage(
                            executor,
                            "commands.permission.player.permissions.hasperm_usage",
                        )
                    }
                }
            }

            object Help {
                fun sendGroupHeader(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.group_header",
                    )
                }

                fun sendGroupCreate(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.group_create",
                    )
                }

                fun sendGroupDelete(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.group_delete",
                    )
                }

                fun sendGroupList(executor: CommandExecutor) {
                    sendMessage(executor, "commands.permission.help.group_list")
                }

                fun sendGroupInfo(executor: CommandExecutor) {
                    sendMessage(executor, "commands.permission.help.group_info")
                }

                fun sendGroupSetPriority(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.group_setpriority",
                    )
                }

                fun sendGroupSetDisplay(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.group_setdisplay",
                    )
                }

                fun sendGroupGrant(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.group_grant",
                    )
                }

                fun sendGroupRevoke(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.group_revoke",
                    )
                }

                fun sendGroupRemove(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.group_remove",
                    )
                }

                fun sendGroupListPerms(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.group_listperms",
                    )
                }

                fun sendGroupHasPerm(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.group_hasperm",
                    )
                }

                fun sendPlayerHeader(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.player_header",
                    )
                }

                fun sendPlayerInfo(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.player_info",
                    )
                }

                fun sendPlayerAddGroup(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.player_addgroup",
                    )
                }

                fun sendPlayerRemoveGroup(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.player_removegroup",
                    )
                }

                fun sendPlayerSetGroups(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.player_setgroups",
                    )
                }

                fun sendPlayerListGroups(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.player_listgroups",
                    )
                }

                fun sendPlayerGrant(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.player_grant",
                    )
                }

                fun sendPlayerRevoke(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.player_revoke",
                    )
                }

                fun sendPlayerRemove(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.player_remove",
                    )
                }

                fun sendPlayerListPerms(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.player_listperms",
                    )
                }

                fun sendPlayerListOwnPerms(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.player_listownperms",
                    )
                }

                fun sendPlayerHasPerm(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.player_hasperm",
                    )
                }

                fun sendPlayerCheck(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.permission.help.player_check",
                    )
                }
            }
        }

        object Plugin {
            fun sendSubcommandsAvailable(executor: CommandExecutor) {
                sendMessage(executor, "commands.plugin.subcommands.available")
            }

            object List {
                fun sendNoPlugins(executor: CommandExecutor) {
                    sendMessage(executor, "commands.plugin.list.no_plugins")
                }

                fun sendHeader(executor: CommandExecutor) {
                    sendMessage(executor, "commands.plugin.list.header")
                }

                fun sendPlugin(
                    executor: CommandExecutor,
                    name: String,
                    version: String,
                    description: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.plugin.list.format",
                        "name" to name,
                        "version" to version,
                        "description" to description,
                    )
                }
            }

            object Info {
                fun sendNotFound(executor: CommandExecutor, name: String) {
                    sendMessage(
                        executor,
                        "commands.plugin.info.not_found",
                        "name" to name,
                    )
                }

                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.plugin.info.usage")
                }

                fun sendHeader(executor: CommandExecutor, name: String) {
                    sendMessage(
                        executor,
                        "commands.plugin.info.header",
                        "name" to name,
                    )
                }

                fun sendVersion(executor: CommandExecutor, version: String) {
                    sendMessage(
                        executor,
                        "commands.plugin.info.version",
                        "version" to version,
                    )
                }

                fun sendDescription(
                    executor: CommandExecutor,
                    description: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.plugin.info.description",
                        "description" to description,
                    )
                }

                fun sendAuthors(executor: CommandExecutor, authors: String) {
                    sendMessage(
                        executor,
                        "commands.plugin.info.authors",
                        "authors" to authors,
                    )
                }

                fun sendDependencies(
                    executor: CommandExecutor,
                    dependencies: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.plugin.info.dependencies",
                        "dependencies" to dependencies,
                    )
                }
            }

            object Load {
                fun sendSuccess(executor: CommandExecutor, name: String) {
                    sendMessage(
                        executor,
                        "commands.plugin.load.success",
                        "name" to name,
                    )
                }

                fun sendFailed(executor: CommandExecutor, name: String) {
                    sendMessage(
                        executor,
                        "commands.plugin.load.failed",
                        "name" to name,
                    )
                }

                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.plugin.load.usage")
                }
            }

            object Unload {
                fun sendSuccess(executor: CommandExecutor, name: String) {
                    sendMessage(
                        executor,
                        "commands.plugin.unload.success",
                        "name" to name,
                    )
                }

                fun sendFailed(executor: CommandExecutor, name: String) {
                    sendMessage(
                        executor,
                        "commands.plugin.unload.failed",
                        "name" to name,
                    )
                }

                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.plugin.unload.usage")
                }
            }

            object Reload {
                fun sendSuccessSingle(executor: CommandExecutor, name: String) {
                    sendMessage(
                        executor,
                        "commands.plugin.reload.success_single",
                        "name" to name,
                    )
                }

                fun sendSuccessAll(executor: CommandExecutor) {
                    sendMessage(executor, "commands.plugin.reload.success_all")
                }

                fun sendFailed(executor: CommandExecutor, name: String) {
                    sendMessage(
                        executor,
                        "commands.plugin.reload.failed",
                        "name" to name,
                    )
                }

                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.plugin.reload.usage")
                }
            }
        }

        object Block {
            fun sendGlobalOrLevelRequired(executor: CommandExecutor) {
                sendMessage(executor, "commands.block.global_or_level_required")
            }

            fun sendAirNotEditable(executor: CommandExecutor) {
                sendMessage(executor, "commands.block.air_not_editable")
            }

            fun sendLevelNotFound(executor: CommandExecutor, level: String) {
                sendMessage(
                    executor,
                    "commands.block.level_not_found",
                    "level" to level,
                )
            }

            fun sendBlockNotFound(executor: CommandExecutor, id: UShort) {
                sendMessage(
                    executor,
                    "commands.block.block_not_found",
                    "id" to id,
                )
            }

            fun sendInvalidProperty(
                executor: CommandExecutor,
                property: String,
            ) {
                sendMessage(
                    executor,
                    "commands.block.invalid_property",
                    "property" to property,
                )
            }

            fun sendInvalidId(executor: CommandExecutor) {
                sendMessage(executor, "commands.block.invalid_id")
            }

            fun sendIdAlreadyExists(executor: CommandExecutor, id: UShort) {
                sendMessage(
                    executor,
                    "commands.block.id_already_exists",
                    "id" to id,
                )
            }

            object Info {
                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.block.info.usage")
                }

                fun sendHeader(executor: CommandExecutor, id: UShort) {
                    sendMessage(
                        executor,
                        "commands.block.info.header",
                        "id" to id,
                    )
                }

                fun sendName(executor: CommandExecutor, name: String) {
                    sendMessage(
                        executor,
                        "commands.block.info.name",
                        "name" to name,
                    )
                }

                fun sendFallback(executor: CommandExecutor, fallback: Byte) {
                    sendMessage(
                        executor,
                        "commands.block.info.fallback",
                        "fallback" to fallback,
                    )
                }

                fun sendSolidity(executor: CommandExecutor, solidity: String) {
                    sendMessage(
                        executor,
                        "commands.block.info.solidity",
                        "solidity" to solidity,
                    )
                }

                fun sendMovementSpeed(executor: CommandExecutor, speed: Byte) {
                    sendMessage(
                        executor,
                        "commands.block.info.movement_speed",
                        "speed" to speed,
                    )
                }

                fun sendTextures(
                    executor: CommandExecutor,
                    top: UShort,
                    side: UShort,
                    bottom: UShort,
                ) {
                    sendMessage(
                        executor,
                        "commands.block.info.textures",
                        "top" to top,
                        "side" to side,
                        "bottom" to bottom,
                    )
                }

                fun sendExtendedTextures(
                    executor: CommandExecutor,
                    top: UShort,
                    left: UShort,
                    right: UShort,
                    front: UShort,
                    back: UShort,
                    bottom: UShort,
                ) {
                    sendMessage(
                        executor,
                        "commands.block.info.extended_textures",
                        "top" to top,
                        "left" to left,
                        "right" to right,
                        "front" to front,
                        "back" to back,
                        "bottom" to bottom,
                    )
                }

                fun sendTransmitsLight(
                    executor: CommandExecutor,
                    transmits: Boolean,
                ) {
                    sendMessage(
                        executor,
                        "commands.block.info.transmits_light",
                        "transmits" to transmits,
                    )
                }

                fun sendWalkSound(executor: CommandExecutor, sound: String) {
                    sendMessage(
                        executor,
                        "commands.block.info.walk_sound",
                        "sound" to sound,
                    )
                }

                fun sendFullBright(executor: CommandExecutor, bright: Boolean) {
                    sendMessage(
                        executor,
                        "commands.block.info.full_bright",
                        "bright" to bright,
                    )
                }

                fun sendShape(executor: CommandExecutor, shape: Byte) {
                    sendMessage(
                        executor,
                        "commands.block.info.shape",
                        "shape" to shape,
                    )
                }

                fun sendBlockDraw(executor: CommandExecutor, draw: String) {
                    sendMessage(
                        executor,
                        "commands.block.info.block_draw",
                        "draw" to draw,
                    )
                }

                fun sendFog(
                    executor: CommandExecutor,
                    density: Byte,
                    r: Byte,
                    g: Byte,
                    b: Byte,
                ) {
                    sendMessage(
                        executor,
                        "commands.block.info.fog",
                        "density" to density,
                        "r" to r,
                        "g" to g,
                        "b" to b,
                    )
                }

                fun sendBounds(
                    executor: CommandExecutor,
                    minX: Byte,
                    minY: Byte,
                    minZ: Byte,
                    maxX: Byte,
                    maxY: Byte,
                    maxZ: Byte,
                ) {
                    sendMessage(
                        executor,
                        "commands.block.info.bounds",
                        "minX" to minX,
                        "minY" to minY,
                        "minZ" to minZ,
                        "maxX" to maxX,
                        "maxY" to maxY,
                        "maxZ" to maxZ,
                    )
                }

                fun sendExtended(executor: CommandExecutor, extended: Boolean) {
                    sendMessage(
                        executor,
                        "commands.block.info.extended",
                        "extended" to extended,
                    )
                }
            }

            object Edit {
                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.block.edit.usage")
                }

                fun sendProperties(executor: CommandExecutor) {
                    sendMessage(executor, "commands.block.edit.properties")
                }

                fun sendSuccess(
                    executor: CommandExecutor,
                    id: UShort,
                    name: String,
                    property: String,
                    value: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.block.edit.success",
                        "id" to id,
                        "name" to name,
                        "property" to property,
                        "value" to value,
                    )
                }

                fun sendSuccessIdChanged(
                    executor: CommandExecutor,
                    oldId: UShort,
                    newId: UShort,
                    name: String,
                    property: String,
                    value: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.block.edit.success_id_changed",
                        "oldId" to oldId,
                        "newId" to newId,
                        "name" to name,
                        "property" to property,
                        "value" to value,
                    )
                }
            }

            object Add {
                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.block.add.usage")
                }

                fun sendSuccess(
                    executor: CommandExecutor,
                    id: UShort,
                    name: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.block.add.success",
                        "id" to id,
                        "name" to name,
                    )
                }
            }

            object Delete {
                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.block.delete.usage")
                }

                fun sendSuccess(
                    executor: CommandExecutor,
                    id: UShort,
                    name: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.block.delete.success",
                        "id" to id,
                        "name" to name,
                    )
                }
            }

            object List {
                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.block.list.usage")
                }

                fun sendHeaderGlobal(executor: CommandExecutor) {
                    sendMessage(executor, "commands.block.list.header_global")
                }

                fun sendHeaderLevel(executor: CommandExecutor, level: String) {
                    sendMessage(
                        executor,
                        "commands.block.list.header_level",
                        "level" to level,
                    )
                }

                fun sendHeaderAll(executor: CommandExecutor) {
                    sendMessage(executor, "commands.block.list.header_all")
                }

                fun sendFormat(
                    executor: CommandExecutor,
                    id: UShort,
                    name: String,
                ) {
                    sendMessage(
                        executor,
                        "commands.block.list.format",
                        "id" to id,
                        "name" to name,
                    )
                }

                fun sendNoBlocks(executor: CommandExecutor) {
                    sendMessage(executor, "commands.block.list.no_blocks")
                }
            }

            object Reload {
                fun sendUsage(executor: CommandExecutor) {
                    sendMessage(executor, "commands.block.reload.usage")
                }

                fun sendSuccess(executor: CommandExecutor) {
                    sendMessage(executor, "commands.block.reload.success")
                }

                fun sendFailed(executor: CommandExecutor, error: String) {
                    sendMessage(
                        executor,
                        "commands.block.reload.failed",
                        "error" to error,
                    )
                }
            }

            object Subcommands {
                fun sendAvailable(executor: CommandExecutor) {
                    sendMessage(
                        executor,
                        "commands.block.subcommands.available",
                    )
                }
            }
        }

        object Teleport {
            fun sendInvalidPosition(executor: CommandExecutor) {
                sendMessage(executor, "commands.teleport.invalid_position")
            }

            fun sendSuccessSelfToPlayer(executor: CommandExecutor, player: String) {
                sendMessage(
                    executor,
                    "commands.teleport.success.self_player",
                    "player" to player,
                )
            }

            fun sendSuccessPlayerToPlayer(
                executor: CommandExecutor,
                player: String,
                target: String,
            ) {
                sendMessage(
                    executor,
                    "commands.teleport.success.player_to_player",
                    "player" to player,
                    "target" to target,
                )
            }

            fun sendSuccessSelfToLocation(
                executor: CommandExecutor,
                x: Float,
                y: Float,
                z: Float,
            ) {
                sendMessage(
                    executor,
                    "commands.teleport.success.self_location",
                    "x" to x,
                    "y" to y,
                    "z" to z,
                )
            }

            fun sendSuccessPlayerToLocation(
                executor: CommandExecutor,
                player: String,
                x: Float,
                y: Float,
                z: Float,
            ) {
                sendMessage(
                    executor,
                    "commands.teleport.success.player_to_location",
                    "player" to player,
                    "x" to x,
                    "y" to y,
                    "z" to z,
                )
            }
        }
    }
}
