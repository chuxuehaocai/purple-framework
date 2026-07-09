package dev.naominet.purple.framework.config

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter
import com.alibaba.fastjson2.to
import dev.naominet.purple.framework.logger.Logger
import java.io.File

object ConfigManager {
    val configDirectory = File("configs")
    val configs = mutableListOf<IConfig>()

    inline fun <reified T : IConfig> register(dataConfig: T): T {
        if (!configDirectory.exists()) {
            configDirectory.mkdirs()
        }

        val configFile = File(configDirectory, "${dataConfig.configId}.json")
        val defaultConfigObject = JSON.parseObject(JSON.toJSONString(dataConfig))
        defaultConfigObject.remove("configId")
        if (!configFile.exists()) {
            configFile.writeText(JSON.toJSONString(defaultConfigObject, JSONWriter.Feature.PrettyFormat))
            configs.add(dataConfig)
            Logger.log("Config file does not exist: ${dataConfig.configId}, created a new one.", this.javaClass)
            return dataConfig
        }

        val configObject = JSON.parseObject(configFile.readText())
        var changed = configObject.remove("configId") != null
        for (entry in defaultConfigObject) {
            if (!configObject.containsKey(entry.key)) {
                configObject[entry.key] = entry.value
                changed = true
                Logger.log("Config ${dataConfig.configId} is missing entry: ${entry.key}, filled with default value.", this.javaClass)
            }
        }

        if (changed) {
            configFile.writeText(JSON.toJSONString(configObject, JSONWriter.Feature.PrettyFormat))
            Logger.log("Config ${dataConfig.configId} has been completed and saved.", this.javaClass)
        }

        val config = configObject.to<T>()
        configs.add(config)
        Logger.log("Loaded config file: $configFile", this.javaClass)
        return config
    }
}
