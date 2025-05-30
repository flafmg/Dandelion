package org.dandelion.classic.server.config.model

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStream

class ServerConfig(private val configFileName: String = "config.yml") {
    private var config: ServerConfigData? = null

    fun load() {
        val configFile = File(configFileName)
        if (!configFile.exists()) {
            val resource: InputStream? = javaClass.classLoader.getResourceAsStream(configFileName)
            if (resource != null) {
                config = try {
                    val yaml = Yaml(Constructor(ServerConfigData::class.java))
                    yaml.load(resource) ?: ServerConfigData()
                } catch (_: Exception) {
                    ServerConfigData()
                }
                save(config!!)
            } else {
                save(ServerConfigData())
                config = ServerConfigData()
            }
        } else {
            config = try {
                FileInputStream(configFile).use { inputStream ->
                    val yaml = Yaml(Constructor(ServerConfigData::class.java))
                    yaml.load(inputStream) ?: ServerConfigData()
                }
            } catch (_: Exception) {
                ServerConfigData()
            }
        }
    }

    fun reload() = load()

    fun save(cfg: ServerConfigData = get()): Boolean {
        return try {
            val option = DumperOptions().apply {
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                isPrettyFlow = true
            }
            val yaml = Yaml(option)
            FileWriter(configFileName).use { writer ->
                yaml.dump(cfg, writer)
            }
            true
        } catch (_: Exception) { false }
    }

    fun get(): ServerConfigData {
        if (config == null) load()
        return config!!
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
