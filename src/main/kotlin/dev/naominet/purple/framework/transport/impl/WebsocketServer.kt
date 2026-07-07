package dev.naominet.purple.framework.transport.impl

import dev.naominet.purple.framework.event.Event
import dev.naominet.purple.framework.transport.ITransport
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList

class WebsocketServer(val port: Int, val path: String) : ITransport {
    override val onReceiveData = Event<ByteArray>()
    private val sessions = CopyOnWriteArrayList<DefaultWebSocketServerSession>()
    private val server = embeddedServer(Netty, port = port) {
        install(WebSockets)
        routing {
            webSocket(path) {
                sessions.add(this)
                try {
                    for (frame in incoming) {
                        when(frame){
                            is Frame.Text -> onReceiveData.emit(frame.readText().toByteArray())
                            is Frame.Binary -> onReceiveData.emit(frame.readBytes())
                            else -> Unit
                        }
                    }
                } finally {
                    sessions.remove(this)
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
        if(sessions.isEmpty()){
            throw IllegalStateException("No WebSocket client is connected to ws://0.0.0.0:$port$path")
        }

        runBlocking {
            sessions.forEach { session ->
                session.send(data)
            }
        }
    }
}
