package dev.naominet.purple.framework.core

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.to
import dev.naominet.purple.framework.beans.TextMessageBean
import dev.naominet.purple.framework.config.ConfigManager
import dev.naominet.purple.framework.core.plugin.PluginManager
import dev.naominet.purple.framework.event.EventManager
import dev.naominet.purple.framework.logger.Logger
import dev.naominet.purple.framework.transport.ITransport
import dev.naominet.purple.framework.transport.impl.WebsocketClient
import dev.naominet.purple.framework.transport.impl.WebsocketServer
import java.nio.charset.Charset
import java.util.concurrent.CountDownLatch

object PurpleFramework {
    val version = "alpha 0.0.1"
    lateinit var configuration: FrameConfig
    lateinit var transport: ITransport

    fun start(args : Array<String>) {
        Logger.log("Starting Purple Framework...", this.javaClass)
        Logger.log("Version: $version", this.javaClass)
        Logger.log("Loading framework config...", this.javaClass)
        configuration = ConfigManager.register(FrameConfig())

        if(checkConfiguration()) {
            Logger.log("Trying to load plugins...", this.javaClass)
            PluginManager.init()
            Logger.log("Trying to connect to the target by using ${configuration.protocol} protocol...", this.javaClass)
            if(configuration.protocol == "WebsocketClient"){
                transport = WebsocketClient(configuration.address, configuration.port, configuration.path, configuration.connectToken)

            }
            if(configuration.protocol == "WebsocketServer"){
                transport = WebsocketServer(configuration.port, configuration.path)
            }
            transport.connect()

            originEventListener()
            CountDownLatch(1).await()
        }
    }

    fun originEventListener() {
        transport.onReceiveData.listen { data ->
            run {
                Logger.debug("Received data: ${data.toString(Charset.forName(configuration.encoding))}", this.javaClass)
                val json = data.toString(Charset.forName(configuration.encoding))
                val bean = JSON.parseObject(json).to<TextMessageBean>()

                when (bean.message_type){
                    "group" -> {
                        EventManager.groupMessageEvent.emit(bean)
                    }
                    "private" -> {
                        EventManager.privateMessageEvent.emit(bean)
                    }
                }
            }
        }
    }

    fun checkConfiguration(): Boolean {
        Logger.log("Checking configuration...", this.javaClass)

        val supportedProtocols = listOf("WebsocketClient", "WebsocketServer")
        if (configuration.protocol !in supportedProtocols) {
            throw IllegalArgumentException("Protocol is illegal. Support: ${supportedProtocols.joinToString(", ")}.")
        }

        if (configuration.protocol.endsWith("Client") && configuration.address.isBlank()) {
            throw IllegalArgumentException("Address is illegal. Client mode requires a non-empty address.")
        }

        if (configuration.protocol.endsWith("Server") && configuration.port !in 1..65535) {
            throw IllegalArgumentException("Port is illegal. Server mode requires a port between 1 and 65535.")
        }

        if (!configuration.path.startsWith("/")) {
            throw IllegalArgumentException("Path is illegal. Path must start with /.")
        }

        if (!Charset.isSupported(configuration.encoding)) {
            throw IllegalArgumentException("Encoding is illegal. Unsupported encoding: ${configuration.encoding}.")
        }

        Logger.log("Configuration is legal.", this.javaClass)
        return true
    }
}