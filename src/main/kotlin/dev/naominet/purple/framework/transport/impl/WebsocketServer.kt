package dev.naominet.purple.framework.transport.impl

import dev.naominet.purple.framework.event.Event
import dev.naominet.purple.framework.transport.ITransport
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes

class WebsocketServer(val port: Int, val path: String) : ITransport {
    override val onReceiveData = Event<ByteArray>()
    private val server = embeddedServer(Netty, port = port) {
        install(WebSockets)
        routing {
            webSocket(path) {
                for (frame in incoming) {
                    if (frame is Frame.Binary) {
                        onReceiveData.emit(frame.readBytes())
                    }
                }
            }
        }
    }

    override fun connect() {
        try {
            server.start(wait = false)
        } catch (exception: Exception) {
            throw IllegalStateException("Failed to start WebSocket server on port $port with path $path", exception)
        }
    }

    override fun send(data: ByteArray) {
        onReceiveData.emit(data)
    }
}
