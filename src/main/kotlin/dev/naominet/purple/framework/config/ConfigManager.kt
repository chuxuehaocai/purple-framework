package dev.naominet.purple.framework.config

import dev.naominet.purple.framework.logger.Logger
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

object ConfigManager {
    @PublishedApi
    internal val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    val configDirectory = File("configs")
    val configs = mutableListOf<IConfig>()

    inline fun <reified T : IConfig> register(dataConfig: T): T {
        if (!configDirectory.exists()) {
            configDirectory.mkdirs()
        }

        val configFile = File(configDirectory, "${dataConfig.configId}.json")
        val defaultConfigObject = json.encodeToJsonElement(dataConfig).jsonObject.toMutableMap()
        defaultConfigObject.remove("configId")
        if (!configFile.exists()) {
            configFile.writeText(json.encodeToString(JsonObject(defaultConfigObject)))
            configs.add(dataConfig)
            Logger.log("Config file does not exist: ${dataConfig.configId}, created a new one.", this.javaClass)
            return dataConfig
        }

        val configObject = json.parseToJsonElement(configFile.readText()).jsonObject.toMutableMap()
        var changed = configObject.remove("configId") != null
        for (entry in defaultConfigObject) {
            if (!configObject.containsKey(entry.key)) {
                configObject[entry.key] = entry.value
                changed = true
                Logger.log("Config ${dataConfig.configId} is missing entry: ${entry.key}, filled with default value.", this.javaClass)
            }
        }

        if (changed) {
            configFile.writeText(json.encodeToString(JsonObject(configObject)))
            Logger.log("Config ${dataConfig.configId} has been completed and saved.", this.javaClass)
        }

        val config = json.decodeFromJsonElement<T>(JsonObject(configObject))
        configs.add(config)
        Logger.log("Loaded config file: $configFile", this.javaClass)
        return config
    }
}
