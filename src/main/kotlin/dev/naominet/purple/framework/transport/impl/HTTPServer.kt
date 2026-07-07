package dev.naominet.purple.framework.transport.impl

import dev.naominet.purple.framework.event.Event
import dev.naominet.purple.framework.transport.ITransport
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receive
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

class HTTPServer(val port: Int, val path: String) : ITransport {
    override val onReceiveData = Event<ByteArray>()
    private val server = embeddedServer(Netty, port = port) {
        routing {
            post(path) {
                onReceiveData.emit(call.receive<ByteArray>())
                call.respondBytes(ByteArray(0), status = HttpStatusCode.OK)
            }
        }
    }

    override fun connect() {
        try {
            server.start(wait = false)
        } catch (exception: Exception) {
            throw IllegalStateException("Failed to start HTTP server on port $port with path $path", exception)
        }
    }

    override fun send(data: ByteArray) {
        onReceiveData.emit(data)
    }
}
