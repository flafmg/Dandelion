package org.dandelion.classic.server

import org.dandelion.classic.util.Utils
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.dandelion.classic.util.YamlConfig
import java.io.File

object ServerConfig {
    private lateinit var config: YamlConfig

    const val dandelionVersion = "0.1.3-Dev"

    fun reload() {
        val configFile = File("server.yml")
        if (!configFile.exists()) {
            Utils.copyResourceTo("server.yml", "server.yml")
        }
        config = YamlConfig.load(configFile)
    }
    fun save() {
        config.save()
    }

    val serverSoftware: String
        get() = "Dandelion $dandelionVersion"

    val salt: String
        get() = Salt.get()


    var isPublic: Boolean
        get() = config.getBoolean("server.public", true)
        set(value) {
            config.set("server.public", value)
        }

    var port: Int
        get() = config.getInt("server.port", 25566)
        set(value) {
            config.set("server.port", value)
        }

    var name: String
        get() = config.getString("server.name", "a classic minecraft server")
        set(value) {
            config.set("server.name", value)
        }

    var motd: String
        get() = config.getString("server.motd", "powered by dandelion")
        set(value) {
            config.set("server.motd", value)
        }

    var maxPlayers: Int
        get() = config.getInt("server.max-players", 255)
        set(value) {
            config.set("server.max-players", value)
        }

    var verifyUsers: Boolean
        get() = config.getBoolean("server.verify-users", true)
        set(value) {
            config.set("server.verify-users", value)
        }

    var debugMode: Boolean
        get() = config.getBoolean("server.debug-mode", false)
        set(value) {
            config.set("server.debug-mode", value)
        }

    var defaultLevel: String
        get() = config.getString("level.default-level", "main")
        set(value) {
            config.set("level.default-level", value)
        }
    var levelFormat: String
        get() = config.getString("level.format", "dlvl")
        set(value){
            config.set("level.format", value)
        }
    var autoSaveInterval: Duration
        get() = config.getInt("level.auto-save-interval", 120).seconds
        set(value) {
            config.set("level.auto-save-interval", value.inWholeSeconds)
        }

    var heartbeatEnabled: Boolean
        get() = config.getBoolean("heartbeat.enabled", true)
        set(value) {
            config.set("heartbeat.enabled", value)
        }

    var heartbeatUrl: String
        get() = config.getString("heartbeat.url", "http://www.classicube.net/server/heartbeat/")
        set(value) {
            config.set("heartbeat.url", value)
        }

    var heartbeatData: String
        get() =
            config.getString(
                "heartbeat.data",
                "name={server-name}&port={server-port}&users={players-online}&max={players-max}&public={server-public}&salt={server-salt}&software={server-software}"
            )
        set(value) {
            config.set("heartbeat.data", value)
        }

    var heartbeatInterval: Duration
        get() = config.getInt("heartbeat.interval", 45).seconds
        set(value) {
            config.set("heartbeat.interval", value.inWholeSeconds)
        }
}