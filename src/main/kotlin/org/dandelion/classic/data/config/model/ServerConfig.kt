package org.dandelion.classic.data.config.model

import org.dandelion.classic.data.config.stream.YamlStream

class ServerConfig(private val configFileName: String = "config.yml") {
    private val yaml = YamlStream(configFileName)

    fun load() {
        yaml.load()
    }

    fun reload() = load()

    fun save(): Boolean {
        return try {
            yaml.save()
            true
        } catch (_: Exception) { false }
    }

    fun get(): ServerConfigData {
        val server = yaml.getSection("serverSettings")
        val heartbeat = yaml.getSection("heartbeatSettings")
        return ServerConfigData(
            serverSettings = ServerConfigData.ServerSettings(
                name = server.getString("name", "Dandelion") ?: "Dandelion",
                motd = server.getString("motd", "A minecraft classic server.") ?: "A minecraft classic server.",
                port = server.getInt("port", 25565) ?: 25565,
                maxPlayers = server.getInt("maxPlayers", 12) ?: 12,
                public = server.getBoolean("public", true) ?: true,
                defaultLevel = server.getString("defaultLevel", "default") ?: "default"
            ),
            heartbeatSettings = ServerConfigData.HeartbeatSettings(
                enabled = heartbeat.getBoolean("enabled", true) ?: true,
                interval = heartbeat.getInt("interval", 60) ?: 60,
                url = heartbeat.getString("url", "http://www.classicube.net/server/heartbeat") ?: "http://www.classicube.net/server/heartbeat"
            )
        )
    }

    fun set(data: ServerConfigData) {
        yaml.set("serverSettings.name", data.serverSettings.name)
        yaml.set("serverSettings.motd", data.serverSettings.motd)
        yaml.set("serverSettings.port", data.serverSettings.port)
        yaml.set("serverSettings.maxPlayers", data.serverSettings.maxPlayers)
        yaml.set("serverSettings.public", data.serverSettings.public)
        yaml.set("serverSettings.defaultLevel", data.serverSettings.defaultLevel)
        yaml.set("heartbeatSettings.enabled", data.heartbeatSettings.enabled)
        yaml.set("heartbeatSettings.interval", data.heartbeatSettings.interval)
        yaml.set("heartbeatSettings.url", data.heartbeatSettings.url)
        save()
    }

    data class ServerConfigData(
        var serverSettings: ServerSettings = ServerSettings(),
        var heartbeatSettings: HeartbeatSettings = HeartbeatSettings(),
    ) {
        data class ServerSettings(
            var name: String = "Dandelion",
            var motd: String = "A minecraft classic server.",
            var port: Int = 25565,
            var maxPlayers: Int = 12,
            var public: Boolean = true,
            var defaultLevel: String = "default",
        )
        data class HeartbeatSettings(
            var enabled: Boolean = true,
            var interval: Int = 60,
            var url: String = "http://www.classicube.net/server/heartbeat",
        )
    }
}
