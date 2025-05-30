package org.dandelion.classic.server.config.manager

import org.dandelion.classic.server.config.model.BansConfig
import org.dandelion.classic.server.config.model.OpsConfig
import org.dandelion.classic.server.config.model.PermissionsConfig
import org.dandelion.classic.server.config.model.ServerConfig

object ServerConfigManager {
    val serverConfig = ServerConfig()
    val opsConfig = OpsConfig()
    val bansConfig = BansConfig()
    val permissionsConfig = PermissionsConfig()

    fun loadAll() {
        serverConfig.load()
        opsConfig.load()
        bansConfig.load()
        permissionsConfig.load()
    }
    fun reloadAll() {
        serverConfig.reload()
        opsConfig.reload()
        bansConfig.reload()
        permissionsConfig.reload()
    }
    fun saveAll() {
        serverConfig.save()
        opsConfig.save()
        bansConfig.save()
        permissionsConfig.save()
    }
}
