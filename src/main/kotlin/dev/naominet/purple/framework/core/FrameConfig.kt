package dev.naominet.purple.framework.core

import dev.naominet.purple.framework.config.IConfig
import dev.naominet.purple.framework.embedded.EmbeddedPlugin

data class FrameConfig(
    override val configId: String = "purple-framework-config",
    /**
     * Supported protocol: WebsocketServer, WebsocketClient
     **/
    val protocol: String = "WebsocketClient",
    /**
     * Remote OneBot address. Used by client transports and by HTTPServer mode when calling HTTP API.
     **/
    val address: String = "127.0.0.1",
    /**
     * Remote OneBot port. Used by client transports and by HTTPServer mode when calling HTTP API.
     **/
    val port: Int = 5700,
    /**
     * Transport path. For HTTPServer mode this is the event report path exposed by this framework.
     **/
    val path: String = "/",
    val encoding: String = "UTF-8",
    val debugOutput: Boolean = false,
    val embeddedPlugin: Boolean = true,
) : IConfig
