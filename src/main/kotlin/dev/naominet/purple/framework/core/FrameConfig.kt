package dev.naominet.purple.framework.core

import dev.naominet.purple.framework.config.IConfig
import dev.naominet.purple.framework.embedded.EmbeddedPlugin

data class FrameConfig(
    override val configId: String = "purple-framework-config",
    /**
     * Supported protocol:HTTPClient, HTTPServer, WebsocketServer, WebsocketClient
     **/
    val protocol: String = "HTTPServer",
    /**
     * Only needed when using client mode
     **/
    val address: String = "127.0.0.1",
    val port: Int = 8080,
    val path: String = "/",
    val encoding: String = "UTF-8",
    val debugOutput: Boolean = false,
    val embeddedPlugin: Boolean = true,
) : IConfig
